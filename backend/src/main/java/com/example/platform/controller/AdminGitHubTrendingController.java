package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.GitHubTrendingConfigDto;
import com.example.platform.dto.GitHubTrendingConfigRequest;
import com.example.platform.dto.GitHubTrendingDto;
import com.example.platform.dto.GitHubTrendingStatusDto;
import com.example.platform.dto.GitHubTrendingUpdateRequest;
import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingEntry;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingEntryRepository;
import com.example.platform.service.GitHubTrendingService;
import com.example.platform.service.GitHubTrendingSummaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/github-trending")
public class AdminGitHubTrendingController {

    private final GitHubTrendingService gitHubTrendingService;
    private final GitHubTrendingConfigRepository configRepository;
    private final GitHubTrendingEntryRepository entryRepository;
    private final GitHubTrendingSummaryService summaryService;

    public AdminGitHubTrendingController(GitHubTrendingService gitHubTrendingService,
                                         GitHubTrendingConfigRepository configRepository,
                                         GitHubTrendingEntryRepository entryRepository,
                                         GitHubTrendingSummaryService summaryService) {
        this.gitHubTrendingService = gitHubTrendingService;
        this.configRepository = configRepository;
        this.entryRepository = entryRepository;
        this.summaryService = summaryService;
    }

    @GetMapping("/status")
    public ApiResponse<GitHubTrendingStatusDto> status() {
        GitHubTrendingConfig config = configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)
                .orElseGet(GitHubTrendingConfig::defaultConfig);
        return ApiResponse.ok(GitHubTrendingStatusDto.fromConfig(config));
    }

    @GetMapping
    public ApiResponse<List<GitHubTrendingDto>> list(@RequestParam(defaultValue = "WEEKLY") GitHubTrendingEntry.Period period) {
        return ApiResponse.ok(gitHubTrendingService.listHome(period));
    }

    @PutMapping("/{id}")
    public ApiResponse<GitHubTrendingDto> updateEntry(@PathVariable Long id,
                                                      @Valid @RequestBody GitHubTrendingUpdateRequest request) {
        return ApiResponse.ok(gitHubTrendingService.updateEntry(id, request));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<GitHubTrendingStatusDto>> sync() {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.ok(gitHubTrendingService.startSyncAsync()));
        } catch (GitHubTrendingService.SyncAlreadyRunningException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail(409, ex.getMessage()));
        }
    }

    @PostMapping("/{id}/regenerate-summary")
    @Transactional
    public ApiResponse<GitHubTrendingDto> regenerateSummary(@PathVariable Long id) {
        GitHubTrendingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GitHub trending entry not found: " + id));
        GitHubTrendingSummaryService.SummaryResult result = summaryService.generate(entry);
        entry.setEffectCn(result.effectCn());
        entry.setScenarioCn(result.scenarioCn());
        entry.setSummaryStatus(switch (result.status()) {
            case GENERATED -> GitHubTrendingEntry.SummaryStatus.GENERATED;
            case FAILED -> GitHubTrendingEntry.SummaryStatus.FAILED;
            case NEEDS_REVIEW -> GitHubTrendingEntry.SummaryStatus.NEEDS_REVIEW;
        });
        return ApiResponse.ok(GitHubTrendingDto.fromEntity(entryRepository.save(entry)));
    }

    @GetMapping("/config")
    public ApiResponse<GitHubTrendingConfigDto> getConfig() {
        return ApiResponse.ok(gitHubTrendingService.getConfig());
    }

    @PutMapping("/config")
    public ApiResponse<GitHubTrendingConfigDto> updateConfig(@Valid @RequestBody GitHubTrendingConfigRequest request) {
        return ApiResponse.ok(gitHubTrendingService.updateConfig(request));
    }
}
