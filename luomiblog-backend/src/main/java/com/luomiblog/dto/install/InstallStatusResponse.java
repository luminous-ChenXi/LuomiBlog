package com.luomiblog.dto.install;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstallStatusResponse {
    private boolean installed;
    private boolean locked;
    private String message;
}
