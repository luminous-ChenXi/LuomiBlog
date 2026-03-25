package com.luomiblog.repository;

import com.luomiblog.entity.ArticleFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleFavoriteRepository extends JpaRepository<ArticleFavorite, Long> {

    Optional<ArticleFavorite> findByArticleIdAndUserId(Long articleId, Long userId);

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    long countByArticleId(Long articleId);

    long countByUserId(Long userId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    Page<ArticleFavorite> findByUserId(Long userId, Pageable pageable);

    Page<ArticleFavorite> findByUserIdAndFolderName(Long userId, String folderName, Pageable pageable);

    @Query("SELECT DISTINCT af.folderName FROM ArticleFavorite af WHERE af.userId = :userId")
    List<String> findFoldersByUserId(@Param("userId") Long userId);
}
