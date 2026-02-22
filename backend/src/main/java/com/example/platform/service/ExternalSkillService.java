package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.ExternalSkillDto;
import com.example.platform.dto.ExternalSkillWriteRequest;
import com.example.platform.entity.ExternalSkill;
import com.example.platform.entity.ExternalSkill.Visibility;
import com.example.platform.entity.Tag;
import com.example.platform.repository.ExternalSkillRepository;
import com.example.platform.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExternalSkillService {

    private static final int KEYWORD_MAX_LENGTH = 200;

    private final ExternalSkillRepository externalSkillRepository;
    private final TagRepository tagRepository;

    public ExternalSkillService(ExternalSkillRepository externalSkillRepository,
                               TagRepository tagRepository) {
        this.externalSkillRepository = externalSkillRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public Page<ExternalSkillDto> listAllForAdmin(String keyword, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ExternalSkill> p = externalSkillRepository.findAllForAdmin(kw, pageable);
        return p.map(ExternalSkillDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ExternalSkill> listVisible(String keyword, List<Long> tagIds, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (tagIds == null || tagIds.isEmpty()) {
            return externalSkillRepository.findVisibleByCategoryAndKeyword(Visibility.VISIBLE, kw, pageable);
        }
        return externalSkillRepository.findVisibleByCategoryKeywordAndTagIds(Visibility.VISIBLE, kw, tagIds, tagIds.size(), pageable);
    }

    /** 列表转 DTO 在同一事务内完成，避免 LazyInitializationException（tags 懒加载）. */
    @Transactional(readOnly = true)
    public Page<ExternalSkillDto> listVisibleAsDto(String keyword, List<Long> tagIds, int page, int size) {
        Page<ExternalSkill> p = listVisible(keyword, tagIds, page, size);
        List<ExternalSkillDto> dtos = p.getContent().stream().map(ExternalSkillDto::fromEntity).collect(Collectors.toList());
        return new PageImpl<>(dtos, p.getPageable(), p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ExternalSkill getVisibleById(Long id) {
        ExternalSkill e = externalSkillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("外部 Skill 不存在"));
        if (e.getVisibility() != Visibility.VISIBLE) {
            throw new ResourceNotFoundException("外部 Skill 不存在或已隐藏");
        }
        return e;
    }

    /** 详情转 DTO 在同一事务内完成，避免 LazyInitializationException（tags 懒加载）. */
    @Transactional(readOnly = true)
    public ExternalSkillDto getVisibleDtoById(Long id) {
        ExternalSkill e = getVisibleById(id);
        return ExternalSkillDto.fromEntity(e);
    }

    @Transactional(readOnly = true)
    public ExternalSkill getById(Long id) {
        return externalSkillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("外部 Skill 不存在"));
    }

    @Transactional(readOnly = true)
    public ExternalSkillDto getDtoByIdForAdmin(Long id) {
        ExternalSkill e = getById(id);
        return ExternalSkillDto.fromEntity(e);
    }

    @Transactional
    public ExternalSkill create(ExternalSkillWriteRequest req) {
        ExternalSkill e = new ExternalSkill();
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setContent(req.getContent());
        e.setInstallCommand(req.getInstallCommand());
        e.setSourceUrl(req.getSourceUrl());
        e.setVisibility(Visibility.VISIBLE);
        e.setTags(resolveTags(req.getTags()));
        return externalSkillRepository.save(e);
    }

    @Transactional
    public ExternalSkill update(Long id, ExternalSkillWriteRequest req) {
        ExternalSkill e = getById(id);
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setContent(req.getContent());
        e.setInstallCommand(req.getInstallCommand());
        e.setSourceUrl(req.getSourceUrl());
        e.setTags(resolveTags(req.getTags()));
        return externalSkillRepository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        ExternalSkill e = getById(id);
        externalSkillRepository.delete(e);
    }

    public boolean existsByName(String name) {
        return name != null && !name.isBlank() && externalSkillRepository.existsByNameIgnoreCase(name.trim());
    }

    @Transactional
    public ExternalSkill createFromScraper(String name, String description, String content, String sourceUrl, String installCommandFromPage, List<String> tagNames) {
        String installCommand;
        if (installCommandFromPage != null && !installCommandFromPage.isBlank()) {
            installCommand = installCommandFromPage.trim();
        } else {
            installCommand = sourceUrl != null && !sourceUrl.isBlank()
                    ? ("见来源: " + (sourceUrl.length() > 450 ? sourceUrl.substring(0, 450) + "..." : sourceUrl))
                    : "见管理后台来源链接";
        }
        if (installCommand.length() > 500) installCommand = installCommand.substring(0, 500);
        ExternalSkill e = new ExternalSkill();
        e.setName(name);
        e.setDescription(description);
        e.setContent(content);
        e.setInstallCommand(installCommand);
        e.setSourceUrl(sourceUrl);
        e.setVisibility(Visibility.VISIBLE);
        e.setTags(resolveTags(tagNames != null ? tagNames : new ArrayList<>()));
        return externalSkillRepository.save(e);
    }

    private List<Tag> resolveTags(List<String> names) {
        if (names == null || names.isEmpty()) return new ArrayList<>();
        List<Tag> result = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            String n = name.trim();
            result.add(tagRepository.findByNameIgnoreCase(n).orElseGet(() -> {
                Tag t = new Tag();
                t.setName(n);
                return tagRepository.save(t);
            }));
        }
        return result;
    }
}
