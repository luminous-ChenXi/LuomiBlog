package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private Long targetId;
    private String type;
    private Boolean hasLiked;
    private Integer likeCount;
}
