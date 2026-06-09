package com.innowise.userservice.service;

import io.jsonwebtoken.Claims;

public interface JwtService {
    Claims extractAllClaims(String token);
    String extractUserId(String token);
    String extractRole(String token);
    boolean isTokenValid(String token);
}
