package com.innowise.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "surname",
        "birthDate",
        "email",
        "active",
        "cardsCount",
        "createdAt",
        "updatedAt"
})
public class UserResponse {

    private UUID id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer cardsCount;
}
