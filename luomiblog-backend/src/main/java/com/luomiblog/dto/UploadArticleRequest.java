package com.luomiblog.dto;

import lombok.Data;

@Data
public class UploadArticleRequest {
    private String filename;
    private String content;
    private Boolean autoPublish;
    private Boolean skipExisting;
}
