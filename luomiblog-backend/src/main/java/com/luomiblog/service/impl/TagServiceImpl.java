package com.luomiblog.service.impl;

import com.luomiblog.dto.TagRequest;
import com.luomiblog.dto.TagResponse;
import com.luomiblog.entity.Tag;
import com.luomiblog.repository.TagRepository;
import com.luomiblog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());

        if (tagRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Tag tag = Tag.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : "user")
                .articleCount(0)
                .build();

        tagRepository.save(tag);
        return convertToResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        tag.setName(request.getName());
        if (request.getSlug() != null && !request.getSlug().equals(tag.getSlug())) {
            if (tagRepository.existsBySlug(request.getSlug())) {
                throw new RuntimeException("slug已存在");
            }
            tag.setSlug(request.getSlug());
        }
        tag.setDescription(request.getDescription());
        if (request.getType() != null) {
            tag.setType(request.getType());
        }

        tagRepository.save(tag);
        return convertToResponse(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        tag.setDeletedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        return convertToResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagBySlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        return convertToResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TagResponse> getAllTags(Pageable pageable) {
        return tagRepository.findAllByDeletedAtIsNull(pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getPopularTags(int limit) {
        return tagRepository.findTopByArticleCountDesc(limit)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String keyword) {
        return tagRepository.findByNameContainingAndDeletedAtIsNull(keyword)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private TagResponse convertToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .type(tag.getType())
                .articleCount(tag.getArticleCount())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}
