package com.example.best_schedule.dto;

import lombok.Data;

@Data
public class CreateLectureInput {

    private String date;
    private String startTime;
    private String endTime;
    private String classroom;

    private Long groupId;
    private Long subjectId;
    private Long teacherId;

    // новые поля для фронта
    private String title;
    private String text;
}