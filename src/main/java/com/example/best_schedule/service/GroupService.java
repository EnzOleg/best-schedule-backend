package com.example.best_schedule.service;

import com.example.best_schedule.entity.Group;
import com.example.best_schedule.entity.Role;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.repository.GroupRepository;
import com.example.best_schedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public Group createGroup(String name,
                             Integer course,
                             String specialty,
                             String faculty) {

        Group group = Group.builder()
                .name(name)
                .course(course)
                .specialty(specialty)
                .faculty(faculty)
                .build();

        return groupRepository.save(group);
    }

    public Group addStudentToGroup(Long groupId, Long studentId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        if (!group.getStudents().contains(user)) {
            group.getStudents().add(user);
        }

        return groupRepository.save(group);
    }

    public Group removeStudentFromGroup(Long groupId, Long studentId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        group.getStudents().remove(user);

        return groupRepository.save(group);
    }

    public List<Group> findAll() {
        return groupRepository.findAll();
    }
}