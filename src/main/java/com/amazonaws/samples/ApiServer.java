package com.amazonaws.samples;

import static spark.Spark.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;

import org.mindrot.jbcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class ApiServer {

    public static void main(String[] args) {
        port(4567);

        // Simple JSON mapper
        ObjectMapper mapper = new ObjectMapper();

        // Enable CORS for local frontend (http://localhost:8000)
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "http://localhost:8000");
            response.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        // Handle preflight
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        // Global exception handler to log stacktraces and return JSON
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace();
            res.type("application/json");
            res.status(500);
            try {
                Map<String, Object> out = new HashMap<>();
                out.put("success", false);
                out.put("message", e.getMessage());
                res.body(mapper.writeValueAsString(out));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

    // DynamoDB client used for auth and songs
    AmazonDynamoDB clientDdb = AmazonDynamoDBClientBuilder.standard()
        .withRegion(Regions.US_EAST_1)
        .withCredentials(new ProfileCredentialsProvider("default"))
        .build();

    DynamoDB dynamoDB = new DynamoDB(clientDdb);

    // Health check
        get("/health", (req, res) -> {
            res.type("application/json");
            return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("status", "ok"); }});
        });

        // POST /api/presign -> { bucket, key, expiresMinutes }
        post("/api/presign", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = mapper.readValue(req.body(), new TypeReference<Map<String, Object>>() {});

            String bucket = (String) body.get("bucket");
            String key = (String) body.get("key");
            Integer expires = body.get("expiresMinutes") == null ? 15 : (Integer) body.get("expiresMinutes");

            if (bucket == null || key == null) {
                res.status(400);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "bucket and key required"); }});
            }

            try {
                S3Helper helper = new S3Helper();
                java.net.URL url = helper.generatePresignedUploadUrl(bucket, key, expires);

                Map<String, Object> out = new HashMap<>();
                out.put("success", true);
                out.put("url", url.toString());
                return mapper.writeValueAsString(out);

            } catch (Exception e) {
                res.status(500);
                Map<String, Object> out = new HashMap<>();
                out.put("success", false);
                out.put("message", e.getMessage());
                return mapper.writeValueAsString(out);
            }
        });

        // POST /api/register -> { email, user_name, password }
        post("/api/register", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = mapper.readValue(req.body(), new TypeReference<Map<String, Object>>() {});
            String email = (String) body.get("email");
            String userName = (String) body.get("user_name");
            String password = (String) body.get("password");

            if (email == null || password == null || userName == null) {
                res.status(400);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "missing fields"); }});
            }

            Table loginTable = dynamoDB.getTable("login");
            // check if exists
            Item existing = loginTable.getItem("email", email);
            if (existing != null) {
                res.status(409);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "user exists"); }});
            }

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            loginTable.putItem(new Item().withPrimaryKey("email", email).withString("user_name", userName).withString("password", hashed));

            return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", true); put("message", "registered"); }});
        });

        // POST /api/login -> { email, password }
        post("/api/login", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = mapper.readValue(req.body(), new TypeReference<Map<String, Object>>() {});
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            if (email == null || password == null) {
                res.status(400);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "missing fields"); }});
            }

            Table loginTable = dynamoDB.getTable("login");
            Item item = loginTable.getItem("email", email);
            if (item == null) {
                res.status(401);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "invalid credentials"); }});
            }

            String stored = item.getString("password");
            if (!BCrypt.checkpw(password, stored)) {
                res.status(401);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "invalid credentials"); }});
            }

            // create a simple JWT (insecure: secret in code for demo)
            Algorithm algorithm = Algorithm.HMAC256("secret-demo-key-please-change");
            String token = JWT.create().withSubject(email).withIssuedAt(new Date()).sign(algorithm);

            return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", true); put("token", token); }});
        });

        // GET /api/songs -> scan music table
        get("/api/songs", (req, res) -> {
            res.type("application/json");
            Table music = dynamoDB.getTable("music");
            ItemCollection<ScanOutcome> items = music.scan();
            List<Map<String, Object>> outList = new ArrayList<>();

            for (Item it : items) {
                Map<String, Object> m = new HashMap<>();
                m.put("artist", it.getString("artist"));
                m.put("songKey", it.getString("songKey") != null ? it.getString("songKey") : it.getString("songkey"));
                m.put("title", it.getString("title"));
                m.put("album", it.getString("album"));
                m.put("year", it.getString("year"));
                m.put("image_url", it.getString("image_url"));
                outList.add(m);
            }

            return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("songs", outList); }});
        });

        // POST /api/songs -> save song metadata
        post("/api/songs", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = mapper.readValue(req.body(), new TypeReference<Map<String, Object>>() {});
            String artist = (String) body.get("artist");
            String songKey = (String) body.get("songKey");
            String title = (String) body.get("title");
            String album = (String) body.get("album");
            String year = (String) body.get("year");
            String imageUrl = (String) body.get("image_url");

            if (artist == null || songKey == null) {
                res.status(400);
                return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", false); put("message", "artist and songKey required"); }});
            }

            Table music = dynamoDB.getTable("music");
            Item item = new Item().withPrimaryKey("artist", artist, "songKey", songKey)
                    .withString("title", title)
                    .withString("album", album)
                    .withString("year", year)
                    .withString("image_url", imageUrl);

            music.putItem(item);
            return mapper.writeValueAsString(new HashMap<String, Object>() {{ put("success", true); }});
        });
    }
}
