package edu.language.kbee.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserProgressDto {
    private List<ProgressItemDto> progressItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressItemDto {
        private String category;
        private String title;
        private int progress; // Percentage
        private String detail; // e.g., "42/50 Từ đã thuộc" or "Hoàn thành"
        private String color;
    }
}
