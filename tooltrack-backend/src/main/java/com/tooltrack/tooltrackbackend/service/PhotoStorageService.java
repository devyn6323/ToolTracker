package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class PhotoStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/heic", ".heic",
            "image/heif", ".heif"
    );
    private final Path uploadRoot;

    public PhotoStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo file is empty");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo must be JPEG, PNG, WebP, HEIC, or HEIF");
        }
        try {
            Files.createDirectories(uploadRoot);
            String filename = UUID.randomUUID() + extension;
            Path destination = uploadRoot.resolve(filename).normalize();
            if (!destination.getParent().equals(uploadRoot)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid upload path");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store photo");
        }
    }

    public void delete(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith("/uploads/")) return;
        Path target = uploadRoot.resolve(photoUrl.substring("/uploads/".length())).normalize();
        if (!target.getParent().equals(uploadRoot)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Account deletion must continue even when a stale local file is already unavailable.
        }
    }
}
