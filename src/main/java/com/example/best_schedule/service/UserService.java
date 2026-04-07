package com.example.best_schedule.service;

import com.example.best_schedule.dto.AuthResponse;
import com.example.best_schedule.dto.RegisterInput;
import com.example.best_schedule.entity.Role;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.exception.EmailAlreadyExistsException;
import com.example.best_schedule.exception.InvalidCredentialsException;
import com.example.best_schedule.repository.UserRepository;
import com.example.best_schedule.security.JwtUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public User getCurrentUser() {
    return (User) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    }

    public User register(RegisterInput input) {

        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .name(input.getName())
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .role(Role.valueOf(input.getRole()))
                .build();

        return userRepository.save(user);
    }

    public AuthResponse login(String email, String rawPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        return new AuthResponse(token, user);
    }

    public User updateUser(Long id, String name, String email, String role) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (name != null) user.setName(name);
    if (email != null) user.setEmail(email);
    if (role != null) user.setRole(Role.valueOf(role));

    return userRepository.save(user);
}

    public List<User> findAll() {
        return userRepository.findAll();
    }

}