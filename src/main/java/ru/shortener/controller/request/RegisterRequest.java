package ru.shortener.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Имя пользователя не может быть пустым ")
    @Size(min = 3, max = 20, message = "Имя пользователя должно быть от 3 до 20 символов")
    private String username;

    @NotBlank(message = "Email не может быть пустым ")
    @Email(message = "Не корректный email")
    private String email;

    @NotBlank(message = "Пароль не может быть пустым ")
    @Size(min = 8, message = "Пароль должен быть не менее 8 символов")
    private String password;
}
