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
    public List<ScheduleItem> generateSchedule(
            List<GroupScheduleInput> groupInputs,
            LocalDate startDate,
            int days
    ) {

        // -------------------------
        // 1. ГРУППЫ
        // -------------------------
        List<Group> groups = groupInputs.stream()
                .map(i -> groupRepository.findByIdWithStudents(i.getGroupId())
                        .orElseThrow(() -> new RuntimeException("Group not found: " + i.getGroupId())))
                .toList();

        Map<Long, Integer> groupSize = groups.stream()
                .collect(Collectors.toMap(Group::getId, g -> g.getStudents().size()));

        // -------------------------
        // 2. ПРЕДМЕТЫ ВСЕХ ГРУПП
        // -------------------------
        Map<Long, Integer> hoursPerSubject = new HashMap<>();

        for (GroupScheduleInput input : groupInputs) {
            for (SubjectHoursInput sh : input.getSubjectHours()) {
                hoursPerSubject.merge(sh.getSubjectId(), sh.getHours(), Integer::sum);
            }
        }

        if (hoursPerSubject.isEmpty()) {
            throw new RuntimeException("No subjects provided");
        }

        // -------------------------
        // 3. ДНИ
        // -------------------------
        List<LocalDate> workingDates = new ArrayList<>();
        LocalDate current = startDate;
        int added = 0;

        while (added < days) {
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDates.add(current);
                added++;
            }
            current = current.plusDays(1);
        }

        int D = workingDates.size();
        int S = SLOTS_PER_DAY;

        // -------------------------
        // 4. ПРЕПОДЫ И КАБИНЕТЫ
        // -------------------------
        Map<Long, List<User>> subjectTeachers = new HashMap<>();
        Map<Long, List<Classroom>> subjectClassrooms = new HashMap<>();

        for (Long subjectId : hoursPerSubject.keySet()) {

            Subject subject = subjectRepository.findByIdWithTeachers(subjectId)
                    .orElseThrow();

            List<User> teachers = subject.getTeachers().stream()
                    .map(SubjectTeacher::getTeacher)
                    .distinct()
                    .toList();

            subjectTeachers.put(subjectId, teachers);

            List<SubjectClassroom> links =
                    subjectClassroomRepository.findAllBySubjectIdWithClassroom(subjectId);

            List<Classroom> classrooms = links.stream()
                    .map(SubjectClassroom::getClassroom)
                    .distinct()
                    .toList();

            subjectClassrooms.put(subjectId, classrooms);
        }

        List<Long> subjectIds = new ArrayList<>(hoursPerSubject.keySet());

        List<Long> teacherIds = subjectTeachers.values().stream()
                .flatMap(List::stream)
                .map(User::getId)
                .distinct()
                .toList();

        List<Long> classroomIds = classroomRepository.findAll().stream()
                .map(Classroom::getId)
                .toList();

        // -------------------------
        // 5. MODEL
        // -------------------------
        CpModel model = new CpModel();

        // group -> d -> s -> subject
        Map<Long, BoolVar[][][]> subjectVar = new HashMap<>();

        // teacher / classroom GLOBAL (ВАЖНО!)
        BoolVar[][][] teacherVar = new BoolVar[D][S][teacherIds.size()];
        BoolVar[][][] classroomVar = new BoolVar[D][S][classroomIds.size()];

        // -------------------------
        // VARIABLES PER GROUP
        // -------------------------
        for (Group g : groups) {

            BoolVar[][][] vars = new BoolVar[D][S][subjectIds.size()];
            subjectVar.put(g.getId(), vars);

            for (int d = 0; d < D; d++) {
                for (int s = 0; s < S; s++) {

                    // subject selection
                    LinearExprBuilder sumSub = LinearExpr.newBuilder();

                    for (int i = 0; i < subjectIds.size(); i++) {
                        vars[d][s][i] = model.newBoolVar("g" + g.getId() + "_s_" + d + "_" + s + "_" + i);
                        sumSub.add(vars[d][s][i]);
                    }

                    model.addLessOrEqual(sumSub, 1);

                    // teacher + classroom (GLOBAL sync)
                    LinearExprBuilder sumTeacher = LinearExpr.newBuilder();
                    LinearExprBuilder sumClass = LinearExpr.newBuilder();

                    for (int t = 0; t < teacherIds.size(); t++) {
                        teacherVar[d][s][t] = teacherVar[d][s][t] != null
                                ? teacherVar[d][s][t]
                                : model.newBoolVar("t_" + d + "_" + s + "_" + t);

                        sumTeacher.add(teacherVar[d][s][t]);
                    }

                    for (int c = 0; c < classroomIds.size(); c++) {
                        classroomVar[d][s][c] = classroomVar[d][s][c] != null
                                ? classroomVar[d][s][c]
                                : model.newBoolVar("c_" + d + "_" + s + "_" + c);

                        sumClass.add(classroomVar[d][s][c]);
                    }

                    model.addEquality(sumTeacher, sumSub);
                    model.addEquality(sumClass, sumSub);
                }
            }
        }

        // -------------------------
        // 6. SUBJECT HOURS PER GROUP
        // -------------------------
        for (Group g : groups) {
            BoolVar[][][] vars = subjectVar.get(g.getId());

            Map<Long, Integer> localHours = groupInputs.stream()
                    .filter(i -> i.getGroupId().equals(g.getId()))
                    .flatMap(i -> i.getSubjectHours().stream())
                    .collect(Collectors.toMap(
                            SubjectHoursInput::getSubjectId,
                            SubjectHoursInput::getHours,
                            Integer::sum
                    ));

            for (int i = 0; i < subjectIds.size(); i++) {
                Long subjectId = subjectIds.get(i);

                int hours = localHours.getOrDefault(subjectId, 0);

                LinearExprBuilder sum = LinearExpr.newBuilder();

                for (int d = 0; d < D; d++) {
                    for (int s = 0; s < S; s++) {
                        sum.add(vars[d][s][i]);
                    }
                }

                model.addEquality(sum, hours);
            }
        }

        // -------------------------
        // 7. TEACHER CONFLICT GLOBAL
        // -------------------------
        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {

                for (int t = 0; t < teacherIds.size(); t++) {

                    // один преподаватель в одном слоте
                    model.addLessOrEqual(teacherVar[d][s][t], 1);
                }
            }
        }

        // -------------------------
        // 8. ROOM CONFLICT GLOBAL
        // -------------------------
        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {

                for (int c = 0; c < classroomIds.size(); c++) {
                    model.addLessOrEqual(classroomVar[d][s][c], 1);
                }
            }
        }

        // -------------------------
        // 9. SOLVE
        // -------------------------
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(60);

        CpSolverStatus status = solver.solve(model);

        if (status != CpSolverStatus.OPTIMAL &&
            status != CpSolverStatus.FEASIBLE) {
            throw new RuntimeException("No solution");
        }

        // -------------------------
        // 10. EXTRACT
        // -------------------------
        List<ScheduleItem> result = new ArrayList<>();

        for (Group g : groups) {

            BoolVar[][][] vars = subjectVar.get(g.getId());

            for (int d = 0; d < D; d++) {
                for (int s = 0; s < S; s++) {

                    for (int i = 0; i < subjectIds.size(); i++) {

                        if (solver.value(vars[d][s][i]) == 1) {

                            Long subjectId = subjectIds.get(i);
                            Subject subject = subjectRepository.findById(subjectId).orElseThrow();

                            ScheduleItem item = ScheduleItem.builder()
                                    .date(workingDates.get(d))
                                    .startTime(SLOT_START_TIMES[s])
                                    .endTime(SLOT_END_TIMES[s])
                                    .group(g)
                                    .subject(subject)
                                    .build();

                            result.add(item);
                        }
                    }
                }
            }
        }
        scheduleRepository.saveAll(result);
        return result;
    }
}