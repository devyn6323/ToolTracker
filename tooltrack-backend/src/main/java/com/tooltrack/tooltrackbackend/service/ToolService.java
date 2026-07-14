package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.dto.CheckoutRequest;
import com.tooltrack.tooltrackbackend.dto.ReturnRequest;
import com.tooltrack.tooltrackbackend.dto.ToolRequest;
import com.tooltrack.tooltrackbackend.dto.ToolResponse;
import com.tooltrack.tooltrackbackend.dto.TransactionResponse;
import com.tooltrack.tooltrackbackend.dto.TransferRequest;
import com.tooltrack.tooltrackbackend.dto.UserSummary;
import com.tooltrack.tooltrackbackend.exception.ApiException;
import com.tooltrack.tooltrackbackend.model.AppUser;
import com.tooltrack.tooltrackbackend.model.ToolCondition;
import com.tooltrack.tooltrackbackend.model.ToolItem;
import com.tooltrack.tooltrackbackend.model.ToolStatus;
import com.tooltrack.tooltrackbackend.model.ToolTransaction;
import com.tooltrack.tooltrackbackend.model.TransactionType;
import com.tooltrack.tooltrackbackend.repository.CompanyRepository;
import com.tooltrack.tooltrackbackend.repository.ToolRepository;
import com.tooltrack.tooltrackbackend.repository.ToolTransactionRepository;
import com.tooltrack.tooltrackbackend.repository.UserRepository;
import com.tooltrack.tooltrackbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToolService {
    private final ToolRepository toolRepository;
    private final ToolTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<ToolResponse> list(UserPrincipal principal) {
        return toolRepository.findAllByCompanyIdOrderByName(principal.companyId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ToolResponse get(UUID id, UserPrincipal principal) {
        return toResponse(findTool(id, principal));
    }

    @Transactional(readOnly = true)
    public ToolResponse getByQr(String qrCodeValue, UserPrincipal principal) {
        ToolItem tool = toolRepository.findByQrCodeValueAndCompanyId(qrCodeValue, principal.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tool not found"));
        return toResponse(tool);
    }

    @Transactional
    public ToolResponse create(ToolRequest request, UserPrincipal principal) {
        String assetNumber = request.assetNumber().trim();
        if (toolRepository.existsByCompanyIdAndAssetNumberIgnoreCase(principal.companyId(), assetNumber)) {
            throw new ApiException(HttpStatus.CONFLICT, "Asset number already exists in this company");
        }
        ToolItem tool = new ToolItem();
        tool.setCompany(companyRepository.getReferenceById(principal.companyId()));
        apply(tool, request, true);
        return toResponse(toolRepository.save(tool));
    }

    @Transactional
    public ToolResponse update(UUID id, ToolRequest request, UserPrincipal principal) {
        ToolItem tool = findTool(id, principal);
        if (!tool.getAssetNumber().equalsIgnoreCase(request.assetNumber().trim())
                && toolRepository.existsByCompanyIdAndAssetNumberIgnoreCase(principal.companyId(), request.assetNumber().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Asset number already exists in this company");
        }
        boolean openCheckout = transactionRepository.findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(id).isPresent();
        if (openCheckout && request.status() != null
                && request.status() != ToolStatus.CHECKED_OUT && request.status() != ToolStatus.OVERDUE) {
            throw new ApiException(HttpStatus.CONFLICT, "Return the tool before changing it to that status");
        }
        apply(tool, request, false);
        return toResponse(tool);
    }

    @Transactional
    public TransactionResponse checkout(UUID id, CheckoutRequest request, UserPrincipal principal) {
        ToolItem tool = findTool(id, principal);
        if (tool.getStatus() != ToolStatus.AVAILABLE
                || transactionRepository.findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(id).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Tool is not available for checkout");
        }
        AppUser user = userRepository.getReferenceById(principal.id());
        Instant now = Instant.now();
        ToolTransaction transaction = new ToolTransaction();
        transaction.setTool(tool);
        transaction.setUser(user);
        transaction.setTransactionType(TransactionType.CHECKOUT);
        transaction.setJobName(blankToNull(request.jobName()));
        transaction.setLocation(blankToNull(request.location()));
        transaction.setConditionAtCheckout(request.conditionAtCheckout() == null ? tool.getCondition() : request.conditionAtCheckout());
        transaction.setCheckedOutAt(now);
        transaction.setExpectedReturnAt(request.expectedReturnAt());
        transaction.setNotes(blankToNull(request.notes()));

        tool.setStatus(ToolStatus.CHECKED_OUT);
        if (request.location() != null && !request.location().isBlank()) {
            tool.setCurrentLocation(request.location().trim());
        }
        transactionRepository.save(transaction);
        toolRepository.save(tool);
        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse returnTool(UUID id, ReturnRequest request, UserPrincipal principal) {
        ToolItem tool = findTool(id, principal);
        ToolTransaction transaction = transactionRepository
                .findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(id)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Tool is not currently checked out"));
        boolean manager = principal.role().name().matches("OWNER|ADMIN|MANAGER");
        if (!transaction.getUser().getId().equals(principal.id()) && !manager) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the current holder or a manager can return this tool");
        }

        transaction.setReturnedAt(Instant.now());
        transaction.setConditionAtReturn(request.conditionAtReturn());
        transaction.setNotes(mergeNotes(transaction.getNotes(), request.notes()));
        tool.setCondition(request.conditionAtReturn());
        tool.setStatus(switch (request.conditionAtReturn()) {
            case DAMAGED -> ToolStatus.DAMAGED;
            case MISSING -> ToolStatus.LOST;
            default -> ToolStatus.AVAILABLE;
        });
        if (request.location() != null && !request.location().isBlank()) {
            tool.setCurrentLocation(request.location().trim());
        }
        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse transfer(UUID id, TransferRequest request, UserPrincipal principal) {
        ToolItem tool = findTool(id, principal);
        ToolTransaction current = transactionRepository
                .findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(id)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Tool is not currently checked out"));
        boolean manager = principal.role().name().matches("OWNER|ADMIN|MANAGER");
        if (!current.getUser().getId().equals(principal.id()) && !manager) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the current holder or a manager can transfer this tool");
        }
        if (current.getUser().getId().equals(request.targetUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tool is already assigned to that employee");
        }
        AppUser target = userRepository.findByIdAndCompanyId(request.targetUserId(), principal.companyId())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target employee not found"));

        Instant now = Instant.now();
        current.setReturnedAt(now);
        current.setConditionAtReturn(tool.getCondition());
        current.setNotes(mergeNotes(current.getNotes(), "Transferred to " + target.getName()));

        ToolTransaction transfer = new ToolTransaction();
        transfer.setTool(tool);
        transfer.setUser(target);
        transfer.setTransactionType(TransactionType.TRANSFER);
        transfer.setJobName(current.getJobName());
        transfer.setLocation(request.location() == null || request.location().isBlank()
                ? current.getLocation() : request.location().trim());
        transfer.setConditionAtCheckout(tool.getCondition());
        transfer.setCheckedOutAt(now);
        transfer.setExpectedReturnAt(request.expectedReturnAt() == null
                ? current.getExpectedReturnAt() : request.expectedReturnAt());
        transfer.setNotes(blankToNull(request.notes()));
        if (transfer.getLocation() != null) {
            tool.setCurrentLocation(transfer.getLocation());
        }
        tool.setStatus(ToolStatus.CHECKED_OUT);
        transactionRepository.save(transfer);
        return toTransactionResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(UUID id, UserPrincipal principal) {
        findTool(id, principal);
        return transactionRepository.findAllByToolIdOrderByCheckedOutAtDesc(id).stream()
                .map(this::toTransactionResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ToolResponse> myTools(UserPrincipal principal) {
        return transactionRepository.findAllByUserIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(principal.id()).stream()
                .filter(transaction -> transaction.getTool().getCompany().getId().equals(principal.companyId()))
                .map(ToolTransaction::getTool)
                .map(this::toResponse)
                .toList();
    }

    private ToolItem findTool(UUID id, UserPrincipal principal) {
        return toolRepository.findByIdAndCompanyId(id, principal.companyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tool not found"));
    }

    private void apply(ToolItem tool, ToolRequest request, boolean creating) {
        tool.setAssetNumber(request.assetNumber().trim());
        tool.setName(request.name().trim());
        tool.setCategory(blankToNull(request.category()));
        tool.setManufacturer(blankToNull(request.manufacturer()));
        tool.setModel(blankToNull(request.model()));
        tool.setSerialNumber(blankToNull(request.serialNumber()));
        tool.setPurchaseDate(request.purchaseDate());
        tool.setCondition(request.condition());
        if (request.status() != null) {
            tool.setStatus(request.status());
        } else if (creating) {
            tool.setStatus(ToolStatus.AVAILABLE);
        }
        tool.setCurrentLocation(blankToNull(request.currentLocation()));
        tool.setPhotoUrl(blankToNull(request.photoUrl()));
        tool.setNotes(blankToNull(request.notes()));
    }

    private ToolResponse toResponse(ToolItem tool) {
        ToolTransaction open = transactionRepository
                .findFirstByToolIdAndReturnedAtIsNullOrderByCheckedOutAtDesc(tool.getId()).orElse(null);
        ToolStatus status = tool.getStatus();
        if (open != null && open.getExpectedReturnAt() != null && open.getExpectedReturnAt().isBefore(Instant.now())) {
            status = ToolStatus.OVERDUE;
        }
        return new ToolResponse(tool.getId(), tool.getAssetNumber(), tool.getName(), tool.getCategory(),
                tool.getManufacturer(), tool.getModel(), tool.getSerialNumber(), tool.getPurchaseDate(),
                tool.getCondition(), status, tool.getCurrentLocation(), tool.getQrCodeValue(), tool.getPhotoUrl(),
                tool.getNotes(), tool.getCreatedAt(), open == null ? null : UserSummary.from(open.getUser()),
                open == null ? null : open.getExpectedReturnAt());
    }

    private TransactionResponse toTransactionResponse(ToolTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getTool().getId(),
                UserSummary.from(transaction.getUser()), transaction.getTransactionType(), transaction.getJobName(),
                transaction.getLocation(), transaction.getConditionAtCheckout(), transaction.getConditionAtReturn(),
                transaction.getCheckedOutAt(), transaction.getExpectedReturnAt(), transaction.getReturnedAt(),
                transaction.getNotes());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String mergeNotes(String checkoutNotes, String returnNotes) {
        String returned = blankToNull(returnNotes);
        if (returned == null) return checkoutNotes;
        return checkoutNotes == null ? "Return: " + returned : checkoutNotes + "\nReturn: " + returned;
    }
}
