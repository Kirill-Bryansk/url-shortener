package ru.shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.shortener.controller.AuthRequest;
import ru.shortener.controller.AuthResponse;
import ru.shortener.controller.RegisterRequest;
import ru.shortener.exception.DuplicateException;
import ru.shortener.exception.UserNotFoundException;
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

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("User with email " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        log.debug("Пользователь создан: {}", user.getUsername());

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }

        log.debug("Вход пользователя: {}", user.getUsername());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail());
    }
}
