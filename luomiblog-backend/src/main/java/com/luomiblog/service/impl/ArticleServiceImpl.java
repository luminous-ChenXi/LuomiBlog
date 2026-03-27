package com.luomiblog.service.impl;

import com.luomiblog.dto.ArticleRequest;
import com.luomiblog.dto.ArticleResponse;
import com.luomiblog.dto.ArticleStatsResponse;
import com.luomiblog.entity.*;
import com.luomiblog.repository.*;
import com.luomiblog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleTagRepository articleTagRepository;
    private final UserRepository userRepository;

    @Value("${article.content.path:../luomiblog-frontend/src/content/blog}")
    private String contentPath;

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
                .top(false)
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

        // 如果文章状态为已发布，创建 MD 文件
        if ("published".equals(article.getStatus())) {
            createMarkdownFile(article, request.getTagIds());
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

        String oldStatus = article.getStatus();
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

        // 根据状态变更处理 MD 文件
        String newStatus = article.getStatus();
        if (!oldStatus.equals(newStatus)) {
            if ("published".equals(newStatus)) {
                // 从草稿/归档变为发布，创建 MD 文件
                createMarkdownFile(article, request.getTagIds());
            } else if ("published".equals(oldStatus)) {
                // 从发布变为草稿/归档，删除 MD 文件
                deleteMarkdownFile(article.getSlug());
            }
        } else if ("published".equals(newStatus)) {
            // 状态未变但仍是发布状态，更新 MD 文件
            createMarkdownFile(article, request.getTagIds());
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

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getAdminArticles(Pageable pageable, String search, String category, String status) {
        return articleRepository.findAdminArticles(search, category, status, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional
    public ArticleResponse publishArticle(Long id, Long authorId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权发布此文章");
        }

        article.setStatus("published");
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleRepository.save(article);

        return convertToResponse(article);
    }

    @Override
    @Transactional
    public ArticleResponse archiveArticle(Long id, Long authorId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权归档此文章");
        }

        article.setStatus("archived");
        articleRepository.save(article);

        return convertToResponse(article);
    }

    @Override
    @Transactional
    public ArticleResponse toggleTop(Long id, boolean top, Long authorId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthorId().equals(authorId)) {
            throw new RuntimeException("无权修改此文章");
        }

        article.setTop(top);
        articleRepository.save(article);

        return convertToResponse(article);
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
                .isTop(article.getTop())
                .allowComments(article.getAllowComments())
                .allowSuggestions(article.getAllowSuggestions())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt());

        // 设置分类信息和分类名称
        if (article.getCategoryId() != null) {
            categoryRepository.findById(article.getCategoryId())
                    .ifPresent(category -> {
                        builder.category(
                                ArticleResponse.CategoryInfo.builder()
                                        .id(category.getId())
                                        .name(category.getName())
                                        .slug(category.getSlug())
                                        .build()
                        );
                        builder.categoryName(category.getName());
                    });
        }

        // 设置标签信息
        List<Tag> tags = tagRepository.findByArticleId(article.getId());
        builder.tags(tags.stream()
                .map(tag -> ArticleResponse.TagInfo.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .slug(tag.getSlug())
                        .build())
                .collect(Collectors.toList()));

        // 设置作者信息和作者名称
        userRepository.findById(article.getAuthorId())
                .ifPresent(author -> {
                    builder.author(
                            ArticleResponse.AuthorInfo.builder()
                                    .id(author.getId())
                                    .username(author.getUsername())
                                    .nickname(author.getNickname())
                                    .avatarUrl(author.getAvatarUrl())
                                    .build()
                    );
                    // 使用昵称或用户名作为作者名称
                    builder.authorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
                });

        return builder.build();
    }

    @Override
    public ArticleStatsResponse getArticleStats() {
        return ArticleStatsResponse.builder()
                .total(articleRepository.countTotalArticles())
                .published(articleRepository.countPublishedArticles())
                .draft(articleRepository.countDraftArticles())
                .archived(articleRepository.countArchivedArticles())
                .conflicts(0L) // 冲突数由同步服务单独统计
                .build();
    }

    /**
     * 创建 Markdown 文件
     */
    private void createMarkdownFile(Article article, List<Long> tagIds) {
        try {
            Path blogDir = Paths.get(contentPath);
            if (!Files.exists(blogDir)) {
                Files.createDirectories(blogDir);
            }

            // 获取分类名称
            String categoryName = categoryRepository.findById(article.getCategoryId())
                    .map(Category::getName)
                    .orElse("未分类");

            // 获取标签列表
            List<String> tagNames = tagRepository.findByArticleId(article.getId())
                    .stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());

            // 获取作者名称
            String authorName = userRepository.findById(article.getAuthorId())
                    .map(user -> user.getNickname() != null ? user.getNickname() : user.getUsername())
                    .orElse("管理员");

            // 构建 frontmatter
            StringBuilder frontmatter = new StringBuilder();
            frontmatter.append("---\n");
            frontmatter.append("title: \"").append(escapeYaml(article.getTitle())).append("\"\n");
            frontmatter.append("description: \"").append(escapeYaml(article.getSummary())).append("\"\n");
            frontmatter.append("pubDate: ").append(article.getPublishedAt() != null
                    ? article.getPublishedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            frontmatter.append("author: \"").append(escapeYaml(authorName)).append("\"\n");
            frontmatter.append("category: \"").append(escapeYaml(categoryName)).append("\"\n");
            if (!tagNames.isEmpty()) {
                frontmatter.append("tags: [");
                for (int i = 0; i < tagNames.size(); i++) {
                    if (i > 0) frontmatter.append(", ");
                    frontmatter.append("\"").append(escapeYaml(tagNames.get(i))).append("\"");
                }
                frontmatter.append("]\n");
            }
            frontmatter.append("views: ").append(article.getViewCount()).append("\n");
            frontmatter.append("likes: ").append(article.getLikeCount()).append("\n");
            frontmatter.append("comments: ").append(article.getCommentCount()).append("\n");
            frontmatter.append("---\n\n");

            // 写入文件
            Path filePath = blogDir.resolve(article.getSlug() + ".md");
            String content = frontmatter.toString() + article.getContent();
            Files.writeString(filePath, content, StandardCharsets.UTF_8);

            // 更新文章的文件路径
            article.setFilePath(filePath.toString());
            articleRepository.save(article);

            log.info("Markdown 文件创建成功: {}", filePath);
        } catch (IOException e) {
            log.error("创建 Markdown 文件失败: {}", article.getSlug(), e);
            throw new RuntimeException("创建 Markdown 文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除 Markdown 文件
     */
    private void deleteMarkdownFile(String slug) {
        try {
            Path blogDir = Paths.get(contentPath);
            Path filePath = blogDir.resolve(slug + ".md");

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Markdown 文件删除成功: {}", filePath);
            }
        } catch (IOException e) {
            log.error("删除 Markdown 文件失败: {}", slug, e);
            // 删除失败不抛出异常，因为数据库状态已经更新
        }
    }

    /**
     * 转义 YAML 字符串中的特殊字符
     */
    private String escapeYaml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
