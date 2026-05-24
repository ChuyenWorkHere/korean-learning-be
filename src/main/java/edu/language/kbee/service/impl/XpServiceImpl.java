package edu.language.kbee.service.impl;

import edu.language.kbee.enums.XpSourceType;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.User;
import edu.language.kbee.model.UserXpEvent;
import edu.language.kbee.payload.response.UserXpEventResponse;
import edu.language.kbee.payload.response.UserXpSummaryResponse;
import edu.language.kbee.payload.response.XpAwardResponse;
import edu.language.kbee.repository.UserRepository;
import edu.language.kbee.repository.UserXpEventRepository;
import edu.language.kbee.service.XpService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class XpServiceImpl implements XpService {

    private static final int[] LEVEL_THRESHOLDS = {
            0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200,
            4000, 4900, 5900, 7000, 8200, 9500, 10900, 12400, 14000, 15700
    };

    private final UserRepository userRepository;
    private final UserXpEventRepository xpEventRepository;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public XpAwardResponse awardXp(String userId, XpSourceType sourceType, String sourceId, int amount, String reason) {
        if (amount <= 0 || sourceId == null || sourceId.isBlank()) {
            return noAward(userId);
        }

        if (xpEventRepository.existsByUser_UserIdAndSourceTypeAndSourceId(userId, sourceType, sourceId)) {
            return noAward(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int previousLevel = normalizeLevel(user.getLevel());
        int newTotalXp = Math.max(0, user.getTotalXp()) + amount;
        int newLevel = calculateLevel(newTotalXp);

        user.setTotalXp(newTotalXp);
        user.setLevel(newLevel);
        userRepository.save(user);

        xpEventRepository.save(UserXpEvent.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .xpAmount(amount)
                .reason(reason)
                .build());

        return XpAwardResponse.builder()
                .awarded(true)
                .awardedXp(amount)
                .previousLevel(previousLevel)
                .currentLevel(newLevel)
                .levelUp(newLevel > previousLevel)
                .totalXp(newTotalXp)
                .build();
    }

    @Override
    public UserXpSummaryResponse getMyXpSummary() {
        User user = authUtil.getLoggedInUser();
        return buildSummary(Math.max(0, user.getTotalXp()), normalizeLevel(user.getLevel()));
    }

    @Override
    public List<UserXpEventResponse> getMyRecentXpEvents(int limit) {
        User user = authUtil.getLoggedInUser();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return xpEventRepository.findByUser_UserIdOrderByCreatedDateDesc(user.getUserId(), PageRequest.of(0, safeLimit))
                .stream()
                .map(event -> UserXpEventResponse.builder()
                        .xpEventId(event.getXpEventId())
                        .sourceType(event.getSourceType())
                        .sourceId(event.getSourceId())
                        .xpAmount(event.getXpAmount())
                        .reason(event.getReason())
                        .createdDate(event.getCreatedDate())
                        .build())
                .toList();
    }

    private XpAwardResponse noAward(String userId) {
        return userRepository.findById(userId)
                .map(user -> XpAwardResponse.builder()
                        .awarded(false)
                        .awardedXp(0)
                        .previousLevel(normalizeLevel(user.getLevel()))
                        .currentLevel(normalizeLevel(user.getLevel()))
                        .levelUp(false)
                        .totalXp(Math.max(0, user.getTotalXp()))
                        .build())
                .orElseGet(() -> XpAwardResponse.builder()
                        .awarded(false)
                        .awardedXp(0)
                        .previousLevel(1)
                        .currentLevel(1)
                        .levelUp(false)
                        .totalXp(0)
                        .build());
    }

    private UserXpSummaryResponse buildSummary(int totalXp, int level) {
        int currentLevelXp = requiredXpForLevel(level);
        int nextLevelXp = requiredXpForLevel(level + 1);
        int xpIntoCurrentLevel = Math.max(0, totalXp - currentLevelXp);
        int xpNeededForNextLevel = Math.max(0, nextLevelXp - totalXp);
        int levelRange = Math.max(1, nextLevelXp - currentLevelXp);
        int progressPercent = Math.min(100, Math.round((float) xpIntoCurrentLevel / levelRange * 100));

        return UserXpSummaryResponse.builder()
                .totalXp(totalXp)
                .level(level)
                .currentLevelXp(currentLevelXp)
                .nextLevelXp(nextLevelXp)
                .xpIntoCurrentLevel(xpIntoCurrentLevel)
                .xpNeededForNextLevel(xpNeededForNextLevel)
                .progressPercent(progressPercent)
                .build();
    }

    private int calculateLevel(int totalXp) {
        int level = 1;
        while (level < 100 && totalXp >= requiredXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    private int requiredXpForLevel(int level) {
        if (level <= 1) return 0;
        if (level <= LEVEL_THRESHOLDS.length) return LEVEL_THRESHOLDS[level - 1];
        return 15700 + (level - LEVEL_THRESHOLDS.length) * 2000;
    }

    private int normalizeLevel(int level) {
        return Math.max(1, level);
    }
}
