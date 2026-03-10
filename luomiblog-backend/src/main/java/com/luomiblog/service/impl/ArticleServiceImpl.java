package com.luomiblog.service.impl;

import com.luomiblog.dto.ArticleRequest;
import com.luomiblog.dto.ArticleResponse;
import com.luomiblog.entity.*;
import com.luomiblog.repository.*;
import com.luomiblog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleTagRepository articleTagRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ArticleResponse createArticle(ArticleRequest request, Long authorId) {
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle());

        if (articleRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Article article = Article.builder()
                .title(request.getTitle())
                .slug(slug)
                .summary(request.getSummary())
                .content(request.getContent())
                .categoryId(request.getCategoryId())
                .authorId(authorId)
                .language(request.getLanguage() != null ? request.getLanguage() : "zh")
                .status(request.getStatus() != null ? request.getStatus() : "draft")
                .version("1.0.0")
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .isTop(false)
                .sortOrder(0)
                .allowComments(request.getAllowComments() != null ? request.getAllowComments() : true)
                .allowSuggestions(request.getAllowSuggestions() != null ? request.getAllowSuggestions() : false)
                .syncBailian(false)
                .build();

        if ("published".equals(article.getStatus())) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleRepository.save(article);

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            saveArticleTags(article.getId(), request.getTagIds());
        }

        return convertToResponse(article);
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleRequest request, Long authorId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权修改此文章");
        }

        article.setTitle(request.getTitle());
        if (request.getSlug() != null && !request.getSlug().equals(article.getSlug())) {
            if (articleRepository.existsBySlug(request.getSlug())) {
                throw new RuntimeException("slug已存在");
            }
            article.setSlug(request.getSlug());
        }
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        article.setLanguage(request.getLanguage());
        article.setAllowComments(request.getAllowComments());
        article.setAllowSuggestions(request.getAllowSuggestions());

        if (request.getStatus() != null && !request.getStatus().equals(article.getStatus())) {
            article.setStatus(request.getStatus());
            if ("published".equals(request.getStatus()) && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
        }

        articleRepository.save(article);

        if (request.getTagIds() != null) {
            articleTagRepository.deleteByArticleId(id);
            saveArticleTags(id, request.getTagIds());
        }

        return convertToResponse(article);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id, Long authorId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权删除此文章");
        }

        article.setDeletedAt(LocalDateTime.now());
        articleRepository.save(article);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        return convertToResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        return convertToResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getPublishedArticles(Pageable pageable) {
        return articleRepository.findPublishedArticles(pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getArticlesByCategory(Long categoryId, Pageable pageable) {
        return articleRepository.findByCategoryId(categoryId, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getArticlesByAuthor(Long authorId, Pageable pageable) {
        return articleRepository.findByAuthorId(authorId, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleResponse> searchArticles(String keyword) {
        return articleRepository.searchByKeyword(keyword)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementViewCount(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setLikeCount(article.getLikeCount() + 1);
        articleRepository.save(article);
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            ArticleTag articleTag = ArticleTag.builder()
                    .articleId(articleId)
                    .tagId(tagId)
                    .build();
            articleTagRepository.save(articleTag);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private ArticleResponse convertToResponse(Article article) {
        ArticleResponse.ArticleResponseBuilder builder = ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .aiSummary(article.getAiSummary())
                .knowledgePoints(article.getKnowledgePoints())
                .content(article.getContent())
                .language(article.getLanguage())
                .status(article.getStatus())
                .version(article.getVersion())
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount())
                .wordCount(article.getWordCount())
                .readingTime(article.getReadingTime())
                .isTop(article.getIsTop())
                .allowComments(article.getAllowComments())
                .allowSuggestions(article.getAllowSuggestions())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt());

        if (article.getCategoryId() != null) {
            categoryRepository.findById(article.getCategoryId())
                    .ifPresent(category -> builder.category(
                            ArticleResponse.CategoryInfo.builder()
                                    .id(category.getId())
                                    .name(category.getName())
                                    .slug(category.getSlug())
                                    .build()
                    ));
        }

        List<Tag> tags = tagRepository.findByArticleId(article.getId());
        builder.tags(tags.stream()
                .map(tag -> ArticleResponse.TagInfo.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .slug(tag.getSlug())
                        .build())
                .collect(Collectors.toList()));

        userRepository.findById(article.getAuthorId())
                .ifPresent(author -> builder.author(
                        ArticleResponse.AuthorInfo.builder()
                                .id(author.getId())
                                .username(author.getUsername())
                                .nickname(author.getNickname())
                                .avatarUrl(author.getAvatarUrl())
                                .build()
                ));

        return builder.build();
    }
}
