package com.amazonaws.samples.service;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

public class LoginService {

    private final Table loginTable;

    public LoginService() {
        // Connect to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new ProfileCredentialsProvider("default"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        this.loginTable = dynamoDB.getTable("login");
    }

    // Check email and password
    public LoginResult login(String email, String password) {
        Item user = loginTable.getItem("email", email);

        if (user == null) {
            return new LoginResult(false, "email or password is invalid", null, null);
        }

        if (!password.equals(user.getString("password"))) {
            return new LoginResult(false, "email or password is invalid", null, null);
        }

        return new LoginResult(
                true,
                "Login successful",
                email,
                user.getString("user_name")
        );
    }

    // Register new user
    public LoginResult register(String email, String userName, String password) {
        Item existingUser = loginTable.getItem("email", email);

        if (existingUser != null) {
            return new LoginResult(false, "The email already exists", email, null);
        }

        loginTable.putItem(new Item()
                .withPrimaryKey("email", email)
                .withString("user_name", userName)
                .withString("password", password));

        return new LoginResult(true, "Register successful", email, userName);
    }

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final String email;
        private final String userName;

        public LoginResult(boolean success, String message, String email, String userName) {
            this.success = success;
            this.message = message;
            this.email = email;
            this.userName = userName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getEmail() {
            return email;
        }

        public String getUserName() {
            return userName;
        }
    }
}