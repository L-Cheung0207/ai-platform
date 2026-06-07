package com.example.platform.repository;

import com.example.platform.entity.SkillReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SkillReviewRepository extends JpaRepository<SkillReview, Long> {

    Page<SkillReview> findBySkill_IdOrderByReviewedAtDescCreatedAtDesc(Long skillId, Pageable pageable);

    Optional<SkillReview> findFirstBySkill_IdOrderByReviewedAtDescCreatedAtDesc(Long skillId);

    List<SkillReview> findByReviewedAtBetween(LocalDate start, LocalDate end);

    long countBySkill_Id(Long skillId);

    long countByResult(SkillReview.ReviewResult result);
}
