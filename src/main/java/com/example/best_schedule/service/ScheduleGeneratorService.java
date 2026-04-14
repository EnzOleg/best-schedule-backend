package com.example.best_schedule.service;

import com.example.best_schedule.entity.*;
import com.example.best_schedule.repository.*;
import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
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
    private final SubjectClassroomRepository subjectClassroomRepository;

    private static final int SLOTS_PER_DAY = 5;
    private static final LocalTime[] SLOT_START_TIMES = {
        LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
        LocalTime.of(12, 40), LocalTime.of(14, 10)
    };
    private static final LocalTime[] SLOT_END_TIMES = {
        LocalTime.of(9, 20), LocalTime.of(10, 50), LocalTime.of(12, 20),
        LocalTime.of(14, 0), LocalTime.of(15, 30)
    };

    static {
        Loader.loadNativeLibraries(); // однократная загрузка
    }

    @Transactional
    public List<ScheduleItem> generateSchedule(Long groupId, LocalDate startDate, int days) {
        // 1. Получаем группу со студентами
        Group group = groupRepository.findByIdWithStudents(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        int studentCount = group.getStudents().size();

        // 2. Часы предметов для этой группы
        List<SubjectGroupHours> subjectHours = hoursRepository.findByGroupId(groupId);
        if (subjectHours.isEmpty()) {
            throw new RuntimeException("No subjects with hours defined for this group");
        }

        int totalPairsNeeded = subjectHours.stream().mapToInt(SubjectGroupHours::getHours).sum();

        // 3. Фильтруем дни: пропускаем воскресенья, считаем только рабочие слоты
        List<LocalDate> workingDates = new ArrayList<>();
        LocalDate current = startDate;
        int daysAdded = 0;
        while (daysAdded < days) {
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDates.add(current);
                daysAdded++;
            }
            current = current.plusDays(1);
        }
        int actualDays = workingDates.size();
        int totalSlots = actualDays * SLOTS_PER_DAY;

        if (totalPairsNeeded > totalSlots) {
            throw new RuntimeException("Not enough working slots (" + totalSlots + ") to place " + totalPairsNeeded + " pairs");
        }

        // 4. Загружаем преподавателей и кабинеты для каждого предмета
        Map<Long, List<User>> subjectTeachers = new HashMap<>();
        Map<Long, List<Classroom>> subjectClassrooms = new HashMap<>();

        for (SubjectGroupHours sgh : subjectHours) {
            Long subjectId = sgh.getSubject().getId();
            // Загружаем предмет с преподавателями
            Subject subject = subjectRepository.findByIdWithTeachers(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));

            List<User> teachers = subject.getTeachers().stream()
                    .map(SubjectTeacher::getTeacher)
                    .toList();
            if (teachers.isEmpty()) {
                throw new RuntimeException("No teachers assigned to subject: " + subject.getName());
            }
            subjectTeachers.put(subjectId, teachers);

            // Загружаем разрешённые кабинеты с учётом вместимости
            List<SubjectClassroom> allowedLinks = subjectClassroomRepository
                    .findAllBySubjectIdWithClassroom(subjectId);
            List<Classroom> suitable = allowedLinks.stream()
                    .map(SubjectClassroom::getClassroom)
                    .filter(c -> c.getCapacity() == null || c.getCapacity() >= studentCount)
                    .toList();
            if (suitable.isEmpty()) {
                throw new RuntimeException("No suitable classrooms for subject: " + subject.getName());
            }
            subjectClassrooms.put(subjectId, suitable);
        }

        // 5. Подготовка данных для модели
        List<Long> subjectIds = subjectHours.stream().map(sh -> sh.getSubject().getId()).toList();
        List<Long> teacherIds = subjectTeachers.values().stream()
                .flatMap(List::stream).map(User::getId).distinct().toList();
        List<Long> classroomIds = classroomRepository.findAll().stream().map(Classroom::getId).toList();

        // 6. Модель OR-Tools
        CpModel model = new CpModel();

        int D = actualDays;
        int S = SLOTS_PER_DAY;
        BoolVar[][][] subjectVar = new BoolVar[D][S][subjectIds.size()];
        BoolVar[][][] teacherVar = new BoolVar[D][S][teacherIds.size()];
        BoolVar[][][] classroomVar = new BoolVar[D][S][classroomIds.size()];

        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    subjectVar[d][s][subIdx] = model.newBoolVar("sub_" + d + "_" + s + "_" + subIdx);
                }
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

        // Ограничение по часам
        for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
            Long subjectId = subjectIds.get(subIdx);
            int requiredHours = subjectHours.stream()
                    .filter(sh -> sh.getSubject().getId().equals(subjectId))
                    .findFirst().get().getHours();
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int d = 0; d < D; d++) {
                for (int s = 0; s < S; s++) {
                    sum.add(subjectVar[d][s][subIdx]);
                }
            }
            model.addEquality(sum, requiredHours);
        }

        // Связь предмет-преподаватель
        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    Long subjectId = subjectIds.get(subIdx);
                    List<Long> allowedTeacherIds = subjectTeachers.get(subjectId).stream()
                            .map(User::getId).toList();
                    for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                        Long teacherId = teacherIds.get(tIdx);
                        if (!allowedTeacherIds.contains(teacherId)) {
                            model.addImplication(subjectVar[d][s][subIdx], teacherVar[d][s][tIdx].not());
                        }
                    }
                }
            }
        }

        // Связь предмет-кабинет
        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    Long subjectId = subjectIds.get(subIdx);
                    List<Long> allowedClassroomIds = subjectClassrooms.get(subjectId).stream()
                            .map(Classroom::getId).toList();
                    for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                        Long classroomId = classroomIds.get(cIdx);
                        if (!allowedClassroomIds.contains(classroomId)) {
                            model.addImplication(subjectVar[d][s][subIdx], classroomVar[d][s][cIdx].not());
                        }
                    }
                }
            }
        }

        // Решение
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if (status != CpSolverStatus.FEASIBLE && status != CpSolverStatus.OPTIMAL) {
            throw new RuntimeException("No feasible schedule found");
        }

        // 7. Сохранение результата
        List<ScheduleItem> result = new ArrayList<>();
        for (int d = 0; d < D; d++) {
            LocalDate date = workingDates.get(d);
            for (int s = 0; s < S; s++) {
                Long selectedSubjectId = null;
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    if (solver.value(subjectVar[d][s][subIdx]) == 1) {
                        selectedSubjectId = subjectIds.get(subIdx);
                        break;
                    }
                }
                if (selectedSubjectId == null) continue;

                Subject subject = subjectRepository.findById(selectedSubjectId).orElseThrow();

                Long selectedTeacherId = null;
                for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                    if (solver.value(teacherVar[d][s][tIdx]) == 1) {
                        selectedTeacherId = teacherIds.get(tIdx);
                        break;
                    }
                }
                User teacher = userRepository.findById(selectedTeacherId).orElseThrow();

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