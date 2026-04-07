package com.example.best_schedule.graphql;

import com.example.best_schedule.entity.Subject;
import com.example.best_schedule.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SubjectResolver {

    private final SubjectService subjectService;

    @QueryMapping
    public List<Subject> subjects() {
        return subjectService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Subject createSubject(@Argument String name) {
        return subjectService.createSubject(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Boolean deleteSubject(@Argument Long id) {
        return subjectService.deleteSubject(id);
    }
}