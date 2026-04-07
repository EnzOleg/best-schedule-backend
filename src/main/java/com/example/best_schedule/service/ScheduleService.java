package com.example.best_schedule.service;

import com.example.best_schedule.dto.CreateScheduleInput;
import com.example.best_schedule.entity.*;
import com.example.best_schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

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

    public ScheduleItem createSchedule(CreateScheduleInput input) {

        Group group = groupRepository.findById(input.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Subject subject = subjectRepository.findById(input.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        User teacher = userRepository.findById(input.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (teacher.getRole() != Role.TEACHER) {
            throw new RuntimeException("User is not a teacher");
        }

        LocalDate date = LocalDate.parse(input.getDate());
        LocalTime startTime = LocalTime.parse(input.getStartTime());
        LocalTime endTime = LocalTime.parse(input.getEndTime());

        if (scheduleRepository
                .existsByGroupIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        group.getId(),
                        date,
                        endTime,
                        startTime
                )) {
            throw new RuntimeException("Group already has a class at this time");
        }

        if (scheduleRepository
                .existsByTeacherIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        teacher.getId(),
                        date,
                        endTime,
                        startTime
                )) {
            throw new RuntimeException("Teacher already has a class at this time");
        }

        ScheduleItem item = ScheduleItem.builder()
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .classroom(input.getClassroom())
                .group(group)
                .subject(subject)
                .teacher(teacher)
                .build();

        return scheduleRepository.save(item);
    }

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

    public List<ScheduleItem> getScheduleForWeek(
            Long groupId,
            String startDate,
            String endDate
    ) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        return scheduleRepository.findByGroupIdAndDateBetween(
                groupId,
                start,
                end
        );
    }

    public List<ScheduleItem> scheduleForMe(
            String startDate,
            String endDate
    ) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (currentUser.getRole() == Role.STUDENT) {

            Group group = groupRepository
                    .findByStudentsId(currentUser.getId())
                    .orElseThrow(() ->
                            new RuntimeException("Student has no group"));

            return scheduleRepository
                    .findByGroupIdAndDateBetween(
                            group.getId(),
                            start,
                            end
                    );
        }

        if (currentUser.getRole() == Role.TEACHER) {

            return scheduleRepository
                    .findByTeacherIdAndDateBetween(
                            currentUser.getId(),
                            start,
                            end
                    );
        }

        // ADMIN
        return scheduleRepository.findByDateBetween(start, end);
    }
    
    public ScheduleItem getById(Long id) {
    return scheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
}
}