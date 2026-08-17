package ru.shortener.exception;

import javax.management.relation.RoleInfoNotFoundException;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String shortCode) {
        super("Ссылка не найдена: " + shortCode);
    }
}
