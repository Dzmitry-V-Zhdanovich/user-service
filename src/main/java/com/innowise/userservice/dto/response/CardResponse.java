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
        "number",
        "holder",
        "userId",
        "expirationDate",
        "active",
        "createdAt",
        "updatedAt"
})
public class CardResponse {

    private UUID id;
    private UUID userId;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
