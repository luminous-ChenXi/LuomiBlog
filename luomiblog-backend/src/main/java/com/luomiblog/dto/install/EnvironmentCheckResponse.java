package com.luomiblog.dto.install;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EnvironmentCheckResponse {
    private boolean allPassed;
    private List<CheckItem> checks;

    @Data
    @Builder
    public static class CheckItem {
        private String name;
        private boolean passed;
        private String message;
        private String suggestion;
    }
}
