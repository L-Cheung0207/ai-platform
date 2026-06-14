package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.SkillDto;
import com.example.platform.entity.Skill;
import com.example.platform.entity.Skill.AssetLevel;
import com.example.platform.entity.Skill.BuildPriority;
import com.example.platform.entity.Skill.CreationSource;
import com.example.platform.entity.Skill.LifecycleStatus;
import com.example.platform.entity.Skill.RiskLevel;
import com.example.platform.entity.Skill.SkillCategory;
import com.example.platform.entity.Skill.TemplateValidationStatus;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SkillService {

    private static final int KEYWORD_MAX_LENGTH = 200;

    private final SkillRepository skillRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final GitLabSkillPublishService gitLabSkillPublishService;

    public SkillService(SkillRepository skillRepository, TagRepository tagRepository,
                        UserRepository userRepository,
                        GitLabSkillPublishService gitLabSkillPublishService) {
        this.skillRepository = skillRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.gitLabSkillPublishService = gitLabSkillPublishService;
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> listAllForAdmin(String keyword, int page, int size) {
        return listAllForAdmin(keyword, null, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> listAllForAdmin(String keyword, String assetLevel, String lifecycleStatus, int page, int size) {
        return listAllForAdmin(keyword, assetLevel, lifecycleStatus, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> listAllForAdmin(String keyword, String assetLevel, String lifecycleStatus, String skillCategory, String buildPriority, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        AssetLevel level = parseEnum(AssetLevel.class, assetLevel, "资产层级");
        LifecycleStatus status = parseEnum(LifecycleStatus.class, lifecycleStatus, "生命周期状态");
        SkillCategory category = parseEnum(SkillCategory.class, skillCategory, "Skill 分类");
        BuildPriority priority = parseEnum(BuildPriority.class, buildPriority, "建设优先级");
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Skill> p = skillRepository.findAllForAdmin(kw, level, status, category, priority, pageable);
        return p.map(SkillDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SkillDto> listVisible(String keyword, List<Long> tagIds, int page, int size) {
        return listVisible(keyword, tagIds, null, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SkillDto> listVisible(String keyword, List<Long> tagIds, String assetLevel, String lifecycleStatus, int page, int size) {
        return listVisible(keyword, tagIds, assetLevel, lifecycleStatus, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SkillDto> listVisible(String keyword, List<Long> tagIds, String assetLevel, String lifecycleStatus, String skillCategory, String buildPriority, int page, int size) {
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("关键词最多 " + KEYWORD_MAX_LENGTH + " 字符");
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        AssetLevel level = parseEnum(AssetLevel.class, assetLevel, "资产层级");
        LifecycleStatus status = LifecycleStatus.APPROVED;
        SkillCategory category = parseEnum(SkillCategory.class, skillCategory, "Skill 分类");
        BuildPriority priority = parseEnum(BuildPriority.class, buildPriority, "建设优先级");
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Skill> p = tagIds == null || tagIds.isEmpty()
            ? skillRepository.findVisibleByCategoryAndKeyword(Visibility.VISIBLE, level, status, category, priority, kw, pageable)
            : skillRepository.findVisibleByCategoryKeywordAndTagIds(Visibility.VISIBLE, level, status, category, priority, kw, tagIds, tagIds.size(), pageable);
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
    public SkillDto getPublicById(Long id) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        if (s.getVisibility() != Visibility.VISIBLE || s.getLifecycleStatus() != LifecycleStatus.APPROVED) {
            throw new ResourceNotFoundException("Skill 不存在或尚未通过评审");
        }
        return SkillDto.fromEntity(s);
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> listMy(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return skillRepository.findByUploaderIdOrderByUpdatedAtDesc(userId, pageable).map(SkillDto::fromEntity);
    }

    @Transactional
    public SkillDto create(CreateSkillRequest req, Long uploaderId) {
        return createWithSource(req, uploaderId, CreationSource.MANUAL);
    }

    @Transactional
    public SkillDto createWithSource(CreateSkillRequest req, Long uploaderId, CreationSource creationSource) {
        User uploader = userRepository.findById(uploaderId).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Skill s = new Skill();
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setCloneCommand(req.getCloneCommand());
        s.setContentMd(req.getContentMd());
        s.setSkillPackageFiles(req.getSkillPackageFiles());
        s.setUploader(uploader);
        s.setVisibility(Visibility.VISIBLE);
        s.setCreationSource(creationSource != null ? creationSource : CreationSource.MANUAL);
        ensureLifecycleChangeAllowed(null, req.getLifecycleStatus());
        applyAssetFields(s, req, uploader.getUsername());
        clearTemplateValidation(s);
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
        boolean templateChanged = templateFieldsChanged(s, req);
        ensureLifecycleChangeAllowed(s.getLifecycleStatus(), req.getLifecycleStatus());
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setCloneCommand(req.getCloneCommand());
        s.setContentMd(req.getContentMd());
        s.setSkillPackageFiles(req.getSkillPackageFiles());
        applyAssetFields(s, req, s.getUploader() != null ? s.getUploader().getUsername() : null);
        if (templateChanged) {
            resetTemplateValidationAfterChange(s);
        }
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
        gitLabSkillPublishService.deleteSkillDirectory(s.getSkillDirectory(), s.getName());
        skillRepository.delete(s);
    }

    @Transactional
    public SkillDto adminUpdate(Long id, CreateSkillRequest req) {
        Skill s = skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        boolean templateChanged = templateFieldsChanged(s, req);
        ensureLifecycleChangeAllowed(s.getLifecycleStatus(), req.getLifecycleStatus());
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setCloneCommand(req.getCloneCommand());
        s.setContentMd(req.getContentMd());
        s.setSkillPackageFiles(req.getSkillPackageFiles());
        applyAssetFields(s, req, s.getUploader() != null ? s.getUploader().getUsername() : null);
        if (templateChanged) {
            resetTemplateValidationAfterChange(s);
        }
        s.setTags(resolveTags(req.getTags()));
        s = skillRepository.save(s);
        return SkillDto.fromEntity(s);
    }

    private void applyAssetFields(Skill s, CreateSkillRequest req, String defaultMaintainer) {
        s.setAssetLevel(req.getAssetLevel() != null ? req.getAssetLevel() : AssetLevel.TEAM);
        s.setLifecycleStatus(req.getLifecycleStatus() != null ? req.getLifecycleStatus() : LifecycleStatus.CANDIDATE);
        s.setSkillCategory(req.getSkillCategory() != null ? req.getSkillCategory() : SkillCategory.CODING_IMPLEMENTATION);
        s.setBuildPriority(req.getBuildPriority() != null ? req.getBuildPriority() : BuildPriority.P2);
        s.setMaintainer(blankToNull(req.getMaintainer()) != null ? req.getMaintainer().trim() : defaultMaintainer);
        s.setTeamName(blankToNull(req.getTeamName()));
        s.setVersion(blankToNull(req.getVersion()) != null ? req.getVersion().trim() : "1.0.0");
        s.setSourceRepositoryUrl(blankToNull(req.getSourceRepositoryUrl()));
        s.setSkillDirectory(blankToNull(req.getSkillDirectory()));
        s.setApplicableScenarios(blankToNull(req.getApplicableScenarios()));
        s.setNonApplicableScenarios(blankToNull(req.getNonApplicableScenarios()));
        s.setInputRequirements(blankToNull(req.getInputRequirements()));
        s.setExecutionSteps(blankToNull(req.getExecutionSteps()));
        s.setOutputFormat(blankToNull(req.getOutputFormat()));
        s.setValidationMethod(blankToNull(req.getValidationMethod()));
        s.setQualityStandard(blankToNull(req.getQualityStandard()));
        s.setReferenceMaterials(blankToNull(req.getReferenceMaterials()));
        s.setRiskLevel(req.getRiskLevel() != null ? req.getRiskLevel() : RiskLevel.LOW);
        s.setReviewNotes(blankToNull(req.getReviewNotes()));
        s.setTrialStartedAt(req.getTrialStartedAt());
        s.setTrialEndsAt(req.getTrialEndsAt());
        s.setLastReviewedAt(req.getLastReviewedAt());
        s.setNextReviewAt(req.getNextReviewAt());
    }

    private void ensureLifecycleChangeAllowed(LifecycleStatus currentStatus, LifecycleStatus requestedStatus) {
        if (requestedStatus != LifecycleStatus.APPROVED) {
            return;
        }
        if (currentStatus == LifecycleStatus.APPROVED) {
            return;
        }
        throw new IllegalArgumentException("已入库状态只能通过模板校验后的评审通过产生");
    }

    private boolean templateFieldsChanged(Skill skill, CreateSkillRequest req) {
        return !sameText(skill.getName(), req.getName())
                || !sameText(skill.getDescription(), req.getDescription())
                || !sameText(skill.getCloneCommand(), req.getCloneCommand())
                || !sameText(skill.getContentMd(), req.getContentMd())
                || !sameText(skill.getSkillPackageFiles(), req.getSkillPackageFiles())
                || !sameText(skill.getSourceRepositoryUrl(), req.getSourceRepositoryUrl())
                || !sameText(skill.getSkillDirectory(), req.getSkillDirectory())
                || !sameText(skill.getMaintainer(), req.getMaintainer())
                || !sameText(skill.getTeamName(), req.getTeamName())
                || !sameText(skill.getVersion(), req.getVersion())
                || !sameText(skill.getApplicableScenarios(), req.getApplicableScenarios())
                || !sameText(skill.getNonApplicableScenarios(), req.getNonApplicableScenarios())
                || !sameText(skill.getInputRequirements(), req.getInputRequirements())
                || !sameText(skill.getExecutionSteps(), req.getExecutionSteps())
                || !sameText(skill.getOutputFormat(), req.getOutputFormat())
                || !sameText(skill.getValidationMethod(), req.getValidationMethod())
                || !sameText(skill.getQualityStandard(), req.getQualityStandard())
                || !sameText(skill.getReferenceMaterials(), req.getReferenceMaterials())
                || !Objects.equals(skill.getRiskLevel(), req.getRiskLevel());
    }

    private boolean sameText(String current, String next) {
        return Objects.equals(blankToNull(current), blankToNull(next));
    }

    private void resetTemplateValidationAfterChange(Skill skill) {
        clearTemplateValidation(skill);
        if (skill.getLifecycleStatus() == LifecycleStatus.APPROVED) {
            skill.setLifecycleStatus(LifecycleStatus.NEEDS_REVIEW);
            if (blankToNull(skill.getReviewNotes()) == null) {
                skill.setReviewNotes("模板内容已变更，需要重新校验并评审。");
            }
        }
    }

    private void clearTemplateValidation(Skill skill) {
        skill.setTemplateValidationStatus(TemplateValidationStatus.UNVALIDATED);
        skill.setTemplateValidationNotes(null);
        skill.setLastValidatedAt(null);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(label + "不合法：" + value);
        }
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
