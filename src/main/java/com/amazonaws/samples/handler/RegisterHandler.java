package com.amazonaws.samples.handler;

import com.amazonaws.samples.service.LoginService;
import com.amazonaws.samples.service.LoginService.LoginResult;

import java.util.HashMap;
import java.util.Map;

public class RegisterHandler {

    // Handle register request
    public Map<String, Object> handleRequest(Map<String, String> input) {

        String email = input.get("email");
        String userName = input.get("userName");
        String password = input.get("password");

        LoginService loginService = new LoginService();
        LoginResult result = loginService.register(email, userName, password);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("email", result.getEmail());
        response.put("userName", result.getUserName());

        return response;
    }
}