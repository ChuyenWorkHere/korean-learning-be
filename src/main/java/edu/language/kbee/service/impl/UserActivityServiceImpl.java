package edu.language.kbee.service.impl;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.User;
import edu.language.kbee.model.UserActivity;
import edu.language.kbee.payload.ActivityHeatmapDto;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.repository.UserActivityRepository;
import edu.language.kbee.repository.UserRepository;
import edu.language.kbee.service.UserActivityService;
import edu.language.kbee.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new ResourceNotFoundException("Người dùng chưa đăng nhập");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
    }

    @Override
    public void recordActivity() {
        User user = getAuthenticatedUser();
        recordActivity(user.getUserId());
    }

    @Override
    public void recordActivity(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        LocalDate today = LocalDate.now();

        Optional<UserActivity> existingActivity = userActivityRepository.findByUser_UserIdAndActivityDate(user.getUserId(), today);

        if (existingActivity.isPresent()) {
            UserActivity activity = existingActivity.get();
            activity.setActivityCount(activity.getActivityCount() + 1);
            userActivityRepository.save(activity);
        } else {
            UserActivity activity = UserActivity.builder()
                    .user(user)
                    .activityDate(today)
                    .activityCount(1)
                    .build();
            userActivityRepository.save(activity);
        }

        // Keep maintaining the user's overall streak logic
        userService.updateUserStreak(user.getUserId());
    }

    @Override
    public List<ActivityHeatmapDto> getHeatmapData() {
        User user = getAuthenticatedUser();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(364);

        List<UserActivity> activities = userActivityRepository.findAllByUser_UserIdAndActivityDateBetweenOrderByActivityDateAsc(
                user.getUserId(), startDate, endDate);

        var activityMap = activities.stream()
                .collect(Collectors.toMap(UserActivity::getActivityDate, UserActivity::getActivityCount));

        List<ActivityHeatmapDto> heatmap = new ArrayList<>();
        
        for (int i = 363; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            int count = activityMap.getOrDefault(date, 0);
            
            heatmap.add(ActivityHeatmapDto.builder()
                    .date(date)
                    .count(count)
                    .dayName(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .monthName(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .build());
        }

        return heatmap;
    }

    @Override
    public List<UserDto> getStreakLeaderboard() {
        List<User> topUsers = userRepository.findTop10ByRoles_RoleNameEqualsOrderByCurrentStreakDesc(RoleName.USER);
        
        return topUsers.stream().map(u -> modelMapper.map(u, UserDto.class)).collect(Collectors.toList());
    }
}
