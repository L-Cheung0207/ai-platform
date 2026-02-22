package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.entity.AiTool;
import com.example.platform.repository.AiToolRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiToolService {

    private final AiToolRepository aiToolRepository;
    private final String uploadBaseUrl;

    public AiToolService(AiToolRepository aiToolRepository,
                         @Value("${app.upload.base-url:/uploads}") String uploadBaseUrl) {
        this.aiToolRepository = aiToolRepository;
        this.uploadBaseUrl = uploadBaseUrl;
    }

    @Transactional(readOnly = true)
    public Page<AiTool> list(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "id"));
        if (category != null && !category.isBlank()) {
            String val = category.trim();
            return aiToolRepository.findByCategoryOrTagOrderByIdDesc(val, pageable);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            return aiToolRepository.findByNameContainingOrDescriptionContainingOrderByIdDesc(kw, kw, pageable);
        }
        return aiToolRepository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public AiTool getById(Long id) {
        return aiToolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("AI 工具不存在"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLatest(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        return aiToolRepository.findAllByOrderByIdDesc(pageable).getContent().stream()
                .map(this::toResponseItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return aiToolRepository.findDistinctCategoryNamesOrderByCategoryName();
    }

    /**
     * 按 tag 分组返回工具列表，用于分组展示。每个工具按其 tagNames 归属到对应分组。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGroupedByTag(String keyword) {
        Pageable limit500 = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "id"));
        List<AiTool> all;
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            Page<AiTool> p = aiToolRepository.findByNameContainingOrDescriptionContainingOrderByIdDesc(kw, kw, limit500);
            all = p.getContent();
        } else {
            Page<AiTool> p = aiToolRepository.findAllByOrderByIdDesc(limit500);
            all = p.getContent();
        }
        List<String> tagOrder = getDistinctTags();
        Map<String, List<AiTool>> byTag = new LinkedHashMap<>();
        for (AiTool t : all) {
            String tags = t.getTagNames();
            if (tags == null || tags.isBlank()) {
                byTag.computeIfAbsent("其他", k -> new ArrayList<>()).add(t);
                continue;
            }
            boolean added = false;
            for (String tag : tags.split(",")) {
                String trimmed = tag.trim();
                if (trimmed.isEmpty()) continue;
                byTag.computeIfAbsent(trimmed, k -> new ArrayList<>()).add(t);
                added = true;
            }
            if (!added) {
                byTag.computeIfAbsent("其他", k -> new ArrayList<>()).add(t);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String tag : tagOrder) {
            List<AiTool> items = byTag.get(tag);
            if (items == null || items.isEmpty()) continue;
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("category", tag);
            group.put("items", items.stream().map(this::toResponseItem).collect(Collectors.toList()));
            result.add(group);
        }
        if (byTag.containsKey("其他") && !byTag.get("其他").isEmpty()) {
            Map<String, Object> other = new LinkedHashMap<>();
            other.put("category", "其他");
            other.put("items", byTag.get("其他").stream().map(this::toResponseItem).collect(Collectors.toList()));
            result.add(other);
        }
        return result;
    }

    private Map<String, Object> toResponseItem(AiTool t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("summary", t.getSummary());
        m.put("content", t.getContent());
        m.put("description", t.getSummary() != null ? t.getSummary() : t.getDescription());
        m.put("url", t.getUrl());
        m.put("logoUrl", getLogoDisplayUrl(t));
        m.put("categoryName", t.getCategoryName());
        m.put("tagNames", t.getTagNames());
        m.put("sourceUrl", t.getSourceUrl());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctTags() {
        List<String> rows = aiToolRepository.findAllTagNames();
        Set<String> tags = new TreeSet<>();
        for (String s : rows) {
            if (s != null && !s.isBlank()) {
                for (String t : s.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) tags.add(trimmed);
                }
            }
        }
        return new ArrayList<>(tags);
    }

    public String getLogoDisplayUrl(AiTool tool) {
        if (tool == null) return null;
        if (tool.getLogoPath() != null && !tool.getLogoPath().isBlank()) {
            return uploadBaseUrl + "/" + tool.getLogoPath();
        }
        return tool.getLogoUrl();
    }

    @Transactional
    public void delete(Long id) {
        AiTool t = getById(id);
        aiToolRepository.delete(t);
    }

    public boolean existsBySourceUrl(String url) {
        return aiToolRepository.existsBySourceUrl(url);
    }

    @Transactional
    public AiTool create(String name, String summary, String content, String url, String logoUrl, String logoPath, String categoryName, String tagNames, String sourceUrl) {
        AiTool t = new AiTool();
        t.setName(name);
        t.setSummary(summary != null && summary.length() > 500 ? summary.substring(0, 500) : summary);
        t.setContent(content);
        t.setDescription(summary != null ? summary : content);
        t.setUrl(url);
        t.setLogoUrl(logoUrl);
        t.setLogoPath(logoPath);
        t.setCategoryName(categoryName);
        t.setTagNames(tagNames != null && tagNames.length() > 500 ? tagNames.substring(0, 500) : tagNames);
        t.setSourceUrl(sourceUrl);
        return aiToolRepository.save(t);
    }
}
