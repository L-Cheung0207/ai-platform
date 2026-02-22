package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.entity.McpServer;
import com.example.platform.repository.McpServerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class McpServerService {

    private final McpServerRepository mcpServerRepository;

    public McpServerService(McpServerRepository mcpServerRepository) {
        this.mcpServerRepository = mcpServerRepository;
    }

    @Transactional(readOnly = true)
    public Page<McpServer> list(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "id"));
        if (category != null && !category.isBlank()) {
            String val = category.trim();
            return mcpServerRepository.findByTagOrderByIdDesc(val, pageable);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            return mcpServerRepository.findByNameContainingOrDescriptionContainingOrderByIdDesc(kw, kw, pageable);
        }
        return mcpServerRepository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public McpServer getById(Long id) {
        return mcpServerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("MCP 服务器不存在"));
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctTags() {
        List<String> rows = mcpServerRepository.findAllTagNames();
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGroupedByTag(String keyword) {
        Pageable limit500 = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "id"));
        List<McpServer> all;
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            Page<McpServer> p = mcpServerRepository.findByNameContainingOrDescriptionContainingOrderByIdDesc(kw, kw, limit500);
            all = p.getContent();
        } else {
            Page<McpServer> p = mcpServerRepository.findAllByOrderByIdDesc(limit500);
            all = p.getContent();
        }
        List<String> tagOrder = getDistinctTags();
        Map<String, List<McpServer>> byTag = new LinkedHashMap<>();
        for (McpServer t : all) {
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
            List<McpServer> items = byTag.get(tag);
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

    private Map<String, Object> toResponseItem(McpServer t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("summary", t.getSummary());
        m.put("content", t.getContent());
        m.put("description", t.getSummary() != null ? t.getSummary() : t.getDescription());
        m.put("url", t.getUrl());
        m.put("logoUrl", t.getLogoUrl());
        m.put("tagNames", t.getTagNames());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLatest(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        return mcpServerRepository.findAllByOrderByIdDesc(pageable).getContent().stream()
                .map(this::toResponseItem)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        McpServer t = getById(id);
        mcpServerRepository.delete(t);
    }

    public boolean existsByName(String name) {
        return name != null && !name.isBlank() && mcpServerRepository.existsByNameIgnoreCase(name.trim());
    }

    @Transactional
    public McpServer create(String name, String summary, String content, String url, String logoUrl, String tagNames) {
        McpServer t = new McpServer();
        t.setName(name);
        t.setSummary(summary != null && summary.length() > 500 ? summary.substring(0, 500) : summary);
        t.setContent(content);
        t.setDescription(summary != null ? summary : content);
        t.setUrl(url);
        t.setLogoUrl(logoUrl);
        t.setTagNames(tagNames != null && tagNames.length() > 500 ? tagNames.substring(0, 500) : tagNames);
        return mcpServerRepository.save(t);
    }
}
