package edu.language.kbee.service.impl;

import edu.language.kbee.payload.AdminUserBillingDto;
import edu.language.kbee.payload.AdminUserDetailsDto;
import edu.language.kbee.payload.AdminUserProgressDto;
import edu.language.kbee.payload.AdminUserSubmissionDto;
import edu.language.kbee.model.PaymentTransaction;
import edu.language.kbee.model.SubscriptionPackage;
import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.enums.RoleName;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.LessonProgress;
import edu.language.kbee.model.StudentSubmission;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.UpdateProfileRequest;
import edu.language.kbee.repository.*;
import edu.language.kbee.service.CloudinaryService;
import edu.language.kbee.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private final CloudinaryService cloudinaryService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final UserCourseRepository userCourseRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public UserDto updateProfile(UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new ResourceNotFoundException("Người dùng chưa đăng nhập");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getGenderName() != null) {
            user.setGenderName(request.getGenderName());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getUserAvatar() != null && !request.getUserAvatar().isEmpty()) {
            if (user.getUserAvatar() != null && !user.getUserAvatar().equals(request.getUserAvatar())) {
                try {
                    String oldAvatarUrl = user.getUserAvatar();
                    // Extract public_id from Cloudinary URL
                    // Example URL: https://res.cloudinary.com/dxyz/image/upload/v123456789/folder/image_name.png
                    if (oldAvatarUrl.contains("cloudinary.com")) {
                        int lastSlashIndex = oldAvatarUrl.lastIndexOf("/");
                        int nextToLastSlashIndex = oldAvatarUrl.lastIndexOf("/", lastSlashIndex - 1);
                        if (nextToLastSlashIndex != -1 && lastSlashIndex != -1) {
                            String folderAndName = oldAvatarUrl.substring(nextToLastSlashIndex + 1);
                            int dotIndex = folderAndName.lastIndexOf(".");
                            String publicId = dotIndex != -1 ? folderAndName.substring(0, dotIndex) : folderAndName;
                            cloudinaryService.deleteFile(publicId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to delete old avatar: " + e.getMessage());
                }
            }
            user.setUserAvatar(request.getUserAvatar());
        }

        User updatedUser = userRepository.save(user);

        return mapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void updateUserStreak(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        LocalDate today = java.time.LocalDate.now();
        LocalDate lastActivity = user.getLastActivityDate();

        if (lastActivity == null) {
            user.setCurrentStreak(1);
            user.setLastActivityDate(today);
            userRepository.save(user);
        } else if (lastActivity.isEqual(today)) {
            // Already updated today, do nothing.
        } else if (lastActivity.plusDays(1).isEqual(today)) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
            user.setLastActivityDate(today);
            userRepository.save(user);
        } else if (lastActivity.isBefore(today.minusDays(1))) {
            user.setCurrentStreak(1);
            user.setLastActivityDate(today);
            userRepository.save(user);
        }
    }

    @Override
    public List<UserDto> findAllStudents() {
        List<User> students = userRepository.findAllByRoles_RoleNameEquals(RoleName.USER);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return students.stream().map(s -> {
            UserDto userDto = mapper.map(s, UserDto.class);
            if (s.getLastActivityDate() == null || s.getLastActivityDate().isBefore(yesterday)) {
                userDto.setCurrentStreak(0);
            }
            userDto.setIsPremium(s.getIsPremium());

            return userDto;
        }).toList();
    }
    @Override
    public UserDto toggleUserLock(String userId, boolean isLocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setIsLocked(isLocked ? (byte) 1 : (byte) 0);
        User updatedUser = userRepository.save(user);

        UserDto userDto = mapper.map(updatedUser, UserDto.class);
        userDto.setIsPremium(updatedUser.getIsPremium());
        return userDto;
    }

    @Override
    public AdminUserDetailsDto getAdminUserDetails(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        int realStreak = user.getCurrentStreak();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        if (user.getLastActivityDate() == null || user.getLastActivityDate().isBefore(yesterday)) {
            realStreak = 0;
        }

        return AdminUserDetailsDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .isLocked(user.getIsLocked())
                .joinDate(user.getCreatedDate())
                .userAvatar(user.getUserAvatar())
                .currentStreak(realStreak)
                .isPremium(user.getIsPremium())
                .premiumExpiryDate(user.getPremiumExpiryDate())
                .build();
    }

    @Override
    public AdminUserBillingDto getAdminUserBilling(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByUsernameOrderByCreatedAtDesc(user.getUsername());
        
        String currentPlan = "Miễn phí";
        String price = "0";
        String billingCycle = "N/A";
        String renewalDate = user.getPremiumExpiryDate() != null ? user.getPremiumExpiryDate().toString() : "N/A";

        if (user.getIsPremium() == 1 && !transactions.isEmpty()) {
            PaymentTransaction latestTxn = transactions.get(0);
            SubscriptionPackage pkg = subscriptionPackageRepository.findById(latestTxn.getPackageId()).orElse(null);
            if (pkg != null) {
                currentPlan = pkg.getName();
                price = pkg.getPrice().toString();
                billingCycle = pkg.getDurationDays() + " ngày";
            }
        }

        List<AdminUserBillingDto.InvoiceDto> invoices = transactions.stream().map(txn -> {
            SubscriptionPackage pkg = subscriptionPackageRepository.findById(txn.getPackageId()).orElse(null);
            String amt = pkg != null ? pkg.getPrice().toString() : "0";
            return AdminUserBillingDto.InvoiceDto.builder()
                    .id(txn.getTxnRef())
                    .date(txn.getCreatedAt())
                    .amount(amt)
                    .status("Đã thanh toán")
                    .build();
        }).toList();

        return AdminUserBillingDto.builder()
                .currentPlan(currentPlan)
                .price(price)
                .billingCycle(billingCycle)
                .renewalDate(renewalDate)
                .invoices(invoices)
                .build();
    }

    @Override
    public AdminUserProgressDto getAdminUserProgress(String userId) {
        List<AdminUserProgressDto.ProgressItemDto> items = new ArrayList<>();
        
        List<LessonProgress> lessonProgresses = lessonProgressRepository.findByUser_UserIdAndStatusOrderByCompletedAtDesc(userId, ProgressStatus.COMPLETED);
        
        for (LessonProgress lp : lessonProgresses.stream().limit(20).toList()) {
            String categoryName = "Bài học";
            String color = "bg-primary";
            
            if (lp.getLesson() != null && lp.getLesson().getType() != null) {
                switch(lp.getLesson().getType()) {
                    case GRAMMAR: categoryName = "Ngữ pháp"; break;
                    case READING: categoryName = "Đọc hiểu"; break;
                    case LISTENING: categoryName = "Nghe hiểu"; break;
                    case WRITING: categoryName = "Viết"; break;
                    case SPEAKING: categoryName = "Nói"; break;
                }
            }

            String dateStr = lp.getCompletedAt() != null ? 
                lp.getCompletedAt().toLocalDate().toString() : "Đã xong";

            items.add(AdminUserProgressDto.ProgressItemDto.builder()
                .category(categoryName)
                .title(lp.getLesson() != null ? lp.getLesson().getTitle() : "Bài học không tên")
                .progress(100)
                .detail("Hoàn thành " + dateStr)
                .color(color)
                .build());
        }

        return AdminUserProgressDto.builder()
                .progressItems(items)
                .build();
    }

    @Override
    public List<AdminUserSubmissionDto> getAdminUserSubmissions(String userId) {
        List<StudentSubmission> submissions = submissionRepository.findByUser_UserIdOrderByCreatedDateDesc(userId);
        
        return submissions.stream().map(s -> AdminUserSubmissionDto.builder()
                .id(s.getId())
                .title(s.getLesson() != null ? s.getLesson().getTitle() : "Bài học")
                .type(s.getSubmissionType() != null ? s.getSubmissionType().name() : "Khác")
                .submittedAt(s.getCreatedDate())
                .aiScore(s.getAiScore())
                .tutorScore(s.getTutorScore())
                .status(s.getTutorScore() != null ? "Đã đánh giá" : (s.getAiScore() != null ? "AI đã chấm" : "Chờ chấm"))
                .build()
        ).toList();
    }
}
