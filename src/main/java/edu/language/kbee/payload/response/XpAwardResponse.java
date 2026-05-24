package edu.language.kbee.payload.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpAwardResponse {
    private int awardedXp;
    private boolean awarded;
    private boolean levelUp;
    private int previousLevel;
    private int currentLevel;
    private int totalXp;
}
