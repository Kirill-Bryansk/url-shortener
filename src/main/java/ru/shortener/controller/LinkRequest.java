package ru.shortener.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LinkRequest {

    @NotBlank(message = "URL не может быть пустым")
    @Pattern(
            regexp = "^(https?://)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+$",
            message = "Некорректный URL"
    )
    private String originalUrl;
}
