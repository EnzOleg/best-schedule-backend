package com.example.best_schedule.repository;

import com.example.best_schedule.entity.SubjectClassroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SubjectClassroomRepository extends JpaRepository<SubjectClassroom, Long> {
    void deleteBySubjectId(Long subjectId);

    @Query("SELECT sc FROM SubjectClassroom sc JOIN FETCH sc.classroom WHERE sc.subject.id = :subjectId")
    List<SubjectClassroom> findAllBySubjectIdWithClassroom(@Param("subjectId") Long subjectId);
}