package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shortener.controller.AuthRequest;
import ru.shortener.controller.AuthResponse;
import ru.shortener.controller.RefreshRequest;
import ru.shortener.controller.RegisterRequest;
import ru.shortener.exception.DuplicateException;
import ru.shortener.exception.InvalidCredentialsException;
import ru.shortener.model.User;
import ru.shortener.repository.UserRepository;
import ru.shortener.security.JwtService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("Пользователь с email " + request.getEmail() + " уже существует");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        //userRepository.save(user); // При гонке может вызвать 500 а нужен 409
        try { // отлавливает ошибку
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Если гонка проиграна, то пользователь уже существует — тот же текст, что и выше
            throw new DuplicateException("Пользователь с email " + request.getEmail() + " уже существует");
        }

        log.debug("Пользователь создан: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    public AuthResponse login(AuthRequest request) {
        // Одинаковая ошибка и для несуществующего email, и для неверного пароля,
        // чтобы не раскрывать, зарегистрирован ли пользователь
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Неверный email или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Неверный email или пароль");
        }

        log.debug("Вход пользователя: {}", user.getUsername());
        return buildAuthResponse(user);
    }

    /** Обмен валидного refresh-токена на новую пару access+refresh. */
    public AuthResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.rotate(request.getRefreshToken());
        return buildAuthResponse(user);
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }


    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResponse(accessToken, refreshToken, user.getEmail());
    }
}
