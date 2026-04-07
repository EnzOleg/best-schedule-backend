package com.example.best_schedule.dto;

import lombok.Data;

@Data
public class CreateHomeworkInput {
    private String text;
    private String date;
    private Long scheduleItemId;
}