package com.example.best_schedule.repository;

import com.example.best_schedule.entity.SubjectGroupHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectGroupHoursRepository extends JpaRepository<SubjectGroupHours, Long> {
    void deleteBySubjectId(Long subjectId);
    List<SubjectGroupHours> findByGroupId(Long groupId);
}