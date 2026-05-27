package com.innowise.userservice.integration.service;

import com.innowise.userservice.dto.request.CreateUserRequest;
import com.innowise.userservice.dto.request.UpdateUserRequest;
import com.innowise.userservice.dto.response.UserResponse;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import com.redis.testcontainers.RedisContainer;
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
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@DisplayName("UserService Integration Tests")
public class UserServiceIntegrationTest {

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
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private CreateUserRequest createRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        createRequest = CreateUserRequest.builder()
                .name("Иван")
                .surname("Петров")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan.test@example.com")
                .build();
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // When
        UserResponse response = userService.createUser(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Иван");
        assertThat(response.getEmail()).isEqualTo("ivan.test@example.com");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();

        // Verify in database
        assertThat(userRepository.existsByEmail("ivan.test@example.com")).isTrue();
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {
        // Given
        UserResponse created = userService.createUser(createRequest);

        // When
        UserResponse found = userService.getUserById(created.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Иван");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        // Given
        UserResponse created = userService.createUser(createRequest);

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .name("Иван-Обновлённый")
                .surname("Петров-Обновлённый")
                .build();

        // When
        UserResponse updated = userService.updateUser(created.getId(), updateRequest);

        // Then
        assertThat(updated.getName()).isEqualTo("Иван-Обновлённый");
        assertThat(updated.getSurname()).isEqualTo("Петров-Обновлённый");

        // Verify in database
        UserResponse afterUpdate = userService.getUserById(created.getId());
        assertThat(afterUpdate.getName()).isEqualTo("Иван-Обновлённый");
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void shouldDeactivateUserSuccessfully() {
        // Given
        UserResponse created = userService.createUser(createRequest);
        assertThat(created.getActive()).isTrue();

        // When
        userService.setUserActiveStatus(created.getId(), false);

        // Then
        UserResponse deactivated = userService.getUserById(created.getId());
        assertThat(deactivated.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void shouldGetAllUsersWithPagination() {
        // Given
        for (int i = 1; i <= 5; i++) {
            CreateUserRequest request = CreateUserRequest.builder()
                    .name("User" + i)
                    .surname("Test" + i)
                    .birthDate(LocalDate.now())
                    .email("user" + i + "@test.com")
                    .build();
            userService.createUser(request);
        }

        // When
        Page<UserResponse> page = userService.getAllUsers(null, null, null, PageRequest.of(0, 2));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent().size()).isEqualTo(2);
    }
}
