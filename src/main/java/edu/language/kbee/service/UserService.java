package edu.language.kbee.service;

import edu.language.kbee.dto.admin.AdminUserBillingDto;
import edu.language.kbee.dto.admin.AdminUserDetailsDto;
import edu.language.kbee.dto.admin.AdminUserProgressDto;
import edu.language.kbee.dto.admin.AdminUserSubmissionDto;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.UpdateProfileRequest;

import java.util.List;

public interface UserService {
    UserDto updateProfile(UpdateProfileRequest request);

    void updateUserStreak(String userId);

    List<UserDto> findAllStudents();

    UserDto toggleUserLock(String userId, boolean isLocked);

    AdminUserDetailsDto getAdminUserDetails(String userId);
    AdminUserBillingDto getAdminUserBilling(String userId);
    AdminUserProgressDto getAdminUserProgress(String userId);
    List<AdminUserSubmissionDto> getAdminUserSubmissions(String userId);
}
