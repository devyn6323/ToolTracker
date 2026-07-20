package com.tooltrack.tooltrackbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tooltrack.tooltrackbackend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.util.Locale;
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
    private final boolean cloudStorage;
    private final Cloudinary cloudinary;

    public PhotoStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory,
                               @Value("${app.photo.storage:local}") String storageMode,
                               @Value("${app.cloudinary.url:}") String cloudinaryUrl) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.cloudStorage = "cloudinary".equals(storageMode.trim().toLowerCase(Locale.ROOT));
        if (!cloudStorage && !"local".equals(storageMode.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("PHOTO_STORAGE must be local or cloudinary");
        }
        if (cloudStorage && cloudinaryUrl.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_URL is required when PHOTO_STORAGE=cloudinary");
        }
        this.cloudinary = cloudStorage ? new Cloudinary(cloudinaryUrl.trim()) : null;
    }

    public String store(MultipartFile file, UUID companyId) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo file is empty");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo must be JPEG, PNG, WebP, HEIC, or HEIF");
        }
        if (cloudStorage) {
            return storeInCloud(file, companyId);
        }
        try {
            Files.createDirectories(uploadRoot);
            String filename = companyId + "-" + UUID.randomUUID() + extension;
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

    private String storeInCloud(MultipartFile file, UUID companyId) {
        String publicId = "tooltrack/companies/" + companyId + "/tools/" + UUID.randomUUID();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false));
            Object secureUrl = result.get("secure_url");
            if (!(secureUrl instanceof String url) || !url.startsWith("https://")) {
                throw new IOException("Cloud storage did not return a secure URL");
            }
            return url;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not store photo in cloud storage");
        }
    }

    public void delete(String photoUrl) {
        if (photoUrl == null) return;
        if (photoUrl.startsWith("https://res.cloudinary.com/")) {
            deleteFromCloud(photoUrl);
            return;
        }
        if (!photoUrl.startsWith("/uploads/")) return;
        Path target = uploadRoot.resolve(photoUrl.substring("/uploads/".length())).normalize();
        if (!target.getParent().equals(uploadRoot)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Account deletion must continue even when a stale local file is already unavailable.
        }
    }

    public void deleteOwned(String photoUrl, UUID companyId) {
        if (!isOwnedBy(photoUrl, companyId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Photo does not belong to this company");
        }
        delete(photoUrl);
    }

    public void requireOwned(String photoUrl, UUID companyId) {
        if (photoUrl != null && !isOwnedBy(photoUrl, companyId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo was not uploaded by this company");
        }
    }

    private boolean isOwnedBy(String photoUrl, UUID companyId) {
        if (photoUrl == null) return false;
        String folder = "tooltrack/companies/" + companyId + "/tools/";
        if (photoUrl.startsWith("https://res.cloudinary.com/")) {
            try {
                String path = URI.create(photoUrl).getPath();
                int upload = path.indexOf("/upload/");
                if (upload < 0) return false;
                return path.substring(upload + "/upload/".length()).replaceFirst("^v\\d+/", "").startsWith(folder);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return photoUrl.startsWith("/uploads/" + companyId + "-");
    }

    private void deleteFromCloud(String photoUrl) {
        if (cloudinary == null) return;
        try {
            String path = URI.create(photoUrl).getPath();
            int upload = path.indexOf("/upload/");
            if (upload < 0) return;
            String publicId = path.substring(upload + "/upload/".length()).replaceFirst("^v\\d+/", "");
            int extension = publicId.lastIndexOf('.');
            if (extension > publicId.lastIndexOf('/')) publicId = publicId.substring(0, extension);
            if (!publicId.startsWith("tooltrack/tools/") && !publicId.startsWith("tooltrack/companies/")) return;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception ignored) {
            // Deletion of account data must continue if a cloud asset is already unavailable.
        }
    }
}
