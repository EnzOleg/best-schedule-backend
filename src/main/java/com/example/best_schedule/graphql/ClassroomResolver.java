package com.example.best_schedule.graphql;

import com.example.best_schedule.dto.CreateClassroomInput;
import com.example.best_schedule.entity.Classroom;
import com.example.best_schedule.entity.ClassroomType;
import com.example.best_schedule.repository.ClassroomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ClassroomResolver {

    private final ClassroomRepository classroomRepository;

    @QueryMapping
    public List<Classroom> classrooms() {
        return classroomRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public List<Classroom> createClassrooms(@Argument List<CreateClassroomInput> input) {
        List<Classroom> result = new ArrayList<>();
        for (CreateClassroomInput item : input) {
            Classroom classroom = Classroom.builder()
                    .name(item.getName())
                    .capacity(item.getCapacity())
                    .type(ClassroomType.valueOf(item.getType()))
                    .build();
            result.add(classroomRepository.save(classroom));
        }
        return result;
    }
}