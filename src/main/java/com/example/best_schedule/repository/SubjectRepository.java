package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.teachers WHERE s.id = :id")
    Optional<Subject> findByIdWithTeachers(@Param("id") Long id);

    // Один предмет с деталями
    @Query("""
        SELECT DISTINCT s FROM Subject s
        LEFT JOIN FETCH s.teachers st
        LEFT JOIN FETCH s.allowedClassrooms ac
        LEFT JOIN FETCH s.groupHours gh
        LEFT JOIN FETCH gh.group
        WHERE s.id = :id
    """)
    Optional<Subject> findByIdWithDetails(@Param("id") Long id);

    // Все предметы с деталями
    @Query("""
        SELECT DISTINCT s FROM Subject s
        LEFT JOIN FETCH s.teachers st
        LEFT JOIN FETCH s.allowedClassrooms ac
        LEFT JOIN FETCH ac.classroom
        LEFT JOIN FETCH s.groupHours gh
        LEFT JOIN FETCH gh.group
    """)
    List<Subject> findAllWithDetails();

       @Query("""
       SELECT DISTINCT s FROM Subject s
       LEFT JOIN FETCH s.teachers st
       LEFT JOIN FETCH st.teacher
       LEFT JOIN FETCH s.allowedClassrooms ac
       LEFT JOIN FETCH ac.classroom
       LEFT JOIN FETCH s.groupHours gh
       LEFT JOIN FETCH gh.group
       """)
       List<Subject> findAllWithDetailsForGenerator();

    // Если вдруг нужен только предмет без лишнего мусора
    @Query("""
        SELECT s FROM Subject s
        WHERE s.id = :id
    """)
    Optional<Subject> findByIdPlain(@Param("id") Long id);
}