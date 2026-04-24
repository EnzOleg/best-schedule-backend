package com.example.best_schedule.graphql;

import com.example.best_schedule.dto.CreateHomeworkInput;
import com.example.best_schedule.entity.Homework;
import com.example.best_schedule.entity.Role;
import com.example.best_schedule.entity.ScheduleItem;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.service.HomeworkService;
import com.example.best_schedule.service.ScheduleService;
import com.example.best_schedule.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeworkResolver {

    private final HomeworkService homeworkService;
    private final UserService userService;
    private final ScheduleService scheduleService;

    @QueryMapping
    public List<Homework> homeworkForSchedule(@Argument Long scheduleItemId) {
        ScheduleItem item = scheduleService.getById(scheduleItemId);
        return homeworkService.getHomeworkForSchedule(item);
    }

    @MutationMapping
    public Homework createHomework(@Argument CreateHomeworkInput input) {
        User teacher = userService.getCurrentUser();

        if (teacher.getRole() != Role.TEACHER) {
            throw new RuntimeException("Only teachers can create homework");
        }

        if (input.getText() == null || input.getText().isBlank()) {
            throw new RuntimeException("Text is required");
        }

        ScheduleItem item = scheduleService.getById(input.getScheduleItemId());

        return homeworkService.createHomework(
            input.getText(),
            item.getDate(),
            item,
            teacher
        );
    }

    @MutationMapping
    public Homework updateHomework(
            @Argument Long id,
            @Argument String text
    ) {
        User teacher = userService.getCurrentUser();
        return homeworkService.updateHomework(
                id,
                text,
                teacher
        );
    }

    @MutationMapping
    public Boolean deleteHomework(@Argument Long id) {
        User teacher = userService.getCurrentUser();
        return homeworkService.deleteHomework(id, teacher);
    }
}