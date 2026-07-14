package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.PhotoUploadResponse;
import com.tooltrack.tooltrackbackend.service.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final PhotoStorageService photoStorageService;

    @PostMapping("/tool-photo")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public PhotoUploadResponse uploadToolPhoto(@RequestPart("photo") MultipartFile photo) {
        return new PhotoUploadResponse(photoStorageService.store(photo));
    }
}
