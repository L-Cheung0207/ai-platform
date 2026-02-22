package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.entity.Category;
import com.example.platform.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> list(String type) {
        if (type != null && !type.isBlank()) {
            return categoryRepository.findByTypeOrderBySortOrderAscNameAsc(type.trim());
        }
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("分类不存在"));
    }
}
