package com.example.best_schedule.dto;

import lombok.Data;

@Data
public class RegisterInput {
    private String name;
    private String email;
    private String password;
    private String role;
}