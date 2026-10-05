package com.luomiblog.repository;

import com.luomiblog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT a FROM Article a WHERE a.status = 'published' AND a.deletedAt IS NULL ORDER BY a.top DESC, a.publishedAt DESC")
    Page<Article> findPublishedArticles(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.categoryId = :categoryId AND a.status = 'published' AND a.deletedAt IS NULL")
    Page<Article> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.authorId = :authorId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    Page<Article> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    @Query(value = "SELECT * FROM article WHERE status = 'published' AND deleted_at IS NULL " +
            "AND MATCH(title, content) AGAINST(:keyword IN BOOLEAN MODE) " +
            "ORDER BY MATCH(title, content) AGAINST(:keyword IN BOOLEAN MODE) DESC",
            nativeQuery = true)
    List<Article> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT a FROM Article a WHERE a.status = :status AND a.deletedAt IS NULL")
    Page<Article> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.deletedAt IS NULL " +
           "AND (:search IS NULL OR a.title LIKE %:search% OR a.content LIKE %:search%) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "ORDER BY a.createdAt DESC")
    Page<Article> findAdminArticles(
            @Param("search") String search,
            @Param("category") String category,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.deletedAt IS NULL")
    Long countTotalArticles();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = 'published' AND a.deletedAt IS NULL")
    Long countPublishedArticles();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = 'draft' AND a.deletedAt IS NULL")
    Long countDraftArticles();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = 'archived' AND a.deletedAt IS NULL")
    Long countArchivedArticles();

    /**
     * 统计创建时间在某时间点之前的文章数
     */
    long countByCreatedAtBefore(java.time.LocalDateTime dateTime);

    // ==================== [BUG-017 修复] 原子计数 ====================
    // 旧实现为「读实体→内存+1→save 全字段更新」：并发丢失更新，
    // 且每次自增都重写 content LONGTEXT 溢出页。数据库端原子自增是行业标准做法。

    /** 浏览量原子自增（绕过实体加载，不触碰 content 大字段） */
    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    /** 点赞数原子递减，下限 0（防历史脏数据变负） */
    @Modifying
    @Query("UPDATE Article a SET a.likeCount = CASE WHEN a.likeCount > 0 THEN a.likeCount - 1 ELSE 0 END WHERE a.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.commentCount = a.commentCount + 1 WHERE a.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.commentCount = CASE WHEN a.commentCount > 0 THEN a.commentCount - 1 ELSE 0 END WHERE a.id = :id")
    int decrementCommentCount(@Param("id") Long id);

    /** 轻量读取计数值（避免为拿一个 int 加载整行含 LONGTEXT） */
    @Query("SELECT a.likeCount FROM Article a WHERE a.id = :id")
    Optional<Integer> getLikeCountById(@Param("id") Long id);

    /** [BUG-042 修复] 统计接口轻量化：标量查询替代整行加载 */
    @Query("SELECT a.viewCount FROM Article a WHERE a.id = :id")
    Optional<Integer> getViewCountById(@Param("id") Long id);

    @Query("SELECT a.commentCount FROM Article a WHERE a.id = :id")
    Optional<Integer> getCommentCountById(@Param("id") Long id);

    @Query("SELECT COUNT(a) > 0 FROM Article a WHERE a.id = :id")
    boolean existsByIdAndNotDeleted(@Param("id") Long id);
}
