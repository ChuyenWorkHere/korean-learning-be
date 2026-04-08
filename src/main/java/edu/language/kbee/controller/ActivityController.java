package edu.language.kbee.controller;

import edu.language.kbee.payload.ActivityHeatmapDto;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final UserActivityService userActivityService;

    @PostMapping("/record")
    public ResponseEntity<?> recordActivity() {
        userActivityService.recordActivity();
        return ResponseEntity.ok(Map.of("message", "Activity recorded successfully"));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<ActivityHeatmapDto>> getHeatmap() {
        return ResponseEntity.ok(userActivityService.getHeatmapData());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserDto>> getLeaderboard() {
        return ResponseEntity.ok(userActivityService.getStreakLeaderboard());
    }
}
