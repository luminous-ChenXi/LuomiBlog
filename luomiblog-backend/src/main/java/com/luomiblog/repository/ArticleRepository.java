package com.luomiblog.repository;

import com.luomiblog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
