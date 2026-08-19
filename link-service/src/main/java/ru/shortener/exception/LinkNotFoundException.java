package ru.shortener.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String shortCode) {
        super("Ссылка не найдена: " + shortCode);
    }
}
