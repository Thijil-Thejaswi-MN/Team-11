package com.team11.insurance.kyc.controller;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.KycStatus;
import com.team11.insurance.kyc.dto.KycStatusResponse;
import com.team11.insurance.kyc.service.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.common.logging.SensitiveDataMasker;
import com.team11.insurance.filestore.DocumentCategory;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.kyc.entity.KycRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * REST controller for KYC verification endpoints (INS-F-002, SAD §4.3.1, API Contract §3).
 * <p>
 * Base path: {@code /kyc} (serviced under servlet context-path {@code /api/v1}).
 */
@RestController
@RequestMapping("/kyc")
public class KycController {

    private static final Logger log = LoggerFactory.getLogger(KycController.class);
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final String REJECTED_SIMULATION_PAN = "ZZZZZ0000Z";

    private final KycService kycService;
    private final IFileStore fileStore;
    private final com.team11.insurance.admin.IAuditLog auditLog;

    @org.springframework.beans.factory.annotation.Autowired
    public KycController(KycService kycService, IFileStore fileStore, com.team11.insurance.admin.IAuditLog auditLog) {
        this.kycService = kycService;
        this.fileStore = fileStore;
        this.auditLog = auditLog;
    }

    public KycController(KycService kycService, IFileStore fileStore) {
        this(kycService, fileStore, null);
    }

    /**
     * Retrieves the authenticated customer's KYC verification status (API Contract 02 §3.2).
     * Strictly restricted to CUSTOMER role; staff and system admins are prohibited.
     * Derives identity strictly from trusted server-side authenticated CurrentUser context.
     */
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        KycStatus status = kycService.getStatus(currentUser.getUserId());
        return ResponseEntity.ok(new KycStatusResponse(status.getDisplayValue()));
    }

    /**
     * Submits KYC details and document files (API Contract 02 §3.1, INS-F-002, INS-SR-007).
     * Role CUSTOMER only; userId comes from CurrentUser, never from the request.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycStatusResponse> uploadKyc(
            CurrentUser currentUser,
            @RequestParam("panNumber") String panNumber,
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam(value = "files[]", required = false) List<MultipartFile> filesArray,
            @RequestParam(value = "files", required = false) List<MultipartFile> filesPlain,
            @RequestParam(value = "file", required = false) MultipartFile singleFile) {

        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        if (panNumber == null || panNumber.trim().isEmpty()) {
            throw new ValidationException("PAN number is required.");
        }
        String cleanPan = panNumber.trim().toUpperCase();
        if (!PAN_PATTERN.matcher(cleanPan).matches()) {
            throw new ValidationException("Malformed PAN number: must be 5 letters followed by 4 digits and 1 letter.");
        }

        if (aadhaarNumber == null || aadhaarNumber.trim().isEmpty()) {
            throw new ValidationException("Aadhaar number is required.");
        }
        String cleanAadhaar = aadhaarNumber.trim();

        List<MultipartFile> allFiles = new ArrayList<>();
        if (filesArray != null) allFiles.addAll(filesArray);
        if (filesPlain != null) allFiles.addAll(filesPlain);
        if (singleFile != null) allFiles.add(singleFile);

        if (allFiles.isEmpty()) {
            throw new ValidationException("At least one KYC document file is required.");
        }

        List<Long> storedDocIds = new ArrayList<>();
        for (MultipartFile f : allFiles) {
            Long docId = fileStore.put(f, DocumentCategory.KYC, currentUser);
            storedDocIds.add(docId);
        }
        String documentPath = storedDocIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // Simulated KYC provider (SRS assumption A-01):
        // PAN "ZZZZZ0000Z" -> Rejected; any other well-formed PAN -> Verified
        KycStatus status;
        if (REJECTED_SIMULATION_PAN.equals(cleanPan)) {
            status = KycStatus.REJECTED;
        } else {
            status = KycStatus.VERIFIED;
        }

        log.info("Customer {} submitted KYC with PAN {} and Aadhaar {}: simulated status is {}",
                currentUser.getUserId(),
                SensitiveDataMasker.maskPan(cleanPan),
                SensitiveDataMasker.maskAadhaar(cleanAadhaar),
                status);

        KycRecord record = kycService.submitKyc(currentUser.getUserId(), cleanPan, cleanAadhaar, documentPath, status);

        if (auditLog != null && currentUser != null) {
            auditLog.record(currentUser.getUserId(), "KYC_SUBMITTED", "KycRecord", record.getId(),
                    "KYC submitted: PAN " + SensitiveDataMasker.maskPan(cleanPan) + ", Aadhaar " + SensitiveDataMasker.maskAadhaar(cleanAadhaar));
            auditLog.record(currentUser.getUserId(), "KYC_VERIFICATION_RESULT", "KycRecord", record.getId(),
                    "KYC verification result: " + record.getStatus());
        }

        return ResponseEntity.ok(new KycStatusResponse(record.getStatus().getDisplayValue()));
    }
}
