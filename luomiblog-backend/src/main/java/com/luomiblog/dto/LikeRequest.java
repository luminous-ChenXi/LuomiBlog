package com.luomiblog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequest {

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotNull(message = "点赞类型不能为空")
    private String type;

    private Boolean isLike;

    private String visitorId;
}
