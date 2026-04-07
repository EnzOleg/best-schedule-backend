package com.example.best_schedule.service;

import com.example.best_schedule.entity.Subject;
import com.example.best_schedule.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public Subject createSubject(String name) {
        Subject subject = Subject.builder()
                .name(name)
                .build();
        return subjectRepository.save(subject);
    }

    public boolean deleteSubject(Long id) {

        if (!subjectRepository.existsById(id)) {
            throw new RuntimeException("Subject not found");
        }

        //    if (scheduleRepository.existsBySubjectId(id)) {
        //        throw new SubjectInUseException("Subject is used in schedule");
        //    }

        subjectRepository.deleteById(id);
        return true;
    }

    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }
}