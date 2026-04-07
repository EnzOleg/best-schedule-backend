package com.example.best_schedule.service;

import com.example.best_schedule.entity.Homework;
import com.example.best_schedule.entity.ScheduleItem;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.repository.HomeworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;

    public Homework createHomework(
            String text,
            LocalDate date,
            ScheduleItem scheduleItem,
            User teacher
    ) {
        Homework hw = new Homework();
        hw.setText(text);
        hw.setDate(date);
        hw.setScheduleItem(scheduleItem);
        hw.setTeacher(teacher);

        return homeworkRepository.save(hw);
    }

    public List<Homework> getHomeworkForSchedule(ScheduleItem item) {
        return homeworkRepository.findByScheduleItem(item);
    }

    public Homework updateHomework(
            Long id,
            String text,
            LocalDate date,
            User teacher
    ) {
        Homework hw = homeworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        if (!hw.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Not your homework");
        }

        if (text != null && !text.isBlank()) {
            hw.setText(text);
        }

        if (date != null) {
            hw.setDate(date);
        }

        return homeworkRepository.save(hw);
    }

    public Boolean deleteHomework(Long id, User teacher) {
        Homework hw = homeworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        if (!hw.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Not your homework");
        }

        homeworkRepository.delete(hw);
        return true;
    }
}