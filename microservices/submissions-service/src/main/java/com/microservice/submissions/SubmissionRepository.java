package com.microservice.submissions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;

@Repository
public class SubmissionRepository {

	private final DynamoDbTable<Submission> table;

	public SubmissionRepository(DynamoDbEnhancedClient enhancedClient,
								@Value("${aws.dynamodb.submissions-table:submissions}") String tableName) {
		this.table = enhancedClient.table(tableName, TableSchema.fromBean(Submission.class));
	}

	public List<Submission> findAll() {
		return table.scan().items().stream().toList();
	}

	public Submission save(Submission submission) {
		table.putItem(submission);
		return submission;
	}
}
