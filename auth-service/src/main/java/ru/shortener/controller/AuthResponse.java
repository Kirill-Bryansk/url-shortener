package ru.shortener.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token; // access-токен, 15 минут
    private String refreshToken; // для /refresh и /logout
    private String email;
}
