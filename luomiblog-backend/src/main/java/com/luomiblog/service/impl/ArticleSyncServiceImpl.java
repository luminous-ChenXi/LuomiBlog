package com.luomiblog.service.impl;

import com.luomiblog.dto.ArticleFileInfo;
import com.luomiblog.dto.ArticleSyncResult;
import com.luomiblog.entity.Article;
import com.luomiblog.entity.User;
import com.luomiblog.repository.ArticleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.ArticleSyncService;
import com.luomiblog.service.MemoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSyncServiceImpl implements ArticleSyncService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final MemoryCacheService cacheService;

    @Value("${article.content.path:../luomiblog-frontend/src/content/blog}")
    private String contentPath;

    private static final String SYNC_STATUS_KEY = "article:sync:status";
    private static final String SYNC_LOCK_KEY = "article:sync:lock";
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL
    );
    private static final Pattern YAML_ENTRY_PATTERN = Pattern.compile(
        "^(\\w+):\\s*(.+)$", Pattern.MULTILINE
    );

    @Override
    @Transactional
    public void syncArticlesFromFiles() {
        if (!acquireSyncLock()) {
            log.warn("文章同步正在进行中，跳过本次同步");
            return;
        }

        try {
            log.info("开始同步文章文件到数据库...");
            Path blogDir = Paths.get(contentPath);
            
            if (!Files.exists(blogDir)) {
                log.error("文章目录不存在: {}", blogDir.toAbsolutePath());
                return;
            }

            List<ArticleFileInfo> fileInfos = scanArticleFiles(blogDir);
            log.info("扫描到 {} 个文章文件", fileInfos.size());

            int created = 0, updated = 0, unchanged = 0, conflicts = 0;
            List<ArticleSyncResult.ConflictInfo> conflictList = new ArrayList<>();

            User defaultAuthor = getDefaultAuthor();

            for (ArticleFileInfo fileInfo : fileInfos) {
                try {
                    SyncAction action = processArticleFile(fileInfo, defaultAuthor);
                    switch (action) {
                        case CREATED -> created++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> unchanged++;
                        case CONFLICT -> {
                            conflicts++;
                            conflictList.add(buildConflictInfo(fileInfo));
                        }
                    }
                } catch (Exception e) {
                    log.error("处理文章文件失败: {}", fileInfo.getFileName(), e);
                }
            }

            updateSyncStatus(fileInfos.size(), created + updated, conflicts);
            log.info("文章同步完成: 创建={}, 更新={}, 未变={}, 冲突={}", 
                created, updated, unchanged, conflicts);

        } catch (Exception e) {
            log.error("文章同步失败", e);
        } finally {
            releaseSyncLock();
        }
    }

    @Override
    public List<String> detectConflicts() {
        Path blogDir = Paths.get(contentPath);
        if (!Files.exists(blogDir)) {
            return Collections.emptyList();
        }

        List<ArticleFileInfo> fileInfos = scanArticleFiles(blogDir);
        return fileInfos.stream()
            .filter(this::hasConflict)
            .map(ArticleFileInfo::getSlug)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resolveConflict(Long articleId, String resolution) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("文章不存在"));
        Objects.requireNonNull(article, "文章不能为null");

        switch (resolution) {
            case "USE_FILE" -> {
                Path filePath = Paths.get(contentPath, article.getSlug() + ".md");
                if (Files.exists(filePath)) {
                    ArticleFileInfo fileInfo = parseArticleFile(filePath);
                    updateArticleFromFile(article, fileInfo, getDefaultAuthor());
                    article.setContentHash(fileInfo.getContentHash());
                    articleRepository.save(article);
                    log.info("冲突解决: 使用文件版本, articleId={}", articleId);
                }
            }
            case "USE_DB" -> {
                article.setContentHash(calculateHash(article.getContent()));
                articleRepository.save(article);
                log.info("冲突解决: 使用数据库版本, articleId={}", articleId);
            }
            case "MERGE" -> {
                log.info("冲突解决: 需要手动合并, articleId={}", articleId);
            }
            default -> throw new IllegalArgumentException("未知的解决策略: " + resolution);
        }
    }

    @Override
    public boolean isSyncNeeded() {
        Path blogDir = Paths.get(contentPath);
        if (!Files.exists(blogDir)) {
            return false;
        }

        List<ArticleFileInfo> fileInfos = scanArticleFiles(blogDir);
        for (ArticleFileInfo fileInfo : fileInfos) {
            Optional<Article> existing = articleRepository.findBySlug(fileInfo.getSlug());
            if (existing.isEmpty()) {
                return true;
            }
            if (!fileInfo.getContentHash().equals(existing.get().getContentHash())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SyncStatus getSyncStatus() {
        return cacheService.get(SYNC_STATUS_KEY, SyncStatus.class);
    }

    private List<ArticleFileInfo> scanArticleFiles(Path blogDir) {
        try (Stream<Path> paths = Files.list(blogDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".md"))
                .map(this::parseArticleFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("扫描文章文件失败", e);
            return Collections.emptyList();
        }
    }

    private ArticleFileInfo parseArticleFile(Path filePath) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            String fileName = filePath.getFileName().toString();
            String slug = fileName.replace(".md", "");

            Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
            if (!matcher.find()) {
                log.warn("文件缺少Frontmatter: {}", fileName);
                return null;
            }

            String frontmatter = matcher.group(1);
            String body = matcher.group(2).trim();

            Map<String, String> yamlData = parseYaml(frontmatter);

            return ArticleFileInfo.builder()
                .fileName(fileName)
                .slug(slug)
                .title(yamlData.getOrDefault("title", slug))
                .description(yamlData.get("description"))
                .content(body)
                .author(yamlData.get("author"))
                .pubDate(parseDate(yamlData.get("pubDate")))
                .tags(parseTags(yamlData.get("tags")))
                .category(yamlData.get("category"))
                .cover(yamlData.get("cover"))
                .fileSize(Files.size(filePath))
                .fileModifiedTime(LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(filePath).toInstant(), 
                    java.time.ZoneId.systemDefault()
                ))
                .contentHash(calculateHash(content))
                .build();

        } catch (Exception e) {
            log.error("解析文章文件失败: {}", filePath, e);
            return null;
        }
    }

    private Map<String, String> parseYaml(String frontmatter) {
        Map<String, String> data = new HashMap<>();
        Matcher matcher = YAML_ENTRY_PATTERN.matcher(frontmatter);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            value = value.replaceAll("^['\"]|['\"]$", "");
            data.put(key, value);
        }
        return data;
    }

    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagsStr.replace("[", "").replace("]", "").split(","))
            .map(String::trim)
            .map(s -> s.replaceAll("^['\"]|['\"]$", ""))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }

    private SyncAction processArticleFile(ArticleFileInfo fileInfo, User author) {
        Optional<Article> existing = articleRepository.findBySlug(fileInfo.getSlug());

        if (existing.isEmpty()) {
            Article article = createArticleFromFile(fileInfo, author);
            articleRepository.save(article);
            log.info("创建新文章: {}", fileInfo.getTitle());
            return SyncAction.CREATED;
        }

        Article article = existing.get();
        String dbHash = article.getContentHash();
        String fileHash = fileInfo.getContentHash();

        if (dbHash == null || !dbHash.equals(fileHash)) {
            if (isConflict(article, fileInfo)) {
                log.warn("检测到冲突: {}", fileInfo.getTitle());
                return SyncAction.CONFLICT;
            }
            updateArticleFromFile(article, fileInfo, author);
            articleRepository.save(article);
            log.info("更新文章: {}", fileInfo.getTitle());
            return SyncAction.UPDATED;
        }

        return SyncAction.UNCHANGED;
    }

    private boolean isConflict(Article article, ArticleFileInfo fileInfo) {
        LocalDateTime dbUpdated = article.getUpdatedAt();
        LocalDateTime fileModified = fileInfo.getFileModifiedTime();
        
        if (dbUpdated == null || fileModified == null) {
            return false;
        }

        return Math.abs(java.time.Duration.between(dbUpdated, fileModified).toMinutes()) > 5;
    }

    private boolean hasConflict(ArticleFileInfo fileInfo) {
        Optional<Article> existing = articleRepository.findBySlug(fileInfo.getSlug());
        return existing.isPresent() && isConflict(existing.get(), fileInfo);
    }

    private ArticleSyncResult.ConflictInfo buildConflictInfo(ArticleFileInfo fileInfo) {
        Optional<Article> existing = articleRepository.findBySlug(fileInfo.getSlug());
        return ArticleSyncResult.ConflictInfo.builder()
            .slug(fileInfo.getSlug())
            .title(fileInfo.getTitle())
            .conflictType("BOTH_MODIFIED")
            .fileModifiedTime(fileInfo.getFileModifiedTime().toString())
            .dbModifiedTime(existing.map(Article::getUpdatedAt).map(LocalDateTime::toString).orElse(""))
            .suggestion("文件和数据库都被修改，请选择保留版本")
            .build();
    }

    private Article createArticleFromFile(ArticleFileInfo fileInfo, User author) {
        return Article.builder()
            .title(fileInfo.getTitle())
            .slug(fileInfo.getSlug())
            .summary(fileInfo.getDescription())
            .content(fileInfo.getContent())
            .contentHash(fileInfo.getContentHash())
            .filePath(contentPath + "/" + fileInfo.getFileName())
            .authorId(author != null ? author.getId() : 1L)
            .language("zh")
            .status("published")
            .version("1.0.0")
            .viewCount(0)
            .likeCount(0)
            .commentCount(0)
            .wordCount(fileInfo.getContent() != null ? fileInfo.getContent().length() : 0)
            .readingTime(calculateReadingTime(fileInfo.getContent()))
            .top(false)
            .sortOrder(0)
            .allowComments(true)
            .allowSuggestions(false)
            .syncBailian(false)
            .publishedAt(fileInfo.getPubDate() != null ? fileInfo.getPubDate() : LocalDateTime.now())
            .build();
    }

    private void updateArticleFromFile(Article article, ArticleFileInfo fileInfo, User author) {
        article.setTitle(fileInfo.getTitle());
        article.setSummary(fileInfo.getDescription());
        article.setContent(fileInfo.getContent());
        article.setContentHash(fileInfo.getContentHash());
        article.setWordCount(fileInfo.getContent() != null ? fileInfo.getContent().length() : 0);
        article.setReadingTime(calculateReadingTime(fileInfo.getContent()));
        article.setVersion(incrementVersion(article.getVersion()));
    }

    private String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.0.0";
        }
        try {
            String[] parts = version.split("\\.");
            int patch = Integer.parseInt(parts[2]) + 1;
            return parts[0] + "." + parts[1] + "." + patch;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private int calculateReadingTime(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        int wordCount = content.length();
        return Math.max(1, wordCount / 300);
    }

    private User getDefaultAuthor() {
        List<User> admins = userRepository.findByRoleId(1);
        if (!admins.isEmpty()) {
            return admins.get(0);
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private boolean acquireSyncLock() {
        Boolean locked = cacheService.get(SYNC_LOCK_KEY, Boolean.class);
        if (locked != null && locked) {
            return false;
        }
        cacheService.set(SYNC_LOCK_KEY, true, 300);
        return true;
    }

    private void releaseSyncLock() {
        cacheService.delete(SYNC_LOCK_KEY);
    }

    private void updateSyncStatus(int total, int synced, int conflicts) {
        SyncStatus status = new SyncStatus(total, synced, conflicts, System.currentTimeMillis());
        cacheService.set(SYNC_STATUS_KEY, status, 3600);
    }

    private enum SyncAction {
        CREATED, UPDATED, UNCHANGED, CONFLICT
    }
}
