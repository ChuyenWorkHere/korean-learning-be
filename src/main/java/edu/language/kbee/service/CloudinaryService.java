package edu.language.kbee.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {

    String uploadAudio(MultipartFile file) throws IOException;

    String uploadImage(MultipartFile file) throws IOException;

    void deleteFile(String publicId) throws IOException;
}
