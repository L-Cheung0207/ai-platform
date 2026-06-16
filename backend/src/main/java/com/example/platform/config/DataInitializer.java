package com.example.platform.config;

import com.example.platform.entity.Category;
import com.example.platform.entity.ForumPost;
import com.example.platform.entity.LearningArticle;
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
import com.example.platform.entity.SkillReview;
import com.example.platform.entity.SkillReview.ReviewResult;
import com.example.platform.entity.SkillReview.ReviewStage;
import com.example.platform.entity.SkillReview.ReviewerRole;
import com.example.platform.entity.SkillUsageEvent;
import com.example.platform.entity.Tag;
import com.example.platform.entity.User;
import com.example.platform.entity.ForumCategory;
import com.example.platform.repository.CategoryRepository;
import com.example.platform.repository.ForumCategoryRepository;
import com.example.platform.repository.ForumPostRepository;
import com.example.platform.repository.LearningArticleRepository;
import com.example.platform.repository.SkillFeedbackRepository;
import com.example.platform.repository.SkillRepository;
import com.example.platform.repository.SkillReviewRepository;
import com.example.platform.repository.SkillUsageEventRepository;
import com.example.platform.repository.TagRepository;
import com.example.platform.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final TagRepository tagRepository;
    private final ForumCategoryRepository forumCategoryRepository;
    private final LearningArticleRepository learningArticleRepository;
    private final ForumPostRepository forumPostRepository;
    private final SkillReviewRepository skillReviewRepository;
    private final SkillFeedbackRepository skillFeedbackRepository;
    private final SkillUsageEventRepository skillUsageEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DataInitializer(UserRepository userRepository, CategoryRepository categoryRepository,
                           SkillRepository skillRepository, TagRepository tagRepository,
                           ForumCategoryRepository forumCategoryRepository,
                           LearningArticleRepository learningArticleRepository,
                           ForumPostRepository forumPostRepository,
                           SkillReviewRepository skillReviewRepository,
                           SkillFeedbackRepository skillFeedbackRepository,
                           SkillUsageEventRepository skillUsageEventRepository,
                           PasswordEncoder passwordEncoder, Environment environment) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.tagRepository = tagRepository;
        this.forumCategoryRepository = forumCategoryRepository;
        this.learningArticleRepository = learningArticleRepository;
        this.forumPostRepository = forumPostRepository;
        this.skillReviewRepository = skillReviewRepository;
        this.skillFeedbackRepository = skillFeedbackRepository;
        this.skillUsageEventRepository = skillUsageEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategoriesIfEmpty();
        seedForumCategoriesIfEmpty();
        seedAdminIfNone();
        seedDemoContentIfEnabled();
        seedPilotSkillsIfEnabled();
    }

    private void seedCategoriesIfEmpty() {
        if (categoryRepository.count() > 0) return;
        String[][] defaults = {
            { "Skill/Rule", "SKILL_RULE", "10" },
            { "AI知识库", "ARTICLE", "20" },
        };
        for (int i = 0; i < defaults.length; i++) {
            Category c = new Category();
            c.setName(defaults[i][0]);
            c.setType(defaults[i][1]);
            c.setSortOrder(Integer.parseInt(defaults[i][2]));
            categoryRepository.save(c);
        }
    }

    private void seedForumCategoriesIfEmpty() {
        if (forumCategoryRepository.count() > 0) return;
        String[][] defaults = {
                { "提问求助", "help", "遇到问题时先来这里发问", "10" },
                { "经验分享", "share", "沉淀实战经验和踩坑总结", "20" },
                { "方案讨论", "discussion", "围绕方案、工具和架构展开讨论", "30" },
                { "最佳实践", "practice", "发布团队通用方法和最佳实践", "40" },
        };
        for (String[] row : defaults) {
            ForumCategory category = new ForumCategory();
            category.setName(row[0]);
            category.setSlug(row[1]);
            category.setDescription(row[2]);
            category.setSortOrder(Integer.parseInt(row[3]));
            category.setEnabled(true);
            forumCategoryRepository.save(category);
        }
    }

    private void seedAdminIfNone() {
        if (userRepository.existsByRole(User.Role.ADMIN)) {
            return;
        }
        String username = environment.getProperty("app.admin.init-username", "admin");
        String password = environment.getProperty("app.admin.init-password", "admin123");
        if (userRepository.existsByUsername(username)) {
            User existing = userRepository.findByUsername(username).orElseThrow();
            existing.setRole(User.Role.ADMIN);
            existing.setSkillGovernanceRole(ReviewerRole.SECURITY_QUALITY);
            userRepository.save(existing);
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setSkillGovernanceRole(ReviewerRole.SECURITY_QUALITY);
        userRepository.save(admin);
    }

    private void seedDemoContentIfEnabled() {
        if (!environment.getProperty("app.seed.demo-content", Boolean.class, false)) {
            return;
        }
        User admin = userRepository.findFirstByRole(User.Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Admin user must exist before seeding demo content"));
        seedDemoArticlesIfEmpty(admin);
        seedDemoForumPostsIfEmpty(admin);
    }

    private void seedDemoArticlesIfEmpty(User author) {
        if (learningArticleRepository.count() > 0) {
            return;
        }
        for (int i = 0; i < demoArticles().size(); i++) {
            DemoArticle demo = demoArticles().get(i);
            LearningArticle article = new LearningArticle();
            article.setTitle(demo.title());
            article.setContent(demo.content());
            article.setAuthor(author);
            article.setStatus(LearningArticle.Status.PUBLISHED);
            article.setContentType(LearningArticle.ContentType.MARKDOWN);
            Instant timestamp = Instant.now().minusSeconds((long) (10 - i) * 7200);
            article.setCreatedAt(timestamp);
            article.setUpdatedAt(timestamp.plusSeconds(1800));
            learningArticleRepository.save(article);
        }
    }

    private void seedDemoForumPostsIfEmpty(User author) {
        if (forumPostRepository.count() > 0) {
            return;
        }
        List<ForumCategory> categories = forumCategoryRepository.findByEnabledTrueOrderBySortOrderAscNameAsc();
        if (categories.isEmpty()) {
            return;
        }
        for (int i = 0; i < demoForumPosts().size(); i++) {
            DemoForumPost demo = demoForumPosts().get(i);
            ForumPost post = new ForumPost();
            post.setTitle(demo.title());
            post.setContent(demo.content());
            post.setAuthor(author);
            post.setCategory(categories.get(i % categories.size()));
            post.setPostType(demo.postType());
            post.setStatus(ForumPost.PostStatus.NORMAL);
            post.setPinned(demo.pinned());
            post.setFeatured(demo.featured());
            post.setViewCount(40L + i * 13L);
            post.setLikeCount(3L + i);
            post.setReplyCount((long) (i % 5));
            post.setFavoriteCount(1L + (i % 4));
            post.setLastActivityAt(Instant.now().minusSeconds((long) (10 - i) * 5400));
            forumPostRepository.save(post);
        }
    }

    private void seedPilotSkillsIfEnabled() {
        if (!environment.getProperty("app.seed.pilot-skills", Boolean.class, false)) {
            return;
        }
        User admin = userRepository.findFirstByRole(User.Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Admin user must exist before seeding pilot skills"));
        for (PilotSkill pilot : pilotSkills()) {
            var existing = skillRepository.findFirstBySkillDirectoryIgnoreCase(pilot.skillDirectory());
            if (existing.isPresent()) {
                Skill skill = existing.get();
                skill.setCloneCommand(pilot.cloneCommand());
                skill.setSourceRepositoryUrl(pilot.sourceRepositoryUrl());
                applyPilotTrialWindow(skill, pilot);
                skillRepository.save(skill);
                continue;
            }
            Skill skill = createPilotSkill(pilot, admin);
            seedPilotGovernance(skill, pilot);
        }
    }

    private Skill createPilotSkill(PilotSkill pilot, User uploader) {
        Skill skill = new Skill();
        skill.setName(pilot.name());
        skill.setDescription(pilot.description());
        skill.setCloneCommand(pilot.cloneCommand());
        skill.setContentMd(pilot.contentMd());
        skill.setSourceRepositoryUrl(pilot.sourceRepositoryUrl());
        skill.setSkillDirectory(pilot.skillDirectory());
        skill.setAssetLevel(pilot.assetLevel());
        skill.setLifecycleStatus(pilot.lifecycleStatus());
        skill.setSkillCategory(defaultSkillCategory(pilot.skillDirectory()));
        skill.setBuildPriority(defaultBuildPriority(pilot.skillDirectory()));
        skill.setCreationSource(CreationSource.SEED);
        skill.setMaintainer("平台工程");
        skill.setTeamName(pilot.teamName());
        skill.setVersion("1.0.0");
        skill.setApplicableScenarios(pilot.applicableScenarios());
        skill.setNonApplicableScenarios(pilot.nonApplicableScenarios());
        skill.setInputRequirements(pilot.inputRequirements());
        skill.setExecutionSteps(pilot.executionSteps());
        skill.setOutputFormat(pilot.outputFormat());
        skill.setValidationMethod(pilot.validationMethod());
        skill.setQualityStandard(pilot.qualityStandard());
        skill.setReferenceMaterials(pilot.referenceMaterials());
        skill.setRiskLevel(pilot.riskLevel());
        applyPilotTrialWindow(skill, pilot);
        skill.setTemplateValidationStatus(TemplateValidationStatus.PASSED);
        skill.setTemplateValidationNotes("试点样板模板必填项已通过");
        skill.setLastValidatedAt(Instant.now());
        skill.setReviewNotes(pilot.reviewNotes());
        skill.setLastReviewedAt(pilot.lastReviewedAt());
        skill.setNextReviewAt(pilot.nextReviewAt());
        skill.setUploader(uploader);
        skill.setVisibility(Visibility.VISIBLE);
        skill.setTags(pilot.tags().stream().map(this::resolveTag).toList());
        return skillRepository.save(skill);
    }

    private void applyPilotTrialWindow(Skill skill, PilotSkill pilot) {
        if (pilot.lifecycleStatus() != LifecycleStatus.TRIAL) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (skill.getTrialStartedAt() == null || skill.getTrialStartedAt().isAfter(today)) {
            skill.setTrialStartedAt(today.minusDays(7));
        }
        if (skill.getTrialEndsAt() == null || skill.getTrialEndsAt().isAfter(today.plusDays(14))) {
            skill.setTrialEndsAt(today.plusDays(7));
        }
    }

    private void seedPilotGovernance(Skill skill, PilotSkill pilot) {
        if (skillUsageEventRepository.countBySkill_Id(skill.getId()) == 0 && pilot.sampleSavedMinutes() > 0) {
            SkillUsageEvent usage = new SkillUsageEvent();
            usage.setSkill(skill);
            usage.setUserName("试点用户");
            usage.setScenario(pilot.applicableScenarios());
            usage.setSavedMinutes(pilot.sampleSavedMinutes());
            applyPilotQualitySignals(usage, pilot.skillDirectory());
            skillUsageEventRepository.save(usage);
        }

        if (skillFeedbackRepository.countBySkill_Id(skill.getId()) == 0 && hasText(pilot.feedback())) {
            SkillFeedback feedback = new SkillFeedback();
            feedback.setSkill(skill);
            feedback.setSubmitterName("试点团队");
            feedback.setFeedbackType(FeedbackType.SUCCESS_CASE);
            feedback.setStatus(FeedbackStatus.OPEN);
            feedback.setRating(5);
            feedback.setEstimatedSavedMinutes(pilot.sampleSavedMinutes());
            feedback.setContent(pilot.feedback());
            skillFeedbackRepository.save(feedback);
        }

        if (skillReviewRepository.countBySkill_Id(skill.getId()) == 0 && pilot.lifecycleStatus() == LifecycleStatus.APPROVED) {
            SkillReview review = new SkillReview();
            review.setSkill(skill);
            ReviewerRole reviewerRole = defaultReviewerRole(skill);
            review.setReviewerName(defaultReviewerName(reviewerRole));
            review.setReviewerRole(reviewerRole);
            review.setReviewStage(skill.getAssetLevel() == AssetLevel.COMPANY ? ReviewStage.COMPANY_REVIEW : ReviewStage.TEAM_REVIEW);
            review.setResult(ReviewResult.PASSED);
            review.setTruthful(true);
            review.setAccurate(true);
            review.setReusable(true);
            review.setExecutable(true);
            review.setSecure(true);
            review.setVerifiable(true);
            review.setMaintainable(true);
            review.setNotes("试点样板已满足入库标准");
            review.setReviewedAt(pilot.lastReviewedAt() != null ? pilot.lastReviewedAt() : LocalDate.now());
            review.setNextReviewAt(pilot.nextReviewAt());
            skillReviewRepository.save(review);
        }
    }

    private ReviewerRole defaultReviewerRole(Skill skill) {
        if (skill.getRiskLevel() == RiskLevel.HIGH) {
            return ReviewerRole.SECURITY_QUALITY;
        }
        if (skill.getAssetLevel() == AssetLevel.COMPANY) {
            return ReviewerRole.TECHNICAL_COMMITTEE;
        }
        return ReviewerRole.TECH_LEAD;
    }

    private String defaultReviewerName(ReviewerRole role) {
        if (role == ReviewerRole.SECURITY_QUALITY) return "安全/质量团队";
        if (role == ReviewerRole.TECHNICAL_COMMITTEE) return "技术委员会";
        if (role == ReviewerRole.PLATFORM_ENGINEERING) return "平台工程";
        return "Tech Lead";
    }

    private void applyPilotQualitySignals(SkillUsageEvent usage, String skillDirectory) {
        if ("java-code-review".equals(skillDirectory)) {
            usage.setReviewIssuesBefore(8);
            usage.setReviewIssuesAfter(3);
        } else if ("unit-test-generator".equals(skillDirectory)) {
            usage.setTestCoverageBefore(52.0);
            usage.setTestCoverageAfter(68.0);
        } else if ("tech-solution-draft".equals(skillDirectory)) {
            usage.setNewcomerOnboardingSavedMinutes(120);
        }
    }

    private Tag resolveTag(String name) {
        return tagRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setName(name);
            return tagRepository.save(tag);
        });
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private SkillCategory defaultSkillCategory(String skillDirectory) {
        return switch (skillDirectory) {
            case "java-code-review" -> SkillCategory.CODE_REVIEW;
            case "unit-test-generator" -> SkillCategory.TESTING_VALIDATION;
            case "api-doc-generator" -> SkillCategory.DOCUMENTATION_KNOWLEDGE;
            case "incident-triage" -> SkillCategory.OPS_TROUBLESHOOTING;
            case "tech-solution-draft" -> SkillCategory.ARCHITECTURE_DESIGN;
            default -> SkillCategory.CODING_IMPLEMENTATION;
        };
    }

    private BuildPriority defaultBuildPriority(String skillDirectory) {
        return switch (skillDirectory) {
            case "java-code-review", "unit-test-generator" -> BuildPriority.P0;
            case "incident-triage", "tech-solution-draft" -> BuildPriority.P1;
            default -> BuildPriority.P2;
        };
    }

    private List<PilotSkill> pilotSkills() {
        LocalDate reviewedAt = LocalDate.now();
        LocalDate nextReviewAt = reviewedAt.plusMonths(1);
        return List.of(
                new PilotSkill(
                        "java-code-review",
                        "代码 Review Skill",
                        "根据公司编码规范、安全要求和常见缺陷清单审查代码变更，输出问题、风险和修复建议。",
                        "git clone https://git.example.com/ai-skills/java-code-review.git",
                        "https://git.example.com/ai-skills/java-code-review",
                        AssetLevel.TEAM,
                        LifecycleStatus.APPROVED,
                        RiskLevel.MEDIUM,
                        "效能团队",
                        List.of("测试与安全", "代码 Review"),
                        "PR 提交前、复杂重构后、线上缺陷修复后。",
                        "需求尚未明确、缺少代码差异或仅需要业务验收时不适用。",
                        "代码 diff、相关业务规则、测试结果和风险背景。",
                        "1. 阅读变更范围。\n2. 对照规范检查正确性、安全和性能。\n3. 标注阻塞问题和建议项。\n4. 给出测试补充建议。",
                        "Markdown Review 报告：问题清单、风险等级、修复建议、测试建议。",
                        "人工复核高风险项，并运行项目测试或静态检查。",
                        "必须区分阻塞问题和建议项；高风险问题需要给出代码位置、影响范围和修复方向。",
                        "公司 Java 编码规范、安全审查清单、Spring Boot API 分层约定。",
                        skillMd("java-code-review", "根据公司编码规范、安全要求和常见缺陷审查 Java/Spring Boot 代码变更。", "代码 Review Skill", "面向真实代码变更执行结构化审查，重点关注正确性、安全、性能、可维护性和测试缺口。"),
                        "满足第一批试点 Skill 入库标准。",
                        reviewedAt,
                        nextReviewAt,
                        36,
                        "在支付模块重构 Review 中提前发现空指针风险和缺少回归测试。"
                ),
                new PilotSkill(
                        "unit-test-generator",
                        "单元测试生成 Skill",
                        "按项目测试框架生成覆盖核心分支的单元测试，补齐边界条件和失败路径。",
                        "git clone https://git.example.com/ai-skills/unit-test-generator.git",
                        "https://git.example.com/ai-skills/unit-test-generator",
                        AssetLevel.TEAM,
                        LifecycleStatus.TRIAL,
                        RiskLevel.MEDIUM,
                        "质量团队",
                        List.of("测试与安全", "单元测试"),
                        "新增服务方法、缺陷修复、复杂分支逻辑补测时使用。",
                        "缺少可运行测试框架、依赖外部系统且无法 mock、需求仍在频繁变化时不适用。",
                        "目标类或方法、已有测试目录、测试框架、关键业务分支和异常场景。",
                        "1. 阅读目标代码和已有测试风格。\n2. 识别核心分支、边界值和异常路径。\n3. 生成可运行测试用例和必要 mock。\n4. 标注仍需人工确认的业务断言。",
                        "测试代码补丁、覆盖场景清单、运行命令和未覆盖风险。",
                        "运行对应单元测试；必要时检查覆盖率和关键路径断言。",
                        "生成测试必须可编译，断言应验证业务结果而不是只覆盖代码行。",
                        "JUnit/Mockito 使用规范、项目测试目录约定、关键业务规则说明。",
                        skillMd("unit-test-generator", "按项目测试框架生成覆盖核心分支的单元测试。", "单元测试生成 Skill", "用于把核心分支、异常路径和边界条件转化为可运行测试。"),
                        "试用中，重点收集生成测试的可运行率和误断言情况。",
                        null,
                        nextReviewAt,
                        45,
                        "生成用例后人工调整很少，适合先在订单和支付服务推广。"
                ),
                new PilotSkill(
                        "api-doc-generator",
                        "接口文档生成 Skill",
                        "从 Controller、DTO、注释和错误码约定中生成统一格式的接口说明。",
                        "git clone https://git.example.com/ai-skills/api-doc-generator.git",
                        "https://git.example.com/ai-skills/api-doc-generator",
                        AssetLevel.TEAM,
                        LifecycleStatus.REVIEWING,
                        RiskLevel.LOW,
                        "平台工程",
                        List.of("文档知识", "接口文档"),
                        "接口新增、字段变更、联调前补齐说明文档时使用。",
                        "接口行为尚未确定、DTO 字段含义缺少业务确认、需要正式对外合同文档时不适用。",
                        "Controller 路径、DTO、错误码、鉴权方式、示例请求和响应。",
                        "1. 解析接口路径、方法、参数和响应结构。\n2. 对照错误码和鉴权约定补充说明。\n3. 生成统一 Markdown 文档。\n4. 标注缺失注释和待确认字段。",
                        "Markdown 接口文档：接口概览、请求参数、响应字段、错误码、示例。",
                        "由接口负责人检查字段含义，并使用联调样例确认请求/响应一致。",
                        "文档必须能支持前后端联调，字段说明不得凭空推断业务含义。",
                        "API 风格指南、错误码规范、DTO 命名约定。",
                        skillMd("api-doc-generator", "从 Controller 和 DTO 生成统一格式接口文档。", "接口文档生成 Skill", "把代码结构、错误码和鉴权约定沉淀为联调文档。"),
                        "评审中，待统一字段示例格式。",
                        null,
                        nextReviewAt,
                        30,
                        "联调前补文档速度明显提升，但部分字段业务含义仍需负责人补充。"
                ),
                new PilotSkill(
                        "incident-triage",
                        "线上问题排查 Skill",
                        "根据日志、监控、慢 SQL 和变更记录输出排查路径、风险判断和回滚建议。",
                        "git clone https://git.example.com/ai-skills/incident-triage.git",
                        "https://git.example.com/ai-skills/incident-triage",
                        AssetLevel.COMPANY,
                        LifecycleStatus.APPROVED,
                        RiskLevel.HIGH,
                        "平台工程",
                        List.of("排障运维", "线上问题"),
                        "线上告警、接口超时、错误率升高、发布后异常定位时使用。",
                        "缺少日志或监控证据、涉及客户隐私明文、需要立即执行生产变更时不适用。",
                        "告警信息、日志片段、监控截图摘要、近期发布记录、影响范围和已尝试操作。",
                        "1. 归纳现象和影响面。\n2. 按时间线关联发布、流量和错误变化。\n3. 给出假设、验证命令和优先级。\n4. 输出缓解、回滚和复盘补充项。",
                        "排查报告：现象、影响、假设排序、验证步骤、缓解方案、复盘事项。",
                        "由值班负责人复核；任何生产操作必须走既有变更或应急流程。",
                        "不得输出未经确认的生产操作指令；必须区分证据、推断和建议。",
                        "SRE 值班手册、发布检查清单、慢 SQL 分析规范、信息安全红线。",
                        skillMd("incident-triage", "根据日志、监控和变更记录输出线上问题排查路径。", "线上问题排查 Skill", "帮助值班人员把现象、证据和处置步骤结构化。"),
                        "高风险公司级资产，已强调人工责任和生产变更边界。",
                        reviewedAt,
                        nextReviewAt,
                        60,
                        "一次接口超时排查中快速定位到新增索引缺失，缩短了排查会议时间。"
                ),
                new PilotSkill(
                        "tech-solution-draft",
                        "技术方案初稿 Skill",
                        "根据需求描述生成模块拆解、接口、数据模型、风险清单和待确认问题。",
                        "git clone https://git.example.com/ai-skills/tech-solution-draft.git",
                        "https://git.example.com/ai-skills/tech-solution-draft",
                        AssetLevel.TEAM,
                        LifecycleStatus.CANDIDATE,
                        RiskLevel.LOW,
                        "业务研发",
                        List.of("架构设计", "技术方案"),
                        "需求评审后、技术方案会前、需要快速形成讨论初稿时使用。",
                        "需求目标不清、关键业务规则未确认、需要最终架构决策或成本承诺时不适用。",
                        "需求描述、现有模块边界、相关接口、数据约束、非功能要求和上线时间。",
                        "1. 拆解业务目标和关键流程。\n2. 识别模块、接口和数据模型变化。\n3. 列出风险、依赖和待确认问题。\n4. 生成可供评审的方案初稿。",
                        "技术方案 Markdown：背景、目标、模块设计、接口草案、数据模型、风险和问题。",
                        "由 Tech Lead 评审模块边界、接口兼容性、风险和工作量估算。",
                        "方案必须明确假设和待确认项，不得把 AI 推断当作最终决策。",
                        "技术方案模板、架构评审清单、接口设计规范。",
                        skillMd("tech-solution-draft", "根据需求描述生成技术方案讨论初稿。", "技术方案初稿 Skill", "用于把需求快速整理成可评审的工程方案草案。"),
                        "候选中，待真实方案评审验证。",
                        null,
                        null,
                        25,
                        "能快速形成讨论框架，后续需要补充业务专有约束。"
                )
        );
    }

    private String skillMd(String skillName, String description, String title, String body) {
        return "---\n"
                + "name: " + skillName + "\n"
                + "description: " + description + "\n"
                + "---\n"
                + "# " + title + "\n\n"
                + body;
    }

    private List<DemoArticle> demoArticles() {
        return List.of(
                new DemoArticle("MCP 接入排障手册：从 401 到稳定可用",
                        """
                        ## 适用场景
                        当团队第一次把内部服务接入 MCP Server，最容易卡在鉴权、跨域和 schema 对齐。

                        ## 排查顺序
                        1. 先确认 token 是否真的进入请求头。
                        2. 再看服务端是否按预期解析角色和租户。
                        3. 最后比对 tool schema，避免字段名漂移。

                        ## 建议落地
                        - 给每个工具补最小 smoke test。
                        - 把 401、403、422 区分写进日志。
                        - 在开发环境保留一套可复现请求样例。
                        """),
                new DemoArticle("提示词评审清单：上线前至少看这 8 项",
                        """
                        ## 核心检查项
                        - 目标是否单一
                        - 输入约束是否明确
                        - 失败时是否有回退策略
                        - 是否暴露内部实现细节

                        ## 常见问题
                        最容易被忽略的是输出边界，尤其是“允许自由发挥”这类描述，往往会把结果拉散。
                        """),
                new DemoArticle("前端埋点命名约定：减少一半复盘沟通成本",
                        """
                        推荐统一使用 `页面.模块.动作` 形式命名，例如 `forum.editor.submit`。

                        这样做的好处：
                        - 查询维度稳定
                        - 跨端对齐更容易
                        - 复盘时不需要额外翻译事件语义
                        """),
                new DemoArticle("知识库文章怎么写得更像团队资产",
                        """
                        一篇可复用的知识库文章至少要回答三件事：
                        1. 这个问题为什么值得记下来
                        2. 正确做法是什么
                        3. 什么时候不要照着做

                        如果缺少“不适用场景”，文章通常很快就会过时。
                        """),
                new DemoArticle("用灰度发布验证 AI 功能，不要直接赌全量",
                        """
                        建议按三层指标观察：
                        - 质量：任务完成率、人工回退率
                        - 成本：平均调用次数、token 消耗
                        - 稳定性：超时率、重试率

                        先验证趋势，再决定是否扩量。
                        """),
                new DemoArticle("论坛问答的高质量模板",
                        """
                        ## 推荐结构
                        - 背景
                        - 现象
                        - 已尝试方案
                        - 希望获得的帮助

                        这样能让回答者更快进入问题上下文，而不是先补齐问题描述。
                        """),
                new DemoArticle("AI 工具选型时，先比约束，再比功能",
                        """
                        常见误区是先看 feature list。
                        更有效的顺序通常是：
                        1. 数据安全要求
                        2. 集成方式
                        3. 成本模型
                        4. 真实体验
                        """),
                new DemoArticle("从一次 500 错误复盘里提炼出来的后端守则",
                        """
                        - 参数校验前置
                        - 三方依赖要带超时和兜底
                        - 管理接口错误要可定位
                        - 能返回 4xx 的不要一律打成 500
                        """),
                new DemoArticle("如何给内部技能写一段真正有用的说明",
                        """
                        好说明不是介绍“它很强”，而是明确：
                        - 什么时候用
                        - 给它什么输入
                        - 产出长什么样
                        - 哪些结论必须人工确认
                        """),
                new DemoArticle("周报自动生成前，需要先统一字段口径",
                        """
                        如果“新增用户”“活跃用户”“触达用户”各自定义不一致，再好的自动化都只能放大混乱。

                        建议先把字段释义沉淀到知识库，再做报表汇总。
                        """)
        );
    }

    private List<DemoForumPost> demoForumPosts() {
        return List.of(
                new DemoForumPost("大家的 MCP Server 首次联调都踩过哪些坑？",
                        "我们这周在接内部工具时，遇到了 token 透传、schema 版本不一致和超时重试三个问题。想收集团队里更高频的坑位，顺便整理成排障清单。",
                        ForumPost.PostType.DISCUSSION, true, false),
                new DemoForumPost("知识库文章发布前，是否需要统一模板？",
                        "现在每个人写法差异很大，有的是结论导向，有的是流水账。大家更倾向于强模板，还是保留自由度？希望听听一线使用体验。",
                        ForumPost.PostType.QUESTION, false, false),
                new DemoForumPost("分享：把 AI 排障记录拆成症状/根因/验证 后，复盘效率明显高了",
                        "最近两次线上问题复盘都用了这个结构。最大的变化是大家不再围着现象打转，而是更容易把结论沉淀成后续可复用资产。",
                        ForumPost.PostType.SHARE, false, true),
                new DemoForumPost("论坛发帖 403 的根因最终是 token 没补上",
                        "定位结果是前端路由虽然判定了已登录，但在部分 requiresAuth 页面没有重新设置 axios 默认 Authorization，导致发帖接口被后端按未认证请求处理。",
                        ForumPost.PostType.SHARE, false, false),
                new DemoForumPost("想把 AI 工具页做成分层推荐，应该按什么维度排？",
                        "我目前想到的是按上手成本、接入复杂度、适用团队规模三个维度。有没有更适合内部场景的排序方式？",
                        ForumPost.PostType.QUESTION, false, false),
                new DemoForumPost("讨论：技能市场里是否要突出‘已验证’标签",
                        "现在列表里所有技能视觉权重差不多，但实际可直接落地的比例并不一样。大家觉得是否需要更强的状态区分？",
                        ForumPost.PostType.DISCUSSION, false, false),
                new DemoForumPost("分享一个写提示词评审意见的做法",
                        "我会强制自己把评审意见拆成三类：需求缺失、约束不足、验证不充分。这样作者拿到反馈后更容易按类修复。",
                        ForumPost.PostType.SHARE, false, true),
                new DemoForumPost("论坛分类还需要继续细分吗？",
                        "目前分类数量不多，优点是简单；缺点是热门帖子容易混在一起。想听听大家对‘少分类 + 强标签’这套方案的看法。",
                        ForumPost.PostType.DISCUSSION, false, false),
                new DemoForumPost("有没有人愿意共享一套 AI 功能灰度观察指标？",
                        "我们准备给新的智能推荐能力做灰度，但还在讨论到底看点击、完成率还是人工回退。欢迎把你们用过的指标贴出来。",
                        ForumPost.PostType.QUESTION, false, false),
                new DemoForumPost("从这次论坛改版里学到的一件事：展示项越多，发帖门槛越高",
                        "去掉关联内容之后，编辑页明显更聚焦。后续如果再加功能，我会先问一句：它是在帮助表达，还是只是让表单更重？",
                        ForumPost.PostType.SHARE, false, true)
        );
    }

    private record PilotSkill(
            String skillDirectory,
            String name,
            String description,
            String cloneCommand,
            String sourceRepositoryUrl,
            AssetLevel assetLevel,
            LifecycleStatus lifecycleStatus,
            RiskLevel riskLevel,
            String teamName,
            List<String> tags,
            String applicableScenarios,
            String nonApplicableScenarios,
            String inputRequirements,
            String executionSteps,
            String outputFormat,
            String validationMethod,
            String qualityStandard,
            String referenceMaterials,
            String contentMd,
            String reviewNotes,
            LocalDate lastReviewedAt,
            LocalDate nextReviewAt,
            int sampleSavedMinutes,
            String feedback
    ) {}

    private record DemoArticle(String title, String content) {}

    private record DemoForumPost(String title, String content, ForumPost.PostType postType, boolean pinned, boolean featured) {}
}
