package com.example.platform.repository;

import com.example.platform.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    List<Category> findByTypeOrderBySortOrderAscNameAsc(String type);
}
