package com.luomiblog.repository;

import com.luomiblog.entity.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleTagRepository extends JpaRepository<ArticleTag, Long> {

    @Query("SELECT at FROM ArticleTag at WHERE at.articleId = :articleId")
    List<ArticleTag> findByArticleId(@Param("articleId") Long articleId);

    /** [BUG-041 修复] 批量取多篇文章的标签关联（替代每篇一次的 N+1） */
    @Query("SELECT at FROM ArticleTag at WHERE at.articleId IN :articleIds")
    List<ArticleTag> findByArticleIdIn(@Param("articleIds") java.util.Collection<Long> articleIds);

    @Query("SELECT at FROM ArticleTag at WHERE at.tagId = :tagId")
    List<ArticleTag> findByTagId(@Param("tagId") Long tagId);

    @Modifying
    @Query("DELETE FROM ArticleTag at WHERE at.articleId = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);

    boolean existsByArticleIdAndTagId(Long articleId, Long tagId);
}
