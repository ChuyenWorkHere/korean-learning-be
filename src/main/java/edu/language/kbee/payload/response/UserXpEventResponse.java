package edu.language.kbee.payload.response;

import edu.language.kbee.enums.XpSourceType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserXpEventResponse {
    private String xpEventId;
    private XpSourceType sourceType;
    private String sourceId;
    private int xpAmount;
    private String reason;
    private LocalDateTime createdDate;
}
