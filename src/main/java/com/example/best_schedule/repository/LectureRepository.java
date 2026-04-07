package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

        List<Lecture> findByTeacherIdAndDateBetween(Long teacherId, LocalDate start, LocalDate end);
        List<Lecture> findByGroupIdAndDateBetween(Long groupId, LocalDate start, LocalDate end);
        
}