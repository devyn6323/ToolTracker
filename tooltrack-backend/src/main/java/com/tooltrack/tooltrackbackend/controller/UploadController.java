package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.PhotoUploadResponse;
import com.tooltrack.tooltrackbackend.dto.PhotoDeleteRequest;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.service.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
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
    public PhotoUploadResponse uploadToolPhoto(@RequestPart("photo") MultipartFile photo,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return new PhotoUploadResponse(photoStorageService.store(photo, principal.companyId()));
    }

    @DeleteMapping("/tool-photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public void deleteUnusedToolPhoto(@Valid @RequestBody PhotoDeleteRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        photoStorageService.deleteOwned(request.url(), principal.companyId());
    }
}
