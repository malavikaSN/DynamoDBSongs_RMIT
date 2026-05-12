package com.amazonaws.samples;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

import java.util.HashMap;
import java.util.Map;

public class FixImageUrls {

    private static final String MUSIC_TABLE = "music";
    private static final String SUBSCRIPTIONS_TABLE = "subscriptions";
    private static final String IMAGE_BUCKET = "a2-113-music-app-v1";

    public static void main(String[] args) throws Exception {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table musicTable = dynamoDB.getTable(MUSIC_TABLE);
        Table subscriptionsTable = dynamoDB.getTable(SUBSCRIPTIONS_TABLE);

        S3Helper s3Helper = new S3Helper();

        Map<String, String> urlMap = new HashMap<>();

        int musicUpdated = updateMusicImages(musicTable, s3Helper, urlMap);
        int subscriptionUpdated = updateSubscriptionImages(subscriptionsTable, urlMap);

        System.out.println("Done.");
        System.out.println("Music items updated: " + musicUpdated);
        System.out.println("Subscription items updated: " + subscriptionUpdated);
    }

    // Update image_url in music table
    private static int updateMusicImages(Table musicTable, S3Helper s3Helper, Map<String, String> urlMap) {
        int count = 0;

        ItemCollection<ScanOutcome> items = musicTable.scan();

        for (Item item : items) {
            try {
                String imageUrl = item.getString("image_url");

                if (imageUrl == null || !imageUrl.contains("raw.githubusercontent.com")) {
                    continue;
                }

                String fileName = getFileNameFromUrl(imageUrl);
                String s3Key = "artists/" + fileName;
                String s3Url;

                // Avoid uploading the same image many times
                if (urlMap.containsKey(imageUrl)) {
                    s3Url = urlMap.get(imageUrl);
                } else {
                    s3Url = s3Helper.uploadImageFromUrl(IMAGE_BUCKET, imageUrl, s3Key);
                    urlMap.put(imageUrl, s3Url);
                }

                item.withString("image_url", s3Url);
                musicTable.putItem(item);

                count++;
                System.out.println("Updated music image: " + item.getString("artist") + " -> " + s3Url);

            } catch (Exception e) {
                System.out.println("Failed to update music item: " + item.toJSON());
                System.out.println(e.getMessage());
            }
        }

        return count;
    }

    // Update old image_url in subscriptions table
    private static int updateSubscriptionImages(Table subscriptionsTable, Map<String, String> urlMap) {
        int count = 0;

        ItemCollection<ScanOutcome> items = subscriptionsTable.scan();

        for (Item item : items) {
            try {
                String imageUrl = item.getString("image_url");

                if (imageUrl == null || !imageUrl.contains("raw.githubusercontent.com")) {
                    continue;
                }

                String fileName = getFileNameFromUrl(imageUrl);
                String s3Url = "https://" + IMAGE_BUCKET + ".s3.amazonaws.com/artists/" + fileName;

                item.withString("image_url", s3Url);
                subscriptionsTable.putItem(item);

                count++;
                System.out.println("Updated subscription image: " + item.getString("songkey") + " -> " + s3Url);

            } catch (Exception e) {
                System.out.println("Failed to update subscription item: " + item.toJSON());
                System.out.println(e.getMessage());
            }
        }

        return count;
    }

    // Extract file name from URL
    private static String getFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }
}