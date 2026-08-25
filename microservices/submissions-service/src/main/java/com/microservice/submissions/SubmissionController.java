package com.microservice.submissions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionRepository repository;

    public SubmissionController(SubmissionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Submission> listSubmissions() {
        return repository.findAll();
    }

    @PostMapping
    public Submission createSubmission(@RequestBody Submission submission,
                                       @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (submission.getId() == null || submission.getId().isBlank()) {
            submission.setId(UUID.randomUUID().toString());
        }
        submission.setUserId(userId == null ? "anonymous" : userId);
        if (submission.getStatus() == null || submission.getStatus().isBlank()) {
            submission.setStatus("CREATED");
        }
        return repository.save(submission);
    }
}
