package com.example.platform.repository;

import com.example.platform.entity.SkillUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SkillUsageEventRepository extends JpaRepository<SkillUsageEvent, Long> {

    long countBySkill_Id(Long skillId);

    List<SkillUsageEvent> findBySkill_Id(Long skillId);

    boolean existsBySkill_IdAndExternalEventId(Long skillId, String externalEventId);

    long countByCreatedAtAfter(Instant since);

    List<SkillUsageEvent> findByCreatedAtAfter(Instant since);

    List<SkillUsageEvent> findByCreatedAtBetween(Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(e.savedMinutes), 0) FROM SkillUsageEvent e")
    Long sumSavedMinutes();

    @Query("SELECT COALESCE(SUM(e.savedMinutes), 0) FROM SkillUsageEvent e WHERE e.skill.id = :skillId")
    Long sumSavedMinutesBySkillId(@Param("skillId") Long skillId);
}
