package com.innowise.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String name;

    @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
    private String surname;

    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    @Email(message = "Неверный формат email")
    private String email;
}
