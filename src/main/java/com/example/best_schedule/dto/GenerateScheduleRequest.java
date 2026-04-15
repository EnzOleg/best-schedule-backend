package com.example.best_schedule.dto;

import lombok.Data;
import java.util.List;
import java.time.LocalDate;

@Data
public class GenerateScheduleRequest {
    private List<GroupScheduleInput> groups;
    private LocalDate startDate;
    private int days;
}