package com.example.platform.dto;

import com.example.platform.entity.Skill;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SkillReviewQueueItemDtoTest {

    @Test
    void fromSkillUsesTrialEndDateForTrialReminder() {
        Skill skill = new Skill();
        skill.setId(12L);
        skill.setName("Trial Skill");
        skill.setLifecycleStatus(Skill.LifecycleStatus.TRIAL);
        skill.setTrialEndsAt(LocalDate.of(2026, 6, 7));
        skill.setNextReviewAt(LocalDate.of(2026, 7, 1));

        SkillReviewQueueItemDto dto = SkillReviewQueueItemDto.fromSkill(skill, LocalDate.of(2026, 5, 31));

        assertThat(dto.dueType()).isEqualTo("TRIAL");
        assertThat(dto.dueAt()).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(dto.trialEndsAt()).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(dto.nextReviewAt()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(dto.daysUntilReview()).isEqualTo(7);
        assertThat(dto.overdue()).isFalse();
    }

    @Test
    void fromSkillKeepsReviewDateForNonTrialSkill() {
        Skill skill = new Skill();
        skill.setId(13L);
        skill.setName("Review Skill");
        skill.setLifecycleStatus(Skill.LifecycleStatus.NEEDS_REVIEW);
        skill.setNextReviewAt(LocalDate.of(2026, 5, 30));

        SkillReviewQueueItemDto dto = SkillReviewQueueItemDto.fromSkill(skill, LocalDate.of(2026, 5, 31));

        assertThat(dto.dueType()).isEqualTo("REVIEW");
        assertThat(dto.dueAt()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(dto.daysUntilReview()).isEqualTo(-1);
        assertThat(dto.overdue()).isTrue();
    }
}
