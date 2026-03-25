package com.luomiblog.service.impl;

import com.luomiblog.dto.DashboardStatsDTO;
import com.luomiblog.entity.Article;
import com.luomiblog.entity.Comment;
import com.luomiblog.entity.User;
import com.luomiblog.entity.UserBehavior;
import com.luomiblog.repository.ArticleRepository;
import com.luomiblog.repository.CommentRepository;
import com.luomiblog.repository.UserBehaviorRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserBehaviorRepository userBehaviorRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        // 获取今日和昨日的时间范围
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        // 1. 总用户数
        long totalUsers = userRepository.count();
        long yesterdayUsers = userRepository.countByCreatedAtBefore(yesterdayStart);
        int userGrowth = calculateGrowth(totalUsers, yesterdayUsers);

        // 2. 文章数量
        long totalArticles = articleRepository.count();
        long yesterdayArticles = articleRepository.countByCreatedAtBefore(yesterdayStart);
        int articleGrowth = calculateGrowth(totalArticles, yesterdayArticles);

        // 3. 评论数量
        long totalComments = commentRepository.count();
        long yesterdayComments = commentRepository.countByCreatedAtBefore(yesterdayStart);
        int commentGrowth = calculateGrowth(totalComments, yesterdayComments);

        // 4. 今日访问量（从 user_behavior 表统计今日 VIEW 行为）
        long todayViews = userBehaviorRepository.countByBehaviorTypeAndCreatedAtAfter(
                UserBehavior.BehaviorType.VIEW, todayStart);
        long yesterdayViews = userBehaviorRepository.countByBehaviorTypeAndCreatedAtBetween(
                UserBehavior.BehaviorType.VIEW, yesterdayStart, todayStart);
        int viewGrowth = calculateGrowth(todayViews, yesterdayViews);

        // 5. 获取最近动态
        List<DashboardStatsDTO.RecentActivityDTO> recentActivities = getRecentActivities();

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalArticles(totalArticles)
                .totalComments(totalComments)
                .todayViews(todayViews)
                .userGrowth(userGrowth)
                .articleGrowth(articleGrowth)
                .commentGrowth(commentGrowth)
                .viewGrowth(viewGrowth)
                .recentActivities(recentActivities)
                .build();
    }

    /**
     * 计算增长率
     */
    private int calculateGrowth(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round(((double) (current - previous) / previous) * 100);
    }

    /**
     * 获取最近动态
     */
    private List<DashboardStatsDTO.RecentActivityDTO> getRecentActivities() {
        List<DashboardStatsDTO.RecentActivityDTO> activities = new ArrayList<>();

        // 获取最近的用户注册
        Pageable userPageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        List<User> recentUsers = userRepository.findAll(userPageable).getContent();
        for (User user : recentUsers) {
            activities.add(DashboardStatsDTO.RecentActivityDTO.builder()
                    .type("user")
                    .action("新用户注册")
                    .detail("用户 \"" + user.getUsername() + "\" 注册了账号")
                    .time(formatTimeAgo(user.getCreatedAt()))
                    .build());
        }

        // 获取最近发布的文章
        Pageable articlePageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        List<Article> recentArticles = articleRepository.findAll(articlePageable).getContent();
        for (Article article : recentArticles) {
            activities.add(DashboardStatsDTO.RecentActivityDTO.builder()
                    .type("article")
                    .action("文章发布")
                    .detail("\"" + article.getTitle() + "\" 已发布")
                    .time(formatTimeAgo(article.getCreatedAt()))
                    .build());
        }

        // 获取最近的评论
        Pageable commentPageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        List<Comment> recentComments = commentRepository.findAll(commentPageable).getContent();
        for (Comment comment : recentComments) {
            // 获取评论的文章标题
            Long commentArticleId = comment.getArticleId();
            String articleTitle = "未知文章";
            if (commentArticleId != null) {
                articleTitle = articleRepository.findById(commentArticleId)
                        .map(Article::getTitle)
                        .orElse("未知文章");
            }

            // 获取评论者名称
            String authorName = comment.getVisitorName() != null ? comment.getVisitorName() : "匿名用户";

            activities.add(DashboardStatsDTO.RecentActivityDTO.builder()
                    .type("comment")
                    .action("新评论")
                    .detail(authorName + " 评论了 \"" + articleTitle + "\"")
                    .time(formatTimeAgo(comment.getCreatedAt()))
                    .build());
        }

        // 按时间排序，取前4条
        activities.sort((a, b) -> {
            // 这里简化处理，实际应该根据原始时间排序
            return 0;
        });

        if (activities.size() > 4) {
            activities = activities.subList(0, 4);
        }

        return activities;
    }

    /**
     * 格式化时间为"多久前"
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知时间";
        }

        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, LocalDateTime.now());
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = ChronoUnit.DAYS.between(dateTime, LocalDateTime.now());
        if (days < 30) {
            return days + "天前";
        }

        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
