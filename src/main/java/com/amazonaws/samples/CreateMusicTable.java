package com.amazonaws.samples;

import java.util.Arrays;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class CreateMusicTable {

    public static void main(String[] args) throws Exception {

        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new ProfileCredentialsProvider("default"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        String tableName = "music";

        try {
            System.out.println("Attempting to create music table; please wait...");

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)

                    .withKeySchema(
                            new KeySchemaElement("artist", KeyType.HASH), // partition key
                            new KeySchemaElement("songkey", KeyType.RANGE)) // sort key

                    .withAttributeDefinitions(
                            new AttributeDefinition("title",  ScalarAttributeType.S),
                            new AttributeDefinition("album",  ScalarAttributeType.S),
                            new AttributeDefinition("songkey",  ScalarAttributeType.S),
                            new AttributeDefinition("artist", ScalarAttributeType.S),
                            new AttributeDefinition("year",   ScalarAttributeType.S))

                    // GSI 1: title-artist-index
                    .withGlobalSecondaryIndexes(
                            new GlobalSecondaryIndex()
                                    .withIndexName("title-artist-index")
                                    .withKeySchema(
                                            new KeySchemaElement("title", KeyType.HASH),
                                            new KeySchemaElement("artist",  KeyType.RANGE))
                                    .withProjection(new Projection()
                                            .withProjectionType(ProjectionType.ALL)),

                            // GSI 2: artist-album-index
                            new GlobalSecondaryIndex()
                                    .withIndexName("album-artist-index")
                                    .withKeySchema(
                                            new KeySchemaElement("album", KeyType.HASH),
                                            new KeySchemaElement("artist",  KeyType.RANGE))
                                    .withProjection(new Projection()
                                            .withProjectionType(ProjectionType.ALL)))

                    // LSI: title-year-index
                    .withLocalSecondaryIndexes(
                            new LocalSecondaryIndex()
                                    .withIndexName("artist-year-index")
                                    .withKeySchema(
                                            new KeySchemaElement("artist", KeyType.HASH),
                                            new KeySchemaElement("year",  KeyType.RANGE))
                                    .withProjection(new Projection()
                                            .withProjectionType(ProjectionType.ALL)))

                    .withBillingMode(BillingMode.PAY_PER_REQUEST);

            // Create the table
            Table table = dynamoDB.createTable(request);
            table.waitForActive();
            System.out.println("Success. Table status: " + table.getDescription().getTableStatus());

        } catch (Exception e) {
            System.err.println("Unable to create table: ");
            System.err.println(e.getMessage());
        }
    }
}