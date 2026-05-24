package edu.language.kbee.payload.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserXpSummaryResponse {
    private int totalXp;
    private int level;
    private int currentLevelXp;
    private int nextLevelXp;
    private int xpIntoCurrentLevel;
    private int xpNeededForNextLevel;
    private int progressPercent;
}
