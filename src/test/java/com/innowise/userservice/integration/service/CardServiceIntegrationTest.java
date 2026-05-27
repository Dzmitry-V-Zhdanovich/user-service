package com.innowise.userservice.integration.service;

import com.innowise.userservice.dto.request.CreateCardRequest;
import com.innowise.userservice.dto.request.UpdateCardRequest;
import com.innowise.userservice.dto.response.CardResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateResourceException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.exception.TooManyCardsException;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.CardService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@DisplayName("CardService Integration Tests")
public class CardServiceIntegrationTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class JacksonTestConfig {
        @org.springframework.context.annotation.Bean
        public com.fasterxml.jackson.databind.ObjectMapper testObjectMapper() {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_name")
            .withPassword("test_password");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private CardService cardService;

    @Autowired
    private UserRepository userRepository;

    private UUID testUserId;
    private CreateCardRequest createCardRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .name("Иван")
                .surname("Петров")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("card_service_test_" + System.currentTimeMillis() + "@example.com")
                .active(true)
                .build();
        User savedUser = userRepository.save(user);
        testUserId = savedUser.getId();

        createCardRequest = CreateCardRequest.builder()
                .userId(testUserId.toString())
                .number("4111111111111111")
                .holder("IVAN PETROV")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .build();
    }

    @Test
    @DisplayName("Should create card successfully")
    void shouldCreateCardSuccessfully() {
        // When
        CardResponse response = cardService.createCard(createCardRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getNumber()).isEqualTo("4111111111111111");
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();
        createCardRequest.setUserId(nonExistentUserId.toString());

        // When & Then
        assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when card limit exceeded")
    void shouldThrowExceptionWhenCardLimitExceeded() {
        // Given
        for (int i = 1; i <= 5; i++) {
            CreateCardRequest request = CreateCardRequest.builder()
                    .userId(testUserId.toString())
                    .number("411111111111111" + i)
                    .holder("IVAN PETROV")
                    .expirationDate(LocalDate.of(2028, 12, 31))
                    .build();
            cardService.createCard(request);
        }

        // When & Then
        assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                .isInstanceOf(TooManyCardsException.class);
    }

    @Test
    @DisplayName("Should throw exception when card number already exists")
    void shouldThrowExceptionWhenCardNumberExists() {
        // Given
        cardService.createCard(createCardRequest);

        // When & Then
        assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should find card by id")
    void shouldFindCardById() {
        // Given
        CardResponse created = cardService.createCard(createCardRequest);

        // When
        CardResponse found = cardService.getCardById(created.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getNumber()).isEqualTo("4111111111111111");
    }

    @Test
    @DisplayName("Should throw exception when card not found")
    void shouldThrowExceptionWhenCardNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> cardService.getCardById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all cards by user id")
    void shouldGetAllCardsByUserId() {
        // Given
        cardService.createCard(createCardRequest);

        CreateCardRequest secondRequest = CreateCardRequest.builder()
                .userId(testUserId.toString())
                .number("4222222222222222")
                .holder("IVAN PETROV")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .build();
        cardService.createCard(secondRequest);

        // When
        List<CardResponse> cards = cardService.getCardsByUserId(testUserId);

        // Then
        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(CardResponse::getNumber)
                .containsExactlyInAnyOrder("4111111111111111", "4222222222222222");
    }

    @Test
    @DisplayName("Should update card successfully")
    void shouldUpdateCardSuccessfully() {
        // Given
        CardResponse created = cardService.createCard(createCardRequest);

        UpdateCardRequest updateRequest = UpdateCardRequest.builder()
                .holder("IVAN PETROV UPDATED")
                .expirationDate(LocalDate.of(2029, 12, 31))
                .build();

        // When
        CardResponse updated = cardService.updateCard(created.getId(), updateRequest);

        // Then
        assertThat(updated.getHolder()).isEqualTo("IVAN PETROV UPDATED");
        assertThat(updated.getExpirationDate()).isEqualTo(LocalDate.of(2029, 12, 31));
        assertThat(updated.getNumber()).isEqualTo("4111111111111111");
    }

    @Test
    @DisplayName("Should deactivate card successfully")
    void shouldDeactivateCardSuccessfully() {
        // Given
        CardResponse created = cardService.createCard(createCardRequest);
        assertThat(created.getActive()).isTrue();

        // When
        cardService.setCardActiveStatus(created.getId(), false);

        // Then
        CardResponse deactivated = cardService.getCardById(created.getId());
        assertThat(deactivated.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should delete card successfully")
    void shouldDeleteCardSuccessfully() {
        // Given
        CardResponse created = cardService.createCard(createCardRequest);

        // When
        cardService.deleteCard(created.getId());

        // Then
        assertThatThrownBy(() -> cardService.getCardById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
