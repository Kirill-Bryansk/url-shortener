package ru.shortener.controller;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
class LinkRequest {
    private String originalUrl;
}
