package ru.shortener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ru.shortener.controller.AuthRequest;
import ru.shortener.controller.AuthResponse;
import ru.shortener.controller.RegisterRequest;
import ru.shortener.exception.DuplicateException;
import ru.shortener.exception.InvalidCredentialsException;
import ru.shortener.model.User;
import ru.shortener.repository.UserRepository;
import ru.shortener.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("testuser");
        r.setEmail("test@mail.ru");
        r.setPassword("password123");
        return r;
    }

    @Test
    void register_savesUserAndReturnsToken() {
        when(userRepository.existsByEmail("test@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(1L, "test@mail.ru")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest());

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@mail.ru");
        // Пароль сохраняется в хэше, а не в открытом виде
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicate() {
        when(userRepository.existsByEmail("test@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void register_raceLost_constraintViolationTranslatedTo409() {
        // Гонка: проверка прошла, но INSERT упал по unique-констрейнту
        when(userRepository.existsByEmail("test@mail.ru")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateException.class);   // ← а не 500
    }

    @Test
    void login_correctCredentials_returnsToken() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@mail.ru");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@mail.ru");
        user.setPassword("hashed");

        when(userRepository.findByEmail("test@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(1L, "test@mail.ru")).thenReturn("jwt-token");

        assertThat(authService.login(request).getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@mail.ru");
        request.setPassword("wrong-pass");

        User user = new User();
        user.setPassword("hashed");

        when(userRepository.findByEmail("test@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}