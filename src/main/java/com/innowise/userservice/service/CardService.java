package com.innowise.userservice.service;

import com.innowise.userservice.dto.request.CreateCardRequest;
import com.innowise.userservice.dto.request.UpdateCardRequest;
import com.innowise.userservice.dto.response.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CardService {

    CardResponse createCard(CreateCardRequest request);

    CardResponse getCardById(UUID id);

    List<CardResponse> getCardsByUserId(UUID userId);

    Page<CardResponse> getCardsByUserId(UUID userId, Boolean active, String number, Pageable pageable);

    CardResponse updateCard(UUID id, UpdateCardRequest request);

    void setCardActiveStatus(UUID id, Boolean active);

    void deleteCard(UUID id);
}
