package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.NewsWriteRequest;
import com.example.platform.entity.News;
import com.example.platform.repository.NewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Transactional(readOnly = true)
    public Page<News> listForAdmin(String keyword, LocalDate publishDate, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "publishDate", "createdAt"));
        return newsRepository.findForAdmin(keyword != null && keyword.isBlank() ? null : keyword, publishDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<News> list(int page, int size, LocalDate publishDate) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (publishDate != null) {
            return newsRepository.findAllByPublishDateOrderByCreatedAtDesc(publishDate, pageable);
        }
        // 默认展示最近 10 天
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(9);
        pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "publishDate", "createdAt"));
        return newsRepository.findByPublishDateBetweenOrderByPublishDateDescCreatedAtDesc(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public News getById(Long id) {
        return newsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("资讯不存在"));
    }

    @Transactional
    public News create(NewsWriteRequest req) {
        News n = new News();
        n.setTitle(req.getTitle());
        n.setSummary(req.getSummary());
        n.setSourceUrl(req.getSourceUrl());
        n.setSourceName(req.getSourceName());
        n.setPublishDate(req.getPublishDate());
        return newsRepository.save(n);
    }

    @Transactional
    public News update(Long id, NewsWriteRequest req) {
        News n = getById(id);
        n.setTitle(req.getTitle());
        n.setSummary(req.getSummary());
        n.setSourceUrl(req.getSourceUrl());
        n.setSourceName(req.getSourceName());
        n.setPublishDate(req.getPublishDate());
        return newsRepository.save(n);
    }

    @Transactional
    public void delete(Long id) {
        News n = getById(id);
        newsRepository.delete(n);
    }
}
