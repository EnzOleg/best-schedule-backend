package com.example.best_schedule.dto;

import lombok.Data;

@Data
public class CreateClassroomInput {
    private String name;
    private Integer capacity;
    private String type;  
}