package com.example.best_schedule.service;

import com.example.best_schedule.dto.CreateLectureInput;
import com.example.best_schedule.entity.Lecture;
import com.example.best_schedule.repository.GroupRepository;
import com.example.best_schedule.repository.LectureRepository;
import com.example.best_schedule.repository.SubjectRepository;
import com.example.best_schedule.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {

    private final LectureRepository lectureRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public Lecture createLecture(CreateLectureInput input) {
        Lecture lecture = new Lecture();
        lecture.setDate(LocalDate.parse(input.getDate()));
        lecture.setStartTime(LocalTime.parse(input.getStartTime()));
        lecture.setEndTime(LocalTime.parse(input.getEndTime()));
        lecture.setClassroom(input.getClassroom());
        lecture.setTitle(input.getTitle());
        lecture.setText(input.getText());

        lecture.setGroup(groupRepository.findById(input.getGroupId()).orElseThrow());
        lecture.setSubject(subjectRepository.findById(input.getSubjectId()).orElseThrow());
        lecture.setTeacher(userRepository.findById(input.getTeacherId()).orElseThrow());

        return lectureRepository.save(lecture);
    }

    public Lecture updateLecture(Long id, CreateLectureInput input) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found"));

        if (input.getDate() != null) lecture.setDate(LocalDate.parse(input.getDate()));
        if (input.getStartTime() != null) lecture.setStartTime(LocalTime.parse(input.getStartTime()));
        if (input.getEndTime() != null) lecture.setEndTime(LocalTime.parse(input.getEndTime()));
        if (input.getClassroom() != null) lecture.setClassroom(input.getClassroom());
        if (input.getTitle() != null) lecture.setTitle(input.getTitle());
        if (input.getText() != null) lecture.setText(input.getText());

        if (input.getGroupId() != null) {
            lecture.setGroup(groupRepository.findById(input.getGroupId()).orElseThrow());
        }
        if (input.getSubjectId() != null) {
            lecture.setSubject(subjectRepository.findById(input.getSubjectId()).orElseThrow());
        }
        if (input.getTeacherId() != null) {
            lecture.setTeacher(userRepository.findById(input.getTeacherId()).orElseThrow());
        }

        return lectureRepository.save(lecture);
    }

    public Boolean deleteLecture(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found"));
        lectureRepository.delete(lecture);
        return true;
    }

    public List<Lecture> getLecturesForMe(Long userId, String start, String end) {
        return lectureRepository.findByTeacherIdAndDateBetween(
            userId,
            LocalDate.parse(start),
            LocalDate.parse(end)
        );
    }

    public List<Lecture> getByGroupAndDateRange(Long groupId, String start, String end) {
        return lectureRepository.findByGroupIdAndDateBetween(
                groupId,
                LocalDate.parse(start),
                LocalDate.parse(end)
        );
    }

    public List<Lecture> getByTeacherAndDateRange(Long teacherId, String start, String end) {
        return lectureRepository.findByTeacherIdAndDateBetween(
                teacherId,
                LocalDate.parse(start),
                LocalDate.parse(end)
        );
    }
}