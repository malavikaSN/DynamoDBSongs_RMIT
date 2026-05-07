package com.amazonaws.samples;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class CreateSubscriptionTable {

    public static void main(String[] args) throws Exception {

        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        String tableName = "subscriptions";

        try {
            System.out.println("Attempting to create subscriptions table; please wait...");

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)

                    .withKeySchema(
                            new KeySchemaElement("email",   KeyType.HASH), // partition key
                            new KeySchemaElement("songkey", KeyType.RANGE)) // sort key


                    .withAttributeDefinitions(
                            new AttributeDefinition("email",   ScalarAttributeType.S),
                            new AttributeDefinition("songkey", ScalarAttributeType.S))


                    .withGlobalSecondaryIndexes(
                            new GlobalSecondaryIndex()
                                    .withIndexName("song-subscribers-index")
                                    .withKeySchema(
                                            new KeySchemaElement("songkey", KeyType.HASH),
                                            new KeySchemaElement("email",   KeyType.RANGE))
                                    .withProjection(new Projection()
                                            .withProjectionType(ProjectionType.ALL)))

                    .withBillingMode(BillingMode.PAY_PER_REQUEST);

            Table table = dynamoDB.createTable(request);
            table.waitForActive();
            System.out.println("Success. Table status: " + table.getDescription().getTableStatus());

        } catch (Exception e) {
            System.err.println("Unable to create subscriptions table: ");
            System.err.println(e.getMessage());
        }
    }
}