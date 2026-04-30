package com.amazonaws.samples;

import java.util.Arrays;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class CreateLoginTable {

    static final String STUDENT_ID   = "s3123456";
    static final String STUDENT_NAME = "JaneDoe";

    static final String[] PASSWORDS = {"012345", "123456", "234567", "345678", "456789",
            "567890", "678901", "789012", "890123", "901234"
    };

    public static void main(String[] args) throws Exception {

        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new ProfileCredentialsProvider("default"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        String tableName = "login";

        try {
            System.out.println("Attempting to create login table; please wait...");

            Table table = dynamoDB.createTable(tableName,
                    Arrays.asList(new KeySchemaElement("email", KeyType.HASH)),   // partition key
                    Arrays.asList(new AttributeDefinition("email", ScalarAttributeType.S)),
                    new CreateTableRequest()
                            .withTableName(tableName)
                            .withBillingMode(BillingMode.PAY_PER_REQUEST)
                            .withKeySchema(
                                    new KeySchemaElement("email", KeyType.HASH))
                            .withAttributeDefinitions(
                                    new AttributeDefinition("email", ScalarAttributeType.S)).getProvisionedThroughput()
            );

            table.waitForActive();
            System.out.println("Success. Table status: " + table.getDescription().getTableStatus());

        } catch (Exception e) {
            System.err.println("Unable to create table: ");
            System.err.println(e.getMessage());
        }


        try {
            Table table = dynamoDB.getTable(tableName);
            System.out.println("Inserting 10 users...");

            for (int i = 0; i < 10; i++) {
                table.putItem(new Item()
                        .withPrimaryKey("email", STUDENT_ID + i + "@student.rmit.edu.au")
                        .withString("user_name", STUDENT_NAME + i)
                        .withString("password",  PASSWORDS[i]));

                System.out.println("  Inserted: " + STUDENT_ID + i + "@student.rmit.edu.au");
            }

            System.out.println("Success. 10 users inserted.");

        } catch (Exception e) {
            System.err.println("Unable to insert users: ");
            System.err.println(e.getMessage());
        }


        try {
            Table table = dynamoDB.getTable(tableName);
            System.out.println("\nVerifying login table contents:");

            for (Item item : table.scan()) {
                System.out.println("  " + item.toJSONPretty());
            }

        } catch (Exception e) {
            System.err.println("Unable to scan table: ");
            System.err.println(e.getMessage());
        }
    }
}