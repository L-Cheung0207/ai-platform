package com.example.platform.service;

import com.example.platform.entity.Rule;
import com.example.platform.entity.Skill;
import com.example.platform.entity.ExternalSkill;
import com.example.platform.entity.Tag;
import com.example.platform.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> list(String q, String forEntity) {
        if ("skills".equalsIgnoreCase(forEntity)) {
            return mergeSkillTags();
        }
        if ("rules".equalsIgnoreCase(forEntity)) {
            return tagRepository.findTagsUsedByVisibleRules(Rule.Visibility.VISIBLE);
        }
        if (q != null && !q.isBlank()) {
            return tagRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim());
        }
        return tagRepository.findAll();
    }

    private List<Tag> mergeSkillTags() {
        Map<Long, Tag> tagsById = tagRepository.findTagsUsedByVisibleSkills(
                        Skill.Visibility.VISIBLE,
                        Skill.LifecycleStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(Tag::getId, t -> t, (left, right) -> left));
        tagRepository.findTagsUsedByVisibleExternalSkills(ExternalSkill.Visibility.VISIBLE)
                .forEach(t -> tagsById.putIfAbsent(t.getId(), t));
        return tagsById.values().stream()
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
