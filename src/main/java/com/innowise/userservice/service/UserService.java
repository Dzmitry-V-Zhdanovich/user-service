package com.innowise.userservice.service;

import com.innowise.userservice.dto.request.CreateUserRequest;
import com.innowise.userservice.dto.request.UpdateUserRequest;
import com.innowise.userservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    Page<UserResponse> getAllUsers(String name, String surname, Boolean active, Pageable pageable);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void setUserActiveStatus(UUID id, Boolean active);

    void deleteUser(UUID id);
}
