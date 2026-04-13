package com.example.best_schedule.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GenerateScheduleInput {
    private Long groupId;
    private LocalDate startDate;
    private int days;          // количество дней для генерации
}