package com.example.platform.service;

import com.example.platform.entity.Rule;
import com.example.platform.entity.Skill;
import com.example.platform.entity.Tag;
import com.example.platform.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> list(String q, String forEntity) {
        if ("skills".equalsIgnoreCase(forEntity)) {
            return tagRepository.findTagsUsedByVisibleSkills(Skill.Visibility.VISIBLE);
        }
        if ("rules".equalsIgnoreCase(forEntity)) {
            return tagRepository.findTagsUsedByVisibleRules(Rule.Visibility.VISIBLE);
        }
        if (q != null && !q.isBlank()) {
            return tagRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim());
        }
        return tagRepository.findAll();
    }


    @Transactional
    public Tag create(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("标签名不能为空");
        }
        return tagRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    Tag t = new Tag();
                    t.setName(normalized);
                    return tagRepository.save(t);
                });
    }
}
