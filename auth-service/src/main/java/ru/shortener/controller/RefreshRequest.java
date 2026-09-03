package ru.shortener.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token обязателен")
    private String refreshToken;
}
