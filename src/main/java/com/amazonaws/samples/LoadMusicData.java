package com.amazonaws.samples;

import java.io.File;
import java.util.Iterator;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoadMusicData {

    public static void main(String[] args) throws Exception {

        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new ProfileCredentialsProvider("default"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        Table table = dynamoDB.getTable("music");

        JsonParser parser = new JsonFactory()
                .createParser(new File("2026a2_songs.json"));

        JsonNode rootNode = new ObjectMapper().readTree(parser);

        JsonNode songsNode = rootNode.path("songs");
        Iterator<JsonNode> iter = songsNode.iterator();

        ObjectNode currentNode;
        int successCount = 0;

        while (iter.hasNext()) {
            currentNode = (ObjectNode) iter.next();

            String title    = currentNode.path("title").asText();
            String album    = currentNode.path("album").asText();
            String artist   = currentNode.path("artist").asText();
            String year     = currentNode.path("year").asText();
            String imageUrl = currentNode.path("img_url").asText();
            String songKey = title + "#" + album + "#" + year;

            try {
                // title -> partition key
                // album -> sort key
                table.putItem(new Item()
                        .withPrimaryKey("artist", artist, "songKey", songKey)
                        .withString("title", title)
                        .withString("album", album)
                        .withString("year", year)
                        .withString("image_url", imageUrl));

                successCount++;
                System.out.println("PutItem succeeded: " + artist + " | " + songKey);

            } catch (Exception e) {
                System.err.println("Unable to add song: " + artist + " | " + songKey);
                System.err.println(e.getMessage());
                break;
            }
        }

        parser.close();
        System.out.println("\nDone. Successfully loaded " + successCount + " songs.");
    }
}