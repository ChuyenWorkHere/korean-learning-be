package edu.language.kbee.payload.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyResultResponse {
    private String message;
    private XpAwardResponse xpAward;
}
