package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.ArticleWriteRequest;
import com.example.platform.entity.LearningArticle;
import com.example.platform.entity.LearningArticle.ContentType;
import com.example.platform.entity.LearningArticle.Status;
import com.example.platform.entity.User;
import com.example.platform.repository.LearningArticleRepository;
import com.example.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleService {

    private final LearningArticleRepository articleRepository;
    private final UserRepository userRepository;

    public ArticleService(LearningArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<LearningArticle> listPublished(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return articleRepository.findByStatusOrderByUpdatedAtDesc(Status.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LearningArticle> listAllForAdmin(Integer page, Integer size, String statusFilter) {
        Pageable pageable = PageRequest.of(Math.max(0, (page != null ? page : 1) - 1), Math.min(50, Math.max(1, size != null ? size : 20)), Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                Status s = Status.valueOf(statusFilter.toUpperCase());
                return articleRepository.findByStatusOrderByUpdatedAtDesc(s, pageable);
            } catch (IllegalArgumentException e) {
                return articleRepository.findAllByOrderByUpdatedAtDesc(pageable);
            }
        }
        return articleRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public LearningArticle getPublishedById(Long id) {
        LearningArticle a = articleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("该知识不存在"));
        if (a.getStatus() != Status.PUBLISHED) {
            throw new ResourceNotFoundException("该知识不存在或未发布");
        }
        return a;
    }

    @Transactional(readOnly = true)
    public LearningArticle getById(Long id) {
        return articleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("该知识不存在"));
    }

    @Transactional
    public LearningArticle create(ArticleWriteRequest req, Long authorUserId) {
        User author = userRepository.findById(authorUserId).orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        LearningArticle a = new LearningArticle();
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        a.setContentType(parseContentType(req.getContentType()));
        a.setAuthor(author);
        a.setStatus(parseStatus(req.getStatus()));
        return articleRepository.save(a);
    }

    @Transactional
    public LearningArticle update(Long id, ArticleWriteRequest req) {
        LearningArticle a = getById(id);
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        a.setContentType(parseContentType(req.getContentType()));
        a.setStatus(parseStatus(req.getStatus()));
        return articleRepository.save(a);
    }

    @Transactional
    public void delete(Long id) {
        LearningArticle a = getById(id);
        articleRepository.delete(a);
    }

    private static Status parseStatus(String s) {
        if (s == null || s.isBlank()) return Status.DRAFT;
        try {
            return Status.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.DRAFT;
        }
    }

    private static ContentType parseContentType(String s) {
        if (s == null || s.isBlank()) return ContentType.RICH_TEXT;
        try {
            return ContentType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContentType.RICH_TEXT;
        }
    }
}
