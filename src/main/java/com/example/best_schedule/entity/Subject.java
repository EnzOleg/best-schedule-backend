package com.example.best_schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    // Тип кабинета, который требуется для этого предмета (опционально)
    @Enumerated(EnumType.STRING)
    private ClassroomType requiredClassroomType;

    // в класс Subject добавляем
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubjectTeacher> teachers = new ArrayList<>();

    // Доступные кабинеты (многие ко многим через промежуточную таблицу)
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubjectClassroom> allowedClassrooms = new ArrayList<>();

    // Часы для групп (многие ко многим с дополнительным полем hours)
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubjectGroupHours> groupHours = new ArrayList<>();
}