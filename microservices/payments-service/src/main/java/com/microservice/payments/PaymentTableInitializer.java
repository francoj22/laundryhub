package com.microservice.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Configuration
public class PaymentTableInitializer {

    @Bean
    CommandLineRunner initPaymentsTable(DynamoDbEnhancedClient enhancedClient,
                                        @Value("${aws.dynamodb.payments-table:payments}") String tableName,
                                        @Value("${aws.dynamodb.auto-create-table:true}") boolean autoCreateTable) {
        return args -> {
            if (!autoCreateTable) {
                return;
            }

            DynamoDbTable<Payment> table = enhancedClient.table(tableName, TableSchema.fromBean(Payment.class));
            try {
                table.describeTable();
            } catch (ResourceNotFoundException ex) {
                table.createTable();
            }
        };
    }
}
