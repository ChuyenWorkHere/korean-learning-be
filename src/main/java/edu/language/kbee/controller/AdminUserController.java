package edu.language.kbee.controller;

import edu.language.kbee.payload.AdminUserBillingDto;
import edu.language.kbee.payload.AdminUserDetailsDto;
import edu.language.kbee.payload.AdminUserProgressDto;
import edu.language.kbee.payload.AdminUserSubmissionDto;
import edu.language.kbee.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailsDto> getUserDetails(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getAdminUserDetails(userId));
    }

    @GetMapping("/{userId}/billing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserBillingDto> getUserBilling(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getAdminUserBilling(userId));
    }

    @GetMapping("/{userId}/progress")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserProgressDto> getUserProgress(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getAdminUserProgress(userId));
    }

    @GetMapping("/{userId}/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserSubmissionDto>> getUserSubmissions(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getAdminUserSubmissions(userId));
    }
}
