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

    @Query("SELECT a FROM Article a WHERE a.status = 'published' AND a.deletedAt IS NULL ORDER BY a.isTop DESC, a.publishedAt DESC")
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

    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = 'published' AND a.deletedAt IS NULL")
    Long countPublishedArticles();
}
