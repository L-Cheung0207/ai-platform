package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.SkillArchiveCandidateDto;
import com.example.platform.dto.SkillAssetMetricsDto;
import com.example.platform.dto.SkillFeedbackDto;
import com.example.platform.dto.SkillFeedbackRequest;
import com.example.platform.dto.SkillFeedbackStatusRequest;
import com.example.platform.dto.SkillGitLabPublishResultDto;
import com.example.platform.dto.SkillGovernanceSummaryDto;
import com.example.platform.dto.SkillLeaderboardItemDto;
import com.example.platform.dto.SkillMonthlyAwardCandidateDto;
import com.example.platform.dto.SkillMonthlyReportDto;
import com.example.platform.dto.SkillMonthlyTrendDto;
import com.example.platform.dto.SkillOperationReportDto;
import com.example.platform.dto.SkillOperationsDto;
import com.example.platform.dto.SkillPilotMilestoneDto;
import com.example.platform.dto.SkillQuarterlyReportDto;
import com.example.platform.dto.SkillReviewDto;
import com.example.platform.dto.SkillReviewQueueItemDto;
import com.example.platform.dto.SkillReviewRequest;
import com.example.platform.dto.SkillTemplateValidationDto;
import com.example.platform.dto.SkillTemplateValidationItemDto;
import com.example.platform.dto.SkillToolchainTelemetryRequest;
import com.example.platform.dto.SkillUsageRequest;
import com.example.platform.entity.Skill;
import com.example.platform.entity.Skill.AssetLevel;
import com.example.platform.entity.Skill.BuildPriority;
import com.example.platform.entity.Skill.CreationSource;
import com.example.platform.entity.Skill.LifecycleStatus;
import com.example.platform.entity.Skill.RiskLevel;
import com.example.platform.entity.Skill.SkillCategory;
import com.example.platform.entity.Skill.TemplateValidationStatus;
import com.example.platform.entity.Skill.Visibility;
import com.example.platform.entity.SkillFeedback;
import com.example.platform.entity.SkillFeedback.FeedbackStatus;
import com.example.platform.entity.SkillFeedback.FeedbackType;
import com.example.platform.entity.SkillOperationReport;
import com.example.platform.entity.SkillReview;
import com.example.platform.entity.SkillReview.ReviewResult;
import com.example.platform.entity.SkillReview.ReviewStage;
import com.example.platform.entity.SkillReview.ReviewerRole;
import com.example.platform.entity.SkillUsageEvent;
import com.example.platform.entity.SkillUsageEvent.ToolchainSource;
import com.example.platform.entity.User;
import com.example.platform.repository.SkillFeedbackRepository;
import com.example.platform.repository.SkillOperationReportRepository;
import com.example.platform.repository.SkillRepository;
import com.example.platform.repository.SkillReviewRepository;
import com.example.platform.repository.SkillUsageEventRepository;
import com.example.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SkillGovernanceService {

    private static final int PAGE_SIZE_LIMIT = 50;
    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})[- ]?Q([1-4])$", Pattern.CASE_INSENSITIVE);
    private static final List<SensitiveContentPattern> SENSITIVE_CONTENT_PATTERNS = List.of(
            new SensitiveContentPattern(Pattern.compile("-----BEGIN (?:RSA |DSA |EC |OPENSSH |PGP )?PRIVATE KEY-----", Pattern.CASE_INSENSITIVE), "私钥"),
            new SensitiveContentPattern(Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"), "云访问密钥"),
            new SensitiveContentPattern(Pattern.compile("\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b"), "JWT/令牌"),
            new SensitiveContentPattern(Pattern.compile("\\b(?:api[_-]?key|access[_-]?key|secret[_-]?key|client[_-]?secret|password|passwd|pwd|token)\\s*[:=]\\s*['\\\"]?[A-Za-z0-9_./+=:-]{8,}", Pattern.CASE_INSENSITIVE), "凭据字段"),
            new SensitiveContentPattern(Pattern.compile("\\b[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b"), "身份证号")
    );

    private final SkillRepository skillRepository;
    private final SkillReviewRepository skillReviewRepository;
    private final SkillFeedbackRepository skillFeedbackRepository;
    private final SkillUsageEventRepository skillUsageEventRepository;
    private final SkillOperationReportRepository skillOperationReportRepository;
    private final UserRepository userRepository;
    private final GitLabSkillPublishService gitLabSkillPublishService;

    public SkillGovernanceService(SkillRepository skillRepository,
                                  SkillReviewRepository skillReviewRepository,
                                  SkillFeedbackRepository skillFeedbackRepository,
                                  SkillUsageEventRepository skillUsageEventRepository,
                                  SkillOperationReportRepository skillOperationReportRepository,
                                  UserRepository userRepository,
                                  GitLabSkillPublishService gitLabSkillPublishService) {
        this.skillRepository = skillRepository;
        this.skillReviewRepository = skillReviewRepository;
        this.skillFeedbackRepository = skillFeedbackRepository;
        this.skillUsageEventRepository = skillUsageEventRepository;
        this.skillOperationReportRepository = skillOperationReportRepository;
        this.userRepository = userRepository;
        this.gitLabSkillPublishService = gitLabSkillPublishService;
    }

    @Transactional(readOnly = true)
    public SkillGovernanceSummaryDto getPublicSummary(Long skillId) {
        Skill skill = getSkill(skillId, true);
        return buildSummary(skill.getId());
    }

    @Transactional
    public SkillGovernanceSummaryDto recordUsage(Long skillId, SkillUsageRequest request) {
        Skill skill = getSkill(skillId, true);
        String externalEventId = blankToNull(request.externalEventId());
        if (externalEventId != null
                && skillUsageEventRepository.existsBySkill_IdAndExternalEventId(skill.getId(), externalEventId)) {
            return buildSummary(skill.getId());
        }
        SkillUsageEvent event = new SkillUsageEvent();
        event.setSkill(skill);
        event.setUserName(blankToNull(request.userName()));
        event.setScenario(blankToNull(request.scenario()));
        event.setSavedMinutes(nonNegative(request.savedMinutes()));
        event.setNewcomerOnboardingSavedMinutes(nonNegative(request.newcomerOnboardingSavedMinutes()));
        event.setReviewIssuesBefore(nonNegativeOrNull(request.reviewIssuesBefore()));
        event.setReviewIssuesAfter(nonNegativeOrNull(request.reviewIssuesAfter()));
        event.setTestCoverageBefore(percentOrNull(request.testCoverageBefore()));
        event.setTestCoverageAfter(percentOrNull(request.testCoverageAfter()));
        event.setToolchainSource(request.toolchainSource() != null ? request.toolchainSource() : ToolchainSource.MANUAL);
        event.setExternalEventId(externalEventId);
        event.setRepository(blankToNull(request.repository()));
        event.setBranchName(blankToNull(request.branchName()));
        event.setCommitSha(blankToNull(request.commitSha()));
        event.setCiStatus(blankToNull(request.ciStatus()));
        skillUsageEventRepository.save(event);
        return buildSummary(skill.getId());
    }

    @Transactional
    public SkillGovernanceSummaryDto recordToolchainTelemetry(SkillToolchainTelemetryRequest request) {
        ToolchainSource source = request.toolchainSource();
        if (source == null || source == ToolchainSource.MANUAL) {
            throw new IllegalArgumentException("工具链遥测来源不能为 MANUAL");
        }
        Long skillId = resolveTelemetrySkillId(request);
        return recordUsage(skillId, new SkillUsageRequest(
                request.userName(),
                request.scenario(),
                request.savedMinutes(),
                request.newcomerOnboardingSavedMinutes(),
                request.reviewIssuesBefore(),
                request.reviewIssuesAfter(),
                request.testCoverageBefore(),
                request.testCoverageAfter(),
                source,
                request.externalEventId(),
                request.repository(),
                request.branchName(),
                request.commitSha(),
                request.ciStatus()
        ));
    }

    @Transactional
    public SkillFeedbackDto submitFeedback(Long skillId, SkillFeedbackRequest request) {
        Skill skill = getSkill(skillId, true);
        SkillFeedback feedback = new SkillFeedback();
        feedback.setSkill(skill);
        feedback.setSubmitterName(blankToNull(request.submitterName()));
        feedback.setFeedbackType(request.feedbackType() != null ? request.feedbackType() : FeedbackType.IMPROVEMENT);
        feedback.setContent(request.content().trim());
        feedback.setEstimatedSavedMinutes(nonNegative(request.estimatedSavedMinutes()));
        feedback.setRating(request.rating());
        feedback.setStatus(FeedbackStatus.OPEN);
        return SkillFeedbackDto.fromEntity(skillFeedbackRepository.save(feedback));
    }

    @Transactional
    public SkillAssetMetricsDto metrics() {
        return buildMetrics(true);
    }

    @Transactional
    public SkillOperationsDto operationsReport() {
        SkillAssetMetricsDto metrics = buildMetrics(true);
        List<SkillMonthlyAwardCandidateDto> awardCandidates = monthlyAwardCandidates();
        return new SkillOperationsDto(
                metrics,
                monthlyTrends(),
                topSkills(),
                reviewQueue(),
                awardCandidates,
                pilotMilestones(metrics, awardCandidates),
                archiveCandidates()
        );
    }

    @Transactional
    public SkillMonthlyReportDto monthlyReport(String month) {
        YearMonth reportMonth = parseReportMonth(month);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate startDate = reportMonth.atDay(1);
        LocalDate endDate = reportMonth.atEndOfMonth();
        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = reportMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        SkillAssetMetricsDto metrics = buildMetrics(true);
        List<SkillUsageEvent> usageEvents = skillUsageEventRepository.findByCreatedAtBetween(start, end);
        List<SkillFeedback> feedbacks = skillFeedbackRepository.findByCreatedAtBetween(start, end);
        List<SkillReview> reviews = skillReviewRepository.findByReviewedAtBetween(startDate, endDate);

        long savedMinutes = usageEvents.stream().mapToLong(SkillUsageEvent::getSavedMinutes).sum();
        QualityImpact monthlyQualityImpact = qualityImpact(usageEvents);
        long closedFeedbackCount = feedbacks.stream()
                .filter(feedback -> feedback.getStatus() == FeedbackStatus.REVIEWED
                        || feedback.getStatus() == FeedbackStatus.RESOLVED)
                .count();
        long passedReviewCount = reviews.stream()
                .filter(review -> review.getResult() == ReviewResult.PASSED)
                .count();
        List<SkillLeaderboardItemDto> monthlyTopSkills = topSkillsFor(usageEvents, feedbacks);
        List<SkillMonthlyAwardCandidateDto> monthlyAwardCandidates = monthlyAwardCandidatesFor(usageEvents, feedbacks);
        List<SkillPilotMilestoneDto> pilotMilestones = pilotMilestones(metrics, monthlyAwardCandidates);
        List<String> highlights = monthlyHighlights(reportMonth, usageEvents, feedbacks, reviews, savedMinutes, monthlyQualityImpact);
        List<String> risks = monthlyRisks(metrics, feedbacks);
        List<String> recommendations = monthlyRecommendations(metrics, monthlyTopSkills, feedbacks);
        double feedbackClosedRate = ratio(closedFeedbackCount, feedbacks.size());
        double reviewPassRate = ratio(passedReviewCount, reviews.size());
        double savedHours = roundHours(savedMinutes);
        Map<String, Long> monthlyReviewRoleCounts = reviewRoleCounts(reviews);
        String markdown = buildMonthlyReportMarkdown(
                reportMonth,
                metrics,
                usageEvents.size(),
                feedbacks.size(),
                reviews.size(),
                savedHours,
                monthlyQualityImpact.newcomerOnboardingSavedHours(),
                monthlyQualityImpact.reviewIssueReductionRate(),
                monthlyQualityImpact.testCoverageIncreasePoints(),
                feedbackClosedRate,
                reviewPassRate,
                monthlyTopSkills,
                monthlyAwardCandidates,
                pilotMilestones,
                monthlyReviewRoleCounts,
                highlights,
                risks,
                recommendations
        );
        SkillOperationReport savedReport = saveMonthlyReport(
                reportMonth,
                usageEvents.size(),
                feedbacks.size(),
                reviews.size(),
                savedHours,
                feedbackClosedRate,
                reviewPassRate,
                markdown
        );

        return new SkillMonthlyReportDto(
                savedReport.getId(),
                reportMonth.toString(),
                savedReport.getGeneratedAt(),
                metrics,
                usageEvents.size(),
                feedbacks.size(),
                reviews.size(),
                savedHours,
                monthlyQualityImpact.newcomerOnboardingSavedHours(),
                monthlyQualityImpact.reviewIssueReductionRate(),
                monthlyQualityImpact.testCoverageIncreasePoints(),
                feedbackClosedRate,
                reviewPassRate,
                monthlyTopSkills,
                monthlyAwardCandidates,
                pilotMilestones,
                monthlyReviewRoleCounts,
                highlights,
                risks,
                recommendations,
                markdown
        );
    }

    @Transactional
    public SkillQuarterlyReportDto quarterlyReport(String quarter) {
        QuarterRange range = parseReportQuarter(quarter);
        SkillAssetMetricsDto metrics = buildMetrics(true);
        List<SkillUsageEvent> usageEvents = skillUsageEventRepository.findByCreatedAtBetween(range.start(), range.end());
        List<SkillFeedback> feedbacks = skillFeedbackRepository.findByCreatedAtBetween(range.start(), range.end());
        List<SkillReview> reviews = skillReviewRepository.findByReviewedAtBetween(range.startDate(), range.endDate());
        List<SkillLeaderboardItemDto> topSkills = topSkillsFor(usageEvents, feedbacks);
        List<SkillMonthlyAwardCandidateDto> awardCandidates = monthlyAwardCandidatesFor(usageEvents, feedbacks);
        List<SkillArchiveCandidateDto> archiveCandidates = archiveCandidates();
        QualityImpact qualityImpact = qualityImpact(usageEvents);
        long savedMinutes = usageEvents.stream().mapToLong(SkillUsageEvent::getSavedMinutes).sum();
        long closedFeedbackCount = feedbacks.stream()
                .filter(feedback -> feedback.getStatus() == FeedbackStatus.REVIEWED
                        || feedback.getStatus() == FeedbackStatus.RESOLVED)
                .count();
        long passedReviewCount = reviews.stream()
                .filter(review -> review.getResult() == ReviewResult.PASSED)
                .count();
        Map<String, Long> reviewRoleCounts = reviewRoleCounts(reviews);
        List<String> governanceFindings = quarterlyGovernanceFindings(metrics, reviews, reviewRoleCounts, archiveCandidates);
        List<String> risks = quarterlyRisks(metrics, feedbacks, archiveCandidates);
        List<String> recommendations = quarterlyRecommendations(metrics, topSkills, feedbacks, archiveCandidates);
        double feedbackClosedRate = ratio(closedFeedbackCount, feedbacks.size());
        double reviewPassRate = ratio(passedReviewCount, reviews.size());
        double savedHours = roundHours(savedMinutes);
        String markdown = buildQuarterlyReportMarkdown(
                range,
                metrics,
                usageEvents.size(),
                feedbacks.size(),
                reviews.size(),
                savedHours,
                qualityImpact.newcomerOnboardingSavedHours(),
                qualityImpact.reviewIssueReductionRate(),
                qualityImpact.testCoverageIncreasePoints(),
                feedbackClosedRate,
                reviewPassRate,
                topSkills,
                awardCandidates,
                archiveCandidates,
                reviewRoleCounts,
                governanceFindings,
                risks,
                recommendations
        );

        return new SkillQuarterlyReportDto(
                range.label(),
                range.startMonth().toString(),
                range.endMonth().toString(),
                Instant.now(),
                metrics,
                usageEvents.size(),
                feedbacks.size(),
                reviews.size(),
                savedHours,
                qualityImpact.newcomerOnboardingSavedHours(),
                qualityImpact.reviewIssueReductionRate(),
                qualityImpact.testCoverageIncreasePoints(),
                feedbackClosedRate,
                reviewPassRate,
                topSkills,
                awardCandidates,
                archiveCandidates,
                reviewRoleCounts,
                governanceFindings,
                risks,
                recommendations,
                markdown
        );
    }

    @Transactional(readOnly = true)
    public Page<SkillOperationReportDto> listMonthlyReports(int page, int size) {
        Pageable pageable = pageRequest(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));
        return skillOperationReportRepository.findAllByOrderByGeneratedAtDesc(pageable)
                .map(SkillOperationReportDto::fromEntity);
    }

    private SkillAssetMetricsDto buildMetrics(boolean refreshReviewStatus) {
        List<Skill> skills = skillRepository.findAll();
        LocalDate today = LocalDate.now();
        if (refreshReviewStatus) {
            markOverdueSkillsForReview(skills, today);
        }
        Map<String, Long> assetLevelCounts = countByAssetLevel(skills);
        Map<String, Long> lifecycleStatusCounts = countByLifecycleStatus(skills);
        Map<String, Long> skillCategoryCounts = countBySkillCategory(skills);
        Map<String, Long> buildPriorityCounts = countByBuildPriority(skills);

        long needsReviewCount = skills.stream()
                .filter(s -> s.getLifecycleStatus() == LifecycleStatus.NEEDS_REVIEW
                        || isReviewOverdue(s, today))
                .count();
        long overdueSkillCount = skills.stream()
                .filter(s -> isReviewOverdue(s, today))
                .count();
        long templateValidatedCount = skills.stream()
                .filter(s -> s.getTemplateValidationStatus() == TemplateValidationStatus.PASSED)
                .count();
        long templateValidationFailedCount = skills.stream()
                .filter(s -> s.getTemplateValidationStatus() == TemplateValidationStatus.FAILED)
                .count();
        long highRiskCount = skills.stream().filter(s -> s.getRiskLevel() == RiskLevel.HIGH).count();
        long totalUsageCount = skillUsageEventRepository.count();
        long monthlyUsageCount = skillUsageEventRepository.countByCreatedAtAfter(Instant.now().minus(30, ChronoUnit.DAYS));
        long savedMinutes = valueOrZero(skillUsageEventRepository.sumSavedMinutes());
        List<SkillUsageEvent> usageEvents = skillUsageEventRepository.findAll();
        QualityImpact qualityImpact = qualityImpact(usageEvents);
        ToolchainUsageStats toolchainUsageStats = toolchainUsageStats(usageEvents);
        long feedbackCount = skillFeedbackRepository.count();
        long openFeedbackCount = skillFeedbackRepository.countByStatus(FeedbackStatus.OPEN);
        long feedbackClosedCount = skillFeedbackRepository.countByStatus(FeedbackStatus.REVIEWED)
                + skillFeedbackRepository.countByStatus(FeedbackStatus.RESOLVED);
        long reviewCount = skillReviewRepository.count();
        long passedReviewCount = skillReviewRepository.countByResult(ReviewResult.PASSED);

        return new SkillAssetMetricsDto(
                skills.size(),
                assetLevelCounts.get(AssetLevel.TEAM.name()),
                assetLevelCounts.get(AssetLevel.COMPANY.name()),
                templateValidatedCount,
                templateValidationFailedCount,
                lifecycleStatusCounts.get(LifecycleStatus.APPROVED.name()),
                needsReviewCount,
                overdueSkillCount,
                ratio(overdueSkillCount, skills.size()),
                highRiskCount,
                monthlyUsageCount,
                totalUsageCount,
                toolchainUsageStats.manualUsageCount(),
                toolchainUsageStats.toolchainUsageCount(),
                toolchainUsageStats.sourceCounts().get(ToolchainSource.CI.name()),
                toolchainUsageStats.sourceCounts().get(ToolchainSource.CODE_REVIEW.name()),
                toolchainUsageStats.sourceCounts().get(ToolchainSource.TEST_COVERAGE.name()),
                roundHours(savedMinutes),
                qualityImpact.newcomerOnboardingSavedHours(),
                qualityImpact.reviewIssueReductionRate(),
                qualityImpact.testCoverageIncreasePoints(),
                qualityImpact.qualitySignalCount(),
                feedbackCount,
                openFeedbackCount,
                ratio(feedbackClosedCount, feedbackCount),
                reviewCount,
                ratio(passedReviewCount, reviewCount),
                assetLevelCounts,
                lifecycleStatusCounts,
                skillCategoryCounts,
                buildPriorityCounts,
                toolchainUsageStats.sourceCounts()
        );
    }

    @Transactional
    public SkillTemplateValidationDto validateTemplate(Long skillId) {
        Skill skill = getSkill(skillId, false);
        SkillTemplateValidationDto report = buildTemplateValidationReport(skill);
        skill.setTemplateValidationStatus(report.passed() ? TemplateValidationStatus.PASSED : TemplateValidationStatus.FAILED);
        skill.setTemplateValidationNotes(report.notes());
        skill.setLastValidatedAt(report.validatedAt());
        skillRepository.save(skill);
        return report;
    }

    @Transactional(readOnly = true)
    public Page<SkillReviewDto> listReviews(Long skillId, int page, int size) {
        getSkill(skillId, false);
        Pageable pageable = pageRequest(page, size, Sort.by(Sort.Direction.DESC, "reviewedAt", "createdAt"));
        return skillReviewRepository.findBySkill_IdOrderByReviewedAtDescCreatedAtDesc(skillId, pageable)
                .map(SkillReviewDto::fromEntity);
    }

    @Transactional
    public SkillReviewDto createReview(Long skillId, SkillReviewRequest request, Long reviewerUserId) {
        Skill skill = getSkill(skillId, false);
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("评审用户不存在"));
        LocalDate reviewedAt = request.reviewedAt() != null ? request.reviewedAt() : LocalDate.now();
        ReviewerRole reviewerRole = reviewer.getSkillGovernanceRole() != null
                ? reviewer.getSkillGovernanceRole()
                : ReviewerRole.CONTRIBUTOR;
        ReviewStage reviewStage = request.reviewStage() != null ? request.reviewStage() : ReviewStage.TEAM_REVIEW;
        enforceApprovalGate(skill, request.result(), reviewerRole);
        SkillReview review = new SkillReview();
        review.setSkill(skill);
        review.setReviewerName(reviewer.getUsername());
        review.setReviewerRole(reviewerRole);
        review.setReviewStage(reviewStage);
        review.setResult(request.result());
        review.setTruthful(Boolean.TRUE.equals(request.truthful()));
        review.setAccurate(Boolean.TRUE.equals(request.accurate()));
        review.setReusable(Boolean.TRUE.equals(request.reusable()));
        review.setExecutable(Boolean.TRUE.equals(request.executable()));
        review.setSecure(Boolean.TRUE.equals(request.secure()));
        review.setVerifiable(Boolean.TRUE.equals(request.verifiable()));
        review.setMaintainable(Boolean.TRUE.equals(request.maintainable()));
        review.setNotes(blankToNull(request.notes()));
        review.setReviewedAt(reviewedAt);
        review.setNextReviewAt(request.nextReviewAt());

        boolean alreadyPublishedToGitLab = hasGitLabMergeRequest(skill);
        skill.setLastReviewedAt(reviewedAt);
        skill.setNextReviewAt(request.nextReviewAt());
        skill.setReviewNotes(blankToNull(request.notes()));
        skill.setLifecycleStatus(statusFromReviewResult(request.result()));
        publishApprovedPackageIfNeeded(skill, alreadyPublishedToGitLab);
        skillRepository.save(skill);

        return SkillReviewDto.fromEntity(skillReviewRepository.save(review));
    }

    private void enforceApprovalGate(Skill skill, ReviewResult result, ReviewerRole reviewerRole) {
        if (result != ReviewResult.PASSED) {
            return;
        }
        if (skill.getTemplateValidationStatus() != TemplateValidationStatus.PASSED) {
            throw new IllegalArgumentException("模板校验通过后才能评审入库");
        }
        if (reviewerRole == ReviewerRole.CONTRIBUTOR) {
            throw new IllegalArgumentException("贡献者不能直接评审入库");
        }
        if (skill.getAssetLevel() == AssetLevel.COMPANY
                && reviewerRole != ReviewerRole.PLATFORM_ENGINEERING
                && reviewerRole != ReviewerRole.TECHNICAL_COMMITTEE
                && reviewerRole != ReviewerRole.SECURITY_QUALITY) {
            throw new IllegalArgumentException("公司级资产需由平台/效能团队、技术委员会或安全/质量团队评审通过");
        }
        if (skill.getRiskLevel() == RiskLevel.HIGH
                && reviewerRole != ReviewerRole.TECHNICAL_COMMITTEE
                && reviewerRole != ReviewerRole.SECURITY_QUALITY) {
            throw new IllegalArgumentException("高风险 Skill 需由技术委员会或安全/质量团队评审通过");
        }
    }

    private void publishApprovedPackageIfNeeded(Skill skill, boolean alreadyPublishedToGitLab) {
        if (skill.getLifecycleStatus() != LifecycleStatus.APPROVED
                || skill.getCreationSource() != CreationSource.SKILL_CREATOR_PACKAGE
                || alreadyPublishedToGitLab
                || hasGitLabMergeRequest(skill)) {
            return;
        }
        SkillGitLabPublishResultDto publication = gitLabSkillPublishService.publishPackage(
                skill.getSkillDirectory(),
                skill.getName(),
                publishableFiles(skill)
        );
        applyGitLabPublication(skill, publication);
    }

    private void applyGitLabPublication(Skill skill, SkillGitLabPublishResultDto publication) {
        if (publication == null || !"PUBLISHED".equals(publication.status())) {
            appendReviewNote(skill, publication != null ? publication.message() : null);
            return;
        }
        if (hasText(publication.repositoryUrl())) {
            skill.setSourceRepositoryUrl(publication.repositoryUrl());
            skill.setCloneCommand(buildGitCloneCommand(publication.repositoryUrl()));
        }
        appendReviewNote(skill, "GitLab MR：" + publication.mergeRequestUrl());
        appendReviewNote(skill, "GitLab 分支：" + publication.branchName());
        appendReviewNote(skill, "GitLab 路径：" + publication.skillPath());
    }

    private Map<String, byte[]> publishableFiles(Skill skill) {
        Map<String, byte[]> files = parsePackageFiles(skill.getSkillPackageFiles());
        if (!files.isEmpty()) {
            return files;
        }
        if (!hasText(skill.getContentMd())) {
            throw new IllegalArgumentException("GitLab 发布失败：Skill 缺少可发布文件");
        }
        return Map.of("SKILL.md", skill.getContentMd().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Map<String, byte[]> parsePackageFiles(String json) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        String raw = blankToNull(json);
        if (raw == null) {
            return files;
        }
        try {
            Map<?, ?> decoded = new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw, Map.class);
            for (Map.Entry<?, ?> entry : decoded.entrySet()) {
                if (entry.getKey() instanceof String path && entry.getValue() instanceof String content) {
                    files.put(path, Base64.getDecoder().decode(content));
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("GitLab 发布失败：Skill 包文件归档损坏");
        }
        return files;
    }

    private String buildGitCloneCommand(String repositoryUrl) {
        String url = blankToNull(repositoryUrl);
        if (url == null) {
            return "git clone <待评审后生成的仓库地址>";
        }
        if (url.endsWith(".git")) {
            return "git clone " + url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return "git clone " + url + ".git";
        }
        return "git clone " + url;
    }

    private void appendReviewNote(Skill skill, String note) {
        String cleaned = blankToNull(note);
        if (cleaned == null) {
            return;
        }
        String current = blankToNull(skill.getReviewNotes());
        skill.setReviewNotes(current == null ? cleaned : current + "\n" + cleaned);
    }

    private boolean hasGitLabMergeRequest(Skill skill) {
        String notes = skill.getReviewNotes();
        return notes != null && notes.contains("GitLab MR：");
    }

    @Transactional(readOnly = true)
    public Page<SkillFeedbackDto> listFeedback(Long skillId, String status, int page, int size) {
        if (skillId != null) getSkill(skillId, false);
        FeedbackStatus feedbackStatus = parseEnum(FeedbackStatus.class, status, "反馈状态");
        Pageable pageable = pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SkillFeedback> feedbackPage;
        if (skillId != null && feedbackStatus != null) {
            feedbackPage = skillFeedbackRepository.findBySkill_IdAndStatusOrderByCreatedAtDesc(skillId, feedbackStatus, pageable);
        } else if (skillId != null) {
            feedbackPage = skillFeedbackRepository.findBySkill_IdOrderByCreatedAtDesc(skillId, pageable);
        } else if (feedbackStatus != null) {
            feedbackPage = skillFeedbackRepository.findByStatusOrderByCreatedAtDesc(feedbackStatus, pageable);
        } else {
            feedbackPage = skillFeedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return feedbackPage.map(SkillFeedbackDto::fromEntity);
    }

    @Transactional
    public SkillFeedbackDto updateFeedbackStatus(Long skillId, Long feedbackId, SkillFeedbackStatusRequest request) {
        SkillFeedback feedback = skillFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("反馈不存在"));
        if (!feedback.getSkill().getId().equals(skillId)) {
            throw new ResourceNotFoundException("反馈不存在");
        }
        feedback.setStatus(request.status());
        return SkillFeedbackDto.fromEntity(skillFeedbackRepository.save(feedback));
    }

    private List<SkillMonthlyTrendDto> monthlyTrends() {
        ZoneId zone = ZoneId.systemDefault();
        YearMonth startMonth = YearMonth.now(zone).minusMonths(5);
        Instant since = startMonth.atDay(1).atStartOfDay(zone).toInstant();
        Map<YearMonth, TrendBucket> buckets = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            buckets.put(startMonth.plusMonths(i), new TrendBucket());
        }

        for (SkillUsageEvent event : skillUsageEventRepository.findByCreatedAtAfter(since)) {
            YearMonth month = YearMonth.from(event.getCreatedAt().atZone(zone));
            TrendBucket bucket = buckets.get(month);
            if (bucket != null) {
                bucket.usageCount++;
                bucket.savedMinutes += event.getSavedMinutes();
            }
        }
        for (SkillFeedback feedback : skillFeedbackRepository.findByCreatedAtAfter(since)) {
            YearMonth month = YearMonth.from(feedback.getCreatedAt().atZone(zone));
            TrendBucket bucket = buckets.get(month);
            if (bucket != null) {
                bucket.feedbackCount++;
            }
        }

        return buckets.entrySet().stream()
                .map(entry -> new SkillMonthlyTrendDto(
                        entry.getKey().toString(),
                        entry.getValue().usageCount,
                        entry.getValue().feedbackCount,
                        roundHours(entry.getValue().savedMinutes)
                ))
                .toList();
    }

    private List<SkillLeaderboardItemDto> topSkills() {
        Map<Long, SkillBucket> buckets = new HashMap<>();
        for (Skill skill : skillRepository.findAll()) {
            buckets.put(skill.getId(), new SkillBucket(skill));
        }
        for (SkillUsageEvent event : skillUsageEventRepository.findAll()) {
            Skill skill = event.getSkill();
            if (skill == null || skill.getId() == null) continue;
            SkillBucket bucket = buckets.computeIfAbsent(skill.getId(), id -> new SkillBucket(skill));
            bucket.usageCount++;
            bucket.savedMinutes += event.getSavedMinutes();
        }
        for (SkillFeedback feedback : skillFeedbackRepository.findAll()) {
            Skill skill = feedback.getSkill();
            if (skill == null || skill.getId() == null) continue;
            SkillBucket bucket = buckets.computeIfAbsent(skill.getId(), id -> new SkillBucket(skill));
            bucket.feedbackCount++;
            if (feedback.getStatus() == FeedbackStatus.OPEN) {
                bucket.openFeedbackCount++;
            }
        }

        return buckets.values().stream()
                .filter(bucket -> bucket.usageCount > 0 || bucket.feedbackCount > 0)
                .sorted(Comparator.comparingLong(SkillBucket::impactScore).reversed())
                .limit(10)
                .map(SkillBucket::toDto)
                .toList();
    }

    private List<SkillReviewQueueItemDto> reviewQueue() {
        LocalDate today = LocalDate.now();
        LocalDate dueSoon = today.plusDays(14);
        return skillRepository.findAll().stream()
                .filter(skill -> skill.getLifecycleStatus() != LifecycleStatus.ARCHIVED)
                .filter(skill -> queueDueAt(skill, dueSoon) != null)
                .sorted(Comparator.comparing(skill -> queueDueAt(skill, dueSoon)))
                .limit(10)
                .map(skill -> SkillReviewQueueItemDto.fromSkill(skill, today))
                .toList();
    }

    private LocalDate queueDueAt(Skill skill, LocalDate dueSoon) {
        List<LocalDate> dueDates = new ArrayList<>();
        if (skill.getNextReviewAt() != null
                && (skill.getLifecycleStatus() == LifecycleStatus.NEEDS_REVIEW
                || !skill.getNextReviewAt().isAfter(dueSoon))) {
            dueDates.add(skill.getNextReviewAt());
        }
        if (skill.getLifecycleStatus() == LifecycleStatus.TRIAL
                && skill.getTrialEndsAt() != null
                && !skill.getTrialEndsAt().isAfter(dueSoon)) {
            dueDates.add(skill.getTrialEndsAt());
        }
        return dueDates.stream().min(LocalDate::compareTo).orElse(null);
    }

    private List<SkillMonthlyAwardCandidateDto> monthlyAwardCandidates() {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        return monthlyAwardCandidatesFor(
                skillUsageEventRepository.findByCreatedAtAfter(since),
                skillFeedbackRepository.findByCreatedAtAfter(since)
        );
    }

    private List<SkillArchiveCandidateDto> archiveCandidates() {
        Instant staleSince = Instant.now().minus(90, ChronoUnit.DAYS);
        LocalDate today = LocalDate.now();
        Map<Long, ArchiveCandidateBucket> buckets = new HashMap<>();
        for (Skill skill : skillRepository.findAll()) {
            if (isArchiveCandidateEligible(skill)) {
                buckets.put(skill.getId(), new ArchiveCandidateBucket(skill));
            }
        }
        for (SkillUsageEvent event : skillUsageEventRepository.findAll()) {
            Skill skill = event.getSkill();
            if (skill == null || skill.getId() == null) continue;
            ArchiveCandidateBucket bucket = buckets.get(skill.getId());
            if (bucket != null) {
                bucket.recordUsage(event);
            }
        }
        for (SkillFeedback feedback : skillFeedbackRepository.findAll()) {
            Skill skill = feedback.getSkill();
            if (skill == null || skill.getId() == null) continue;
            ArchiveCandidateBucket bucket = buckets.get(skill.getId());
            if (bucket != null) {
                bucket.recordFeedback(feedback);
            }
        }

        return buckets.values().stream()
                .map(bucket -> bucket.toDto(staleSince, today))
                .filter(candidate -> candidate != null)
                .sorted(Comparator.comparingLong(SkillArchiveCandidateDto::riskScore).reversed()
                        .thenComparing(SkillArchiveCandidateDto::skillName, Comparator.nullsLast(String::compareTo)))
                .limit(10)
                .toList();
    }

    private boolean isArchiveCandidateEligible(Skill skill) {
        return skill.getId() != null
                && skill.getLifecycleStatus() != LifecycleStatus.ARCHIVED
                && skill.getVisibility() != Visibility.HIDDEN;
    }

    private List<SkillPilotMilestoneDto> pilotMilestones(SkillAssetMetricsDto metrics,
                                                         List<SkillMonthlyAwardCandidateDto> awardCandidates) {
        List<Skill> skills = skillRepository.findAll();
        long pilotTeamCount = skills.stream()
                .map(Skill::getTeamName)
                .filter(this::hasText)
                .distinct()
                .count();
        long maintainedSkillCount = skills.stream()
                .filter(skill -> hasText(skill.getMaintainer()))
                .count();
        long scenarioSkillCount = skills.stream()
                .filter(skill -> hasText(skill.getApplicableScenarios()))
                .count();
        long reusableAssetCount = skills.stream()
                .filter(skill -> skill.getAssetLevel() == AssetLevel.TEAM || skill.getAssetLevel() == AssetLevel.COMPANY)
                .count();
        long approvedReusableAssetCount = skills.stream()
                .filter(skill -> skill.getAssetLevel() == AssetLevel.TEAM || skill.getAssetLevel() == AssetLevel.COMPANY)
                .filter(skill -> skill.getLifecycleStatus() == LifecycleStatus.APPROVED)
                .count();
        long reportArchiveCount = skillOperationReportRepository.count();

        return List.of(
                milestone(
                        "阶段一：试点准备",
                        "第 1 周",
                        "确定试点团队、场景清单、模板和评审人",
                        "试点范围、Skill 模板、候选清单",
                        conditions(pilotTeamCount >= 1, scenarioSkillCount >= 5, metrics.templateValidatedCount() >= 5),
                        3,
                        List.of("试点团队 " + pilotTeamCount + " 个", "有场景说明 Skill " + scenarioSkillCount + " 个", "模板通过 " + metrics.templateValidatedCount() + " 个"),
                        nextActions(
                                pilotTeamCount >= 1 ? null : "补充试点团队",
                                scenarioSkillCount >= 5 ? null : "补齐至少 5 个候选 Skill 的适用场景",
                                metrics.templateValidatedCount() >= 5 ? null : "对候选 Skill 执行模板校验"
                        )
                ),
                milestone(
                        "阶段二：集中沉淀",
                        "第 2-3 周",
                        "围绕高频任务创建第一批 Skill，并在真实任务中使用",
                        "5-10 个候选 Skill、使用记录",
                        conditions(metrics.totalSkills() >= 5, reusableAssetCount >= 3, metrics.monthlyUsageCount() >= 5),
                        3,
                        List.of("Skill 总数 " + metrics.totalSkills() + " 个", "团队级/公司级资产 " + reusableAssetCount + " 个", "近 30 天使用 " + metrics.monthlyUsageCount() + " 次"),
                        nextActions(
                                metrics.totalSkills() >= 5 ? null : "继续沉淀真实任务驱动的候选 Skill",
                                reusableAssetCount >= 3 ? null : "将成熟 Skill 沉淀为团队级/公司级资产",
                                metrics.monthlyUsageCount() >= 5 ? null : "在真实研发任务中记录使用效果"
                        )
                ),
                milestone(
                        "阶段三：评审入库",
                        "第 4 周",
                        "组织评审，修订可复用内容，进入团队资产库",
                        "团队级资产库、评审记录",
                        conditions(approvedReusableAssetCount >= 3, metrics.reviewCount() >= 3, maintainedSkillCount >= 5),
                        3,
                        List.of("已入库团队级/公司级资产 " + approvedReusableAssetCount + " 个", "评审记录 " + metrics.reviewCount() + " 条", "有维护人 Skill " + maintainedSkillCount + " 个"),
                        nextActions(
                                approvedReusableAssetCount >= 3 ? null : "推动至少 3 个团队级/公司级资产评审入库",
                                metrics.reviewCount() >= 3 ? null : "补齐评审记录和评审角色",
                                maintainedSkillCount >= 5 ? null : "明确候选 Skill 维护人"
                        )
                ),
                milestone(
                        "阶段四：效果复盘",
                        "第 5-6 周",
                        "统计使用效果、失败案例、改进建议和推广条件",
                        "试点复盘报告、规模化建议",
                        conditions(reportArchiveCount >= 1, metrics.feedbackCount() >= 5, metrics.estimatedSavedHours() > 0),
                        3,
                        List.of("归档月报 " + reportArchiveCount + " 份", "反馈 " + metrics.feedbackCount() + " 条", "累计节省 " + metrics.estimatedSavedHours() + "h"),
                        nextActions(
                                reportArchiveCount >= 1 ? null : "生成并归档月度复盘报告",
                                metrics.feedbackCount() >= 5 ? null : "引导使用者提交失败案例和改进建议",
                                metrics.estimatedSavedHours() > 0 ? null : "记录每次使用的节省工时"
                        )
                ),
                milestone(
                        "持续运营：规模化推广",
                        "第 2-3 个月起",
                        "建立公司级 Skill 资产库，形成月度运营与季度治理节奏",
                        "公司级资产库、统一目录、月度看板",
                        conditions(metrics.companyAssetCount() >= 1, !awardCandidates.isEmpty(), metrics.feedbackClosedRate() >= 70),
                        3,
                        List.of("公司级资产 " + metrics.companyAssetCount() + " 个", "高价值候选 " + awardCandidates.size() + " 个", "反馈闭环率 " + metrics.feedbackClosedRate() + "%"),
                        nextActions(
                                metrics.companyAssetCount() >= 1 ? null : "评审升级首批公司级 Skill",
                                !awardCandidates.isEmpty() ? null : "积累高价值 Skill 候选数据",
                                metrics.feedbackClosedRate() >= 70 ? null : "提升反馈处理闭环率"
                        )
                )
        );
    }

    private List<SkillLeaderboardItemDto> topSkillsFor(List<SkillUsageEvent> usageEvents, List<SkillFeedback> feedbacks) {
        Map<Long, SkillBucket> buckets = new HashMap<>();
        for (SkillUsageEvent event : usageEvents) {
            Skill skill = event.getSkill();
            if (skill == null || skill.getId() == null) continue;
            SkillBucket bucket = buckets.computeIfAbsent(skill.getId(), id -> new SkillBucket(skill));
            bucket.usageCount++;
            bucket.savedMinutes += event.getSavedMinutes();
        }
        for (SkillFeedback feedback : feedbacks) {
            Skill skill = feedback.getSkill();
            if (skill == null || skill.getId() == null) continue;
            SkillBucket bucket = buckets.computeIfAbsent(skill.getId(), id -> new SkillBucket(skill));
            bucket.feedbackCount++;
            if (feedback.getStatus() == FeedbackStatus.OPEN) {
                bucket.openFeedbackCount++;
            }
        }
        return buckets.values().stream()
                .sorted(Comparator.comparingLong(SkillBucket::impactScore).reversed())
                .limit(5)
                .map(SkillBucket::toDto)
                .toList();
    }

    private List<SkillMonthlyAwardCandidateDto> monthlyAwardCandidatesFor(List<SkillUsageEvent> usageEvents,
                                                                          List<SkillFeedback> feedbacks) {
        Map<Long, AwardCandidateBucket> buckets = new HashMap<>();
        for (SkillUsageEvent event : usageEvents) {
            Skill skill = event.getSkill();
            if (skill == null || skill.getId() == null) continue;
            AwardCandidateBucket bucket = buckets.computeIfAbsent(skill.getId(), id -> new AwardCandidateBucket(skill));
            bucket.usageCount++;
            bucket.savedMinutes += event.getSavedMinutes();
        }
        for (SkillFeedback feedback : feedbacks) {
            Skill skill = feedback.getSkill();
            if (skill == null || skill.getId() == null) continue;
            AwardCandidateBucket bucket = buckets.computeIfAbsent(skill.getId(), id -> new AwardCandidateBucket(skill));
            bucket.feedbackCount++;
            bucket.savedMinutes += feedback.getEstimatedSavedMinutes();
            if (feedback.getFeedbackType() == FeedbackType.SUCCESS_CASE) {
                bucket.successCaseCount++;
            }
            if (feedback.getStatus() == FeedbackStatus.OPEN) {
                bucket.openFeedbackCount++;
            }
        }
        return buckets.values().stream()
                .filter(bucket -> bucket.score() > 0)
                .sorted(Comparator.comparingLong(AwardCandidateBucket::score).reversed())
                .limit(6)
                .map(AwardCandidateBucket::toDto)
                .toList();
    }

    private List<String> monthlyHighlights(YearMonth month,
                                           List<SkillUsageEvent> usageEvents,
                                           List<SkillFeedback> feedbacks,
                                           List<SkillReview> reviews,
                                           long savedMinutes,
                                           QualityImpact qualityImpact) {
        List<String> highlights = new ArrayList<>();
        highlights.add(month + " 记录 Skill 使用 " + usageEvents.size() + " 次，估算节省 " + roundHours(savedMinutes) + " 小时。");
        highlights.add("收到反馈 " + feedbacks.size() + " 条，完成评审 " + reviews.size() + " 次。");
        if (qualityImpact.qualitySignalCount() > 0) {
            highlights.add("记录质量效果信号 " + qualityImpact.qualitySignalCount() + " 条，新人上手节省 "
                    + qualityImpact.newcomerOnboardingSavedHours() + " 小时，Review 问题减少率 "
                    + qualityImpact.reviewIssueReductionRate() + "%，测试覆盖平均提升 "
                    + qualityImpact.testCoverageIncreasePoints() + " 个百分点。");
        }
        long toolchainSignals = usageEvents.stream().filter(this::isToolchainEvent).count();
        if (toolchainSignals > 0) {
            highlights.add("自动采集工具链信号 " + toolchainSignals + " 条，可用于追踪 CI、代码评审和测试覆盖效果。");
        }
        long successCases = feedbacks.stream()
                .filter(feedback -> feedback.getFeedbackType() == FeedbackType.SUCCESS_CASE)
                .count();
        if (successCases > 0) {
            highlights.add("沉淀成功案例 " + successCases + " 条，可用于周会推广和样板复用。");
        }
        return highlights;
    }

    private List<String> monthlyRisks(SkillAssetMetricsDto metrics, List<SkillFeedback> feedbacks) {
        List<String> risks = new ArrayList<>();
        long failureCases = feedbacks.stream()
                .filter(feedback -> feedback.getFeedbackType() == FeedbackType.FAILURE_CASE)
                .count();
        if (failureCases > 0) {
            risks.add("本月存在 " + failureCases + " 条失败案例，需要补充不适用场景或验证方式。");
        }
        if (metrics.openFeedbackCount() > 0) {
            risks.add("仍有 " + metrics.openFeedbackCount() + " 条反馈待处理，影响反馈闭环率。");
        }
        if (metrics.overdueSkillCount() > 0) {
            risks.add(metrics.overdueSkillCount() + " 个 Skill 已超过复审日期，需要维护人确认是否更新或归档。");
        }
        if (metrics.highRiskCount() > 0) {
            risks.add("高风险 Skill 共 " + metrics.highRiskCount() + " 个，需保持人工复核和安全边界。");
        }
        if (risks.isEmpty()) {
            risks.add("暂无明显运营风险，继续关注反馈和复审节奏。");
        }
        return risks;
    }

    private List<String> monthlyRecommendations(SkillAssetMetricsDto metrics,
                                                List<SkillLeaderboardItemDto> topSkills,
                                                List<SkillFeedback> feedbacks) {
        List<String> recommendations = new ArrayList<>();
        if (!topSkills.isEmpty()) {
            SkillLeaderboardItemDto top = topSkills.get(0);
            recommendations.add("优先复盘高影响 Skill：" + top.skillName() + "，提炼使用案例并推动团队复用。");
        }
        if (metrics.openFeedbackCount() > 0) {
            recommendations.add("安排维护人处理待处理反馈，并把共性问题补入 SKILL.md 或 references。");
        }
        if (metrics.overdueSkillCount() > 0) {
            recommendations.add("对过期 Skill 发起周期性评审，通过后更新 nextReviewAt，不稳定资产归档。");
        }
        if (metrics.qualitySignalCount() == 0) {
            recommendations.add("在记录使用时补充新人上手、Review 问题和测试覆盖等质量效果信号。");
        }
        if (metrics.toolchainUsageCount() == 0) {
            recommendations.add("将 CI、代码评审和测试覆盖结果接入 Skill 使用记录，减少人工补录。");
        }
        boolean hasImprovement = feedbacks.stream()
                .anyMatch(feedback -> feedback.getFeedbackType() == FeedbackType.IMPROVEMENT);
        if (hasImprovement) {
            recommendations.add("把改进建议合并为下一轮模板或脚本优化任务。");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("维持当前运营节奏，下月继续观察使用量、节省工时和反馈质量。");
        }
        return recommendations;
    }

    private List<String> quarterlyGovernanceFindings(SkillAssetMetricsDto metrics,
                                                     List<SkillReview> reviews,
                                                     Map<String, Long> reviewRoleCounts,
                                                     List<SkillArchiveCandidateDto> archiveCandidates) {
        List<String> findings = new ArrayList<>();
        findings.add("当前团队级/公司级资产 " + (metrics.teamAssetCount() + metrics.companyAssetCount())
                + " 个，公司级资产 " + metrics.companyAssetCount() + " 个。");
        long passedReviews = reviews.stream()
                .filter(review -> review.getResult() == ReviewResult.PASSED)
                .count();
        findings.add("季度完成评审 " + reviews.size() + " 次，评审通过率 " + ratio(passedReviews, reviews.size()) + "%。");
        long committeeReviews = reviewRoleCounts.getOrDefault(ReviewerRole.TECHNICAL_COMMITTEE.name(), 0L)
                + reviewRoleCounts.getOrDefault(ReviewerRole.SECURITY_QUALITY.name(), 0L)
                + reviewRoleCounts.getOrDefault(ReviewerRole.PLATFORM_ENGINEERING.name(), 0L);
        if (committeeReviews > 0) {
            findings.add("平台、技术委员会或安全/质量角色参与评审 " + committeeReviews + " 次。");
        } else {
            findings.add("本季度暂无平台、技术委员会或安全/质量角色评审记录。");
        }
        if (metrics.toolchainUsageCount() > 0) {
            findings.add("已接入工具链信号 " + metrics.toolchainUsageCount() + " 条，其中 CI "
                    + metrics.ciSignalCount() + " 条，代码评审 " + metrics.codeReviewSignalCount()
                    + " 条，测试覆盖 " + metrics.testCoverageSignalCount() + " 条。");
        } else {
            findings.add("当前仍未采集到工具链信号，需要推动 CI、代码评审或测试覆盖平台上报。");
        }
        if (archiveCandidates.isEmpty()) {
            findings.add("暂无下架/归档候选，资产健康度稳定。");
        } else {
            findings.add("识别下架/归档候选 " + archiveCandidates.size() + " 个，需要维护人确认保留、修订或下架。");
        }
        return findings;
    }

    private List<String> quarterlyRisks(SkillAssetMetricsDto metrics,
                                        List<SkillFeedback> feedbacks,
                                        List<SkillArchiveCandidateDto> archiveCandidates) {
        List<String> risks = new ArrayList<>();
        long failureCases = feedbacks.stream()
                .filter(feedback -> feedback.getFeedbackType() == FeedbackType.FAILURE_CASE)
                .count();
        if (failureCases > 0) {
            risks.add("本季度存在 " + failureCases + " 条失败案例，需要补充边界、不适用场景和验证方法。");
        }
        if (metrics.openFeedbackCount() > 0) {
            risks.add("仍有 " + metrics.openFeedbackCount() + " 条反馈待处理，影响季度治理闭环。");
        }
        if (!archiveCandidates.isEmpty()) {
            risks.add("下架/归档候选 " + archiveCandidates.size() + " 个，需要纳入季度治理会议确认。");
        }
        if (metrics.highRiskCount() > 0) {
            risks.add("高风险 Skill 共 " + metrics.highRiskCount() + " 个，需要持续保留人工复核边界。");
        }
        if (metrics.overdueSkillCount() > 0) {
            risks.add(metrics.overdueSkillCount() + " 个 Skill 已超过复审日期，需要尽快完成复审或归档。");
        }
        if (risks.isEmpty()) {
            risks.add("暂无明显季度治理风险，继续观察使用、反馈和复审节奏。");
        }
        return risks;
    }

    private List<String> quarterlyRecommendations(SkillAssetMetricsDto metrics,
                                                  List<SkillLeaderboardItemDto> topSkills,
                                                  List<SkillFeedback> feedbacks,
                                                  List<SkillArchiveCandidateDto> archiveCandidates) {
        List<String> recommendations = new ArrayList<>();
        if (!topSkills.isEmpty()) {
            SkillLeaderboardItemDto top = topSkills.get(0);
            recommendations.add("将季度高影响 Skill「" + top.skillName() + "」沉淀为推广样板，补充成功案例和适用边界。");
        }
        if (!archiveCandidates.isEmpty()) {
            recommendations.add("对下架/归档候选发起维护人复核，确认修订、降级、隐藏或归档动作。");
        }
        if (metrics.companyAssetCount() == 0) {
            recommendations.add("选择成熟团队级 Skill 进入公司级评审，形成统一资产入口。");
        }
        if (metrics.feedbackClosedRate() < 80) {
            recommendations.add("把待处理反馈纳入下季度治理看板，目标反馈闭环率不低于 80%。");
        }
        if (metrics.toolchainUsageCount() == 0) {
            recommendations.add("下季度优先打通工具链遥测上报，形成真实调用、CI、评审和覆盖率证据。");
        }
        boolean hasImprovement = feedbacks.stream()
                .anyMatch(feedback -> feedback.getFeedbackType() == FeedbackType.IMPROVEMENT);
        if (hasImprovement) {
            recommendations.add("把季度改进建议合并为模板、脚本或 references 的维护任务。");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("保持当前治理节奏，下季度重点扩大高价值 Skill 使用面。");
        }
        return recommendations;
    }

    private String buildMonthlyReportMarkdown(YearMonth month,
                                              SkillAssetMetricsDto metrics,
                                              long monthlyUsageCount,
                                              long monthlyFeedbackCount,
                                              long monthlyReviewCount,
                                              double monthlySavedHours,
                                              double monthlyNewcomerOnboardingSavedHours,
                                              double monthlyReviewIssueReductionRate,
                                              double monthlyTestCoverageIncreasePoints,
                                              double monthlyFeedbackClosedRate,
                                              double monthlyReviewPassRate,
                                              List<SkillLeaderboardItemDto> topSkills,
                                              List<SkillMonthlyAwardCandidateDto> monthlyAwardCandidates,
                                              List<SkillPilotMilestoneDto> pilotMilestones,
                                              Map<String, Long> monthlyReviewRoleCounts,
                                              List<String> highlights,
                                              List<String> risks,
                                              List<String> recommendations) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# AI Skill 月度运营报告 - ").append(month).append("\n\n");
        markdown.append("生成时间：").append(Instant.now()).append("\n\n");
        markdown.append("## 核心指标\n\n");
        markdown.append("| 指标 | 数值 |\n");
        markdown.append("| --- | ---: |\n");
        markdown.append("| 资产总数 | ").append(metrics.totalSkills()).append(" |\n");
        markdown.append("| 团队级/公司级资产 | ").append(metrics.teamAssetCount() + metrics.companyAssetCount()).append(" |\n");
        markdown.append("| 本月使用次数 | ").append(monthlyUsageCount).append(" |\n");
        markdown.append("| 本月节省工时 | ").append(monthlySavedHours).append("h |\n");
        markdown.append("| 新人上手节省 | ").append(monthlyNewcomerOnboardingSavedHours).append("h |\n");
        markdown.append("| Review 问题减少率 | ").append(monthlyReviewIssueReductionRate).append("% |\n");
        markdown.append("| 测试覆盖平均提升 | ").append(monthlyTestCoverageIncreasePoints).append(" 个百分点 |\n");
        markdown.append("| 本月反馈数 | ").append(monthlyFeedbackCount).append(" |\n");
        markdown.append("| 本月反馈闭环率 | ").append(monthlyFeedbackClosedRate).append("% |\n");
        markdown.append("| 本月评审数 | ").append(monthlyReviewCount).append(" |\n");
        markdown.append("| 本月评审通过率 | ").append(monthlyReviewPassRate).append("% |\n");
        markdown.append("| 当前待复审 | ").append(metrics.needsReviewCount()).append(" |\n");
        markdown.append("| 过期 Skill 比例 | ").append(metrics.overdueSkillRate()).append("% |\n\n");

        appendPilotMilestones(markdown, pilotMilestones);
        appendList(markdown, "## 本月亮点", highlights);
        appendReviewRoleCounts(markdown, monthlyReviewRoleCounts);
        appendAwardCandidates(markdown, monthlyAwardCandidates);

        markdown.append("## 高影响 Skill\n\n");
        if (topSkills.isEmpty()) {
            markdown.append("暂无本月使用或反馈数据。\n\n");
        } else {
            markdown.append("| Skill | 层级 | 状态 | 使用 | 反馈 | 节省工时 |\n");
            markdown.append("| --- | --- | --- | ---: | ---: | ---: |\n");
            for (SkillLeaderboardItemDto item : topSkills) {
                markdown.append("| ")
                        .append(item.skillName())
                        .append(" | ")
                        .append(nullToDash(item.assetLevel()))
                        .append(" | ")
                        .append(nullToDash(item.lifecycleStatus()))
                        .append(" | ")
                        .append(item.usageCount())
                        .append(" | ")
                        .append(item.feedbackCount())
                        .append(" | ")
                        .append(item.savedHours())
                        .append("h |\n");
            }
            markdown.append("\n");
        }

        appendList(markdown, "## 风险与问题", risks);
        appendList(markdown, "## 下月建议", recommendations);
        return markdown.toString();
    }

    private String buildQuarterlyReportMarkdown(QuarterRange range,
                                                SkillAssetMetricsDto metrics,
                                                long quarterlyUsageCount,
                                                long quarterlyFeedbackCount,
                                                long quarterlyReviewCount,
                                                double quarterlySavedHours,
                                                double quarterlyNewcomerOnboardingSavedHours,
                                                double quarterlyReviewIssueReductionRate,
                                                double quarterlyTestCoverageIncreasePoints,
                                                double quarterlyFeedbackClosedRate,
                                                double quarterlyReviewPassRate,
                                                List<SkillLeaderboardItemDto> topSkills,
                                                List<SkillMonthlyAwardCandidateDto> awardCandidates,
                                                List<SkillArchiveCandidateDto> archiveCandidates,
                                                Map<String, Long> reviewRoleCounts,
                                                List<String> governanceFindings,
                                                List<String> risks,
                                                List<String> recommendations) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# AI Skill 季度治理报告 - ").append(range.label()).append("\n\n");
        markdown.append("统计周期：").append(range.startMonth()).append(" 至 ").append(range.endMonth()).append("\n\n");
        markdown.append("生成时间：").append(Instant.now()).append("\n\n");
        markdown.append("## 核心指标\n\n");
        markdown.append("| 指标 | 数值 |\n");
        markdown.append("| --- | ---: |\n");
        markdown.append("| 资产总数 | ").append(metrics.totalSkills()).append(" |\n");
        markdown.append("| 团队级/公司级资产 | ").append(metrics.teamAssetCount() + metrics.companyAssetCount()).append(" |\n");
        markdown.append("| 季度使用次数 | ").append(quarterlyUsageCount).append(" |\n");
        markdown.append("| 季度节省工时 | ").append(quarterlySavedHours).append("h |\n");
        markdown.append("| 新人上手节省 | ").append(quarterlyNewcomerOnboardingSavedHours).append("h |\n");
        markdown.append("| Review 问题减少率 | ").append(quarterlyReviewIssueReductionRate).append("% |\n");
        markdown.append("| 测试覆盖平均提升 | ").append(quarterlyTestCoverageIncreasePoints).append(" 个百分点 |\n");
        markdown.append("| 季度反馈数 | ").append(quarterlyFeedbackCount).append(" |\n");
        markdown.append("| 季度反馈闭环率 | ").append(quarterlyFeedbackClosedRate).append("% |\n");
        markdown.append("| 季度评审数 | ").append(quarterlyReviewCount).append(" |\n");
        markdown.append("| 季度评审通过率 | ").append(quarterlyReviewPassRate).append("% |\n");
        markdown.append("| 当前下架候选 | ").append(archiveCandidates.size()).append(" |\n\n");

        appendList(markdown, "## 治理发现", governanceFindings);
        appendReviewRoleCounts(markdown, reviewRoleCounts);
        appendQuarterlyAwardCandidates(markdown, awardCandidates);
        appendArchiveCandidates(markdown, archiveCandidates);

        markdown.append("## 季度高影响 Skill\n\n");
        if (topSkills.isEmpty()) {
            markdown.append("暂无季度使用或反馈数据。\n\n");
        } else {
            markdown.append("| Skill | 层级 | 状态 | 使用 | 反馈 | 节省工时 |\n");
            markdown.append("| --- | --- | --- | ---: | ---: | ---: |\n");
            for (SkillLeaderboardItemDto item : topSkills) {
                markdown.append("| ")
                        .append(item.skillName())
                        .append(" | ")
                        .append(nullToDash(item.assetLevel()))
                        .append(" | ")
                        .append(nullToDash(item.lifecycleStatus()))
                        .append(" | ")
                        .append(item.usageCount())
                        .append(" | ")
                        .append(item.feedbackCount())
                        .append(" | ")
                        .append(item.savedHours())
                        .append("h |\n");
            }
            markdown.append("\n");
        }

        appendList(markdown, "## 季度风险", risks);
        appendList(markdown, "## 下季度建议", recommendations);
        return markdown.toString();
    }

    private void appendPilotMilestones(StringBuilder markdown, List<SkillPilotMilestoneDto> milestones) {
        markdown.append("## 试点里程碑\n\n");
        markdown.append("| 阶段 | 周期 | 状态 | 进度 | 交付物 | 下一步 |\n");
        markdown.append("| --- | --- | --- | ---: | --- | --- |\n");
        for (SkillPilotMilestoneDto milestone : milestones) {
            markdown.append("| ")
                    .append(milestone.phase())
                    .append(" | ")
                    .append(milestone.period())
                    .append(" | ")
                    .append(milestoneStatusLabel(milestone.status()))
                    .append(" | ")
                    .append(milestone.progressPercent())
                    .append("% | ")
                    .append(milestone.deliverable())
                    .append(" | ")
                    .append(milestone.nextActions().isEmpty() ? "保持推进" : String.join("；", milestone.nextActions()))
                    .append(" |\n");
        }
        markdown.append("\n");
    }

    private void appendAwardCandidates(StringBuilder markdown, List<SkillMonthlyAwardCandidateDto> candidates) {
        markdown.append("## 本月高价值候选\n\n");
        if (candidates.isEmpty()) {
            markdown.append("暂无可推荐的月度贡献候选。\n\n");
            return;
        }
        markdown.append("| Skill | 分数 | 使用 | 节省工时 | 成功案例 | 推荐理由 |\n");
        markdown.append("| --- | ---: | ---: | ---: | ---: | --- |\n");
        for (SkillMonthlyAwardCandidateDto candidate : candidates) {
            markdown.append("| ")
                    .append(candidate.skillName())
                    .append(" | ")
                    .append(candidate.score())
                    .append(" | ")
                    .append(candidate.usageCount())
                    .append(" | ")
                    .append(candidate.savedHours())
                    .append("h | ")
                    .append(candidate.successCaseCount())
                    .append(" | ")
                    .append(String.join("；", candidate.reasons()))
                    .append(" |\n");
        }
        markdown.append("\n");
    }

    private void appendQuarterlyAwardCandidates(StringBuilder markdown, List<SkillMonthlyAwardCandidateDto> candidates) {
        markdown.append("## 季度高价值候选\n\n");
        if (candidates.isEmpty()) {
            markdown.append("暂无可推荐的季度贡献候选。\n\n");
            return;
        }
        markdown.append("| Skill | 分数 | 使用 | 节省工时 | 成功案例 | 推荐理由 |\n");
        markdown.append("| --- | ---: | ---: | ---: | ---: | --- |\n");
        for (SkillMonthlyAwardCandidateDto candidate : candidates) {
            markdown.append("| ")
                    .append(candidate.skillName())
                    .append(" | ")
                    .append(candidate.score())
                    .append(" | ")
                    .append(candidate.usageCount())
                    .append(" | ")
                    .append(candidate.savedHours())
                    .append("h | ")
                    .append(candidate.successCaseCount())
                    .append(" | ")
                    .append(String.join("；", candidate.reasons()))
                    .append(" |\n");
        }
        markdown.append("\n");
    }

    private void appendArchiveCandidates(StringBuilder markdown, List<SkillArchiveCandidateDto> candidates) {
        markdown.append("## 下架/归档候选\n\n");
        if (candidates.isEmpty()) {
            markdown.append("暂无需要下架或归档评估的 Skill。\n\n");
            return;
        }
        markdown.append("| Skill | 风险分 | 等级 | 使用 | 待处理反馈 | 失败案例 | 建议动作 | 原因 |\n");
        markdown.append("| --- | ---: | --- | ---: | ---: | ---: | --- | --- |\n");
        for (SkillArchiveCandidateDto candidate : candidates) {
            markdown.append("| ")
                    .append(candidate.skillName())
                    .append(" | ")
                    .append(candidate.riskScore())
                    .append(" | ")
                    .append(nullToDash(candidate.riskLevel()))
                    .append(" | ")
                    .append(candidate.usageCount())
                    .append(" | ")
                    .append(candidate.openFeedbackCount())
                    .append(" | ")
                    .append(candidate.failureCaseCount())
                    .append(" | ")
                    .append(candidate.recommendedAction())
                    .append(" | ")
                    .append(String.join("；", candidate.reasons()))
                    .append(" |\n");
        }
        markdown.append("\n");
    }

    private void appendReviewRoleCounts(StringBuilder markdown, Map<String, Long> roleCounts) {
        markdown.append("## 评审角色分布\n\n");
        if (roleCounts.values().stream().allMatch(count -> count == 0)) {
            markdown.append("本月暂无评审记录。\n\n");
            return;
        }
        markdown.append("| 角色 | 评审次数 |\n");
        markdown.append("| --- | ---: |\n");
        for (var entry : roleCounts.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            markdown.append("| ")
                    .append(reviewRoleLabel(entry.getKey()))
                    .append(" | ")
                    .append(entry.getValue())
                    .append(" |\n");
        }
        markdown.append("\n");
    }

    private SkillOperationReport saveMonthlyReport(YearMonth month,
                                                   long monthlyUsageCount,
                                                   long monthlyFeedbackCount,
                                                   long monthlyReviewCount,
                                                   double monthlySavedHours,
                                                   double monthlyFeedbackClosedRate,
                                                   double monthlyReviewPassRate,
                                                   String markdown) {
        SkillOperationReport report = new SkillOperationReport();
        report.setReportMonth(month.toString());
        report.setGeneratedAt(Instant.now());
        report.setMonthlyUsageCount(monthlyUsageCount);
        report.setMonthlyFeedbackCount(monthlyFeedbackCount);
        report.setMonthlyReviewCount(monthlyReviewCount);
        report.setMonthlySavedHours(monthlySavedHours);
        report.setMonthlyFeedbackClosedRate(monthlyFeedbackClosedRate);
        report.setMonthlyReviewPassRate(monthlyReviewPassRate);
        report.setMarkdown(markdown);
        return skillOperationReportRepository.save(report);
    }

    private void appendList(StringBuilder markdown, String title, List<String> items) {
        markdown.append(title).append("\n\n");
        for (String item : items) {
            markdown.append("- ").append(item).append("\n");
        }
        markdown.append("\n");
    }

    private Map<String, Long> reviewRoleCounts(List<SkillReview> reviews) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReviewerRole role : ReviewerRole.values()) {
            counts.put(role.name(), 0L);
        }
        for (SkillReview review : reviews) {
            ReviewerRole role = review.getReviewerRole() != null ? review.getReviewerRole() : ReviewerRole.TECH_LEAD;
            counts.put(role.name(), counts.get(role.name()) + 1);
        }
        return counts;
    }

    private SkillGovernanceSummaryDto buildSummary(Long skillId) {
        List<SkillUsageEvent> usageEvents = skillUsageEventRepository.findBySkill_Id(skillId);
        long usageCount = usageEvents.size();
        long savedMinutes = usageEvents.stream().mapToLong(SkillUsageEvent::getSavedMinutes).sum();
        QualityImpact qualityImpact = qualityImpact(usageEvents);
        long feedbackCount = skillFeedbackRepository.countBySkill_Id(skillId);
        long openFeedbackCount = skillFeedbackRepository.countBySkill_IdAndStatus(skillId, FeedbackStatus.OPEN);
        long reviewCount = skillReviewRepository.countBySkill_Id(skillId);
        var latestReview = skillReviewRepository.findFirstBySkill_IdOrderByReviewedAtDescCreatedAtDesc(skillId);
        return new SkillGovernanceSummaryDto(
                skillId,
                usageCount,
                roundHours(savedMinutes),
                qualityImpact.newcomerOnboardingSavedHours(),
                qualityImpact.reviewIssueReductionRate(),
                qualityImpact.testCoverageIncreasePoints(),
                feedbackCount,
                openFeedbackCount,
                reviewCount,
                latestReview.map(r -> r.getResult() != null ? r.getResult().name() : null).orElse(null),
                latestReview.map(SkillReview::getReviewedAt).orElse(null)
        );
    }

    private SkillTemplateValidationDto buildTemplateValidationReport(Skill skill) {
        List<SkillTemplateValidationItemDto> items = new ArrayList<>();
        items.add(required("name", "Skill 名称", skill.getName(), "需要短名称表达任务目标"));
        items.add(required("cloneCommand", "安装/获取命令", skill.getCloneCommand(), "需要能定位或安装 Skill 资产"));
        items.add(required("skillMdFrontmatter", "SKILL.md frontmatter", hasValidSkillFrontmatter(skill.getContentMd()), "需要包含 name 和 description"));
        items.add(required("version", "版本", skill.getVersion(), "需要标注版本号"));
        items.add(required("maintainer", "维护人", skill.getMaintainer(), "需要明确过期后由谁维护"));
        items.add(required("applicableScenarios", "适用场景", skill.getApplicableScenarios(), "需要说明何时触发"));
        items.add(required("nonApplicableScenarios", "不适用场景", skill.getNonApplicableScenarios(), "需要标注边界防止误用"));
        items.add(required("inputRequirements", "输入要求", skill.getInputRequirements(), "需要列出代码、日志、文档或业务背景"));
        items.add(required("executionSteps", "执行步骤", skill.getExecutionSteps(), "需要步骤化描述 AI 应如何执行"));
        items.add(required("outputFormat", "输出格式", skill.getOutputFormat(), "需要明确最终产物结构"));
        items.add(required("qualityStandard", "质量标准", skill.getQualityStandard(), "需要说明结果应满足的工程要求"));
        items.add(required("validationMethod", "验证方式", skill.getValidationMethod(), "需要列出测试、检查或人工复核办法"));
        items.add(requiredAny("referenceMaterials", "参考资料", "需要关联规范、业务规则、接口文档或示例", skill.getReferenceMaterials(), skill.getContentMd()));
        items.add(sensitiveContentCheck(skill));
        if (requiresSharedAssetMetadata(skill) && !awaitingGitLabPublication(skill)) {
            items.add(required("sourceRepositoryUrl", "仓库地址", skill.getSourceRepositoryUrl(), "团队级/公司级资产需要关联统一仓库或来源"));
            items.add(required("skillDirectory", "Skill 目录", skill.getSkillDirectory(), "团队级/公司级资产需要记录 skill-creator 生成的目录名"));
        } else {
            items.add(recommended("sourceRepositoryUrl", "仓库地址", skill.getSourceRepositoryUrl(), "建议关联 Skill 仓库或来源"));
            items.add(recommended("skillDirectory", "Skill 目录", skill.getSkillDirectory(), "建议记录 skill-creator 生成的目录名"));
        }
        items.add(recommended("contentMd", "SKILL.md 文档", skill.getContentMd(), "建议保存 SKILL.md 或主要说明文档"));

        long requiredTotal = items.stream().filter(SkillTemplateValidationItemDto::required).count();
        long requiredPassed = items.stream().filter(i -> i.required() && i.passed()).count();
        boolean passed = requiredTotal == requiredPassed;
        String notes = buildValidationNotes(items);
        return new SkillTemplateValidationDto(
                skill.getId(),
                passed ? TemplateValidationStatus.PASSED.name() : TemplateValidationStatus.FAILED.name(),
                passed,
                (int) requiredPassed,
                (int) requiredTotal,
                Instant.now(),
                notes,
                items
        );
    }

    private SkillTemplateValidationItemDto required(String key, String label, String value, String message) {
        return new SkillTemplateValidationItemDto(key, label, true, hasText(value), hasText(value) ? "已填写" : message);
    }

    private SkillTemplateValidationItemDto required(String key, String label, boolean passed, String message) {
        return new SkillTemplateValidationItemDto(key, label, true, passed, passed ? "已通过" : message);
    }

    private SkillTemplateValidationItemDto requiredAny(String key, String label, String message, String... values) {
        boolean passed = false;
        for (String value : values) {
            if (hasText(value)) {
                passed = true;
                break;
            }
        }
        return new SkillTemplateValidationItemDto(key, label, true, passed, passed ? "已填写" : message);
    }

    private SkillTemplateValidationItemDto recommended(String key, String label, String value, String message) {
        return new SkillTemplateValidationItemDto(key, label, false, hasText(value), hasText(value) ? "已填写" : message);
    }

    private SkillTemplateValidationItemDto sensitiveContentCheck(Skill skill) {
        List<String> findings = sensitiveContentFindings(skill);
        if (findings.isEmpty()) {
            return new SkillTemplateValidationItemDto("sensitiveContent", "安全红线", true, true, "未发现高置信度敏感凭据或隐私数据");
        }
        return new SkillTemplateValidationItemDto(
                "sensitiveContent",
                "安全红线",
                true,
                false,
                "发现疑似" + String.join("、", findings) + "，请移除后再入库"
        );
    }

    private List<String> sensitiveContentFindings(Skill skill) {
        String text = String.join("\n",
                safeText(skill.getName()),
                safeText(skill.getDescription()),
                safeText(skill.getCloneCommand()),
                safeText(skill.getContentMd()),
                safeText(skill.getSourceRepositoryUrl()),
                safeText(skill.getSkillDirectory()),
                safeText(skill.getApplicableScenarios()),
                safeText(skill.getNonApplicableScenarios()),
                safeText(skill.getInputRequirements()),
                safeText(skill.getExecutionSteps()),
                safeText(skill.getOutputFormat()),
                safeText(skill.getQualityStandard()),
                safeText(skill.getValidationMethod()),
                safeText(skill.getReferenceMaterials()),
                safeText(skill.getReviewNotes())
        );
        List<String> findings = new ArrayList<>();
        for (SensitiveContentPattern pattern : SENSITIVE_CONTENT_PATTERNS) {
            if (pattern.pattern().matcher(text).find() && !findings.contains(pattern.label())) {
                findings.add(pattern.label());
            }
        }
        return findings;
    }

    private boolean requiresSharedAssetMetadata(Skill skill) {
        return skill.getAssetLevel() == AssetLevel.TEAM || skill.getAssetLevel() == AssetLevel.COMPANY;
    }

    private boolean awaitingGitLabPublication(Skill skill) {
        return gitLabSkillPublishService.isPublicationEnabled()
                && skill.getCreationSource() == CreationSource.SKILL_CREATOR_PACKAGE
                && hasText(skill.getSkillPackageFiles())
                && !hasText(skill.getSourceRepositoryUrl());
    }

    private String buildValidationNotes(List<SkillTemplateValidationItemDto> items) {
        List<String> missing = items.stream()
                .filter(item -> item.required() && !item.passed())
                .map(SkillTemplateValidationItemDto::label)
                .toList();
        if (missing.isEmpty()) return "模板必填项已通过";
        return "缺少必填项：" + String.join("、", missing);
    }

    private Skill getSkill(Long skillId, boolean visibleOnly) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill 不存在"));
        if (visibleOnly && skill.getVisibility() != Visibility.VISIBLE) {
            throw new ResourceNotFoundException("Skill 不存在或已隐藏");
        }
        return skill;
    }

    private Long resolveTelemetrySkillId(SkillToolchainTelemetryRequest request) {
        if (request.skillId() != null) {
            return request.skillId();
        }
        String skillDirectory = blankToNull(request.skillDirectory());
        if (skillDirectory == null) {
            throw new IllegalArgumentException("工具链遥测必须提供 skillId 或 skillDirectory");
        }
        return skillRepository.findFirstBySkillDirectoryIgnoreCase(skillDirectory)
                .map(Skill::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill 目录不存在：" + skillDirectory));
    }

    private Pageable pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page - 1), Math.min(PAGE_SIZE_LIMIT, Math.max(1, size)), sort);
    }

    private LifecycleStatus statusFromReviewResult(ReviewResult result) {
        if (result == ReviewResult.PASSED) return LifecycleStatus.APPROVED;
        if (result == ReviewResult.REJECTED) return LifecycleStatus.ARCHIVED;
        return LifecycleStatus.NEEDS_REVIEW;
    }

    private void markOverdueSkillsForReview(List<Skill> skills, LocalDate today) {
        for (Skill skill : skills) {
            if (!isReviewOverdue(skill, today)) {
                continue;
            }
            if (skill.getLifecycleStatus() == LifecycleStatus.NEEDS_REVIEW) {
                continue;
            }
            skill.setLifecycleStatus(LifecycleStatus.NEEDS_REVIEW);
            if (!hasText(skill.getReviewNotes())) {
                skill.setReviewNotes("复审日期已到期，请维护人确认规范、版本和适用范围。");
            }
        }
    }

    private boolean isReviewOverdue(Skill skill, LocalDate today) {
        return skill.getLifecycleStatus() != LifecycleStatus.ARCHIVED
                && skill.getNextReviewAt() != null
                && !skill.getNextReviewAt().isAfter(today);
    }

    private Map<String, Long> countByAssetLevel(List<Skill> skills) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (AssetLevel level : AssetLevel.values()) {
            counts.put(level.name(), 0L);
        }
        for (Skill skill : skills) {
            AssetLevel level = skill.getAssetLevel() != null ? skill.getAssetLevel() : AssetLevel.TEAM;
            counts.put(level.name(), counts.get(level.name()) + 1);
        }
        return counts;
    }

    private Map<String, Long> countByLifecycleStatus(List<Skill> skills) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LifecycleStatus status : LifecycleStatus.values()) {
            counts.put(status.name(), 0L);
        }
        for (Skill skill : skills) {
            LifecycleStatus status = skill.getLifecycleStatus() != null ? skill.getLifecycleStatus() : LifecycleStatus.CANDIDATE;
            counts.put(status.name(), counts.get(status.name()) + 1);
        }
        return counts;
    }

    private Map<String, Long> countBySkillCategory(List<Skill> skills) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (SkillCategory category : SkillCategory.values()) {
            counts.put(category.name(), 0L);
        }
        for (Skill skill : skills) {
            SkillCategory category = skill.getSkillCategory() != null ? skill.getSkillCategory() : SkillCategory.CODING_IMPLEMENTATION;
            counts.put(category.name(), counts.get(category.name()) + 1);
        }
        return counts;
    }

    private Map<String, Long> countByBuildPriority(List<Skill> skills) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BuildPriority priority : BuildPriority.values()) {
            counts.put(priority.name(), 0L);
        }
        for (Skill skill : skills) {
            BuildPriority priority = skill.getBuildPriority() != null ? skill.getBuildPriority() : BuildPriority.P2;
            counts.put(priority.name(), counts.get(priority.name()) + 1);
        }
        return counts;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(label + "不合法：" + value);
        }
    }

    private YearMonth parseReportMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("月份格式应为 YYYY-MM");
        }
    }

    private QuarterRange parseReportQuarter(String quarter) {
        if (quarter == null || quarter.isBlank()) {
            YearMonth current = YearMonth.now();
            int quarterNumber = quarterOfMonth(current.getMonthValue());
            YearMonth startMonth = YearMonth.of(current.getYear(), (quarterNumber - 1) * 3 + 1);
            return quarterRange(current.getYear(), quarterNumber, startMonth);
        }
        var matcher = QUARTER_PATTERN.matcher(quarter.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("季度格式应为 YYYY-Q1 到 YYYY-Q4");
        }
        int year = Integer.parseInt(matcher.group(1));
        int quarterNumber = Integer.parseInt(matcher.group(2));
        YearMonth startMonth = YearMonth.of(year, (quarterNumber - 1) * 3 + 1);
        return quarterRange(year, quarterNumber, startMonth);
    }

    private QuarterRange quarterRange(int year, int quarterNumber, YearMonth startMonth) {
        ZoneId zone = ZoneId.systemDefault();
        YearMonth endMonth = startMonth.plusMonths(2);
        LocalDate startDate = startMonth.atDay(1);
        LocalDate endDate = endMonth.atEndOfMonth();
        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = endMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        return new QuarterRange(
                year + "-Q" + quarterNumber,
                startMonth,
                endMonth,
                startDate,
                endDate,
                start,
                end
        );
    }

    private int quarterOfMonth(int month) {
        return ((month - 1) / 3) + 1;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasValidSkillFrontmatter(String contentMd) {
        if (!hasText(contentMd)) return false;
        String markdown = contentMd.stripLeading();
        if (!markdown.startsWith("---")) return false;
        int end = markdown.indexOf("\n---", 3);
        if (end < 0) return false;
        String frontmatter = markdown.substring(3, end);
        boolean hasName = false;
        boolean hasDescription = false;
        for (String line : frontmatter.split("\\R")) {
            String normalized = line.trim().toLowerCase();
            if (normalized.startsWith("name:") && normalized.length() > "name:".length()) {
                hasName = true;
            }
            if (normalized.startsWith("description:") && normalized.length() > "description:".length()) {
                hasDescription = true;
            }
        }
        return hasName && hasDescription;
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private Integer nonNegativeOrNull(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    private Double percentOrNull(Double value) {
        if (value == null) return null;
        return Math.max(0, Math.min(100, value));
    }

    private QualityImpact qualityImpact(List<SkillUsageEvent> events) {
        long onboardingSavedMinutes = 0;
        long reviewIssuesBefore = 0;
        long reviewIssuesAfter = 0;
        double coverageIncreaseSum = 0;
        long coverageSampleCount = 0;
        long qualitySignalCount = 0;

        for (SkillUsageEvent event : events) {
            boolean hasSignal = false;
            if (event.getNewcomerOnboardingSavedMinutes() > 0) {
                onboardingSavedMinutes += event.getNewcomerOnboardingSavedMinutes();
                hasSignal = true;
            }
            if (event.getReviewIssuesBefore() != null
                    && event.getReviewIssuesAfter() != null
                    && event.getReviewIssuesBefore() > 0) {
                reviewIssuesBefore += event.getReviewIssuesBefore();
                reviewIssuesAfter += Math.max(0, event.getReviewIssuesAfter());
                hasSignal = true;
            }
            if (event.getTestCoverageBefore() != null && event.getTestCoverageAfter() != null) {
                coverageIncreaseSum += Math.max(0, event.getTestCoverageAfter() - event.getTestCoverageBefore());
                coverageSampleCount++;
                hasSignal = true;
            }
            if (hasSignal) {
                qualitySignalCount++;
            }
        }

        long reducedReviewIssues = Math.max(0, reviewIssuesBefore - reviewIssuesAfter);
        double reviewIssueReductionRate = ratio(reducedReviewIssues, reviewIssuesBefore);
        double testCoverageIncreasePoints = coverageSampleCount > 0
                ? roundOneDecimal(coverageIncreaseSum / coverageSampleCount)
                : 0;
        return new QualityImpact(
                roundHours(onboardingSavedMinutes),
                reviewIssueReductionRate,
                testCoverageIncreasePoints,
                qualitySignalCount
        );
    }

    private ToolchainUsageStats toolchainUsageStats(List<SkillUsageEvent> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ToolchainSource source : ToolchainSource.values()) {
            counts.put(source.name(), 0L);
        }

        long manualUsageCount = 0;
        long toolchainUsageCount = 0;
        for (SkillUsageEvent event : events) {
            ToolchainSource source = event.getToolchainSource() != null
                    ? event.getToolchainSource()
                    : ToolchainSource.MANUAL;
            counts.put(source.name(), counts.get(source.name()) + 1);
            if (source == ToolchainSource.MANUAL) {
                manualUsageCount++;
            } else {
                toolchainUsageCount++;
            }
        }
        return new ToolchainUsageStats(manualUsageCount, toolchainUsageCount, counts);
    }

    private boolean isToolchainEvent(SkillUsageEvent event) {
        ToolchainSource source = event.getToolchainSource();
        return source != null && source != ToolchainSource.MANUAL;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    private double roundHours(long minutes) {
        return Math.round((minutes / 60.0) * 10.0) / 10.0;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int conditions(boolean... values) {
        int passed = 0;
        for (boolean value : values) {
            if (value) {
                passed++;
            }
        }
        return passed;
    }

    private SkillPilotMilestoneDto milestone(String phase,
                                             String period,
                                             String keyAction,
                                             String deliverable,
                                             int passedConditions,
                                             int totalConditions,
                                             List<String> evidence,
                                             List<String> nextActions) {
        int progress = totalConditions <= 0 ? 0 : (int) Math.round(passedConditions * 100.0 / totalConditions);
        String status = progress >= 100 ? "DONE" : progress > 0 ? "IN_PROGRESS" : "PENDING";
        return new SkillPilotMilestoneDto(
                phase,
                period,
                keyAction,
                deliverable,
                status,
                progress,
                evidence,
                nextActions
        );
    }

    private List<String> nextActions(String... actions) {
        List<String> result = new ArrayList<>();
        for (String action : actions) {
            if (hasText(action)) {
                result.add(action);
            }
        }
        return result;
    }

    private String nullToDash(String value) {
        return hasText(value) ? value : "-";
    }

    private String milestoneStatusLabel(String status) {
        if ("DONE".equals(status)) return "已完成";
        if ("IN_PROGRESS".equals(status)) return "推进中";
        if ("PENDING".equals(status)) return "未开始";
        return nullToDash(status);
    }

    private String reviewRoleLabel(String role) {
        if (ReviewerRole.CONTRIBUTOR.name().equals(role)) return "贡献者";
        if (ReviewerRole.TECH_LEAD.name().equals(role)) return "Tech Lead";
        if (ReviewerRole.PLATFORM_ENGINEERING.name().equals(role)) return "平台/效能团队";
        if (ReviewerRole.TECHNICAL_COMMITTEE.name().equals(role)) return "技术委员会";
        if (ReviewerRole.SECURITY_QUALITY.name().equals(role)) return "安全/质量团队";
        return nullToDash(role);
    }

    private record SensitiveContentPattern(Pattern pattern, String label) {}

    private record QualityImpact(
            double newcomerOnboardingSavedHours,
            double reviewIssueReductionRate,
            double testCoverageIncreasePoints,
            long qualitySignalCount
    ) {}

    private record ToolchainUsageStats(
            long manualUsageCount,
            long toolchainUsageCount,
            Map<String, Long> sourceCounts
    ) {}

    private record QuarterRange(
            String label,
            YearMonth startMonth,
            YearMonth endMonth,
            LocalDate startDate,
            LocalDate endDate,
            Instant start,
            Instant end
    ) {}

    private static class TrendBucket {
        long usageCount;
        long feedbackCount;
        long savedMinutes;
    }

    private class SkillBucket {
        private final Skill skill;
        private long usageCount;
        private long feedbackCount;
        private long openFeedbackCount;
        private long savedMinutes;

        private SkillBucket(Skill skill) {
            this.skill = skill;
        }

        private long impactScore() {
            return usageCount * 1000 + savedMinutes + feedbackCount * 100;
        }

        private SkillLeaderboardItemDto toDto() {
            return new SkillLeaderboardItemDto(
                    skill.getId(),
                    skill.getName(),
                    skill.getAssetLevel() != null ? skill.getAssetLevel().name() : null,
                    skill.getLifecycleStatus() != null ? skill.getLifecycleStatus().name() : null,
                    usageCount,
                    feedbackCount,
                    openFeedbackCount,
                    roundHours(savedMinutes)
            );
        }
    }

    private class AwardCandidateBucket {
        private final Skill skill;
        private long usageCount;
        private long feedbackCount;
        private long successCaseCount;
        private long openFeedbackCount;
        private long savedMinutes;

        private AwardCandidateBucket(Skill skill) {
            this.skill = skill;
        }

        private long score() {
            return usageCount * 10
                    + Math.round(roundHours(savedMinutes) * 8)
                    + successCaseCount * 12
                    + feedbackCount * 4
                    - openFeedbackCount * 3;
        }

        private SkillMonthlyAwardCandidateDto toDto() {
            return new SkillMonthlyAwardCandidateDto(
                    skill.getId(),
                    skill.getName(),
                    skill.getAssetLevel() != null ? skill.getAssetLevel().name() : null,
                    skill.getLifecycleStatus() != null ? skill.getLifecycleStatus().name() : null,
                    usageCount,
                    feedbackCount,
                    successCaseCount,
                    openFeedbackCount,
                    roundHours(savedMinutes),
                    score(),
                    reasons()
            );
        }

        private List<String> reasons() {
            List<String> reasons = new ArrayList<>();
            if (usageCount > 0) {
                reasons.add("使用 " + usageCount + " 次");
            }
            double savedHours = roundHours(savedMinutes);
            if (savedHours > 0) {
                reasons.add("节省 " + savedHours + "h");
            }
            if (successCaseCount > 0) {
                reasons.add("成功案例 " + successCaseCount + " 条");
            }
            if (feedbackCount > 0) {
                reasons.add("反馈 " + feedbackCount + " 条");
            }
            if (openFeedbackCount > 0) {
                reasons.add("仍有 " + openFeedbackCount + " 条反馈待处理");
            }
            if (reasons.isEmpty()) {
                reasons.add("近期有资产化贡献");
            }
            return reasons;
        }
    }

    private class ArchiveCandidateBucket {
        private final Skill skill;
        private long usageCount;
        private Instant lastUsageAt;
        private long feedbackCount;
        private long openFeedbackCount;
        private long failureCaseCount;

        private ArchiveCandidateBucket(Skill skill) {
            this.skill = skill;
        }

        private void recordUsage(SkillUsageEvent event) {
            usageCount++;
            Instant createdAt = event.getCreatedAt();
            if (createdAt != null && (lastUsageAt == null || createdAt.isAfter(lastUsageAt))) {
                lastUsageAt = createdAt;
            }
        }

        private void recordFeedback(SkillFeedback feedback) {
            feedbackCount++;
            if (feedback.getStatus() == FeedbackStatus.OPEN) {
                openFeedbackCount++;
            }
            if (feedback.getFeedbackType() == FeedbackType.FAILURE_CASE) {
                failureCaseCount++;
            }
        }

        private SkillArchiveCandidateDto toDto(Instant staleSince, LocalDate today) {
            List<String> reasons = new ArrayList<>();
            long score = 0;
            long daysSinceLastUsage = daysSinceLastUsage(today);

            if (usageCount == 0 && isOlderThan(skill.getCreatedAt(), staleSince)) {
                reasons.add("90 天无人使用");
                score += 30;
            } else if (usageCount > 0 && isOlderThan(lastUsageAt, staleSince)) {
                reasons.add("最近 " + daysSinceLastUsage + " 天无使用");
                score += 25;
            }

            if (skill.getLifecycleStatus() == LifecycleStatus.NEEDS_REVIEW || isReviewOverdue(skill, today)) {
                reasons.add("复审过期或待复审");
                score += 40;
            }

            if (skill.getTemplateValidationStatus() == TemplateValidationStatus.FAILED) {
                reasons.add("模板校验失败");
                score += 35;
            }

            if (failureCaseCount >= 2) {
                reasons.add("失败案例 " + failureCaseCount + " 条");
                score += failureCaseCount * 12;
            }

            if (skill.getRiskLevel() == RiskLevel.HIGH && openFeedbackCount > 0) {
                reasons.add("高风险且有待处理反馈");
                score += 45;
            } else if (openFeedbackCount >= 3) {
                reasons.add("待处理反馈 " + openFeedbackCount + " 条");
                score += 20;
            }

            if (reasons.isEmpty()) {
                return null;
            }

            return new SkillArchiveCandidateDto(
                    skill.getId(),
                    skill.getName(),
                    skill.getAssetLevel() != null ? skill.getAssetLevel().name() : null,
                    skill.getLifecycleStatus() != null ? skill.getLifecycleStatus().name() : null,
                    skill.getRiskLevel() != null ? skill.getRiskLevel().name() : null,
                    skill.getMaintainer(),
                    skill.getTeamName(),
                    usageCount,
                    lastUsageAt,
                    daysSinceLastUsage,
                    feedbackCount,
                    openFeedbackCount,
                    failureCaseCount,
                    skill.getNextReviewAt(),
                    score,
                    score >= 60 ? "建议发起归档/下架评审" : "建议维护人复核",
                    reasons
            );
        }

        private long daysSinceLastUsage(LocalDate today) {
            Instant reference = lastUsageAt != null ? lastUsageAt : skill.getCreatedAt();
            if (reference == null) {
                return 0;
            }
            LocalDate referenceDate = reference.atZone(ZoneId.systemDefault()).toLocalDate();
            return Math.max(0, ChronoUnit.DAYS.between(referenceDate, today));
        }

        private boolean isOlderThan(Instant value, Instant threshold) {
            return value != null && value.isBefore(threshold);
        }
    }
}
