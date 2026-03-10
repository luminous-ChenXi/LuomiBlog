package com.luomiblog.repository;

import com.luomiblog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findAllActive();

    @Query("SELECT c FROM Category c WHERE c.parentId IS NULL AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findRootCategories();

    @Query("SELECT c FROM Category c WHERE c.parentId = :parentId AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findByParentId(Long parentId);
}
