package com.microservice.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ApiGatewayController {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayController.class);

    private final RestClient restClient;

    @Value("${services.submissions.url}")
    private String submissionsServiceUrl;

    @Value("${services.payments.url}")
    private String paymentsServiceUrl;

    public ApiGatewayController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/submissions")
    public ResponseEntity<String> listSubmissions(HttpServletRequest request) {
        return forwardGet(submissionsServiceUrl + "/submissions", request);
    }

    @PostMapping("/submissions")
    public ResponseEntity<String> createSubmission(@RequestBody String body, HttpServletRequest request) {
        return forwardPost(submissionsServiceUrl + "/submissions", body, request, null, null, true);
    }

    @GetMapping("/payments")
    public ResponseEntity<String> listPayments(HttpServletRequest request) {
        return forwardGet(paymentsServiceUrl + "/payments", request);
    }

    @PostMapping("/payments")
    public ResponseEntity<String> createPayment(@RequestBody String body,
                                                @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                HttpServletRequest request) {
        return forwardPost(paymentsServiceUrl + "/payments", body, request, idempotencyKey, null, true);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<String> getPayment(@PathVariable Long paymentId, HttpServletRequest request) {
        return forwardGet(paymentsServiceUrl + "/payments/" + paymentId, request);
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<String> confirmPayment(@PathVariable Long paymentId, HttpServletRequest request) {
        return forwardPost(paymentsServiceUrl + "/payments/" + paymentId + "/confirm", "", request, null, null, true);
    }

    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<String> cancelPayment(@PathVariable Long paymentId, HttpServletRequest request) {
        return forwardPost(paymentsServiceUrl + "/payments/" + paymentId + "/cancel", "", request, null, null, true);
    }

    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<String> refundPayment(@PathVariable Long paymentId,
                                                @RequestBody String body,
                                                HttpServletRequest request) {
        return forwardPost(paymentsServiceUrl + "/payments/" + paymentId + "/refund", body, request, null, null, true);
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<String> paymentWebhook(@RequestBody String body,
                                                 @RequestHeader("X-Webhook-Signature") String signature,
                                                 HttpServletRequest request) {
        return forwardPost(paymentsServiceUrl + "/payments/webhook", body, request, null, signature, false);
    }

    private ResponseEntity<String> forwardGet(String url, HttpServletRequest request) {
        log.debug("Forwarding GET {} for userId={} role={}", url, request.getAttribute("gatewayUserId"), request.getAttribute("gatewayUserRole"));
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .header("X-User-Id", String.valueOf(request.getAttribute("gatewayUserId")))
                    .header("X-User-Role", String.valueOf(request.getAttribute("gatewayUserRole")))
                    .retrieve()
                    .toEntity(String.class);
            log.debug("GET {} completed with status {}", url, response.getStatusCode());
            return relayResponse(response);
        } catch (Exception ex) {
            log.error("GET {} failed: {}", url, ex.getMessage(), ex);
            throw ex;
        }
    }

    private ResponseEntity<String> forwardPost(String url,
                                               String body,
                                               HttpServletRequest request) {
        return forwardPost(url, body, request, null, null, true);
    }

    private ResponseEntity<String> forwardPost(String url,
                                               String body,
                                               HttpServletRequest request,
                                               String idempotencyKey,
                                               String webhookSignature,
                                               boolean includeUserHeaders) {
        log.debug("Forwarding POST {} for userId={} role={} bodyLength={}", url, request.getAttribute("gatewayUserId"), request.getAttribute("gatewayUserRole"), body == null ? 0 : body.length());
        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);
            if (includeUserHeaders) {
                requestSpec = requestSpec
                        .header("X-User-Id", String.valueOf(request.getAttribute("gatewayUserId")))
                        .header("X-User-Role", String.valueOf(request.getAttribute("gatewayUserRole")));
            }
            if (idempotencyKey != null) {
                requestSpec = requestSpec.header("Idempotency-Key", idempotencyKey);
            }
            if (webhookSignature != null) {
                requestSpec = requestSpec.header("X-Webhook-Signature", webhookSignature);
            }

            ResponseEntity<String> response = requestSpec
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
            log.debug("POST {} completed with status {}", url, response.getStatusCode());
            return relayResponse(response);
        } catch (Exception ex) {
            log.error("POST {} failed: {}", url, ex.getMessage(), ex);
            throw ex;
        }
    }

    private ResponseEntity<String> relayResponse(ResponseEntity<String> upstreamResponse) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(upstreamResponse.getStatusCode());
        if (upstreamResponse.getHeaders().getContentType() != null) {
            builder.contentType(upstreamResponse.getHeaders().getContentType());
        }
            // Removed unnecessary Accept header relay
        return builder.body(upstreamResponse.getBody());
    }
}
