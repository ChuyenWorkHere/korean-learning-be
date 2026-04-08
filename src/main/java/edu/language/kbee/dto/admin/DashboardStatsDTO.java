package edu.language.kbee.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalStudents;
    private long premiumSubscriptions;
    private long courseCompletions;
    private BigDecimal totalRevenue;

    // Chart data (Sample arrays for now based on user request)
    private List<BigDecimal> revenueThisYear;
    private List<BigDecimal> revenueLastYear;
    
    private List<Long> newFreeUsersThisWeek;
    private List<Long> newPremiumUsersThisWeek;
}
