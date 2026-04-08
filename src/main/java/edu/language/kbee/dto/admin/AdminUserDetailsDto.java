package edu.language.kbee.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailsDto {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private byte isLocked;
    private LocalDateTime joinDate;
    private String userAvatar;
    private int currentStreak;
    private byte isPremium;
    private LocalDate premiumExpiryDate;
}
