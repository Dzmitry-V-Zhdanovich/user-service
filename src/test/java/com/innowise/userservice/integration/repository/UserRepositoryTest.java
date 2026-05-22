package com.innowise.userservice.integration.repository;

import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("UserRepository Integration Tests")
public class UserRepositoryTest {

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
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Тест")
                .surname("Тестов")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("test@example.com")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should save and find user by id")
    void shouldSaveAndFindUserById() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        Optional<User> found = userRepository.findById(savedUser.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Тест");
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        userRepository.save(testUser);

        // When
        boolean exists = userRepository.existsByEmail("test@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find user with cards using JOIN FETCH")
    void shouldFindUserWithCards() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> found = userRepository.findById(testUser.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentCards()).isNotNull();
    }

    @Test
    @DisplayName("Should update user active status")
    void shouldUpdateUserActiveStatus() {
        // Given
        User savedUser = userRepository.save(testUser);
        assertThat(savedUser.getActive()).isTrue();

        // When
        userRepository.setActiveStatus(savedUser.getId(), false);
        userRepository.flush();

        // Then
        Optional<User> updated = userRepository.findById(savedUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getActive()).isFalse();
    }

    @Test
    @DisplayName("Should find users with pagination")
    void shouldFindUsersWithPagination() {
        // Given
        for (int i = 1; i <= 10; i++) {
            User user = User.builder()
                    .name("User" + i)
                    .surname("Test" + i)
                    .birthDate(LocalDate.now())
                    .email("user" + i + "@test.com")
                    .active(true)
                    .build();
            userRepository.save(user);
        }

        // When
        Page<User> page = userRepository.findAll(PageRequest.of(0, 3));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent().size()).isEqualTo(3);
    }
}
