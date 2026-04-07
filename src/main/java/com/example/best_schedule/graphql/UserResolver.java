package com.example.best_schedule.graphql;

import com.example.best_schedule.dto.AuthResponse;
import com.example.best_schedule.dto.RegisterInput;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserService userService;

    @QueryMapping
    public List<User> users() {
        return userService.findAll();
    }

    @MutationMapping
    public User register(@Argument RegisterInput input) {
        return userService.register(input);
    }

    @MutationMapping
    public AuthResponse login(@Argument String email,
                              @Argument String password) {
        return userService.login(email, password);
    }

    @MutationMapping
    public User updateUser(
            @Argument Long id,
            @Argument String name,
            @Argument String email,
            @Argument String role
    ) {
        return userService.updateUser(id, name, email, role);
    }

    @QueryMapping
    public User me() {
        return (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}