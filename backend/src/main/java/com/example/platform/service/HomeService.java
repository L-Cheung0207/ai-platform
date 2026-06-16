package com.example.platform.service;

import com.example.platform.dto.*;
import com.example.platform.entity.*;
import com.example.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final SkillRepository skillRepository;
    private final LearningArticleRepository articleRepository;
    private final NewsRepository newsRepository;
    private final LlmLeaderboardService llmLeaderboardService;
    private final GitHubTrendingService gitHubTrendingService;

    public HomeService(SkillRepository skillRepository,
                       LearningArticleRepository articleRepository, NewsRepository newsRepository,
                       LlmLeaderboardService llmLeaderboardService,
                       GitHubTrendingService gitHubTrendingService) {
        this.skillRepository = skillRepository;
        this.articleRepository = articleRepository;
        this.newsRepository = newsRepository;
        this.llmLeaderboardService = llmLeaderboardService;
        this.gitHubTrendingService = gitHubTrendingService;
    }

    @Transactional(readOnly = true)
    public HomeDto getHome() {
        HomeDto dto = new HomeDto();
        List<Skill> skills = skillRepository.findTop10ByVisibilityOrderByUpdatedAtDesc(
                Skill.Visibility.VISIBLE
        );
        dto.setLatestSkills(skills.stream().map(SkillDto::fromEntity).collect(Collectors.toList()));
        List<LearningArticle> articles = articleRepository.findTop5ByStatusOrderByUpdatedAtDesc(LearningArticle.Status.PUBLISHED);
        dto.setLatestArticles(articles.stream().map(ArticleDto::fromEntity).collect(Collectors.toList()));
        dto.setLatestNews(newsRepository.findTop10ByOrderByPublishDateDescCreatedAtDesc());
        var leaderboardPage = llmLeaderboardService.list(null, 1, 10, "coding", "desc");
        dto.setLatestLlmLeaderboard(leaderboardPage.getContent());
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
        return dto;
    }
}
