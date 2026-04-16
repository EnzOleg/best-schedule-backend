package com.example.best_schedule.repository;

import com.example.best_schedule.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleItem, Long> {

    List<ScheduleItem> findByGroupId(Long groupId);

    List<ScheduleItem> findByDateBetween(LocalDate start, LocalDate end);

    List<ScheduleItem> findByGroupIdAndDateBetween(
            Long groupId,
            LocalDate start,
            LocalDate end
    );

    List<ScheduleItem> findByTeacherIdAndDateBetween(
            Long teacherId,
            LocalDate start,
            LocalDate end
    );

    boolean existsByGroupIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Long groupId,
            LocalDate date,
            LocalTime endTime,
            LocalTime startTime
    );

    boolean existsByTeacherIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Long teacherId,
            LocalDate date,
            LocalTime endTime,
            LocalTime startTime
    );

    boolean existsByClassroomIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
    Long classroomId, LocalDate date, LocalTime endTime, LocalTime startTime);
}