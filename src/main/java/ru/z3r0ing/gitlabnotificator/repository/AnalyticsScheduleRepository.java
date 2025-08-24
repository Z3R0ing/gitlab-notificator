package ru.z3r0ing.gitlabnotificator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.z3r0ing.gitlabnotificator.model.entity.AnalyticsSchedule;

@Repository
public interface AnalyticsScheduleRepository extends JpaRepository<AnalyticsSchedule, Long> {
}