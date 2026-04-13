package com.example.best_schedule.service;

import com.example.best_schedule.dto.CreateSubjectInput;
import com.example.best_schedule.dto.GroupHoursInput;
import com.example.best_schedule.entity.*;
import com.example.best_schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final GroupRepository groupRepository;
    private final SubjectClassroomRepository subjectClassroomRepository;
    private final SubjectGroupHoursRepository subjectGroupHoursRepository;
    private final SubjectTeacherRepository subjectTeacherRepository;   // <-- добавить
    private final UserRepository userRepository;

    // Простое создание предмета (для обратной совместимости)
    public Subject createSubject(String name) {
        Subject subject = Subject.builder()
                .name(name)
                .build();
        return subjectRepository.save(subject);
    }

    // Расширенное создание предмета с кабинетами, часами и преподавателями
    @Transactional
    public Subject createSubjectWithDetails(CreateSubjectInput input) {
        Subject subject = Subject.builder()
                .name(input.getName())
                .requiredClassroomType(input.getRequiredClassroomType())
                .build();

        Subject saved = subjectRepository.save(subject);

        // Добавляем доступные кабинеты
        if (input.getAllowedClassroomIds() != null) {
            for (Long classroomId : input.getAllowedClassroomIds()) {
                Classroom classroom = classroomRepository.findById(classroomId)
                        .orElseThrow(() -> new RuntimeException("Classroom not found: " + classroomId));
                SubjectClassroom sc = SubjectClassroom.builder()
                        .subject(saved)
                        .classroom(classroom)
                        .build();
                subjectClassroomRepository.save(sc);
            }
        }

        // Добавляем часы для групп
        if (input.getGroupHours() != null) {
            for (GroupHoursInput gh : input.getGroupHours()) {
                Group group = groupRepository.findById(gh.getGroupId())
                        .orElseThrow(() -> new RuntimeException("Group not found: " + gh.getGroupId()));
                SubjectGroupHours sgh = SubjectGroupHours.builder()
                        .subject(saved)
                        .group(group)
                        .hours(gh.getHours())
                        .build();
                subjectGroupHoursRepository.save(sgh);
            }
        }

        // Добавляем преподавателей для предмета
        if (input.getTeacherIds() != null) {
            for (Long teacherId : input.getTeacherIds()) {
                User teacher = userRepository.findById(teacherId)
                        .orElseThrow(() -> new RuntimeException("Teacher not found: " + teacherId));
                SubjectTeacher st = SubjectTeacher.builder()
                        .subject(saved)
                        .teacher(teacher)
                        .build();
                subjectTeacherRepository.save(st);
            }
        }

        return saved;
    }

    // Удаление предмета с каскадным удалением всех связей
    @Transactional
    public boolean deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new RuntimeException("Subject not found");
        }
        subjectClassroomRepository.deleteBySubjectId(id);
        subjectGroupHoursRepository.deleteBySubjectId(id);
        subjectTeacherRepository.deleteBySubjectId(id);   // <-- добавить
        subjectRepository.deleteById(id);
        return true;
    }

    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }
}