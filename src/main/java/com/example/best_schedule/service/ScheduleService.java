package com.example.best_schedule.service;

import com.example.best_schedule.dto.CreateScheduleInput;
import com.example.best_schedule.entity.*;
import com.example.best_schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    @Transactional
    public ScheduleItem createSchedule(CreateScheduleInput input) {
        Group group = groupRepository.findById(input.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Subject subject = subjectRepository.findById(input.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        User teacher = userRepository.findById(input.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        LocalDate date = LocalDate.parse(input.getDate());
        LocalTime startTime = LocalTime.parse(input.getStartTime());
        LocalTime endTime = LocalTime.parse(input.getEndTime());

        // Получаем кабинет через отдельный метод (classroom не меняется после присвоения)
        Classroom classroom = resolveClassroom(input);

        // Проверка вместимости
        int studentCount = group.getStudents().size();
        if (classroom.getCapacity() != null && studentCount > classroom.getCapacity()) {
            throw new RuntimeException("Classroom capacity (" + classroom.getCapacity() +
                    ") is less than number of students in group (" + studentCount + ")");
        }

        // Проверка типа кабинета
        if (subject.getRequiredClassroomType() != null && classroom.getType() != subject.getRequiredClassroomType()) {
            throw new RuntimeException("Classroom type must be " + subject.getRequiredClassroomType() +
                    " for subject " + subject.getName());
        }

        // Проверка, разрешён ли кабинет для предмета
        boolean allowed = subject.getAllowedClassrooms().stream()
                .anyMatch(sc -> sc.getClassroom().getId().equals(classroom.getId()));
        if (!allowed) {
            throw new RuntimeException("Classroom " + classroom.getName() + " is not allowed for subject " + subject.getName());
        }

        // Проверка пересечений
        if (scheduleRepository.existsByGroupIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                group.getId(), date, endTime, startTime)) {
            throw new RuntimeException("Group already has a class at this time");
        }

        if (scheduleRepository.existsByTeacherIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                teacher.getId(), date, endTime, startTime)) {
            throw new RuntimeException("Teacher already has a class at this time");
        }

        if (scheduleRepository.existsByClassroomIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                classroom.getId(), date, endTime, startTime)) {
            throw new RuntimeException("Classroom is already occupied at this time");
        }

        ScheduleItem item = ScheduleItem.builder()
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .classroom(classroom)
                .group(group)
                .subject(subject)
                .teacher(teacher)
                .build();

        return scheduleRepository.save(item);
    }

    // Вспомогательный метод для получения кабинета
    private Classroom resolveClassroom(CreateScheduleInput input) {
        if (input.getClassroomId() != null) {
            return classroomRepository.findById(input.getClassroomId())
                    .orElseThrow(() -> new RuntimeException("Classroom not found by id"));
        } else if (input.getClassroom() != null && !input.getClassroom().isBlank()) {
            return classroomRepository.findAll().stream()
                    .filter(c -> c.getName().equals(input.getClassroom()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Classroom not found by name: " + input.getClassroom()));
        } else {
            throw new RuntimeException("Classroom is required");
        }
    }

    @Transactional
    public boolean deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new RuntimeException("Schedule not found");
        }
        scheduleRepository.deleteById(id);
        return true;
    }

    public List<ScheduleItem> getByGroup(Long groupId) {
        return scheduleRepository.findByGroupId(groupId);
    }

    public List<ScheduleItem> getScheduleForWeek(Long groupId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return scheduleRepository.findByGroupIdAndDateBetween(groupId, start, end);
    }

    public List<ScheduleItem> scheduleForMe(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (currentUser.getRole() == Role.STUDENT) {
            Group group = groupRepository.findByStudentsId(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Student has no group"));
            return scheduleRepository.findByGroupIdAndDateBetween(group.getId(), start, end);
        }

        if (currentUser.getRole() == Role.TEACHER) {
            return scheduleRepository.findByTeacherIdAndDateBetween(currentUser.getId(), start, end);
        }

        // ADMIN
        return scheduleRepository.findByDateBetween(start, end);
    }

    public ScheduleItem getById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }
}