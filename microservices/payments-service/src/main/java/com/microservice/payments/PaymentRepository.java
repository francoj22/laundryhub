package com.microservice.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;

@Repository
public class PaymentRepository {

	private final DynamoDbTable<Payment> table;

	public PaymentRepository(DynamoDbEnhancedClient enhancedClient,
							 @Value("${aws.dynamodb.payments-table:payments}") String tableName) {
		this.table = enhancedClient.table(tableName, TableSchema.fromBean(Payment.class));
	}

	public List<Payment> findAll() {
		return table.scan().items().stream().toList();
	}

	public Payment save(Payment payment) {
		table.putItem(payment);
		return payment;
	}
}
