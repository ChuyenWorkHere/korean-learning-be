package edu.language.kbee.controller;

import edu.language.kbee.service.AssemblyAiService;
import edu.language.kbee.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final CloudinaryService cloudinaryService;
    private final AssemblyAiService assemblyAiService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        try {
            String fileUrl = cloudinaryService.uploadAudio(file);

            response.put("url", fileUrl);
            response.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Lỗi khi tải file lên Cloudinary: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/transcribe")
    public ResponseEntity<?> autoTranscribe(@RequestBody Map<String, String> payload) {
        try {
            String audioUrl = payload.get("audioUrl");
            JsonNode result = assemblyAiService.generateTranscript(audioUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload_image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        try {
            String fileUrl = cloudinaryService.uploadImage(file);
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload_avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        try {
            String fileUrl = cloudinaryService.uploadImage(file);
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi khi tải ảnh lên Cloudinary: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}