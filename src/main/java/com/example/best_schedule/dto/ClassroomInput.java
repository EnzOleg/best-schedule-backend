package com.example.best_schedule.dto;

import com.example.best_schedule.entity.ClassroomType;
import lombok.Data;

@Data
public class ClassroomInput {
    private String name;
    private Integer capacity;
    private ClassroomType type;
}