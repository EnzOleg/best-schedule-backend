package com.example.best_schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "subjects")
// @NamedEntityGraph(
//     name = "Subject.details",
//     attributeNodes = {
//         @NamedAttributeNode("teachers"),
//         @NamedAttributeNode("allowedClassrooms"),
//         @NamedAttributeNode("groupHours")
//     }
// )
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private ClassroomType requiredClassroomType;

    @Builder.Default
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectTeacher> teachers = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectClassroom> allowedClassrooms = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubjectGroupHours> groupHours = new HashSet<>();
}