package com.example.platform.repository;

import com.example.platform.entity.Rule;
import com.example.platform.entity.Skill;
import com.example.platform.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);

    List<Tag> findByNameContainingIgnoreCaseOrderByNameAsc(String q);

    @Query("SELECT DISTINCT t FROM Skill s JOIN s.tags t WHERE s.visibility = :vis ORDER BY t.name")
    List<Tag> findTagsUsedByVisibleSkills(@Param("vis") Skill.Visibility vis);

    @Query("SELECT DISTINCT t FROM Rule r JOIN r.tags t WHERE r.visibility = :vis ORDER BY t.name")
    List<Tag> findTagsUsedByVisibleRules(@Param("vis") Rule.Visibility vis);
}
