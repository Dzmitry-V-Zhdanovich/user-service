package com.innowise.userservice.integration.repository;

import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("CardRepository Integration Tests")
public class CardRepositoryTest {

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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentCardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private PaymentCard testCard;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Тест")
                .surname("Тестов")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("card_test_" + System.currentTimeMillis() + "@example.com")
                .active(true)
                .build();
        testUser = userRepository.save(testUser);

        testCard = PaymentCard.builder()
                .user(testUser)
                .number("4111111111111111")
                .holder("TEST HOLDER")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should save and find card by id")
    void shouldSaveAndFindCardById() {
        // Given
        PaymentCard savedCard = cardRepository.save(testCard);

        // When
        Optional<PaymentCard> found = cardRepository.findById(savedCard.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNumber()).isEqualTo("4111111111111111");
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should find cards by user id")
    void shouldFindCardsByUserId() {
        // Given
        cardRepository.save(testCard);

        PaymentCard secondCard = PaymentCard.builder()
                .user(testUser)
                .number("4222222222222222")
                .holder("TEST HOLDER 2")
                .expirationDate(LocalDate.of(2029, 12, 31))
                .active(true)
                .build();
        cardRepository.save(secondCard);

        // When
        List<PaymentCard> cards = cardRepository.findByUserId(testUser.getId());

        // Then
        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(PaymentCard::getNumber)
                .containsExactlyInAnyOrder("4111111111111111", "4222222222222222");
    }

    @Test
    @DisplayName("Should find active cards by user id")
    void shouldFindActiveCardsByUserId() {
        // Given
        cardRepository.save(testCard);

        PaymentCard inactiveCard = PaymentCard.builder()
                .user(testUser)
                .number("4333333333333333")
                .holder("INACTIVE HOLDER")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(false)
                .build();
        cardRepository.save(inactiveCard);

        // When
        List<PaymentCard> activeCards = cardRepository.findByUserIdAndActiveTrue(testUser.getId());

        // Then
        assertThat(activeCards).hasSize(1);
        assertThat(activeCards.getFirst().getNumber()).isEqualTo("4111111111111111");
    }

    @Test
    @DisplayName("Should find card by number")
    void shouldFindCardByNumber() {
        // Given
        cardRepository.save(testCard);

        // When
        Optional<PaymentCard> found = cardRepository.findByNumber("4111111111111111");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getHolder()).isEqualTo("TEST HOLDER");
    }

    @Test
    @DisplayName("Should check if card number exists")
    void shouldCheckIfCardNumberExists() {
        // Given
        cardRepository.save(testCard);

        // When
        boolean exists = cardRepository.existsByNumber("4111111111111111");
        boolean notExists = cardRepository.existsByNumber("9999999999999999");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should count active cards by user id")
    void shouldCountActiveCardsByUserId() {
        // Given
        cardRepository.save(testCard);

        PaymentCard secondCard = PaymentCard.builder()
                .user(testUser)
                .number("4444444444444444")
                .holder("SECOND HOLDER")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(true)
                .build();
        cardRepository.save(secondCard);

        PaymentCard inactiveCard = PaymentCard.builder()
                .user(testUser)
                .number("4555555555555555")
                .holder("INACTIVE HOLDER")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(false)
                .build();
        cardRepository.save(inactiveCard);

        // When
        int activeCount = cardRepository.countActiveCardsByUserId(testUser.getId());

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update card active status")
    void shouldUpdateCardActiveStatus() {
        // Given
        PaymentCard savedCard = cardRepository.save(testCard);
        assertThat(savedCard.getActive()).isTrue();

        // When
        cardRepository.setActiveStatus(savedCard.getId(), false);
        cardRepository.flush();

        // Then
        Optional<PaymentCard> updated = cardRepository.findById(savedCard.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getActive()).isFalse();
    }

    @Test
    @DisplayName("Should deactivate all cards by user id")
    void shouldDeactivateAllCardsByUserId() {
        // Given
        cardRepository.save(testCard);

        PaymentCard secondCard = PaymentCard.builder()
                .user(testUser)
                .number("4666666666666666")
                .holder("SECOND HOLDER")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(true)
                .build();
        cardRepository.save(secondCard);

        // When
        cardRepository.deactivateAllByUserId(testUser.getId());
        cardRepository.flush();

        // Then
        List<PaymentCard> cards = cardRepository.findByUserId(testUser.getId());
        assertThat(cards).allMatch(card -> !card.getActive());
    }
}
