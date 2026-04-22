package com.example.best_schedule.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "subject_classrooms")
public class SubjectClassroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    public Long getClassroomId() {
        return classroom != null ? classroom.getId() : null;
    }

    public String getClassroomName() {
        return classroom != null ? classroom.getName() : null;
    }

    public Integer getClassroomCapacity() {
        return classroom != null ? classroom.getCapacity() : null;
    }
}