package edu.language.kbee.controller;

import edu.language.kbee.payload.response.UserXpEventResponse;
import edu.language.kbee.payload.response.UserXpSummaryResponse;
import edu.language.kbee.service.XpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/xp")
@RequiredArgsConstructor
public class XpController {

    private final XpService xpService;

    @GetMapping
    public ResponseEntity<UserXpSummaryResponse> getMyXpSummary() {
        return ResponseEntity.ok(xpService.getMyXpSummary());
    }

    @GetMapping("/events")
    public ResponseEntity<List<UserXpEventResponse>> getMyRecentXpEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(xpService.getMyRecentXpEvents(limit));
    }
}
