package com.amazonaws.samples.handler;

import com.amazonaws.samples.service.SubscriptionService;
import com.amazonaws.samples.service.SubscriptionService.SubscriptionResult;
import com.amazonaws.services.dynamodbv2.document.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionHandler {

    private final SubscriptionService subscriptionService = new SubscriptionService();

    // Add subscription
    public Map<String, Object> addSubscription(Map<String, String> input) {
        SubscriptionResult result = subscriptionService.addSubscription(
                input.get("email"),
                input.get("songkey"),
                input.get("title"),
                input.get("artist"),
                input.get("album"),
                input.get("year"),
                input.get("image_url")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        return response;
    }

    // Get user's subscriptions
    public Map<String, Object> getSubscriptions(Map<String, String> input) {
        String email = input.get("email");

        List<Item> subscriptions = subscriptionService.getSubscriptions(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptions", subscriptions);

        return response;
    }

    // Remove subscription
    public Map<String, Object> removeSubscription(Map<String, String> input) {
        SubscriptionResult result = subscriptionService.removeSubscription(
                input.get("email"),
                input.get("songkey")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        return response;
    }
}