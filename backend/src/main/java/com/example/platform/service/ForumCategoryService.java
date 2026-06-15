package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.ForumCategoryDto;
import com.example.platform.dto.ForumCategoryWriteRequest;
import com.example.platform.entity.ForumCategory;
import com.example.platform.repository.ForumCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
public class ForumCategoryService {

    private final ForumCategoryRepository categoryRepository;

    public ForumCategoryService(ForumCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ForumCategoryDto> listPublic() {
        return categoryRepository.findByEnabledTrueOrderBySortOrderAscNameAsc()
                .stream().map(ForumCategoryDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<ForumCategoryDto> listAdmin() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream().map(ForumCategoryDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public ForumCategory getVisible(Long id) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("论坛分类不存在"));
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new ResourceNotFoundException("论坛分类不存在");
        }
        return category;
    }

    @Transactional(readOnly = true)
    public ForumCategory getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("论坛分类不存在"));
    }

    @Transactional
    public ForumCategoryDto create(ForumCategoryWriteRequest req) {
        ForumCategory category = new ForumCategory();
        apply(category, req, true);
        return ForumCategoryDto.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public ForumCategoryDto update(Long id, ForumCategoryWriteRequest req) {
        ForumCategory category = getById(id);
        apply(category, req, false);
        return ForumCategoryDto.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        ForumCategory category = getById(id);
        category.setEnabled(false);
        categoryRepository.save(category);
    }

    private void apply(ForumCategory category, ForumCategoryWriteRequest req, boolean isCreate) {
        String name = trim(req.getName());
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        String slug = trim(req.getSlug());
        if (slug == null || slug.isBlank()) {
            slug = slugify(name);
        } else {
            slug = slugify(slug);
        }
        categoryRepository.findBySlugIgnoreCase(slug).ifPresent(existing -> {
            if (isCreate || !existing.getId().equals(category.getId())) {
                throw new IllegalArgumentException("分类 Slug 已存在");
            }
        });
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trim(req.getDescription()));
        category.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        category.setEnabled(req.getEnabled() == null || req.getEnabled());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "forum-category" : normalized;
    }
}
