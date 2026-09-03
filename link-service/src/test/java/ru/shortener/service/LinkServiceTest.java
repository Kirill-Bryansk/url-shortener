package ru.shortener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.shortener.exception.DuplicateException;
import ru.shortener.exception.LinkNotFoundException;
import ru.shortener.model.Link;
import ru.shortener.repository.LinkRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock private LinkRepository repository;
    @InjectMocks private LinkService linkService;

    @Test
    void createShortLink_savesWithShortCodeAndDefaults() {
        when(repository.existsByOriginalUrlAndUserId("https://ya.ru", 1L)).thenReturn(false);
        when(repository.existsByShortCode(any())).thenReturn(false);
        when(repository.saveAndFlush(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        Link saved = linkService.createShortLink("https://ya.ru", 1L);

        ArgumentCaptor<Link> captor = ArgumentCaptor.forClass(Link.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getShortCode()).hasSize(8);
        assertThat(captor.getValue().getClickCount()).isZero();
        assertThat(saved.getOriginalUrl()).isEqualTo("https://ya.ru");
    }

    @Test
    void createShortLink_duplicateForUser_throwsDuplicate() {
        when(repository.existsByOriginalUrlAndUserId("https://ya.ru", 1L)).thenReturn(true);

        assertThatThrownBy(() -> linkService.createShortLink("https://ya.ru", 1L))
                .isInstanceOf(DuplicateException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getOriginalUrl_incrementsClickCountAtomically() {
        Link link = new Link();
        link.setShortCode("abc12345");
        link.setOriginalUrl("https://ya.ru");

        when(repository.findByShortCode("abc12345")).thenReturn(Optional.of(link));

        String url = linkService.getOriginalUrl("abc12345");

        assertThat(url).isEqualTo("https://ya.ru");
        // Атомарный bulk-UPDATE, а не save() сущности — фикс из пункта 2
        verify(repository).incrementClickCount("abc12345");
        verify(repository, never()).save(any());
    }

    @Test
    void getOriginalUrl_unknownCode_throwsNotFound() {
        when(repository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.getOriginalUrl("nope"))
                .isInstanceOf(LinkNotFoundException.class);
    }

    @Test
    void deleteLink_ofAnotherUser_throwsNotFound() {
        when(repository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.deleteLink(5L, 2L))
                .isInstanceOf(LinkNotFoundException.class);
        verify(repository, never()).deleteByIdAndUserId(any(), any());
    }
}