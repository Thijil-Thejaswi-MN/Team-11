package com.team11.insurance.kyc.service;

import com.team11.insurance.kyc.IKycStatus;
import com.team11.insurance.kyc.KycStatus;
import com.team11.insurance.kyc.entity.KycRecord;
import com.team11.insurance.kyc.repository.KycRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Production implementation of {@link IKycStatus} backed by persistent database storage (INS-F-002).
 */
@Service
@Transactional
public class KycService implements IKycStatus {

    private final KycRepository kycRepository;

    public KycService(KycRepository kycRepository) {
        this.kycRepository = kycRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(long userId) {
        if (userId <= 0) {
            return false;
        }
        return kycRepository.findByUserId(userId)
                .map(r -> r.getStatus() == KycStatus.VERIFIED)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatus getStatus(long userId) {
        if (userId <= 0) {
            return KycStatus.NOT_SUBMITTED;
        }
        return kycRepository.findByUserId(userId)
                .map(KycRecord::getStatus)
                .orElse(KycStatus.NOT_SUBMITTED);
    }

    public KycRecord submitKyc(long userId, String panNumber, String aadhaarNumber, String documentPath) {
        Optional<KycRecord> existing = kycRepository.findByUserId(userId);
        KycRecord record = existing.orElseGet(() -> new KycRecord(userId, panNumber, aadhaarNumber, KycStatus.PENDING));

        record.setPanNumber(panNumber);
        record.setAadhaarNumber(aadhaarNumber);
        record.setDocumentPath(documentPath);
        record.setStatus(KycStatus.PENDING);

        return kycRepository.save(record);
    }

    public KycRecord updateStatus(long userId, KycStatus status) {
        KycRecord record = kycRepository.findByUserId(userId)
                .orElseGet(() -> new KycRecord(userId, null, null, status));
        record.setStatus(status);
        return kycRepository.save(record);
    }
}
