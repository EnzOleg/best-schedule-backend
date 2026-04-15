package com.example.best_schedule.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupScheduleInput {
    private Long groupId;
    private List<SubjectHoursInput> subjectHours;
}