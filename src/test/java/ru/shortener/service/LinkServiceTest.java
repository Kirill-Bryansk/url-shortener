package ru.shortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.shortener.exception.DuplicateException;
import ru.shortener.exception.LinkNotFoundException;
import ru.shortener.model.Link;
import ru.shortener.repository.LinkRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository repository;

    @InjectMocks
    private LinkService service;

    private Link link;

    @BeforeEach
    void setUp() {
        link = new Link();
        link.setId(1L);
        link.setOriginalUrl("https://yandex.ru");
        link.setShortCode("abc12345");
        link.setUserId(1L);
    }

    @Test
    void createShortLink_shouldReturnSavedLink() {
        when(repository.existsByOriginalUrl(any())).thenReturn(false);
        when(repository.save(any(Link.class))).thenReturn(link);

        Link result = service.createShortLink("https://yandex.ru", 1L);

        assertNotNull(result);
        assertEquals("https://yandex.ru", result.getOriginalUrl());
        assertEquals(1L, result.getUserId());
        verify(repository, times(1)).save(any(Link.class));
    }

    @Test
    void createShortLink_shouldThrowDuplicateException() {
        when(repository.existsByOriginalUrl(any())).thenReturn(true);

        assertThrows(DuplicateException.class,
                () -> service.createShortLink("https://yandex.ru", 1L));
    }

    @Test
    void getOriginalUrl_shouldReturnUrlAndIncrementClickCount() {
        when(repository.findByShortCode("abc12345")).thenReturn(Optional.of(link));

        String url = service.getOriginalUrl("abc12345");

        assertEquals("https://yandex.ru", url);
        assertEquals(1L, link.getClickCount());
        verify(repository, times(1)).save(link);
    }

    @Test
    void getOriginalUrl_shouldThrowLinkNotFoundException() {
        when(repository.findByShortCode("notfound")).thenReturn(Optional.empty());

        assertThrows(LinkNotFoundException.class,
                () -> service.getOriginalUrl("notfound"));
    }

    @Test
    void deleteLink_shouldDeleteWhenUserOwnsLink() {
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(link));
        doNothing().when(repository).deleteByIdAndUserId(1L, 1L);

        service.deleteLink(1L, 1L);

        verify(repository, times(1)).deleteByIdAndUserId(1L, 1L);
    }

    @Test
    void deleteLink_shouldThrowWhenLinkNotFound() {
        when(repository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(LinkNotFoundException.class,
                () -> service.deleteLink(999L, 1L));
    }
}