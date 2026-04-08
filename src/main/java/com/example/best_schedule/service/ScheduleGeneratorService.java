package com.example.best_schedule.service;

import com.example.best_schedule.entity.Group;
import com.example.best_schedule.entity.ScheduleItem;
import com.example.best_schedule.entity.Subject;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.repository.GroupRepository;
import com.example.best_schedule.repository.ScheduleRepository;
import com.example.best_schedule.repository.SubjectRepository;
import com.example.best_schedule.repository.UserRepository;
import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.CpSolverStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleGeneratorService {

    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleItemRepository;

    public ScheduleGeneratorService(GroupRepository groupRepository,
                                    SubjectRepository subjectRepository,
                                    UserRepository userRepository,
                                    ScheduleRepository scheduleItemRepository) {
        this.groupRepository = groupRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.scheduleItemRepository = scheduleItemRepository;
    }

    public List<ScheduleItem> generateSchedule(Long groupId, LocalDate startDate, int days) {
        Loader.loadNativeLibraries(); // важно для OR-Tools

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<Subject> subjects = subjectRepository.findAll();
        List<User> teachers = userRepository.findAll(); // можно фильтровать по роли TEACHER

        CpModel model = new CpModel();

        int numSubjects = subjects.size();
        int numDays = days;
        int numSlotsPerDay = 5; // например, 5 пар в день

        // создаём переменные: какой предмет в какой слот
        IntVar[][] scheduleVars = new IntVar[numDays][numSlotsPerDay];
        for (int d = 0; d < numDays; d++) {
            for (int s = 0; s < numSlotsPerDay; s++) {
                scheduleVars[d][s] = model.newIntVar(0, numSubjects - 1, "day_" + d + "_slot_" + s);
            }
        }

        // пример простого ограничения: не повторять один и тот же предмет в день
        for (int d = 0; d < numDays; d++) {
            model.addAllDifferent(scheduleVars[d]);
        }

        // создаём решатель
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        List<ScheduleItem> result = new ArrayList<>();

        if (status == CpSolverStatus.FEASIBLE || status == CpSolverStatus.OPTIMAL) {
            for (int d = 0; d < numDays; d++) {
                for (int s = 0; s < numSlotsPerDay; s++) {
                    int subjectIndex = (int) solver.value(scheduleVars[d][s]);
                    Subject subject = subjects.get(subjectIndex);
                    User teacher = teachers.get(subjectIndex % teachers.size()); // грубое распределение

                    ScheduleItem item = ScheduleItem.builder()
                            .group(group)
                            .subject(subject)
                            .teacher(teacher)
                            .date(startDate.plusDays(d))
                            .startTime(LocalTime.of(8 + s * 2, 0)) // 8:00, 10:00, 12:00...
                            .endTime(LocalTime.of(9 + s * 2, 20))  // 9:20, 11:20...
                            .classroom("101") // фиксированная аудитория
                            .build();

                    scheduleItemRepository.save(item);
                    result.add(item);
                }
            }
        }

        return result;
    }
}