package com.tooltrack.tooltrackbackend.controller;

import com.tooltrack.tooltrackbackend.dto.CheckoutRequest;
import com.tooltrack.tooltrackbackend.dto.ReturnRequest;
import com.tooltrack.tooltrackbackend.dto.ToolRequest;
import com.tooltrack.tooltrackbackend.dto.ToolResponse;
import com.tooltrack.tooltrackbackend.dto.TransactionResponse;
import com.tooltrack.tooltrackbackend.dto.TransferRequest;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import com.tooltrack.tooltrackbackend.service.ToolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {
    private final ToolService toolService;

    @GetMapping
    public List<ToolResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return toolService.list(principal);
    }

    @GetMapping("/my-tools")
    public List<ToolResponse> myTools(@AuthenticationPrincipal UserPrincipal principal) {
        return toolService.myTools(principal);
    }

    @GetMapping("/by-qr/{qrCodeValue}")
    public ToolResponse byQr(@PathVariable String qrCodeValue, @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.getByQr(qrCodeValue, principal);
    }

    @GetMapping("/{id}")
    public ToolResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.get(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ToolResponse create(@Valid @RequestBody ToolRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.create(request, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ToolResponse update(@PathVariable UUID id, @Valid @RequestBody ToolRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.update(id, request, principal);
    }

    @PostMapping("/{id}/checkout")
    public TransactionResponse checkout(@PathVariable UUID id, @Valid @RequestBody CheckoutRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.checkout(id, request, principal);
    }

    @PostMapping("/{id}/return")
    public TransactionResponse returnTool(@PathVariable UUID id, @Valid @RequestBody ReturnRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.returnTool(id, request, principal);
    }

    @PostMapping("/{id}/transfer")
    public TransactionResponse transfer(@PathVariable UUID id, @Valid @RequestBody TransferRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.transfer(id, request, principal);
    }

    @GetMapping("/{id}/history")
    public List<TransactionResponse> history(@PathVariable UUID id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return toolService.history(id, principal);
    }
}
