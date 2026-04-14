package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.teachers WHERE s.id = :id")
    Optional<Subject> findByIdWithTeachers(@Param("id") Long id);
}