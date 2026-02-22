package com.example.platform.service;

import com.example.platform.dto.*;
import com.example.platform.entity.*;
import com.example.platform.repository.*;
import com.example.platform.service.AiToolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final SkillRepository skillRepository;
    private final RuleRepository ruleRepository;
    private final ExternalSkillRepository externalSkillRepository;
    private final LearningArticleRepository articleRepository;
    private final NewsRepository newsRepository;
    private final AiToolService aiToolService;
    private final LlmLeaderboardService llmLeaderboardService;

    public HomeService(SkillRepository skillRepository, RuleRepository ruleRepository,
                       ExternalSkillRepository externalSkillRepository,
                       LearningArticleRepository articleRepository, NewsRepository newsRepository,
                       AiToolService aiToolService, LlmLeaderboardService llmLeaderboardService) {
        this.skillRepository = skillRepository;
        this.ruleRepository = ruleRepository;
        this.externalSkillRepository = externalSkillRepository;
        this.articleRepository = articleRepository;
        this.newsRepository = newsRepository;
        this.aiToolService = aiToolService;
        this.llmLeaderboardService = llmLeaderboardService;
    }

    @Transactional(readOnly = true)
    public HomeDto getHome() {
        HomeDto dto = new HomeDto();
        List<Skill> skills = skillRepository.findTop10ByVisibilityOrderByUpdatedAtDesc(Skill.Visibility.VISIBLE);
        dto.setLatestSkills(skills.stream().map(SkillDto::fromEntity).collect(Collectors.toList()));
        List<Rule> rules = ruleRepository.findTop10ByVisibilityOrderByUpdatedAtDesc(Rule.Visibility.VISIBLE);
        dto.setLatestRules(rules.stream().map(RuleDto::fromEntity).collect(Collectors.toList()));
        List<ExternalSkill> ext = externalSkillRepository.findTop10ByVisibilityOrderByUpdatedAtDesc(ExternalSkill.Visibility.VISIBLE);
        dto.setLatestExternalSkills(ext.stream().map(ExternalSkillDto::fromEntity).collect(Collectors.toList()));
        List<LearningArticle> articles = articleRepository.findTop5ByStatusOrderByUpdatedAtDesc(LearningArticle.Status.PUBLISHED);
        dto.setLatestArticles(articles.stream().map(ArticleDto::fromEntity).collect(Collectors.toList()));
        dto.setLatestNews(newsRepository.findTop10ByOrderByPublishDateDescCreatedAtDesc());
        dto.setLatestAiTools(aiToolService.getLatest(10));
        var leaderboardPage = llmLeaderboardService.list(null, 1, 10, "coding", "desc");
        dto.setLatestLlmLeaderboard(leaderboardPage.getContent());
        return dto;
    }
}
