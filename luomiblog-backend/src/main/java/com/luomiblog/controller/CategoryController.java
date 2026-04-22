package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.CategoryRequest;
import com.luomiblog.dto.CategoryResponse;
import com.luomiblog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.success(categoryService.getAllCategories());
    }

    @GetMapping("/tree")
    public ApiResponse<List<CategoryResponse>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTree());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getCategoryById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ApiResponse.success(categoryService.getCategoryBySlug(slug));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:manage')")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:manage')")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERM_article:manage')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success();
    }
}
