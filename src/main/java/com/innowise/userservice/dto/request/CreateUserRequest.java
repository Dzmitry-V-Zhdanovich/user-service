package com.innowise.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String name;

    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
    private String surname;

    @NotNull(message = "Дата рождения обязательна")
    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Неверный формат email")
    private String email;
}
