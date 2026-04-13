package com.example.best_schedule.dto;

import com.example.best_schedule.entity.ClassroomType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateSubjectInput {
    private String name;
    private ClassroomType requiredClassroomType; // опционально
    private List<Long> allowedClassroomIds;      // список ID кабинетов
    private List<GroupHoursInput> groupHours;    // группы и часы
    private List<Long> teacherIds;
}
