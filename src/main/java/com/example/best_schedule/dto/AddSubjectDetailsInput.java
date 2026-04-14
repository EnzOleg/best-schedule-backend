package com.example.best_schedule.dto;

import lombok.Data;
import java.util.List;

@Data
public class AddSubjectDetailsInput {
    private Long subjectId;
    private List<Long> allowedClassroomIds;
    private List<GroupHoursInput> groupHours;
    private List<Long> teacherIds;
}