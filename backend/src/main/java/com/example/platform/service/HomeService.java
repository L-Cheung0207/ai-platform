package com.example.platform.service;

import com.example.platform.dto.*;
import com.example.platform.entity.*;
import com.example.platform.entity.HomeNavModule.Code;
import com.example.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final SkillRepository skillRepository;
    private final LearningArticleRepository articleRepository;
    private final NewsRepository newsRepository;
    private final LlmLeaderboardService llmLeaderboardService;
    private final GitHubTrendingService gitHubTrendingService;
    private final HomeNavModuleService homeNavModuleService;

    public HomeService(SkillRepository skillRepository,
                       LearningArticleRepository articleRepository, NewsRepository newsRepository,
                       LlmLeaderboardService llmLeaderboardService,
                       GitHubTrendingService gitHubTrendingService,
                       HomeNavModuleService homeNavModuleService) {
        this.skillRepository = skillRepository;
        this.articleRepository = articleRepository;
        this.newsRepository = newsRepository;
        this.llmLeaderboardService = llmLeaderboardService;
        this.gitHubTrendingService = gitHubTrendingService;
        this.homeNavModuleService = homeNavModuleService;
    }

    @Transactional(readOnly = true)
    public HomeDto getHome() {
        HomeDto dto = new HomeDto();
        dto.setNavModules(homeNavModuleService.listPublic());
        Set<Code> visible = homeNavModuleService.visibleCodes();
        if (visible.isEmpty()) {
            visible = EnumSet.allOf(Code.class);
        }

        if (visible.contains(Code.SKILLS)) {
            List<Skill> skills = skillRepository.findTop10ByVisibilityOrderByUpdatedAtDesc(
                    Skill.Visibility.VISIBLE
            );
            dto.setLatestSkills(skills.stream().map(SkillDto::fromEntity).collect(Collectors.toList()));
        } else {
            dto.setLatestSkills(List.of());
        }

        if (visible.contains(Code.ARTICLES)) {
            List<LearningArticle> articles = articleRepository.findTop5ByStatusOrderByUpdatedAtDesc(LearningArticle.Status.PUBLISHED);
            dto.setLatestArticles(articles.stream().map(ArticleDto::fromEntity).collect(Collectors.toList()));
        } else {
            dto.setLatestArticles(List.of());
        }

        if (visible.contains(Code.NEWS)) {
            dto.setLatestNews(newsRepository.findTop10ByOrderByPublishDateDescCreatedAtDesc());
        } else {
            dto.setLatestNews(List.of());
        }

        if (visible.contains(Code.LLM_LEADERBOARD)) {
            var leaderboardPage = llmLeaderboardService.list(null, 1, 10, "coding", "desc");
            dto.setLatestLlmLeaderboard(leaderboardPage.getContent());
        } else {
            dto.setLatestLlmLeaderboard(List.of());
        }

        if (visible.contains(Code.GITHUB_TRENDING)) {
            List<GitHubTrendingDto> weekly = gitHubTrendingService.listHome(GitHubTrendingEntry.Period.WEEKLY);
            List<GitHubTrendingDto> monthly = gitHubTrendingService.listHome(GitHubTrendingEntry.Period.MONTHLY);
            dto.setGithubTrendingWeekly(weekly);
            dto.setGithubTrendingMonthly(monthly);
            dto.setGithubTrendingUpdatedAt(List.of(weekly, monthly).stream()
                    .flatMap(List::stream)
                    .map(GitHubTrendingDto::getLastSeenBatch)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null));
        } else {
            dto.setGithubTrendingWeekly(List.of());
            dto.setGithubTrendingMonthly(List.of());
            dto.setGithubTrendingUpdatedAt(null);
        }
        return dto;
    }
}
