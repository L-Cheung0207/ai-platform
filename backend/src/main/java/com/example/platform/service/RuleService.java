package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.CreateRuleRequest;
import com.example.platform.dto.RuleDto;
import com.example.platform.entity.Rule;
import com.example.platform.entity.Tag;
import com.example.platform.entity.User;
import com.example.platform.entity.Rule.Visibility;
import com.example.platform.repository.RuleRepository;
import com.example.platform.repository.TagRepository;
import com.example.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RuleService {

    private static final int KEYWORD_MAX_LENGTH = 200;

    private final RuleRepository ruleRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public RuleService(RuleRepository ruleRepository, TagRepository tagRepository,
                       UserRepository userRepository) {
        this.ruleRepository = ruleRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<RuleDto> listAllForAdmin(String keyword, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Rule> p = ruleRepository.findAllForAdmin(kw, pageable);
        List<RuleDto> dtos = p.getContent().stream().map(RuleDto::fromEntity).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<RuleDto> listVisible(String keyword, List<Long> tagIds, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Rule> p;
        if (tagIds == null || tagIds.isEmpty()) {
            p = ruleRepository.findVisibleByCategoryAndKeyword(Visibility.VISIBLE, kw, pageable);
        } else {
            p = ruleRepository.findVisibleByCategoryKeywordAndTagIds(Visibility.VISIBLE, kw, tagIds, tagIds.size(), pageable);
        }
        // 必须在事务内完成 DTO 转换，否则 map() 的惰性求值会在事务外触发懒加载
        List<RuleDto> dtos = p.getContent().stream().map(RuleDto::fromEntity).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RuleDto getVisibleById(Long id) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        if (r.getVisibility() != Visibility.VISIBLE) {
            throw new ResourceNotFoundException("Rule 不存在或已隐藏");
        }
        return RuleDto.fromEntity(r);
    }

    @Transactional(readOnly = true)
    public Page<Rule> listMy(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ruleRepository.findByUploaderIdOrderByUpdatedAtDesc(userId, pageable);
    }

    @Transactional
    public RuleDto create(CreateRuleRequest req, Long uploaderId) {
        User uploader = userRepository.findById(uploaderId).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Rule r = new Rule();
        r.setName(req.getName());
        r.setContent(req.getContent());
        r.setUploader(uploader);
        r.setVisibility(Visibility.VISIBLE);
        r.setTags(resolveTags(req.getTags()));
        r = ruleRepository.save(r);
        return RuleDto.fromEntity(r);
    }

    @Transactional
    public Rule update(Long id, CreateRuleRequest req, Long userId) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        if (!r.getUploader().getId().equals(userId)) {
            throw new AccessDeniedException("只能编辑自己上传的内容");
        }
        r.setName(req.getName());
        r.setContent(req.getContent());
        r.setTags(resolveTags(req.getTags()));
        return ruleRepository.save(r);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        if (!r.getUploader().getId().equals(userId)) {
            throw new AccessDeniedException("只能删除自己上传的内容");
        }
        ruleRepository.delete(r);
    }

    @Transactional
    public RuleDto adminHide(Long id) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        r.setVisibility(Visibility.HIDDEN);
        r = ruleRepository.save(r);
        return RuleDto.fromEntity(r);
    }

    @Transactional
    public RuleDto adminUnhide(Long id) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        r.setVisibility(Visibility.VISIBLE);
        r = ruleRepository.save(r);
        return RuleDto.fromEntity(r);
    }

    @Transactional
    public void adminDelete(Long id) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        ruleRepository.delete(r);
    }

    @Transactional
    public RuleDto adminUpdate(Long id, CreateRuleRequest req) {
        Rule r = ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule 不存在"));
        r.setName(req.getName());
        r.setContent(req.getContent());
        r.setTags(resolveTags(req.getTags()));
        r = ruleRepository.save(r);
        return RuleDto.fromEntity(r);
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
