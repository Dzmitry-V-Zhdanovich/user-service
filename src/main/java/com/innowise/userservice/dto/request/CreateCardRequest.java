package com.innowise.userservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCardRequest {

    @NotNull(message = "ID пользователя обязателен")
    private String userId;

    @NotBlank(message = "Номер карты обязателен")
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String number;

    @NotBlank(message = "Держатель карты обязателен")
    @Size(min = 2, max = 200, message = "Имя держателя от 2 до 200 символов")
    private String holder;

    @NotNull(message = "Дата истечения обязательна")
    @Future(message = "Дата истечения должна быть в будущем")
    private LocalDate expirationDate;
}
