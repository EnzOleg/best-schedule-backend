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
        // GROUPS
        // -------------------------
        List<Group> groups = groupInputs.stream()
                .map(i -> groupRepository.findByIdWithStudents(i.getGroupId())
                        .orElseThrow())
                .toList();

        // -------------------------
        // SUBJECTS + HOURS
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

        List<Long> subjectIds = new ArrayList<>(hoursPerSubject.keySet());

        // -------------------------
        // DATES
        // -------------------------
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cur = startDate;

        while (dates.size() < days) {
            if (cur.getDayOfWeek() != DayOfWeek.SUNDAY) {
                dates.add(cur);
            }
            cur = cur.plusDays(1);
        }

        int D = dates.size();
        int S = SLOTS_PER_DAY;

        // -------------------------
        // TEACHERS + CLASSROOMS
        // -------------------------
        Map<Long, List<Long>> subjectTeachers = new HashMap<>();
        Map<Long, List<Long>> subjectClassrooms = new HashMap<>();

        for (Long subjectId : subjectIds) {

            Subject subject = subjectRepository.findByIdWithTeachers(subjectId)
                    .orElseThrow();

            List<Long> teachers = subject.getTeachers().stream()
                    .map(st -> st.getTeacher().getId())
                    .distinct()
                    .toList();

            subjectTeachers.put(subjectId, teachers);

            List<Long> classrooms = subjectClassroomRepository
                    .findAllBySubjectIdWithClassroom(subjectId)
                    .stream()
                    .map(sc -> sc.getClassroom().getId())
                    .distinct()
                    .toList();

            subjectClassrooms.put(subjectId, classrooms);
        }

        List<Long> teacherIds = subjectTeachers.values().stream()
                .flatMap(List::stream).distinct().toList();

        List<Long> classroomIds = classroomRepository.findAll()
                .stream().map(Classroom::getId).toList();

        // -------------------------
        // MODEL
        // -------------------------
        CpModel model = new CpModel();

        Map<Long, BoolVar[][][]> subjectVar = new HashMap<>();
        Map<Long, BoolVar[][][]> teacherVar = new HashMap<>();
        Map<Long, BoolVar[][][]> classroomVar = new HashMap<>();

        // -------------------------
        // VARIABLES
        // -------------------------
        for (Group g : groups) {

            BoolVar[][][] sub = new BoolVar[D][S][subjectIds.size()];
            BoolVar[][][] teach = new BoolVar[D][S][teacherIds.size()];
            BoolVar[][][] room = new BoolVar[D][S][classroomIds.size()];

            subjectVar.put(g.getId(), sub);
            teacherVar.put(g.getId(), teach);
            classroomVar.put(g.getId(), room);

            for (int d = 0; d < D; d++) {
                for (int s = 0; s < S; s++) {

                    LinearExprBuilder sumSub = LinearExpr.newBuilder();
                    LinearExprBuilder sumTeach = LinearExpr.newBuilder();
                    LinearExprBuilder sumRoom = LinearExpr.newBuilder();

                    for (int i = 0; i < subjectIds.size(); i++) {
                        sub[d][s][i] = model.newBoolVar("g" + g.getId()+"_sub_"+d+"_"+s+"_"+i);
                        sumSub.add(sub[d][s][i]);
                    }

                    for (int t = 0; t < teacherIds.size(); t++) {
                        teach[d][s][t] = model.newBoolVar("g"+g.getId()+"_t_"+d+"_"+s+"_"+t);
                        sumTeach.add(teach[d][s][t]);
                    }

                    for (int c = 0; c < classroomIds.size(); c++) {
                        room[d][s][c] = model.newBoolVar("g"+g.getId()+"_c_"+d+"_"+s+"_"+c);
                        sumRoom.add(room[d][s][c]);
                    }

                    model.addLessOrEqual(sumSub, 1);
                    model.addEquality(sumTeach, sumSub);
                    model.addEquality(sumRoom, sumSub);

                    // 🔗 СВЯЗКА subject → teacher/classroom
                    for (int i = 0; i < subjectIds.size(); i++) {

                        Long subjectId = subjectIds.get(i);

                        for (int t = 0; t < teacherIds.size(); t++) {
                            if (!subjectTeachers.get(subjectId).contains(teacherIds.get(t))) {
                                model.addImplication(sub[d][s][i], teach[d][s][t].not());
                            }
                        }

                        for (int c = 0; c < classroomIds.size(); c++) {
                            if (!subjectClassrooms.get(subjectId).contains(classroomIds.get(c))) {
                                model.addImplication(sub[d][s][i], room[d][s][c].not());
                            }
                        }
                    }
                }
            }
        }

        // -------------------------
        // HOURS PER GROUP
        // -------------------------
        for (Group g : groups) {

            BoolVar[][][] sub = subjectVar.get(g.getId());

            Map<Long, Integer> localHours = groupInputs.stream()
                    .filter(i -> i.getGroupId().equals(g.getId()))
                    .flatMap(i -> i.getSubjectHours().stream())
                    .collect(Collectors.toMap(
                            SubjectHoursInput::getSubjectId,
                            SubjectHoursInput::getHours,
                            Integer::sum
                    ));

            for (int i = 0; i < subjectIds.size(); i++) {

                int hours = localHours.getOrDefault(subjectIds.get(i), 0);

                LinearExprBuilder sum = LinearExpr.newBuilder();

                for (int d = 0; d < D; d++) {
                    for (int s = 0; s < S; s++) {
                        sum.add(sub[d][s][i]);
                    }
                }

                model.addEquality(sum, hours);
            }
        }

        // -------------------------
        // GLOBAL CONFLICTS
        // -------------------------
        for (int d = 0; d < D; d++) {
            for (int s = 0; s < S; s++) {

                // teachers
                for (int t = 0; t < teacherIds.size(); t++) {

                    LinearExprBuilder sum = LinearExpr.newBuilder();

                    for (Group g : groups) {
                        sum.add(teacherVar.get(g.getId())[d][s][t]);
                    }

                    model.addLessOrEqual(sum, 1);
                }

                // classrooms
                for (int c = 0; c < classroomIds.size(); c++) {

                    LinearExprBuilder sum = LinearExpr.newBuilder();

                    for (Group g : groups) {
                        sum.add(classroomVar.get(g.getId())[d][s][c]);
                    }

                    model.addLessOrEqual(sum, 1);
                }
            }
        }

        // -------------------------
        // SOLVE
        // -------------------------
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(30);

        CpSolverStatus status = solver.solve(model);

        if (status != CpSolverStatus.FEASIBLE && status != CpSolverStatus.OPTIMAL) {
            throw new RuntimeException("No solution");
        }

        // -------------------------
        // EXTRACT
        // -------------------------
        List<ScheduleItem> result = new ArrayList<>();

        Map<Long, Subject> subjectMap = subjectRepository.findAllById(subjectIds)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));

        Map<Long, User> teacherMap = userRepository.findAllById(teacherIds)
                .stream().collect(Collectors.toMap(User::getId, t -> t));

        Map<Long, Classroom> classroomMap = classroomRepository.findAllById(classroomIds)
                .stream().collect(Collectors.toMap(Classroom::getId, c -> c));

        for (Group g : groups) {

            for (int d = 0; d < D; d++) {
                for (int s = 0; s < S; s++) {

                    Long subjectId = null;

                    for (int i = 0; i < subjectIds.size(); i++) {
                        if (solver.value(subjectVar.get(g.getId())[d][s][i]) == 1) {
                            subjectId = subjectIds.get(i);
                            break;
                        }
                    }

                    if (subjectId == null) continue;

                    Long teacherId = null;
                    for (int t = 0; t < teacherIds.size(); t++) {
                        if (solver.value(teacherVar.get(g.getId())[d][s][t]) == 1) {
                            teacherId = teacherIds.get(t);
                            break;
                        }
                    }

                    Long classroomId = null;
                    for (int c = 0; c < classroomIds.size(); c++) {
                        if (solver.value(classroomVar.get(g.getId())[d][s][c]) == 1) {
                            classroomId = classroomIds.get(c);
                            break;
                        }
                    }

                    result.add(ScheduleItem.builder()
                            .date(dates.get(d))
                            .startTime(SLOT_START_TIMES[s])
                            .endTime(SLOT_END_TIMES[s])
                            .group(g)
                            .subject(subjectMap.get(subjectId))
                            .teacher(teacherMap.get(teacherId))
                            .classroom(classroomMap.get(classroomId))
                            .build());
                }
            }
        }

        scheduleRepository.saveAll(result);

        return result;
    }
}