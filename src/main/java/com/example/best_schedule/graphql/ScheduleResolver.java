package com.example.best_schedule.graphql;

import com.example.best_schedule.dto.CreateScheduleInput;
import com.example.best_schedule.dto.GenerateScheduleInput;
import com.example.best_schedule.entity.ScheduleItem;
import com.example.best_schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ScheduleResolver {

    private final ScheduleService scheduleService;

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public ScheduleItem createSchedule(@Argument CreateScheduleInput input) {
        return scheduleService.createSchedule(input);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public List<ScheduleItem> createMultipleSchedules(@Argument("input") List<CreateScheduleInput> input) {
        if (input == null) {
            throw new IllegalArgumentException("input не может быть null");
        }
        List<ScheduleItem> createdItems = new ArrayList<>();
        for (CreateScheduleInput item : input) {
            createdItems.add(scheduleService.createSchedule(item));
        }
        return createdItems;
    }

    @QueryMapping
    public List<ScheduleItem> scheduleByGroup(@Argument Long groupId) {
        return scheduleService.getByGroup(groupId);
    }

    @QueryMapping
    public List<ScheduleItem> scheduleForWeek(
            @Argument Long groupId,
            @Argument String startDate,
            @Argument String endDate
    ) {
        return scheduleService.getScheduleForWeek(
                groupId,
                startDate,
                endDate
        );
    }

    @QueryMapping
    public List<ScheduleItem> scheduleForMe(
            @Argument String startDate,
            @Argument String endDate
    ) {
        return scheduleService.scheduleForMe(startDate, endDate);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteSchedule(@Argument Long id) {
        return scheduleService.deleteSchedule(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ScheduleItem> generateScheduleForGroup(@Argument GenerateScheduleInput input) {
        return scheduleService.generateForGroup(input.getGroupId(), input.getStartDate(), input.getDays());
    }
}