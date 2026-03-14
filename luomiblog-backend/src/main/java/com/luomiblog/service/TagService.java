package com.luomiblog.service;

import com.luomiblog.dto.TagRequest;
import com.luomiblog.dto.TagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TagService {

    TagResponse createTag(TagRequest request);

    TagResponse updateTag(Long id, TagRequest request);

    void deleteTag(Long id);

    TagResponse getTagById(Long id);

    TagResponse getTagBySlug(String slug);

    Page<TagResponse> getAllTags(Pageable pageable);

    List<TagResponse> getPopularTags(int limit);

    List<TagResponse> searchTags(String keyword);
}
