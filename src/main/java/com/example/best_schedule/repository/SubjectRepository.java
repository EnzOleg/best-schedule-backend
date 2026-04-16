package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.teachers WHERE s.id = :id")
    Optional<Subject> findByIdWithTeachers(@Param("id") Long id);

    // Новый метод с полной загрузкой (преподаватели, кабинеты, часы)
    @Query("SELECT DISTINCT s FROM Subject s " +
           "LEFT JOIN FETCH s.teachers " +
           "LEFT JOIN FETCH s.allowedClassrooms ac " +
           "LEFT JOIN FETCH ac.classroom " +
           "LEFT JOIN FETCH s.groupHours gh " +
           "LEFT JOIN FETCH gh.group " +
           "WHERE s.id = :id")
    Optional<Subject> findByIdWithDetails(@Param("id") Long id);

    // Для списка всех предметов с полной загрузкой
    @Query("SELECT DISTINCT s FROM Subject s " +
           "LEFT JOIN FETCH s.teachers " +
           "LEFT JOIN FETCH s.allowedClassrooms ac " +
           "LEFT JOIN FETCH ac.classroom " +
           "LEFT JOIN FETCH s.groupHours gh " +
           "LEFT JOIN FETCH gh.group")
    List<Subject> findAllWithDetails();
}