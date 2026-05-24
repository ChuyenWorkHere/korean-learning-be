package edu.language.kbee.service;

import edu.language.kbee.enums.XpSourceType;
import edu.language.kbee.payload.response.UserXpEventResponse;
import edu.language.kbee.payload.response.UserXpSummaryResponse;
import edu.language.kbee.payload.response.XpAwardResponse;

import java.util.List;

public interface XpService {
    XpAwardResponse awardXp(String userId, XpSourceType sourceType, String sourceId, int amount, String reason);

    UserXpSummaryResponse getMyXpSummary();

    List<UserXpEventResponse> getMyRecentXpEvents(int limit);
}
