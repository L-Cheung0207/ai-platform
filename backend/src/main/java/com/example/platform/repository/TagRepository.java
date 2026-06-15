package com.example.platform.repository;

import com.example.platform.entity.Rule;
import com.example.platform.entity.Skill;
import com.example.platform.entity.ForumPost;
import com.example.platform.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);

    List<Tag> findByNameContainingIgnoreCaseOrderByNameAsc(String q);

    @Query("SELECT DISTINCT t FROM Skill s JOIN s.tags t WHERE s.visibility = :vis AND s.lifecycleStatus = :status ORDER BY t.name")
    List<Tag> findTagsUsedByVisibleSkills(@Param("vis") Skill.Visibility vis,
                                          @Param("status") Skill.LifecycleStatus status);

    @Query("SELECT DISTINCT t FROM Rule r JOIN r.tags t WHERE r.visibility = :vis ORDER BY t.name")
    List<Tag> findTagsUsedByVisibleRules(@Param("vis") Rule.Visibility vis);

    @Query("SELECT DISTINCT t FROM ForumPost p JOIN p.tags t WHERE p.status IN :statuses ORDER BY t.name")
    List<Tag> findTagsUsedByVisibleForumPosts(@Param("statuses") List<ForumPost.PostStatus> statuses);
}
