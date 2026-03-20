package edu.language.kbee.service;

import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.UpdateProfileRequest;

import java.util.List;

public interface UserService {
    UserDto updateProfile(UpdateProfileRequest request);

    void updateUserStreak(String userId);

    List<UserDto> findAllStudents();
}
