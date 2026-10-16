package com.team11.insurance.kyc;

import com.team11.insurance.kyc.entity.KycRecord;
import com.team11.insurance.kyc.repository.KycRepository;
import com.team11.insurance.kyc.service.KycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("P2-05 / P2-02 IKycStatus Query and Verification Tests")
class KycServiceTest {

    @Autowired
    private KycRepository kycRepository;

    private KycService kycService;

    @BeforeEach
    void setUp() {
        kycService = new KycService(kycRepository);
    }

    @Test
    @DisplayName("User with no KYC record returns false for isVerified and NOT_SUBMITTED for getStatus")
    void testUnsubmittedKyc() {
        assertFalse(kycService.isVerified(9999L));
        assertEquals(KycStatus.NOT_SUBMITTED, kycService.getStatus(9999L));
    }

    @Test
    @DisplayName("User with pending KYC submission returns false for isVerified and PENDING for getStatus")
    void testPendingKycSubmission() {
        long userId = 1001L;
        kycService.submitKyc(userId, "ABCDE1234F", "123456789012", "docs/test.pdf");

        assertFalse(kycService.isVerified(userId));
        assertEquals(KycStatus.PENDING, kycService.getStatus(userId));
    }

    @Test
    @DisplayName("Verified customer returns true for isVerified and VERIFIED for getStatus with verifiedAt timestamp")
    void testVerifiedCustomer() {
        long userId = 2001L;
        kycService.submitKyc(userId, "XYZPA5678K", "987654321098", "docs/kyc.pdf");
        kycService.updateStatus(userId, KycStatus.VERIFIED);

        assertTrue(kycService.isVerified(userId));
        assertEquals(KycStatus.VERIFIED, kycService.getStatus(userId));

        KycRecord record = kycRepository.findByUserId(userId).orElseThrow();
        assertNotNull(record.getVerifiedAt());
    }

    @Test
    @DisplayName("Rejected KYC returns false for isVerified and REJECTED for getStatus")
    void testRejectedCustomer() {
        long userId = 3001L;
        kycService.submitKyc(userId, "REJCT9999Z", "112233445566", null);
        kycService.updateStatus(userId, KycStatus.REJECTED);

        assertFalse(kycService.isVerified(userId));
        assertEquals(KycStatus.REJECTED, kycService.getStatus(userId));
    }

    @Test
    @DisplayName("Invalid or negative userIds return false safely")
    void testInvalidUserIds() {
        assertFalse(kycService.isVerified(0L));
        assertFalse(kycService.isVerified(-5L));
        assertEquals(KycStatus.NOT_SUBMITTED, kycService.getStatus(0L));
        assertEquals(KycStatus.NOT_SUBMITTED, kycService.getStatus(-1L));
    }
}
