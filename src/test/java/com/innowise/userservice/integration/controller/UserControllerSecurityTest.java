package com.innowise.userservice.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.innowise.userservice.config.SecurityConfig;
import com.innowise.userservice.controller.UserController;
import com.innowise.userservice.dto.request.CreateUserRequest;
import com.innowise.userservice.dto.response.UserResponse;
import com.innowise.userservice.service.JwtService;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
public class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private final UUID testUserId = UUID.randomUUID();

    @Test
    @DisplayName("Should return 401 Unauthorized when requesting user endpoints without a JWT token")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + testUserId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow AuthService (ROLE_SERVICE) to create user profiles")
    void shouldAllowServiceToCreateUser() throws Exception {
        UsernamePasswordAuthenticationToken serviceAuth = new UsernamePasswordAuthenticationToken(
                "AuthService",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );

        UserResponse mockResponse = org.mockito.Mockito.mock(UserResponse.class);
        org.mockito.Mockito.when(userService.createUser(Mockito.any(CreateUserRequest.class)))
                .thenReturn(mockResponse);

        CreateUserRequest myRequest = new CreateUserRequest();
        myRequest.setName("John");
        myRequest.setSurname("Doe");
        myRequest.setEmail("john.doe@example.com");
        myRequest.setBirthDate(java.time.LocalDate.of(2000, 1, 1));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String jsonRequest = mapper.writeValueAsString(myRequest);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .with(authentication(serviceAuth)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a regular USER tries to use the POST registration endpoint directly")
    void shouldBlockUserFromDirectRegistrationEndpoint() throws Exception {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                "user-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(userAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow ADMIN to view the full list of users")
    void shouldAllowAdminToGetAllUsers() throws Exception {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/api/v1/users")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a regular USER tries to fetch all users")
    void shouldBlockUserFromGetAllUsers() throws Exception {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                "user-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(get("/api/v1/users")
                        .with(authentication(userAuth)))
                .andExpect(status().isForbidden());
    }
}
