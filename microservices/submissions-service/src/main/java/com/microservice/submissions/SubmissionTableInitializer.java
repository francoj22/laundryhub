package com.microservice.submissions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Configuration
public class SubmissionTableInitializer {

    @Bean
    CommandLineRunner initSubmissionsTable(DynamoDbEnhancedClient enhancedClient,
                                           @Value("${aws.dynamodb.submissions-table:submissions}") String tableName,
                                           @Value("${aws.dynamodb.auto-create-table:true}") boolean autoCreateTable) {
        return args -> {
            if (!autoCreateTable) {
                return;
            }

            DynamoDbTable<Submission> table = enhancedClient.table(tableName, TableSchema.fromBean(Submission.class));
            try {
                table.describeTable();
            } catch (ResourceNotFoundException ex) {
                table.createTable();
            }
        };
    }
}
