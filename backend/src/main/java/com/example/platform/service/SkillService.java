package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.SkillDto;
import com.example.platform.entity.Skill;
import com.example.platform.entity.Tag;
import com.example.platform.entity.User;
import com.example.platform.entity.Skill.Visibility;
import com.example.platform.repository.SkillRepository;
import com.example.platform.repository.TagRepository;
import com.example.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private static final int KEYWORD_MAX_LENGTH = 200;

    private final SkillRepository skillRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public SkillService(SkillRepository skillRepository, TagRepository tagRepository,
                        UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> listAllForAdmin(String keyword, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Skill> p = skillRepository.findAllForAdmin(kw, pageable);
        return p.map(SkillDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SkillDto> listVisible(String keyword, List<Long> tagIds, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Skill> p = tagIds == null || tagIds.isEmpty()
            ? skillRepository.findVisibleByCategoryAndKeyword(Visibility.VISIBLE, kw, pageable)
            : skillRepository.findVisibleByCategoryKeywordAndTagIds(Visibility.VISIBLE, kw, tagIds, tagIds.size(), pageable);
        return p.map(SkillDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public SkillDto getVisibleById(Long id) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        if (s.getVisibility() != Visibility.VISIBLE) {
            throw new ResourceNotFoundException("Skill 不存在或已隐藏");
        }
        return SkillDto.fromEntity(s);
    }

    @Transactional(readOnly = true)
    public Page<Skill> listMy(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return skillRepository.findByUploaderIdOrderByUpdatedAtDesc(userId, pageable);
    }

    @Transactional
    public SkillDto create(CreateSkillRequest req, Long uploaderId) {
        User uploader = userRepository.findById(uploaderId).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Skill s = new Skill();
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setCloneCommand(req.getCloneCommand());
        s.setContentMd(req.getContentMd());
        s.setUploader(uploader);
        s.setVisibility(Visibility.VISIBLE);
        List<Tag> tags = resolveTags(req.getTags());
        s.setTags(tags);
        s = skillRepository.save(s);
        return SkillDto.fromEntity(s);
    }

    @Transactional
    public Skill update(Long id, CreateSkillRequest req, Long userId) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        if (!s.getUploader().getId().equals(userId)) {
            throw new AccessDeniedException("只能编辑自己登记的内容");
        }
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setCloneCommand(req.getCloneCommand());
        s.setContentMd(req.getContentMd());
        s.setTags(resolveTags(req.getTags()));
        return skillRepository.save(s);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        if (!s.getUploader().getId().equals(userId)) {
            throw new AccessDeniedException("只能删除自己登记的内容");
        }
        skillRepository.delete(s);
    }

    @Transactional
    public SkillDto adminHide(Long id) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        s.setVisibility(Visibility.HIDDEN);
        s = skillRepository.save(s);
        return SkillDto.fromEntity(s);
    }

    @Transactional
    public SkillDto adminUnhide(Long id) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        s.setVisibility(Visibility.VISIBLE);
        s = skillRepository.save(s);
        return SkillDto.fromEntity(s);
    }

    @Transactional
    public void adminDelete(Long id) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        skillRepository.delete(s);
    }

    @Transactional
    public SkillDto adminUpdate(Long id, CreateSkillRequest req) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setCloneCommand(req.getCloneCommand());
        s.setContentMd(req.getContentMd());
        s.setTags(resolveTags(req.getTags()));
        s = skillRepository.save(s);
        return SkillDto.fromEntity(s);
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
