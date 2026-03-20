package edu.language.kbee.service.impl;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.UpdateProfileRequest;
import edu.language.kbee.repository.UserRepository;
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
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private final CloudinaryService cloudinaryService;

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
        return students.stream().map(s -> {
            UserDto userDto = mapper.map(s, UserDto.class);
            userDto.setPremium(false);

            return userDto;
        }).toList();
    }
}
