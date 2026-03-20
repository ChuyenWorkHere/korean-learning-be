package edu.language.kbee.controller;

import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.UpdateProfileRequest;
import edu.language.kbee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/students")
    public ResponseEntity<List<UserDto>> findStudents() {
        List<UserDto> students = userService.findAllStudents();
        return ResponseEntity.ok(students);
    }

    @PutMapping("/users/me")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserDto updatedUser = userService.updateProfile(request);
        return ResponseEntity.ok(updatedUser);
    }
}
