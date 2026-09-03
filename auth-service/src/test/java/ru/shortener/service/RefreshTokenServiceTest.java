package ru.shortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.shortener.exception.InvalidCredentialsException;
import ru.shortener.model.RefreshToken;
import ru.shortener.model.User;
import ru.shortener.repository.RefreshTokenRepository;
import ru.shortener.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpiration", 604800000L);
    }

    @Test
    void issue_storesOnlyHash_returnsRawToken() {
        service.issue(1L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).hasSize(64);
    }

    @Test
    void rotate_validToken_revokesItAndReturnsUser() {
        User user = new User();
        user.setId(1L);
        RefreshToken stored = new RefreshToken();
        stored.setUserId(1L);
        stored.setRevoked(false);
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHash(service.sha256("raw-token")))
                .thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(service.rotate("raw-token")).isEqualTo(user);
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void rotate_revokedToken_reusesDetection_revokesAllUserSessions() {
        RefreshToken stolen = new RefreshToken();
        stolen.setUserId(7L);
        stolen.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(service.sha256("stolen")))
                .thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> service.rotate("stolen"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokenRepository).revokeAllByUserId(7L);
    }

    @Test
    void rotate_expiredToken_throws() {
        RefreshToken expired = new RefreshToken();
        expired.setUserId(1L);
        expired.setRevoked(false);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByTokenHash(service.sha256("old")))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("old"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}