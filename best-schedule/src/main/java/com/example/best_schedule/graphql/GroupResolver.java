package com.example.best_schedule.graphql;

import com.example.best_schedule.entity.Group;
import com.example.best_schedule.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class GroupResolver {

    private final GroupService groupService;

    @QueryMapping
    public List<Group> groups() {
        return groupService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Group createGroup(@Argument String name,
                             @Argument Integer course,
                             @Argument String specialty,
                             @Argument String faculty) {

        return groupService.createGroup(name, course, specialty, faculty);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Group addStudentToGroup(@Argument Long groupId,
                                   @Argument Long studentId) {
        return groupService.addStudentToGroup(groupId, studentId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Group removeStudentFromGroup(@Argument Long groupId,
                                        @Argument Long studentId) {
        return groupService.removeStudentFromGroup(groupId, studentId);
    }
}