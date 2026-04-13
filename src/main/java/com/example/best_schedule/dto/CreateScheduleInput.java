package com.example.best_schedule.dto;

import lombok.Data;

@Data
public class CreateScheduleInput {
    private String date;
    private String startTime;
    private String endTime;
    private String classroom;      // для обратной совместимости (строка)
    private Long classroomId;      // новый способ – ссылка на ID кабинета
    private Long groupId;
    private Long subjectId;
    private Long teacherId;
}