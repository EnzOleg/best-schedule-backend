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
    
    @MutationMapping
    public Lecture updateLecture(@Argument Long id, @Argument CreateLectureInput input) {
        return lectureService.updateLecture(id, input);
    }

    @MutationMapping
    public Boolean deleteLecture(@Argument Long id) {
        return lectureService.deleteLecture(id);
    }

    @QueryMapping
    public List<Lecture> lecturesForMe(
            @Argument String startDate,
            @Argument String endDate,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        return switch (user.getRole()) {
            case STUDENT -> lectureService.getForStudent(user.getId(), startDate, endDate);
            case TEACHER -> lectureService.getForTeacher(user.getId(), startDate, endDate);
            case ADMIN -> lectureService.getForAdmin(startDate, endDate);
        };
    }
}