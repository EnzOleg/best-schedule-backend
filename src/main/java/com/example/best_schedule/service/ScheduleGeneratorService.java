package com.example.best_schedule.service;

import com.example.best_schedule.dto.GroupScheduleInput;
import com.example.best_schedule.dto.SubjectHoursInput;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleGeneratorService {

    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final ScheduleRepository scheduleRepository;
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
        Loader.loadNativeLibraries();
    }

    @Transactional
    public List<ScheduleItem> generateSchedule(GroupScheduleInput groupInput, LocalDate startDate, int days) {
        // 1. Группа и количество студентов
        Group group = groupRepository.findByIdWithStudents(groupInput.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        int studentCount = group.getStudents().size();

        // 2. Часы по предметам
        List<SubjectHoursInput> subjectHoursInput = groupInput.getSubjectHours();
        if (subjectHoursInput.isEmpty()) {
            throw new RuntimeException("No subjects with hours provided");
        }
        Map<Long, Integer> hoursPerSubject = subjectHoursInput.stream()
                .collect(Collectors.toMap(SubjectHoursInput::getSubjectId, SubjectHoursInput::getHours));
        int totalPairsNeeded = hoursPerSubject.values().stream().mapToInt(Integer::intValue).sum();

        // 3. Рабочие дни (без воскресений)
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
            throw new RuntimeException("Not enough slots");
        }

        // 4. Данные о преподавателях и кабинетах
        Map<Long, List<User>> subjectTeachers = new HashMap<>();
        Map<Long, List<Classroom>> subjectClassrooms = new HashMap<>();
        for (Long subjectId : hoursPerSubject.keySet()) {
            Subject subject = subjectRepository.findByIdWithTeachers(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));
            List<User> teachers = subject.getTeachers().stream()
                    .map(SubjectTeacher::getTeacher)
                    .distinct()
                    .collect(Collectors.toList());
            if (teachers.isEmpty()) throw new RuntimeException("No teachers for subject " + subject.getName());
            subjectTeachers.put(subjectId, teachers);

            List<SubjectClassroom> allowedLinks = subjectClassroomRepository.findAllBySubjectIdWithClassroom(subjectId);
            List<Classroom> suitable = allowedLinks.stream()
                    .map(SubjectClassroom::getClassroom)
                    .distinct()
                    .filter(c -> c.getCapacity() == null || c.getCapacity() >= studentCount)
                    .collect(Collectors.toList());
            if (suitable.isEmpty()) throw new RuntimeException("No suitable classrooms for subject " + subject.getName());
            subjectClassrooms.put(subjectId, suitable);
        }

        List<Long> subjectIds = new ArrayList<>(hoursPerSubject.keySet());
        List<Long> teacherIds = subjectTeachers.values().stream()
                .flatMap(List::stream).map(User::getId).distinct().collect(Collectors.toList());
        List<Long> classroomIds = classroomRepository.findAll().stream()
                .map(Classroom::getId).collect(Collectors.toList());

        // 5. Модель (без hasLesson, с ограничениями на уникальность преподавателя/кабинета)
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
                // Не более одного предмета на слот
                LinearExprBuilder sumSub = LinearExpr.newBuilder();
                for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
                    sumSub.add(subjectVar[d][s][subIdx]);
                }
                model.addLessOrEqual(sumSub, 1);

                // Преподаватель: ровно один, если есть предмет, иначе 0
                for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                    teacherVar[d][s][tIdx] = model.newBoolVar("teacher_" + d + "_" + s + "_" + tIdx);
                }
                LinearExprBuilder sumTeacher = LinearExpr.newBuilder();
                for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                    sumTeacher.add(teacherVar[d][s][tIdx]);
                }
                // Сумма выбранных преподавателей = сумме выбранных предметов (0 или 1)
                model.addEquality(sumTeacher, sumSub);

                // Кабинет: аналогично
                for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                    classroomVar[d][s][cIdx] = model.newBoolVar("classroom_" + d + "_" + s + "_" + cIdx);
                }
                LinearExprBuilder sumClass = LinearExpr.newBuilder();
                for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                    sumClass.add(classroomVar[d][s][cIdx]);
                }
                model.addEquality(sumClass, sumSub);
            }
        }

        // Ограничение по часам
        for (int subIdx = 0; subIdx < subjectIds.size(); subIdx++) {
            Long subjectId = subjectIds.get(subIdx);
            int requiredHours = hoursPerSubject.get(subjectId);
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
                    List<Long> allowedTeacherIds = subjectTeachers.get(subjectId).stream().map(User::getId).collect(Collectors.toList());
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
                    List<Long> allowedClassroomIds = subjectClassrooms.get(subjectId).stream().map(Classroom::getId).collect(Collectors.toList());
                    for (int cIdx = 0; cIdx < classroomIds.size(); cIdx++) {
                        Long classroomId = classroomIds.get(cIdx);
                        if (!allowedClassroomIds.contains(classroomId)) {
                            model.addImplication(subjectVar[d][s][subIdx], classroomVar[d][s][cIdx].not());
                        }
                    }
                }
            }
        }

        // Уникальность преподавателя в каждом слоте (уже обеспечена через sumTeacher == sumSub)
        // Уникальность кабинета также обеспечена

        // Уникальность преподавателя по всем слотам (нельзя вести две пары одновременно)
        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {
                for (int tIdx = 0; tIdx < teacherIds.size(); tIdx++) {
                    LinearExprBuilder teacherDaySlotSum = LinearExpr.newBuilder();
                    teacherDaySlotSum.add(teacherVar[d][s][tIdx]);
                    model.addLessOrEqual(teacherDaySlotSum, 1); // уже есть, но это на всякий случай
                }
            }
        }

        // Решение
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);
        if (status != CpSolverStatus.FEASIBLE && status != CpSolverStatus.OPTIMAL) {
            throw new RuntimeException("No feasible schedule found");
        }

        // Сохранение
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
                if (selectedSubjectId == null) continue; // пустой слот

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