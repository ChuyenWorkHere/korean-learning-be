package edu.language.kbee.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSubmissionDto {
    private String id;
    private String title;
    private String type;
    private LocalDateTime submittedAt;
    private Integer aiScore;
    private Integer tutorScore;
    private String status;
}
