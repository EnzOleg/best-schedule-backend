package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByStudentsId(Long studentId);
}