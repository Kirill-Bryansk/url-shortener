package ru.shortener.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-64-characters-long-for-hmac-sha256";
    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);
        jwtService.init();   //раньше Spring делал это сам
    }

    @Test
    void generateToken_thenExtract_returnsSameEmailAndUserId() {
        String token = jwtService.generateToken(42L, "user@test.ru");

        assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.ru");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void extractToken_signedWithDifferentKey_throwsSignatureException() {
        String token = jwtService.generateToken(1L, "user@test.ru");

        // Имитируем токен, подписанный чужим секретом
        JwtService foreign = new JwtService();
        ReflectionTestUtils.setField(foreign, "secret", "another-secret-key-that-is-also-at-least-64-characters-long-for-hmac!!!!");
        ReflectionTestUtils.setField(foreign, "expiration", 3_600_000L);
        foreign.init();
        String forged = foreign.generateToken(1L, "user@test.ru");

        assertThatThrownBy(() -> jwtService.extractEmail(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void extractExpiredToken_throwsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); // уже истёк
        String token = jwtService.generateToken(1L, "user@test.ru");

        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void init_shortSecret_failsFast() {
        JwtService bad = new JwtService();
        ReflectionTestUtils.setField(bad, "secret", "too-short");
        assertThatThrownBy(bad::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void init_defaultSecret_failsFast() {
        JwtService bad = new JwtService();
        ReflectionTestUtils.setField(bad, "secret", "your-very-long-secret-key-at-least-64-characters-long");
        assertThatThrownBy(bad::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("дефолтн");
    }
}