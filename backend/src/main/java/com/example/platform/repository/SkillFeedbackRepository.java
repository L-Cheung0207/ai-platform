package com.example.platform.repository;

import com.example.platform.entity.SkillFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SkillFeedbackRepository extends JpaRepository<SkillFeedback, Long> {

    Page<SkillFeedback> findBySkill_IdOrderByCreatedAtDesc(Long skillId, Pageable pageable);

    Page<SkillFeedback> findBySkill_IdAndStatusOrderByCreatedAtDesc(Long skillId, SkillFeedback.FeedbackStatus status, Pageable pageable);

    Page<SkillFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SkillFeedback> findByStatusOrderByCreatedAtDesc(SkillFeedback.FeedbackStatus status, Pageable pageable);

    List<SkillFeedback> findByCreatedAtAfter(Instant since);

    List<SkillFeedback> findByCreatedAtBetween(Instant start, Instant end);

    long countBySkill_Id(Long skillId);

    long countBySkill_IdAndStatus(Long skillId, SkillFeedback.FeedbackStatus status);

    long countByStatus(SkillFeedback.FeedbackStatus status);

    @Query("SELECT COALESCE(SUM(f.estimatedSavedMinutes), 0) FROM SkillFeedback f")
    Long sumEstimatedSavedMinutes();
}
