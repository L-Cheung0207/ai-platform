package com.example.platform.service;

import com.example.platform.entity.Skill;
import com.example.platform.entity.Skill.CreationSource;
import com.example.platform.entity.Tag;
import com.example.platform.entity.User;
import com.example.platform.dto.SkillGitLabPublishResultDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.platform.repository.SkillFeedbackRepository;
import com.example.platform.repository.SkillOperationReportRepository;
import com.example.platform.repository.SkillRepository;
import com.example.platform.repository.SkillReviewRepository;
import com.example.platform.repository.SkillUsageEventRepository;
import com.example.platform.repository.TagRepository;
import com.example.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillPackageImportServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillReviewRepository skillReviewRepository;

    @Mock
    private SkillFeedbackRepository skillFeedbackRepository;

    @Mock
    private SkillUsageEventRepository skillUsageEventRepository;

    @Mock
    private SkillOperationReportRepository skillOperationReportRepository;

    private StubGitLabSkillPublishService gitLabSkillPublishService;

    private SkillPackageImportService importService;

    @BeforeEach
    void setUp() {
        SkillService skillService = new SkillService(skillRepository, tagRepository, userRepository, gitLabSkillPublishService);
        SkillGovernanceService skillGovernanceService = new SkillGovernanceService(
                skillRepository,
                skillReviewRepository,
                skillFeedbackRepository,
                skillUsageEventRepository,
                skillOperationReportRepository,
                userRepository
        );
        gitLabSkillPublishService = new StubGitLabSkillPublishService();
        importService = new SkillPackageImportService(skillService, skillGovernanceService, skillRepository, gitLabSkillPublishService);
    }

    @Test
    void importPackageRejectsZipWithoutQuickValidateEvidence() throws Exception {
        MockMultipartFile file = zipFile("sample-skill.zip", packageEntries(false));

        var result = importService.importPackage(file, 1L);

        assertThat(result.skill()).isNull();
        assertThat(result.assetValidation()).isNull();
        assertThat(result.packageValidation().passed()).isFalse();
        assertThat(result.packageValidation().items())
                .anySatisfy(item -> {
                    assertThat(item.key()).isEqualTo("quickValidateEvidence");
                    assertThat(item.required()).isTrue();
                    assertThat(item.passed()).isFalse();
                });
    }

    @Test
    void importPackageRejectsZipWithoutRepositorySource() throws Exception {
        MockMultipartFile file = zipFile("sample-skill.zip", packageEntries(true, false));

        var result = importService.importPackage(file, 1L);

        assertThat(result.skill()).isNull();
        assertThat(result.assetValidation()).isNull();
        assertThat(result.packageValidation().passed()).isFalse();
        assertThat(result.packageValidation().items())
                .anySatisfy(item -> {
                    assertThat(item.key()).isEqualTo("sourceRepositoryUrl");
                    assertThat(item.required()).isTrue();
                    assertThat(item.passed()).isFalse();
                });
    }

    @Test
    void importPackageMapsRepositoryAndQuickValidateFallback() throws Exception {
        MockMultipartFile file = zipFile("sample-skill.zip", packageEntries(true));
        AtomicReference<Skill> savedSkill = new AtomicReference<>();
        when(skillRepository.findFirstBySkillDirectoryIgnoreCase("sample-skill")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin")));
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill skill = invocation.getArgument(0);
            if (skill.getId() == null) {
                skill.setId(42L);
            }
            savedSkill.set(skill);
            return skill;
        });
        when(skillRepository.findById(42L)).thenAnswer(invocation -> Optional.of(savedSkill.get()));

        var result = importService.importPackage(file, 1L);

        assertThat(result.skill().getId()).isEqualTo(42L);
        assertThat(result.packageValidation().passed()).isTrue();
        assertThat(result.assetValidation().passed()).isTrue();
        assertThat(result.gitLabPublication().status()).isEqualTo("DISABLED");
        assertThat(savedSkill.get().getCreationSource()).isEqualTo(CreationSource.SKILL_CREATOR_PACKAGE);
        assertThat(savedSkill.get().getSourceRepositoryUrl()).isEqualTo("https://git.example.com/ai-skills/sample-skill");
        assertThat(savedSkill.get().getCloneCommand()).isEqualTo("git clone https://git.example.com/ai-skills/sample-skill.git");
        assertThat(savedSkill.get().getValidationMethod()).contains("quick_validate.py 校验通过");
    }

    @Test
    void importPackagePublishesToGitLabWhenEnabledAndRepositorySourceMissing() throws Exception {
        MockMultipartFile file = zipFile("sample-skill.zip", packageEntries(true, false));
        AtomicReference<Skill> savedSkill = new AtomicReference<>();
        gitLabSkillPublishService.enabled = true;
        gitLabSkillPublishService.result = SkillGitLabPublishResultDto.published(
                        "https://git.example.com/platform/ai-skills",
                        "skill/sample-skill-123",
                        "main",
                        "https://git.example.com/platform/ai-skills/-/merge_requests/7",
                        "skills/sample-skill",
                        "abc123"
                );
        when(skillRepository.findFirstBySkillDirectoryIgnoreCase("sample-skill")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin")));
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill skill = invocation.getArgument(0);
            if (skill.getId() == null) {
                skill.setId(43L);
            }
            savedSkill.set(skill);
            return skill;
        });
        when(skillRepository.findById(43L)).thenAnswer(invocation -> Optional.of(savedSkill.get()));

        var result = importService.importPackage(file, 1L);

        assertThat(result.skill().getId()).isEqualTo(43L);
        assertThat(result.packageValidation().passed()).isTrue();
        assertThat(result.gitLabPublication().status()).isEqualTo("PUBLISHED");
        assertThat(savedSkill.get().getSourceRepositoryUrl()).isEqualTo("https://git.example.com/platform/ai-skills");
        assertThat(savedSkill.get().getCloneCommand()).isEqualTo("git clone https://git.example.com/platform/ai-skills.git");
        assertThat(savedSkill.get().getReviewNotes()).contains("GitLab MR：https://git.example.com/platform/ai-skills/-/merge_requests/7");
        assertThat(savedSkill.get().getReviewNotes()).contains("GitLab 路径：skills/sample-skill");
    }

    private MockMultipartFile zipFile(String filename, Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("file", filename, "application/zip", out.toByteArray());
    }

    private Map<String, String> packageEntries(boolean includeQuickValidateEvidence) {
        return packageEntries(includeQuickValidateEvidence, true);
    }

    private Map<String, String> packageEntries(boolean includeQuickValidateEvidence, boolean includeSourceRepository) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("sample-skill/SKILL.md", """
                ---
                name: sample-skill
                description: Sample skill package
                %s
                skillCategory: code_review
                buildPriority: P0
                ---

                # Sample Skill

                ## 适用场景
                Team code review.

                ## 不适用场景
                Production secrets.

                ## 输入要求
                Pull request and changed files.

                ## 执行步骤
                1. Inspect changed files.

                ## 输出格式
                Markdown review report.

                ## 质量标准
                Findings must be actionable.

                ## 参考资料
                references/checklist.md
                """.formatted(includeSourceRepository
                ? "sourceRepositoryUrl: https://git.example.com/ai-skills/sample-skill"
                : ""));
        entries.put("sample-skill/agents/openai.yaml", "name: sample-skill\n");
        entries.put("sample-skill/references/checklist.md", "# Checklist\n");
        if (includeQuickValidateEvidence) {
            entries.put("sample-skill/reports/quick_validate.txt", "quick_validate.py PASSED\n");
        }
        return entries;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hash");
        return user;
    }

    private static class StubGitLabSkillPublishService extends GitLabSkillPublishService {
        private boolean enabled;
        private SkillGitLabPublishResultDto result = SkillGitLabPublishResultDto.disabled("GitLab 自动发布未启用");

        private StubGitLabSkillPublishService() {
            super(new ObjectMapper(), false, "", "", "", "", "main", "skills", "skill");
        }

        @Override
        public boolean isPublicationEnabled() {
            return enabled;
        }

        @Override
        public SkillGitLabPublishResultDto publishPackage(String skillDirectory, String skillName, Map<String, byte[]> files) {
            return result;
        }
    }
}
