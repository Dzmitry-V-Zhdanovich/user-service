package com.innowise.userservice.dto.request;

import jakarta.validation.constraints.Future;
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
public class UpdateCardRequest {

    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
    private String number;

    @Size(min = 2, max = 200, message = "Имя держателя от 2 до 200 символов")
    private String holder;

    @Future(message = "Дата истечения должна быть в будущем")
    private LocalDate expirationDate;
}
