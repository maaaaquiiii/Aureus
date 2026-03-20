package com.aureus.ledger.service;

import com.aureus.ledger.api.UserRequest;
import com.aureus.ledger.api.UserResponse;
import com.aureus.ledger.domain.User;
import com.aureus.ledger.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        if (request.currency() != null) {
            user.setCurrency(request.currency());
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCurrency()
        );
    }
}