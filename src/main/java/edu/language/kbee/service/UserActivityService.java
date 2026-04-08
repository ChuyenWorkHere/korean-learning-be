package edu.language.kbee.service;

import edu.language.kbee.payload.ActivityHeatmapDto;
import edu.language.kbee.payload.UserDto;

import java.util.List;

public interface UserActivityService {
    void recordActivity();
    void recordActivity(String userId);
    List<ActivityHeatmapDto> getHeatmapData();
    List<UserDto> getStreakLeaderboard();
}
