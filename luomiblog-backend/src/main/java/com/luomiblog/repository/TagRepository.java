package com.luomiblog.repository;

import com.luomiblog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    @Query("SELECT t FROM Tag t WHERE t.deletedAt IS NULL ORDER BY t.usageCount DESC")
    List<Tag> findAllActiveOrderByUsage();

    @Query("SELECT t FROM Tag t WHERE t.name IN :names AND t.deletedAt IS NULL")
    List<Tag> findByNameIn(@Param("names") Set<String> names);

    @Query(value = "SELECT t.* FROM tags t " +
            "JOIN article_tags at ON t.id = at.tag_id " +
            "WHERE at.article_id = :articleId AND t.deleted_at IS NULL",
            nativeQuery = true)
    List<Tag> findByArticleId(@Param("articleId") Long articleId);
}
