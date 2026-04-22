package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.TagRequest;
import com.luomiblog.dto.TagResponse;
import com.luomiblog.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ApiResponse<Page<TagResponse>> getAllTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("articleCount").descending());
        return ApiResponse.success(tagService.getAllTags(pageable));
    }

    @GetMapping("/popular")
    public ApiResponse<List<TagResponse>> getPopularTags(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(tagService.getPopularTags(limit));
    }

    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> searchTags(@RequestParam String keyword) {
        return ApiResponse.success(tagService.searchTags(keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTagById(@PathVariable Long id) {
        return ApiResponse.success(tagService.getTagById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<TagResponse> getTagBySlug(@PathVariable String slug) {
        return ApiResponse.success(tagService.getTagBySlug(slug));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:manage')")
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return ApiResponse.success(tagService.createTag(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:manage')")
    public ApiResponse<TagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagRequest request) {
        return ApiResponse.success(tagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERM_article:manage')")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ApiResponse.success();
    }
}
