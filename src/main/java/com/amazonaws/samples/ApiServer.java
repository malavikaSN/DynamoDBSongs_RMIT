package com.amazonaws.samples;

import static spark.Spark.*;

import com.amazonaws.samples.service.LoginService;
import com.amazonaws.samples.service.LoginService.LoginResult;
import com.amazonaws.samples.service.MusicService;
import com.amazonaws.samples.service.SubscriptionService;
import com.amazonaws.samples.service.SubscriptionService.SubscriptionResult;

import com.amazonaws.services.dynamodbv2.document.Item;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiServer {

    public static void main(String[] args) {
        port(4567);

        ObjectMapper mapper = new ObjectMapper();

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
            response.header("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
        });
        
        options("/*", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
            response.header("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
            return "OK";
        });

        get("/health", (req, res) -> {
            res.type("application/json");

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");

            return mapper.writeValueAsString(response);
        });

        // POST /api/register
        post("/api/register", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = mapper.readValue(
                    req.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String email = (String) body.get("email");
            String userName = (String) body.get("user_name");
            String password = (String) body.get("password");

            if (email == null || userName == null || password == null) {
                res.status(400);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "missing fields");

                return mapper.writeValueAsString(response);
            }

            LoginService loginService = new LoginService();
            LoginResult result = loginService.register(email, userName, password);

            if (!result.isSuccess()) {
                res.status(409);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("email", result.getEmail());
            response.put("user_name", result.getUserName());

            return mapper.writeValueAsString(response);
        });

        // POST /api/login
        post("/api/login", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = mapper.readValue(
                    req.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String email = (String) body.get("email");
            String password = (String) body.get("password");

            if (email == null || password == null) {
                res.status(400);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "missing fields");

                return mapper.writeValueAsString(response);
            }

            LoginService loginService = new LoginService();
            LoginResult result = loginService.login(email, password);

            if (!result.isSuccess()) {
                res.status(401);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", result.getMessage());

                return mapper.writeValueAsString(response);
            }

            Algorithm algorithm = Algorithm.HMAC256("secret-demo-key-please-change");
            String token = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .sign(algorithm);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("token", token);
            response.put("email", result.getEmail());
            response.put("user_name", result.getUserName());

            return mapper.writeValueAsString(response);
        });

        // POST /api/songs/query
        post("/api/songs/query", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = mapper.readValue(
                    req.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String title = (String) body.get("title");
            String year = (String) body.get("year");
            String artist = (String) body.get("artist");
            String album = (String) body.get("album");

            MusicService musicService = new MusicService();
            List<Item> items = musicService.querySongs(title, year, artist, album);

            List<Map<String, Object>> songs = convertItemsToSongs(items);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("songs", songs);

            return mapper.writeValueAsString(response);
        });

        // GET /api/songs
        get("/api/songs", (req, res) -> {
            res.type("application/json");

            String title = req.queryParams("title");
            String year = req.queryParams("year");
            String artist = req.queryParams("artist");
            String album = req.queryParams("album");

            MusicService musicService = new MusicService();
            List<Item> items = musicService.querySongs(title, year, artist, album);

            List<Map<String, Object>> songs = convertItemsToSongs(items);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("songs", songs);

            return mapper.writeValueAsString(response);
        });

        // POST /api/songs
        post("/api/songs", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = mapper.readValue(
                    req.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String artist = (String) body.get("artist");
            String songkey = (String) body.get("songkey");
            String title = (String) body.get("title");
            String album = (String) body.get("album");
            String year = (String) body.get("year");
            String imageUrl = (String) body.get("image_url");

            if (artist == null || songkey == null) {
                res.status(400);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "artist and songkey required");

                return mapper.writeValueAsString(response);
            }

            // This endpoint is kept for compatibility with frontend upload flow.
            // Main music loading should still be done by LoadMusicData.java.
            SubscriptionService dummy = null;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Song upload endpoint reserved. Use LoadMusicData for dataset import.");

            return mapper.writeValueAsString(response);
        });

        // POST /api/subscriptions
        post("/api/subscriptions", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = mapper.readValue(
                    req.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            SubscriptionService subscriptionService = new SubscriptionService();

            SubscriptionResult result = subscriptionService.addSubscription(
                    (String) body.get("email"),
                    (String) body.get("songkey"),
                    (String) body.get("title"),
                    (String) body.get("artist"),
                    (String) body.get("album"),
                    (String) body.get("year"),
                    (String) body.get("image_url")
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return mapper.writeValueAsString(response);
        });

        // GET /api/subscriptions?email=xxx
        get("/api/subscriptions", (req, res) -> {
            res.type("application/json");

            String email = req.queryParams("email");

            if (email == null || email.trim().isEmpty()) {
                res.status(400);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "email is required");

                return mapper.writeValueAsString(response);
            }

            SubscriptionService subscriptionService = new SubscriptionService();
            List<Item> items = subscriptionService.getSubscriptions(email);

            List<Map<String, Object>> subscriptions = convertItemsToSongs(items);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscriptions", subscriptions);

            return mapper.writeValueAsString(response);
        });

        // DELETE /api/subscriptions
        delete("/api/subscriptions", (req, res) -> {
            res.type("application/json");

            Map<String, Object> body = mapper.readValue(
                    req.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String email = (String) body.get("email");
            String songkey = (String) body.get("songkey");

            if (email == null || songkey == null) {
                res.status(400);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "email and songkey are required");

                return mapper.writeValueAsString(response);
            }

            SubscriptionService subscriptionService = new SubscriptionService();
            SubscriptionResult result = subscriptionService.removeSubscription(email, songkey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return mapper.writeValueAsString(response);
        });
    }

    private static List<Map<String, Object>> convertItemsToSongs(List<Item> items) {
        List<Map<String, Object>> songs = new ArrayList<>();

        for (Item item : items) {
            Map<String, Object> song = new HashMap<>();
            song.put("artist", item.getString("artist"));
            song.put("songkey", item.getString("songkey"));
            song.put("title", item.getString("title"));
            song.put("album", item.getString("album"));
            song.put("year", item.getString("year"));
            song.put("image_url", item.getString("image_url"));
            songs.add(song);
        }

        return songs;
    }
}