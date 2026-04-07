package com.example.best_schedule.graphql;

import com.example.best_schedule.dto.CreateLectureInput;
import com.example.best_schedule.entity.Lecture;
import com.example.best_schedule.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.best_schedule.entity.User;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LectureResolver {

    private final LectureService lectureService;

    @MutationMapping
    public Lecture createLecture(@Argument CreateLectureInput input) {
        return lectureService.createLecture(input);
    }

    @QueryMapping
    public List<Lecture> lecturesByGroup(
            @Argument Long groupId,
            @Argument String startDate,
            @Argument String endDate
    ) {
        return lectureService.getByGroupAndDateRange(groupId, startDate, endDate);
    }

    @QueryMapping
    public List<Lecture> lecturesByTeacher(
            @Argument Long teacherId,
            @Argument String startDate,
            @Argument String endDate
    ) {
        return lectureService.getByTeacherAndDateRange(teacherId, startDate, endDate);
    }

    @QueryMapping
    public List<Lecture> lecturesForMe(
            @Argument String startDate,
            @Argument String endDate,
            @AuthenticationPrincipal User user  // <- берём прямо из контекста Spring Security
    ) {
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        Long userId = user.getId();
        return lectureService.getLecturesForMe(userId, startDate, endDate);
    }
}