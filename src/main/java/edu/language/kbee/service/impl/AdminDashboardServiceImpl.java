package edu.language.kbee.service.impl;

import edu.language.kbee.payload.DashboardStatsDTO;
import edu.language.kbee.model.PaymentTransaction;
import edu.language.kbee.model.SubscriptionPackage;
import edu.language.kbee.model.User;
import edu.language.kbee.repository.CourseRepository;
import edu.language.kbee.repository.PaymentTransactionRepository;
import edu.language.kbee.repository.SubscriptionPackageRepository;
import edu.language.kbee.repository.UserRepository;
import edu.language.kbee.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CourseRepository courseRepository;
    private final SubscriptionPackageRepository subscriptionPackageRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        long totalStudents = userRepository.count();
        long premiumSubscriptions = userRepository.countPremiumUsers();
        long totalCourses = courseRepository.count();
        BigDecimal totalRevenue = paymentTransactionRepository.calculateTotalRevenue();

        // 1. Calculate Revenue Chart Data (This Year vs Last Year)
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int lastYear = currentYear - 1;

        List<PaymentTransaction> allTransactions = paymentTransactionRepository.findAll();
        Map<Long, BigDecimal> packagePrices = subscriptionPackageRepository.findAll().stream()
                .collect(Collectors.toMap(SubscriptionPackage::getId, SubscriptionPackage::getPrice));

        BigDecimal[] revenueThisYear = new BigDecimal[12];
        BigDecimal[] revenueLastYear = new BigDecimal[12];
        Arrays.fill(revenueThisYear, BigDecimal.ZERO);
        Arrays.fill(revenueLastYear, BigDecimal.ZERO);

        for (PaymentTransaction tx : allTransactions) {
            if (tx.getCreatedAt() != null && tx.getPackageId() != null) {
                int txYear = tx.getCreatedAt().getYear();
                int monthIndex = tx.getCreatedAt().getMonthValue() - 1; // 0-11
                BigDecimal price = packagePrices.getOrDefault(tx.getPackageId(), BigDecimal.ZERO);

                if (txYear == currentYear) {
                    revenueThisYear[monthIndex] = revenueThisYear[monthIndex].add(price);
                } else if (txYear == lastYear) {
                    revenueLastYear[monthIndex] = revenueLastYear[monthIndex].add(price);
                }
            }
        }

        // 2. Calculate User Growth (This Week)
        LocalDateTime startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);

        List<User> allUsers = userRepository.findAll();
        Long[] freeUsersThisWeek = new Long[7];
        Long[] premiumUsersThisWeek = new Long[7];
        Arrays.fill(freeUsersThisWeek, 0L);
        Arrays.fill(premiumUsersThisWeek, 0L);

        for (User u : allUsers) {
            if (u.getCreatedDate() != null) {
                LocalDateTime cd = u.getCreatedDate();
                if (!cd.isBefore(startOfWeek) && !cd.isAfter(endOfWeek)) {
                    int dayIndex = cd.getDayOfWeek().getValue() - 1; // MONDAY is 1 -> index 0
                    if (u.getIsPremium() == 1) {
                        premiumUsersThisWeek[dayIndex]++;
                    } else {
                        freeUsersThisWeek[dayIndex]++;
                    }
                }
            }
        }

        return DashboardStatsDTO.builder()
                .totalStudents(totalStudents)
                .premiumSubscriptions(premiumSubscriptions)
                .totalCourses(totalCourses)
                .totalRevenue(totalRevenue)
                .revenueThisYear(Arrays.asList(revenueThisYear))
                .revenueLastYear(Arrays.asList(revenueLastYear))
                .newFreeUsersThisWeek(Arrays.asList(freeUsersThisWeek))
                .newPremiumUsersThisWeek(Arrays.asList(premiumUsersThisWeek))
                .build();
    }
}
