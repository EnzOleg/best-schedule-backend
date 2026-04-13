package com.example.best_schedule.repository;

import com.example.best_schedule.entity.SubjectTeacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectTeacherRepository extends JpaRepository<SubjectTeacher, Long> {
    void deleteBySubjectId(Long subjectId);
}