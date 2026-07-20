package com.tooltrack.tooltrackbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhotoDeleteRequest(@NotBlank @Size(max = 2048) String url) {
}
