package com.example.best_schedule.service;

import com.example.best_schedule.entity.*;
import com.example.best_schedule.repository.*;
import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScheduleGeneratorService {

    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final ScheduleRepository scheduleRepository;
    private final SubjectGroupHoursRepository hoursRepository;
    private final UserRepository userRepository;

    // Конфигурация временных слотов
    private static final int SLOTS_PER_DAY = 5;
    private static final LocalTime[] SLOT_START_TIMES = {
        LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
        LocalTime.of(12, 40), LocalTime.of(14, 10)
    };
    private static final LocalTime[] SLOT_END_TIMES = {
        LocalTime.of(9, 20), LocalTime.of(10, 50), LocalTime.of(12, 20),
        LocalTime.of(14, 0), LocalTime.of(15, 30)
    };

    @Transactional
    public List<ScheduleItem> generateSchedule(Long groupId, LocalDate startDate, int days) {
        Loader.loadNativeLibraries();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        int studentCount = group.getStudents().size();

        // Получаем список предметов для группы с часами
        List<SubjectGroupHours> subjectHours = hoursRepository.findByGroupId(groupId);
        if (subjectHours.isEmpty()) {
            throw new RuntimeException("No subjects with hours defined for this group");
        }

        // Общее количество пар
        int totalPairsNeeded = subjectHours.stream()
                .mapToInt(sh -> sh.getHours())
                .sum();
        int totalSlots = days * SLOTS_PER_DAY;
        if (totalPairsNeeded > totalSlots) {
            throw new RuntimeException("Not enough slots (" + totalSlots + ") to place " + totalPairsNeeded + " pairs");
        }

        // Собираем доступных преподавателей и кабинеты для каждого предмета
        Map<Long, List<User>> subjectTeachers = new HashMap<>();
        Map<Long, List<Classroom>> subjectClassrooms = new HashMap<>();
        for (SubjectGroupHours sgh : subjectHours) {
            Subject subject = sgh.getSubject();
            // Преподаватели предмета
            List<User> teachers = subject.getTeachers().stream()
                    .map(SubjectTeacher::getTeacher)
                    .toList();
            if (teachers.isEmpty()) {
                throw new RuntimeException("No teachers assigned to subject: " + subject.getName());
            }
            subjectTeachers.put(subject.getId(), teachers);

            // Доступные кабинеты для предмета с учётом вместимости
            List<Classroom> classrooms = subject.getAllowedClassrooms().stream()
                    .map(SubjectClassroom::getClassroom)
                    .filter(c -> c.getCapacity() == null || c.getCapacity() >= studentCount)
                    .toList();
            if (classrooms.isEmpty()) {
                throw new RuntimeException("No suitable classrooms for subject: " + subject.getName());
            }
            subjectClassrooms.put(subject.getId(), classrooms);
        }

        // Индексы
        List<Long> subjectIds = subjectHours.stream().map(sh -> sh.getSubject().getId()).toList();
        List<Long> teacherIds = subjectTeachers.values().stream().flatMap(List::stream).map(User::getId).distinct().toList();
        List<Long> classroomIds = classroomRepository.findAll().stream().map(Classroom::getId).toList();

        // Модель OR-Tools
        CpModel model = new CpModel();

        // Переменные: BoolVar для каждого выбора
        BoolVar[][][] subjectVar = new BoolVar[days][SLOTS_PER_DAY][subjectIds.size()];
        BoolVar[][][] teacherVar = new BoolVar[days][SLOTS_PER_DAY][teacherIds.size()];
        BoolVar[][][] classroomVar = new BoolVar[days][SLOTS_PER_DAY][classroomIds.size()];

        for (int d = 0; d < days; d++) {
            for (int s = 0; s < SLOTS_PER_DAY; s++) {
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    subjectVar[d][s][subIdx] = model.newBoolVar("sub_" + d + "_" + s + "_" + subIdx);
                }
                // Ровно один предмет на слот
                model.addExactlyOne(subjectVar[d][s]);

                for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                    teacherVar[d][s][tIdx] = model.newBoolVar("teacher_" + d + "_" + s + "_" + tIdx);
                }
                model.addExactlyOne(teacherVar[d][s]);

                for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                    classroomVar[d][s][cIdx] = model.newBoolVar("classroom_" + d + "_" + s + "_" + cIdx);
                }
                model.addExactlyOne(classroomVar[d][s]);
            }
        }

        // Ограничение: количество слотов для каждого предмета = заданное количество часов
        for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
            Long subjectId = subjectIds.get(subIdx);
            int requiredHours = subjectHours.stream()
                    .filter(sh -> sh.getSubject().getId().equals(subjectId))
                    .findFirst().get().getHours();
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int d = 0; d < days; d++) {
                for (int s = 0; s < SLOTS_PER_DAY; s++) {
                    sum.add(subjectVar[d][s][subIdx]);
                }
            }
            model.addEquality(sum, requiredHours);
        }

        // Связь предмет-преподаватель: если выбран предмет, то преподаватель должен быть из разрешённых
        for (int d = 0; d < days; d++) {
            for (int s = 0; s < SLOTS_PER_DAY; s++) {
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    Long subjectId = subjectIds.get(subIdx);
                    List<Long> allowedTeacherIds = subjectTeachers.get(subjectId).stream().map(User::getId).toList();
                    for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                        Long teacherId = teacherIds.get(tIdx);
                        if (!allowedTeacherIds.contains(teacherId)) {
                            // Если предмет выбран, то этот преподаватель не может быть выбран
                            model.addImplication(subjectVar[d][s][subIdx], teacherVar[d][s][tIdx].not());
                        }
                    }
                }
            }
        }

        // Связь предмет-кабинет: аналогично
        for (int d = 0; d < days; d++) {
            for (int s = 0; s < SLOTS_PER_DAY; s++) {
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    Long subjectId = subjectIds.get(subIdx);
                    List<Long> allowedClassroomIds = subjectClassrooms.get(subjectId).stream().map(Classroom::getId).toList();
                    for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                        Long classroomId = classroomIds.get(cIdx);
                        if (!allowedClassroomIds.contains(classroomId)) {
                            model.addImplication(subjectVar[d][s][subIdx], classroomVar[d][s][cIdx].not());
                        }
                    }
                }
            }
        }

        // Решаем
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if (status != CpSolverStatus.FEASIBLE && status != CpSolverStatus.OPTIMAL) {
            throw new RuntimeException("No feasible schedule found");
        }

        // Формируем результат
        List<ScheduleItem> result = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            LocalDate date = startDate.plusDays(d);
            for (int s = 0; s < SLOTS_PER_DAY; s++) {
                // Находим выбранный предмет
                Long selectedSubjectId = null;
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    if (solver.value(subjectVar[d][s][subIdx]) == 1) {
                        selectedSubjectId = subjectIds.get(subIdx);
                        break;
                    }
                }
                if (selectedSubjectId == null) continue;

                Subject subject = subjectRepository.findById(selectedSubjectId).orElseThrow();

                // Находим выбранного преподавателя
                Long selectedTeacherId = null;
                for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                    if (solver.value(teacherVar[d][s][tIdx]) == 1) {
                        selectedTeacherId = teacherIds.get(tIdx);
                        break;
                    }
                }
                User teacher = userRepository.findById(selectedTeacherId).orElseThrow();

                // Находим выбранный кабинет
                Long selectedClassroomId = null;
                for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                    if (solver.value(classroomVar[d][s][cIdx]) == 1) {
                        selectedClassroomId = classroomIds.get(cIdx);
                        break;
                    }
                }
                Classroom classroom = classroomRepository.findById(selectedClassroomId).orElseThrow();

                ScheduleItem item = ScheduleItem.builder()
                        .date(date)
                        .startTime(SLOT_START_TIMES[s])
                        .endTime(SLOT_END_TIMES[s])
                        .classroom(classroom)
                        .group(group)
                        .subject(subject)
                        .teacher(teacher)
                        .build();
                result.add(item);
                scheduleRepository.save(item);
            }
        }

        return result;
    }
}