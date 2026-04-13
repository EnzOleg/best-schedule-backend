package com.example.best_schedule.repository;

import com.example.best_schedule.entity.SubjectClassroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectClassroomRepository extends JpaRepository<SubjectClassroom, Long> {
    void deleteBySubjectId(Long subjectId);
}