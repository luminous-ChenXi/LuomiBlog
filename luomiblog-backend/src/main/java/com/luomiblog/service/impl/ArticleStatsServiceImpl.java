package com.luomiblog.service.impl;

import com.luomiblog.dto.ArticleStatsResult;
import com.luomiblog.dto.MyFavoritesResponse;
import com.luomiblog.entity.Article;
import com.luomiblog.entity.ArticleFavorite;
import com.luomiblog.entity.ArticleLike;
import com.luomiblog.entity.Category;
import com.luomiblog.entity.User;
import com.luomiblog.entity.UserBehavior;
import com.luomiblog.repository.ArticleFavoriteRepository;
import com.luomiblog.repository.ArticleLikeRepository;
import com.luomiblog.repository.ArticleRepository;
import com.luomiblog.repository.CategoryRepository;
import com.luomiblog.repository.UserBehaviorRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.ArticleStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleStatsServiceImpl implements ArticleStatsService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @SuppressWarnings("null")
    public boolean recordView(Long articleId, Long userId, String visitorId, String ipAddress, String userAgent) {
        // 1. 检查文章是否存在（[BUG-017 修复] 存在性探测即可，不再加载含 LONGTEXT 的整行）
        if (!articleRepository.existsById(articleId)) {
            log.warn("文章不存在: articleId={}", articleId);
            return false;
        }

        // 2. 检查24小时内是否已浏览过
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Optional<UserBehavior> recentView = userBehaviorRepository.findRecentView(articleId, userId, visitorId, since);

        if (recentView.isPresent()) {
            log.debug("24小时内已浏览过，跳过统计: articleId={}, userId={}, visitorId={}",
                    articleId, userId, visitorId);
            return false;
        }

        // 3. 记录浏览行为 - 使用构造函数避免null警告
        UserBehavior behavior = new UserBehavior();
        behavior.setArticleId(articleId);
        behavior.setUserId(userId);
        behavior.setVisitorId(visitorId);
        behavior.setBehaviorType(UserBehavior.BehaviorType.VIEW);
        behavior.setIpAddress(ipAddress);
        behavior.setUserAgent(userAgent);
        userBehaviorRepository.save(behavior);

        // 4. [BUG-017 修复] 浏览数数据库端原子自增（旧实现读-改-写全字段 save，并发丢计数且重写 LONGTEXT）
        articleRepository.incrementViewCount(articleId);

        log.info("记录文章浏览: articleId={}, userId={}, visitorId={}", articleId, userId, visitorId);
        return true;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public ArticleStatsResult toggleLike(Long articleId, Long userId, String visitorId, String ipAddress) {
        // 1. 检查文章是否存在（不加载整行）
        if (!articleRepository.existsById(articleId)) {
            return ArticleStatsResult.builder()
                    .success(false)
                    .message("文章不存在")
                    .build();
        }

        // 2. 检查是否已点赞（登录用户优先使用userId；匿名必须携带 visitorId，否则唯一键对 NULL 不去重=刷赞入口）
        boolean hasLiked;
        if (userId != null) {
            hasLiked = articleLikeRepository.existsByArticleIdAndUserId(articleId, userId);
        } else if (visitorId != null && !visitorId.isBlank()) {
            hasLiked = articleLikeRepository.existsByArticleIdAndVisitorId(articleId, visitorId);
        } else {
            return ArticleStatsResult.builder()
                    .success(false)
                    .message("缺少有效的访客标识，无法点赞")
                    .build();
        }

        // 3. 切换点赞状态（[BUG-017 修复] 计数全部走数据库端原子 UPDATE）
        if (hasLiked) {
            if (userId != null) {
                articleLikeRepository.deleteByArticleIdAndUserId(articleId, userId);
            } else {
                articleLikeRepository.deleteByArticleIdAndVisitorId(articleId, visitorId);
            }

            articleRepository.decrementLikeCount(articleId);
            int newLikeCount = articleRepository.getLikeCountById(articleId).orElse(0);

            log.info("取消点赞: articleId={}, userId={}, visitorId={}", articleId, userId, visitorId);

            return ArticleStatsResult.builder()
                    .success(true)
                    .action("unlike")
                    .likeCount(newLikeCount)
                    .hasLiked(false)
                    .message("已取消点赞")
                    .build();
        }

        ArticleLike like = new ArticleLike();
        like.setArticleId(articleId);
        like.setUserId(userId);
        like.setVisitorId(visitorId);
        like.setIpAddress(ipAddress);
        try {
            articleLikeRepository.save(like);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 并发双击撞唯一键：幂等返回已点赞态（旧实现 500）
            log.info("并发点赞唯一键冲突，按已点赞处理: articleId={}, userId={}, visitorId={}",
                    articleId, userId, visitorId);
            return ArticleStatsResult.builder()
                    .success(true)
                    .action("like")
                    .likeCount(articleRepository.getLikeCountById(articleId).orElse(0))
                    .hasLiked(true)
                    .message("已点赞")
                    .build();
        }
        articleRepository.incrementLikeCount(articleId);
        int newLikeCount = articleRepository.getLikeCountById(articleId).orElse(0);

        log.info("点赞成功: articleId={}, userId={}, visitorId={}", articleId, userId, visitorId);

        return ArticleStatsResult.builder()
                .success(true)
                .action("like")
                .likeCount(newLikeCount)
                .hasLiked(true)
                .message("点赞成功")
                .build();
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public ArticleStatsResult toggleFavorite(Long articleId, Long userId) {
        // 1. 检查用户是否登录
        if (userId == null) {
            return ArticleStatsResult.builder()
                    .success(false)
                    .message("请先登录后再收藏")
                    .build();
        }

        // 2. 检查文章是否存在
        Optional<Article> articleOpt = articleRepository.findById(articleId);
        if (articleOpt.isEmpty()) {
            return ArticleStatsResult.builder()
                    .success(false)
                    .message("文章不存在")
                    .build();
        }

        // 3. 检查是否已收藏（userId 已确认非空）
        Long nonNullUserId = userId;
        boolean hasFavorited = articleFavoriteRepository.existsByArticleIdAndUserId(articleId, nonNullUserId);

        // 4. 切换收藏状态
        if (hasFavorited) {
            // 取消收藏
            articleFavoriteRepository.deleteByArticleIdAndUserId(articleId, nonNullUserId);

            log.info("取消收藏: articleId={}, userId={}", articleId, nonNullUserId);

            return ArticleStatsResult.builder()
                    .success(true)
                    .action("unfavorite")
                    .favoriteCount((int) articleFavoriteRepository.countByArticleId(articleId))
                    .hasFavorited(false)
                    .message("已取消收藏")
                    .build();
        } else {
            // 添加收藏 - 使用构造函数避免null警告
            ArticleFavorite favorite = new ArticleFavorite();
            favorite.setArticleId(articleId);
            favorite.setUserId(nonNullUserId);
            favorite.setFolderName("默认收藏夹");
            articleFavoriteRepository.save(favorite);

            log.info("收藏成功: articleId={}, userId={}", articleId, nonNullUserId);

            return ArticleStatsResult.builder()
                    .success(true)
                    .action("favorite")
                    .favoriteCount((int) articleFavoriteRepository.countByArticleId(articleId))
                    .hasFavorited(true)
                    .message("收藏成功")
                    .build();
        }
    }

    @Override
    public boolean hasLiked(Long articleId, Long userId, String visitorId) {
        if (userId != null) {
            return articleLikeRepository.existsByArticleIdAndUserId(articleId, userId);
        } else if (visitorId != null) {
            return articleLikeRepository.existsByArticleIdAndVisitorId(articleId, visitorId);
        }
        return false;
    }

    @Override
    public boolean hasFavorited(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return articleFavoriteRepository.existsByArticleIdAndUserId(articleId, userId);
    }

    @Override
    @SuppressWarnings("null")
    public ArticleStatsResult getArticleStats(Long articleId, Long userId, String visitorId) {
        // [BUG-042 修复] 标量查询取计数，不再为统计加载含 content LONGTEXT 的整行
        Optional<Integer> viewCount = articleRepository.getViewCountById(articleId);
        if (viewCount.isEmpty()) {
            return ArticleStatsResult.builder()
                    .success(false)
                    .message("文章不存在")
                    .build();
        }

        return ArticleStatsResult.builder()
                .success(true)
                .viewCount(viewCount.get())
                .likeCount(articleRepository.getLikeCountById(articleId).orElse(0))
                .favoriteCount((int) articleFavoriteRepository.countByArticleId(articleId))
                .hasLiked(hasLiked(articleId, userId, visitorId))
                .hasFavorited(hasFavorited(articleId, userId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MyFavoritesResponse getMyFavorites(Long userId, String folder, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean filterByFolder = folder != null && !folder.isBlank();
        Page<ArticleFavorite> favoritePage = filterByFolder
                ? articleFavoriteRepository.findByUserIdAndFolderName(userId, folder, pageable)
                : articleFavoriteRepository.findByUserId(userId, pageable);

        List<ArticleFavorite> favorites = favoritePage.getContent();

        // 批量取文章 / 分类 / 作者信息，避免 N+1
        Map<Long, Article> articleMap = articleRepository.findAllById(
                        favorites.stream().map(ArticleFavorite::getArticleId).toList())
                .stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        Map<Long, Category> categoryMap = categoryRepository.findAllById(
                        articleMap.values().stream()
                                .map(Article::getCategoryId)
                                .filter(java.util.Objects::nonNull)
                                .toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<Long, User> authorMap = userRepository.findAllById(
                        articleMap.values().stream()
                                .map(Article::getAuthorId)
                                .filter(java.util.Objects::nonNull)
                                .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MyFavoritesResponse.FavoriteItem> items = favorites.stream()
                .map(favorite -> {
                    Article article = articleMap.get(favorite.getArticleId());
                    MyFavoritesResponse.FavoriteItem.FavoriteItemBuilder builder =
                            MyFavoritesResponse.FavoriteItem.builder()
                                    .favoriteId(favorite.getId())
                                    .articleId(favorite.getArticleId())
                                    .folderName(favorite.getFolderName())
                                    .favoritedAt(favorite.getCreatedAt());
                    if (article != null) {
                        String authorName = Optional.ofNullable(article.getAuthorId())
                                .map(authorMap::get)
                                .map(author -> author.getNickname() != null ? author.getNickname() : author.getUsername())
                                .orElse(null);
                        String categoryName = Optional.ofNullable(article.getCategoryId())
                                .map(categoryMap::get)
                                .map(Category::getName)
                                .orElse(null);
                        builder.title(article.getTitle())
                                .slug(article.getSlug())
                                .summary(article.getSummary())
                                .authorName(authorName)
                                .categoryName(categoryName)
                                .viewCount(article.getViewCount())
                                .likeCount(article.getLikeCount());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());

        List<String> folders = articleFavoriteRepository.findFoldersByUserId(userId);
        if (folders == null) {
            folders = Collections.emptyList();
        }

        return MyFavoritesResponse.builder()
                .folders(folders)
                .page(favoritePage.getNumber())
                .size(favoritePage.getSize())
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .items(items)
                .build();
    }
}
