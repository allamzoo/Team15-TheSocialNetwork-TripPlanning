package com.team15.tripplanning.shared.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthContext {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private String token;
    private String userEmail;
    private Long userId;
    private String role;

    public AuthContext() {}

    public AuthContext(HttpServletRequest request, HttpServletResponse response,
                       String token, String userEmail, Long userId, String role) {
        this.request = request;
        this.response = response;
        this.token = token;
        this.userEmail = userEmail;
        this.userId = userId;
        this.role = role;
    }

    public HttpServletRequest getRequest() { return request; }
    public void setRequest(HttpServletRequest request) { this.request = request; }

    public HttpServletResponse getResponse() { return response; }
    public void setResponse(HttpServletResponse response) { this.response = response; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
