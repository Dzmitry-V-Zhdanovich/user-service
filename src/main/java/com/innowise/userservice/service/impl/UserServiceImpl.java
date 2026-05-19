package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.request.CreateUserRequest;
import com.innowise.userservice.dto.request.UpdateUserRequest;
import com.innowise.userservice.dto.response.UserResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateResourceException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import com.innowise.userservice.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Создание пользователя с email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Пользователь", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("Создан пользователь с ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        log.debug("Поиск пользователя по ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        return userMapper.toResponse(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(String name, String surname, Boolean active, Pageable pageable) {
        log.debug("Получение пользователей с фильтрацией: name={}, surname={}, active={}", name, surname, active);

        Specification<User> spec = UserSpecification.fullFilter(name, surname, active);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.debug("Обновление пользователя с ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        userMapper.updateEntityFromRequest(request, user);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Пользователь", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        User updatedUser = userRepository.save(user);
        log.info("Обновлён пользователь с ID: {}", id);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void setUserActiveStatus(UUID id, Boolean active) {
        log.debug("Изменение статуса пользователя {} на active={}", id, active);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь", id);
        }

        userRepository.setActiveStatus(id, active);
        log.info("Статус пользователя {} изменён на active={}", id, active);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.debug("Удаление пользователя с ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь", id);
        }

        userRepository.deleteById(id);
        log.info("Удалён пользователь с ID: {}", id);
    }
}
