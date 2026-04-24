package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findByTeacherIdAndDateBetween(Long teacherId, LocalDate start, LocalDate end);

    List<Lecture> findByGroupIdAndDateBetween(Long groupId, LocalDate start, LocalDate end);

    @Query("""
        select l from Lecture l
        join l.group g
        join g.students s
        where s.id = :userId
        and l.date between :start and :end
    """)
    List<Lecture> findLecturesForStudent(Long userId, LocalDate start, LocalDate end);

    List<Lecture> findByDateBetween(LocalDate start, LocalDate end); 
}