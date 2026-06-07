package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.SkillGitLabPublishResultDto;
import com.example.platform.dto.SkillOperationsDto;
import com.example.platform.dto.SkillReviewDto;
import com.example.platform.dto.SkillReviewRequest;
import com.example.platform.dto.SkillUsageRequest;
import com.example.platform.entity.Skill;
import com.example.platform.entity.Skill.AssetLevel;
import com.example.platform.entity.Skill.LifecycleStatus;
import com.example.platform.entity.Skill.RiskLevel;
import com.example.platform.entity.Skill.TemplateValidationStatus;
import com.example.platform.entity.SkillReview;
import com.example.platform.entity.SkillReview.ReviewResult;
import com.example.platform.entity.SkillReview.ReviewStage;
import com.example.platform.entity.SkillReview.ReviewerRole;
import com.example.platform.entity.SkillFeedback;
import com.example.platform.entity.SkillFeedback.FeedbackStatus;
import com.example.platform.entity.SkillFeedback.FeedbackType;
import com.example.platform.entity.SkillUsageEvent;
import com.example.platform.entity.SkillUsageEvent.ToolchainSource;
import com.example.platform.entity.User;
import com.example.platform.repository.SkillFeedbackRepository;
import com.example.platform.repository.SkillOperationReportRepository;
import com.example.platform.repository.SkillRepository;
import com.example.platform.repository.SkillReviewRepository;
import com.example.platform.repository.SkillUsageEventRepository;
import com.example.platform.repository.TagRepository;
import com.example.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillGovernanceGateTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SkillReviewRepository skillReviewRepository;

    @Mock
    private SkillFeedbackRepository skillFeedbackRepository;

    @Mock
    private SkillUsageEventRepository skillUsageEventRepository;

    @Mock
    private SkillOperationReportRepository skillOperationReportRepository;

    private StubGitLabSkillPublishService gitLabSkillPublishService;
    private SkillService skillService;
    private SkillGovernanceService governanceService;

    @BeforeEach
    void setUp() {
        gitLabSkillPublishService = new StubGitLabSkillPublishService();
        skillService = new SkillService(skillRepository, tagRepository, userRepository, gitLabSkillPublishService);
        governanceService = new SkillGovernanceService(
                skillRepository,
                skillReviewRepository,
                skillFeedbackRepository,
                skillUsageEventRepository,
                skillOperationReportRepository,
                userRepository
        );
    }

    @Test
    void createRejectsManualApprovedLifecycleStatus() {
        User uploader = user(7L, "owner");
        CreateSkillRequest request = baseRequest();
        request.setLifecycleStatus(LifecycleStatus.APPROVED);
        when(userRepository.findById(7L)).thenReturn(Optional.of(uploader));

        assertThatThrownBy(() -> skillService.create(request, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("模板校验后的评审通过");

        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void publicListAlwaysShowsApprovedSkillsOnly() {
        when(skillRepository.findVisibleByCategoryAndKeyword(
                eq(Skill.Visibility.VISIBLE),
                eq(null),
                eq(LifecycleStatus.APPROVED),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        skillService.listVisible(null, List.of(), null, "CANDIDATE", null, null, 1, 20);

        verify(skillRepository).findVisibleByCategoryAndKeyword(
                eq(Skill.Visibility.VISIBLE),
                eq(null),
                eq(LifecycleStatus.APPROVED),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        );
    }

    @Test
    void publicDetailRejectsVisibleSkillBeforeApproval() {
        Skill skill = skill(21L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        skill.setLifecycleStatus(LifecycleStatus.REVIEWING);
        when(skillRepository.findById(21L)).thenReturn(Optional.of(skill));

        assertThatThrownBy(() -> skillService.getPublicById(21L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("尚未通过评审");
    }

    @Test
    void adminDeleteRequestsGitLabDirectoryRemovalBeforeDeletingDatabaseRecord() {
        Skill skill = skill(22L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(22L)).thenReturn(Optional.of(skill));

        skillService.adminDelete(22L);

        assertThat(gitLabSkillPublishService.deleteCalls).isEqualTo(1);
        assertThat(gitLabSkillPublishService.deletedSkillDirectory).isEqualTo("unit-test-skill");
        assertThat(gitLabSkillPublishService.deletedSkillName).isEqualTo("Unit Test Skill");
        verify(skillRepository).delete(skill);
    }

    @Test
    void adminDeleteKeepsDatabaseRecordWhenGitLabRemovalFails() {
        Skill skill = skill(23L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(23L)).thenReturn(Optional.of(skill));
        gitLabSkillPublishService.failDeletion = true;

        assertThatThrownBy(() -> skillService.adminDelete(23L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GitLab 删除失败");

        verify(skillRepository, never()).delete(any(Skill.class));
    }

    @Test
    void createReviewRejectsPassedResultBeforeTemplateValidationPasses() {
        Skill skill = skill(1L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.UNVALIDATED);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "owner", ReviewerRole.TECH_LEAD)));

        assertThatThrownBy(() -> governanceService.createReview(1L, passedReview(ReviewerRole.TECH_LEAD), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("模板校验通过后");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillReviewRepository, never()).save(any(SkillReview.class));
    }

    @Test
    void createReviewRejectsCompanyApprovalByTechLead() {
        Skill skill = skill(2L, AssetLevel.COMPANY, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "owner", ReviewerRole.TECH_LEAD)));

        assertThatThrownBy(() -> governanceService.createReview(2L, passedReview(ReviewerRole.TECH_LEAD), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("公司级资产");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillReviewRepository, never()).save(any(SkillReview.class));
    }

    @Test
    void createReviewRejectsApprovalByContributor() {
        Skill skill = skill(14L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(14L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "contributor", ReviewerRole.CONTRIBUTOR)));

        assertThatThrownBy(() -> governanceService.createReview(14L, passedReview(ReviewerRole.TECH_LEAD), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("贡献者不能直接评审入库");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillReviewRepository, never()).save(any(SkillReview.class));
    }

    @Test
    void createReviewRejectsHighRiskApprovalByTechLead() {
        Skill skill = skill(3L, AssetLevel.TEAM, RiskLevel.HIGH, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(3L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "owner", ReviewerRole.TECH_LEAD)));

        assertThatThrownBy(() -> governanceService.createReview(3L, passedReview(ReviewerRole.TECH_LEAD), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("高风险 Skill");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillReviewRepository, never()).save(any(SkillReview.class));
    }

    @Test
    void createReviewApprovesValidatedTeamSkill() {
        LocalDate reviewedAt = LocalDate.of(2026, 5, 31);
        LocalDate nextReviewAt = LocalDate.of(2026, 8, 31);
        Skill skill = skill(4L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        skill.setLifecycleStatus(LifecycleStatus.REVIEWING);
        when(skillRepository.findById(4L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Reviewer", ReviewerRole.TECH_LEAD)));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillReviewRepository.save(any(SkillReview.class))).thenAnswer(invocation -> {
            SkillReview review = invocation.getArgument(0);
            review.setId(99L);
            return review;
        });

        SkillReviewDto dto = governanceService.createReview(4L, new SkillReviewRequest(
                "Reviewer",
                ReviewerRole.TECH_LEAD,
                ReviewStage.TEAM_REVIEW,
                ReviewResult.PASSED,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "ready",
                reviewedAt,
                nextReviewAt
        ), 7L);

        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.result()).isEqualTo(ReviewResult.PASSED.name());
        assertThat(dto.reviewerRole()).isEqualTo(ReviewerRole.TECH_LEAD.name());
        assertThat(dto.reviewerName()).isEqualTo("Reviewer");
        assertThat(skill.getLifecycleStatus()).isEqualTo(LifecycleStatus.APPROVED);
        assertThat(skill.getLastReviewedAt()).isEqualTo(reviewedAt);
        assertThat(skill.getNextReviewAt()).isEqualTo(nextReviewAt);
        assertThat(skill.getReviewNotes()).isEqualTo("ready");
    }

    @Test
    void createReviewIgnoresRequestReviewerRoleAndUsesAuthenticatedUserRole() {
        Skill skill = skill(12L, AssetLevel.COMPANY, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(12L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "owner", ReviewerRole.TECH_LEAD)));

        assertThatThrownBy(() -> governanceService.createReview(12L, passedReview(ReviewerRole.TECHNICAL_COMMITTEE), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("公司级资产");

        verify(skillRepository, never()).save(any(Skill.class));
        verify(skillReviewRepository, never()).save(any(SkillReview.class));
    }

    @Test
    void createReviewAllowsHighRiskApprovalForSecurityQualityUser() {
        Skill skill = skill(13L, AssetLevel.TEAM, RiskLevel.HIGH, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(13L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, "security", ReviewerRole.SECURITY_QUALITY)));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillReviewRepository.save(any(SkillReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SkillReviewDto dto = governanceService.createReview(13L, passedReview(ReviewerRole.TECH_LEAD), 9L);

        assertThat(dto.reviewerName()).isEqualTo("security");
        assertThat(dto.reviewerRole()).isEqualTo(ReviewerRole.SECURITY_QUALITY.name());
        assertThat(skill.getLifecycleStatus()).isEqualTo(LifecycleStatus.APPROVED);
    }

    @Test
    void adminUpdateResetsApprovedSkillWhenTemplateFieldsChange() {
        Instant validatedAt = Instant.parse("2026-05-01T00:00:00Z");
        Skill skill = skill(5L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        skill.setLifecycleStatus(LifecycleStatus.APPROVED);
        skill.setLastValidatedAt(validatedAt);
        skill.setReviewNotes(null);
        when(skillRepository.findById(5L)).thenReturn(Optional.of(skill));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSkillRequest request = baseRequest();
        request.setLifecycleStatus(LifecycleStatus.APPROVED);
        request.setExecutionSteps("1. Run the improved workflow");

        skillService.adminUpdate(5L, request);

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(captor.capture());
        Skill saved = captor.getValue();
        assertThat(saved.getLifecycleStatus()).isEqualTo(LifecycleStatus.NEEDS_REVIEW);
        assertThat(saved.getTemplateValidationStatus()).isEqualTo(TemplateValidationStatus.UNVALIDATED);
        assertThat(saved.getTemplateValidationNotes()).isNull();
        assertThat(saved.getLastValidatedAt()).isNull();
        assertThat(saved.getReviewNotes()).contains("模板内容已变更");
    }

    @Test
    void validateTemplateRequiresSharedMetadataForTeamAssets() {
        Skill skill = skill(6L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.UNVALIDATED);
        skill.setContentMd(validSkillMd());
        skill.setSourceRepositoryUrl(null);
        skill.setSkillDirectory(null);
        when(skillRepository.findById(6L)).thenReturn(Optional.of(skill));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var report = governanceService.validateTemplate(6L);

        assertThat(report.passed()).isFalse();
        assertThat(report.items())
                .anySatisfy(item -> {
                    assertThat(item.key()).isEqualTo("sourceRepositoryUrl");
                    assertThat(item.required()).isTrue();
                    assertThat(item.passed()).isFalse();
                })
                .anySatisfy(item -> {
                    assertThat(item.key()).isEqualTo("skillDirectory");
                    assertThat(item.required()).isTrue();
                    assertThat(item.passed()).isFalse();
                });
        assertThat(skill.getTemplateValidationStatus()).isEqualTo(TemplateValidationStatus.FAILED);
    }

    @Test
    void operationsReportIncludesArchiveCandidatesForStaleAndUnstableSkills() {
        LocalDate today = LocalDate.now();
        Instant oldCreatedAt = Instant.now().minus(120, java.time.temporal.ChronoUnit.DAYS);
        Skill staleSkill = skill(8L, AssetLevel.TEAM, RiskLevel.HIGH, TemplateValidationStatus.PASSED);
        staleSkill.setLifecycleStatus(LifecycleStatus.APPROVED);
        staleSkill.setCreatedAt(oldCreatedAt);
        staleSkill.setNextReviewAt(today.minusDays(3));
        Skill activeSkill = skill(9L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        activeSkill.setLifecycleStatus(LifecycleStatus.APPROVED);
        activeSkill.setCreatedAt(Instant.now().minus(10, java.time.temporal.ChronoUnit.DAYS));

        SkillUsageEvent activeUsage = usage(activeSkill, Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));
        List<SkillFeedback> feedbacks = List.of(
                feedback(staleSkill, FeedbackType.FAILURE_CASE, FeedbackStatus.OPEN),
                feedback(staleSkill, FeedbackType.FAILURE_CASE, FeedbackStatus.OPEN)
        );

        when(skillRepository.findAll()).thenReturn(List.of(staleSkill, activeSkill));
        when(skillUsageEventRepository.findAll()).thenReturn(List.of(activeUsage));
        when(skillUsageEventRepository.findByCreatedAtAfter(any(Instant.class))).thenReturn(List.of());
        when(skillFeedbackRepository.findAll()).thenReturn(feedbacks);
        when(skillFeedbackRepository.findByCreatedAtAfter(any(Instant.class))).thenReturn(List.of());
        when(skillOperationReportRepository.count()).thenReturn(0L);

        SkillOperationsDto report = governanceService.operationsReport();

        assertThat(report.archiveCandidates()).hasSize(1);
        var candidate = report.archiveCandidates().get(0);
        assertThat(candidate.skillId()).isEqualTo(8L);
        assertThat(candidate.usageCount()).isZero();
        assertThat(candidate.openFeedbackCount()).isEqualTo(2);
        assertThat(candidate.failureCaseCount()).isEqualTo(2);
        assertThat(candidate.riskScore()).isGreaterThanOrEqualTo(100);
        assertThat(candidate.recommendedAction()).isEqualTo("建议发起归档/下架评审");
        assertThat(candidate.reasons())
                .contains("90 天无人使用", "复审过期或待复审", "失败案例 2 条", "高风险且有待处理反馈");
    }

    @Test
    void recordUsageStoresToolchainTelemetry() {
        Skill skill = skill(10L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(skillUsageEventRepository.existsBySkill_IdAndExternalEventId(10L, "github-pr-128-review-1")).thenReturn(false);
        stubEmptySummary(10L);

        governanceService.recordUsage(10L, new SkillUsageRequest(
                "ci-bot",
                "PR #128 code review",
                30,
                0,
                8,
                3,
                62.5,
                70.2,
                ToolchainSource.CODE_REVIEW,
                "github-pr-128-review-1",
                "teleone/checkout",
                "feature/refactor-payment",
                "abc1234",
                "PASSED"
        ));

        ArgumentCaptor<SkillUsageEvent> captor = ArgumentCaptor.forClass(SkillUsageEvent.class);
        verify(skillUsageEventRepository).save(captor.capture());
        SkillUsageEvent saved = captor.getValue();
        assertThat(saved.getSkill()).isEqualTo(skill);
        assertThat(saved.getToolchainSource()).isEqualTo(ToolchainSource.CODE_REVIEW);
        assertThat(saved.getExternalEventId()).isEqualTo("github-pr-128-review-1");
        assertThat(saved.getRepository()).isEqualTo("teleone/checkout");
        assertThat(saved.getBranchName()).isEqualTo("feature/refactor-payment");
        assertThat(saved.getCommitSha()).isEqualTo("abc1234");
        assertThat(saved.getCiStatus()).isEqualTo("PASSED");
        assertThat(saved.getReviewIssuesBefore()).isEqualTo(8);
        assertThat(saved.getReviewIssuesAfter()).isEqualTo(3);
        assertThat(saved.getTestCoverageBefore()).isEqualTo(62.5);
        assertThat(saved.getTestCoverageAfter()).isEqualTo(70.2);
    }

    @Test
    void recordUsageSkipsDuplicateExternalEvent() {
        Skill skill = skill(11L, AssetLevel.TEAM, RiskLevel.LOW, TemplateValidationStatus.PASSED);
        when(skillRepository.findById(11L)).thenReturn(Optional.of(skill));
        when(skillUsageEventRepository.existsBySkill_IdAndExternalEventId(11L, "jenkins-build-44")).thenReturn(true);
        stubEmptySummary(11L);

        governanceService.recordUsage(11L, new SkillUsageRequest(
                "ci-bot",
                "Jenkins build #44",
                20,
                null,
                null,
                null,
                null,
                null,
                ToolchainSource.CI,
                "jenkins-build-44",
                "teleone/platform",
                "main",
                "def5678",
                "PASSED"
        ));

        verify(skillUsageEventRepository, never()).save(any(SkillUsageEvent.class));
    }

    private void stubEmptySummary(Long skillId) {
        when(skillUsageEventRepository.findBySkill_Id(skillId)).thenReturn(List.of());
        when(skillFeedbackRepository.countBySkill_Id(skillId)).thenReturn(0L);
        when(skillFeedbackRepository.countBySkill_IdAndStatus(skillId, FeedbackStatus.OPEN)).thenReturn(0L);
        when(skillReviewRepository.countBySkill_Id(skillId)).thenReturn(0L);
        when(skillReviewRepository.findFirstBySkill_IdOrderByReviewedAtDescCreatedAtDesc(skillId)).thenReturn(Optional.empty());
    }

    private SkillReviewRequest passedReview(ReviewerRole reviewerRole) {
        return new SkillReviewRequest(
                "Reviewer",
                reviewerRole,
                ReviewStage.TEAM_REVIEW,
                ReviewResult.PASSED,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "ready",
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 8, 31)
        );
    }

    private CreateSkillRequest baseRequest() {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Unit Test Skill");
        request.setDescription("Reusable skill for unit tests");
        request.setTags(List.of());
        request.setCloneCommand("git clone https://example.com/skills.git");
        request.setContentMd("# Unit Test Skill\n\nUse this skill in tests.");
        request.setSourceRepositoryUrl("https://example.com/skills");
        request.setSkillDirectory("unit-test-skill");
        request.setAssetLevel(AssetLevel.TEAM);
        request.setLifecycleStatus(LifecycleStatus.CANDIDATE);
        request.setMaintainer("owner");
        request.setTeamName("Platform");
        request.setVersion("1.0.0");
        request.setApplicableScenarios("Java service governance");
        request.setNonApplicableScenarios("Ad-hoc prompts");
        request.setInputRequirements("Existing service code");
        request.setExecutionSteps("1. Run the workflow");
        request.setOutputFormat("Patch and test result");
        request.setValidationMethod("Unit test");
        request.setQualityStandard("All required gates are covered");
        request.setReferenceMaterials("docs/skill-template.md");
        request.setRiskLevel(RiskLevel.LOW);
        return request;
    }

    private String validSkillMd() {
        return """
                ---
                name: unit-test-skill
                description: Reusable skill for unit tests
                ---

                # Unit Test Skill

                Use this skill in tests.
                """;
    }

    private Skill skill(Long id, AssetLevel assetLevel, RiskLevel riskLevel, TemplateValidationStatus validationStatus) {
        User uploader = user(7L, "owner");
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName("Unit Test Skill");
        skill.setDescription("Reusable skill for unit tests");
        skill.setCloneCommand("git clone https://example.com/skills.git");
        skill.setContentMd("# Unit Test Skill\n\nUse this skill in tests.");
        skill.setSourceRepositoryUrl("https://example.com/skills");
        skill.setSkillDirectory("unit-test-skill");
        skill.setAssetLevel(assetLevel);
        skill.setLifecycleStatus(LifecycleStatus.CANDIDATE);
        skill.setMaintainer("owner");
        skill.setTeamName("Platform");
        skill.setVersion("1.0.0");
        skill.setApplicableScenarios("Java service governance");
        skill.setNonApplicableScenarios("Ad-hoc prompts");
        skill.setInputRequirements("Existing service code");
        skill.setExecutionSteps("1. Run the workflow");
        skill.setOutputFormat("Patch and test result");
        skill.setValidationMethod("Unit test");
        skill.setQualityStandard("All required gates are covered");
        skill.setReferenceMaterials("docs/skill-template.md");
        skill.setRiskLevel(riskLevel);
        skill.setTemplateValidationStatus(validationStatus);
        skill.setUploader(uploader);
        skill.setTags(List.of());
        return skill;
    }

    private SkillUsageEvent usage(Skill skill, Instant createdAt) {
        SkillUsageEvent usage = new SkillUsageEvent();
        usage.setSkill(skill);
        usage.setSavedMinutes(30);
        usage.setCreatedAt(createdAt);
        return usage;
    }

    private SkillFeedback feedback(Skill skill, FeedbackType type, FeedbackStatus status) {
        SkillFeedback feedback = new SkillFeedback();
        feedback.setSkill(skill);
        feedback.setFeedbackType(type);
        feedback.setStatus(status);
        feedback.setContent("feedback");
        return feedback;
    }

    private User user(Long id, String username) {
        return user(id, username, ReviewerRole.TECH_LEAD);
    }

    private User user(Long id, String username, ReviewerRole skillGovernanceRole) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setSkillGovernanceRole(skillGovernanceRole);
        return user;
    }

    private static class StubGitLabSkillPublishService extends GitLabSkillPublishService {
        private int deleteCalls;
        private String deletedSkillDirectory;
        private String deletedSkillName;
        private boolean failDeletion;

        private StubGitLabSkillPublishService() {
            super(new ObjectMapper(), false, "", "", "", "", "main", "skills", "skill");
        }

        @Override
        public SkillGitLabPublishResultDto deleteSkillDirectory(String skillDirectory, String skillName) {
            deleteCalls++;
            deletedSkillDirectory = skillDirectory;
            deletedSkillName = skillName;
            if (failDeletion) {
                throw new IllegalArgumentException("GitLab 删除失败");
            }
            return SkillGitLabPublishResultDto.disabled("stub");
        }
    }
}
