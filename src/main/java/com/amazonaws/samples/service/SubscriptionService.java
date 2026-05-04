package com.amazonaws.samples.service;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionService {

    private final Table subscriptionTable;

    public SubscriptionService() {
        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new ProfileCredentialsProvider("default"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        this.subscriptionTable = dynamoDB.getTable("subscriptions");
    }

    // Add a song to user's subscription list
    public SubscriptionResult addSubscription(
            String email,
            String songkey,
            String title,
            String artist,
            String album,
            String year,
            String imageUrl
    ) {
        subscriptionTable.putItem(new Item()
                .withPrimaryKey("email", email, "songkey", songkey)
                .withString("title", title)
                .withString("artist", artist)
                .withString("album", album)
                .withString("year", year)
                .withString("image_url", imageUrl));

        return new SubscriptionResult(true, "Subscription added successfully");
    }

    // Get all subscriptions for one user
    public List<Item> getSubscriptions(String email) {
        List<Item> results = new ArrayList<>();

        ItemCollection<QueryOutcome> items = subscriptionTable.query("email", email);

        for (Item item : items) {
            results.add(item);
        }

        return results;
    }

    // Remove one subscribed song
    public SubscriptionResult removeSubscription(String email, String songkey) {
        subscriptionTable.deleteItem("email", email, "songkey", songkey);

        return new SubscriptionResult(true, "Subscription removed successfully");
    }

    public static class SubscriptionResult {
        private final boolean success;
        private final String message;

        public SubscriptionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}