package com.luomiblog.service.impl;

import com.luomiblog.dto.CategoryRequest;
import com.luomiblog.dto.CategoryResponse;
import com.luomiblog.entity.Category;
import com.luomiblog.repository.CategoryRepository;
import com.luomiblog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .parentId(request.getParentId())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .articleCount(0)
                .build();

        categoryRepository.save(category);
        return convertToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        category.setName(request.getName());
        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
            if (categoryRepository.existsBySlug(request.getSlug())) {
                throw new RuntimeException("slug已存在");
            }
            category.setSlug(request.getSlug());
        }
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        categoryRepository.save(category);
        return convertToResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        return convertToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        return convertToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc();

        Map<Long, CategoryResponse> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, this::convertToResponse));

        List<CategoryResponse> rootCategories = new ArrayList<>();

        for (Category category : categories) {
            CategoryResponse response = categoryMap.get(category.getId());
            if (category.getParentId() == null) {
                rootCategories.add(response);
            } else {
                CategoryResponse parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(response);
                }
            }
        }

        return rootCategories;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .articleCount(category.getArticleCount())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
