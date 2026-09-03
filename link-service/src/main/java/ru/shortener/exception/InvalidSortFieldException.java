package ru.shortener.exception;

/** Запрос сортировки по полю, которого нет в белом списке. */
public class InvalidSortFieldException extends RuntimeException {
    public InvalidSortFieldException(String field) {
        super("Сортировка по полю \"" + field + "\" недоступна");
    }
}
