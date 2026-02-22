package com.example.platform.service;

import com.example.platform.dto.LlmLeaderboardEntryDto;
import com.example.platform.entity.LlmLeaderboardEntry;
import com.example.platform.repository.LlmLeaderboardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LlmLeaderboardService {

    private static final int KEYWORD_MAX = 100;
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "displayOrder", "arenaElo", "coding", "vision", "aaii", "mmluPro", "arcAgi", "modelName", "organization");

    private final LlmLeaderboardRepository repository;

    public LlmLeaderboardService(LlmLeaderboardRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<LlmLeaderboardEntryDto> list(String keyword, int page, int size, String sortBy, String sortOrder) {
        if (keyword != null && keyword.length() > KEYWORD_MAX) {
            keyword = keyword.trim().substring(0, KEYWORD_MAX);
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String field = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "displayOrder";
        Sort.Direction dir = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(dir, field));
        Page<LlmLeaderboardEntry> p = repository.findByKeyword(kw, pageable);
        List<LlmLeaderboardEntryDto> dtos = p.getContent().stream().map(LlmLeaderboardEntryDto::fromEntity).collect(Collectors.toList());
        return new PageImpl<>(dtos, p.getPageable(), p.getTotalElements());
    }
}
