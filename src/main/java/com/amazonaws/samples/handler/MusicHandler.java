package com.amazonaws.samples.handler;

import com.amazonaws.samples.service.MusicService;
import com.amazonaws.services.dynamodbv2.document.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicHandler {

    private final MusicService musicService = new MusicService();

    public Map<String, Object> handleRequest(Map<String, String> input) {

        String title = input.get("title");
        String year = input.get("year");
        String artist = input.get("artist");
        String album = input.get("album");

        List<Item> results = musicService.querySongs(title, year, artist, album);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", results);

        return response;
    }

public static void main(String[] args) {

    MusicHandler handler = new MusicHandler();
    
        // 模拟前端请求
    Map<String, String> input = new HashMap<>();
    
    input.put("artist", "Adele");
        // input.put("title", "Hello");
        // input.put("year", "2015");
        // input.put("album", "25");
    
    Map<String, Object> result = handler.handleRequest(input);
    
    System.out.println(result);
    }
}