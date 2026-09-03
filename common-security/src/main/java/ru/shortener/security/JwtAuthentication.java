package ru.shortener.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.ArrayList;

public class JwtAuthentication extends UsernamePasswordAuthenticationToken {
    private final Long userId;

    public JwtAuthentication(String email, Long userId) {
        super(email, null, new ArrayList<>());
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
