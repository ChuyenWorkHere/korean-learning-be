package edu.language.kbee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import edu.language.kbee.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadAudio(MultipartFile file) throws IOException {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", "kbee_audio_files",
                    "resource_type", "auto"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new IOException("Lỗi khi upload lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", "kbee_image_files",
                    "resource_type", "auto",
                    "timestamp", System.currentTimeMillis()
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new IOException("Lỗi khi upload lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
