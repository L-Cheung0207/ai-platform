package com.example.platform.repository;

import com.example.platform.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {

    List<ForumCategory> findByEnabledTrueOrderBySortOrderAscNameAsc();

    List<ForumCategory> findAllByOrderBySortOrderAscNameAsc();

    Optional<ForumCategory> findBySlugIgnoreCase(String slug);
}
