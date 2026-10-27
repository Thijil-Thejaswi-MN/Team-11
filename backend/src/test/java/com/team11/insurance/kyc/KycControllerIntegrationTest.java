package com.team11.insurance.kyc;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.entity.KycRecord;
import com.team11.insurance.kyc.repository.KycRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.team11.insurance.kyc.service.KycService;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2-05 / INS-F-002 KycController REST Integration & Security Tests")
class KycControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private KycService kycService;

    @BeforeEach
    void setUp() {
        kycRepository.deleteAll();
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(777L, UserRole.CUSTOMER, null));
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("GET /kyc/status for unsubmitted customer returns 200 OK with 'Not Submitted'")
    void testGetStatusUnsubmitted() throws Exception {
        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Not Submitted"));
    }

    @Test
    @DisplayName("GET /kyc/status for verified customer returns 200 OK with 'Verified'")
    void testGetStatusVerifiedCustomer() throws Exception {
        KycRecord record = new KycRecord(777L, "ABCDE1234F", "123456789012", KycStatus.VERIFIED);
        kycRepository.saveAndFlush(record);

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Verified"));
    }

    @Test
    @DisplayName("Finding 3: Unauthenticated caller to GET /kyc/status is rejected with 401 Unauthorized")
    void testGetStatusUnauthenticatedRejected() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }

    @Test
    @DisplayName("Finding 3: INSURER_ADMIN caller to GET /kyc/status is rejected with 403 Forbidden")
    void testGetStatusInsurerAdminRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(101L, UserRole.INSURER_ADMIN, 1L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: UNDERWRITER caller to GET /kyc/status is rejected with 403 Forbidden")
    void testGetStatusUnderwriterRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(102L, UserRole.UNDERWRITER, 1L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: CLAIMS_OFFICER caller to GET /kyc/status is rejected with 403 Forbidden")
    void testGetStatusClaimsOfficerRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(103L, UserRole.CLAIMS_OFFICER, 1L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: SYSTEM_ADMIN caller to GET /kyc/status is rejected under current contract with 403 Forbidden")
    void testGetStatusSystemAdminRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(1L, UserRole.SYSTEM_ADMIN, null));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: Client cannot retrieve another customer's KYC status by supplying alternative user ID")
    void testCrossCustomerAccessDeniedViaAlternativeId() throws Exception {
        // Customer 777 has PENDING status
        kycRepository.saveAndFlush(new KycRecord(777L, "ABCDE1234F", "123456789012", KycStatus.PENDING));

        // Victim customer 888 has VERIFIED status
        kycRepository.saveAndFlush(new KycRecord(888L, "XYZPA5678K", "987654321098", KycStatus.VERIFIED));

        // Attacker Customer 777 tries to pass userId=888 in query param and header
        mockMvc.perform(get("/kyc/status")
                        .param("userId", "888")
                        .header("X-User-Id", "888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Pending")); // Strictly returns authenticated customer's own status (777), not 888!
    }

    @Test
    @DisplayName("TC-KYC-01 Step 1-2: Valid PAN upload returns status Verified and updates customer record")
    void testPostKyc_VerifiedPan() throws Exception {
        MockMultipartFile validPdf = new MockMultipartFile(
                "files[]",
                "identity_doc.pdf",
                "application/pdf",
                "%PDF-1.4 sample identity document content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/kyc")
                        .file(validPdf)
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Verified"));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Verified"));

        assertTrue(kycService.isVerified(777L));
    }

    @Test
    @DisplayName("TC-KYC-01 Step 3: PAN 'ZZZZZ0000Z' returns status Rejected via mock provider")
    void testPostKyc_RejectedPan_ZZZZZ0000Z() throws Exception {
        MockMultipartFile validPdf = new MockMultipartFile(
                "files",
                "kyc_pan_doc.pdf",
                "application/pdf",
                "%PDF-1.4 sample document".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/kyc")
                        .file(validPdf)
                        .param("panNumber", "ZZZZZ0000Z")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Rejected"));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Rejected"));

        assertFalse(kycService.isVerified(777L));
    }

    @Test
    @DisplayName("TC-KYC-01: Malformed PAN fails input validation and returns 422 VALID-422")
    void testPostKyc_MalformedPan_Returns422() throws Exception {
        MockMultipartFile validPdf = new MockMultipartFile(
                "files",
                "doc.pdf",
                "application/pdf",
                "%PDF-1.4 sample document".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/kyc")
                        .file(validPdf)
                        .param("panNumber", "INVALID123")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }

    @Test
    @DisplayName("TC-Sec-07 Step 3: Upload exceeding 5 MB limit returns 413 Payload Too Large (FILE-413)")
    void testPostKyc_FileSizeOver5Mb_Returns413() throws Exception {
        byte[] largeBytes = new byte[6 * 1024 * 1024];
        byte[] pdfHeader = "%PDF-".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(pdfHeader, 0, largeBytes, 0, pdfHeader.length);

        MockMultipartFile largePdf = new MockMultipartFile(
                "files",
                "large_document.pdf",
                "application/pdf",
                largeBytes
        );

        mockMvc.perform(multipart("/kyc")
                        .file(largePdf)
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.errorCode").value("FILE-413"));
    }

    @Test
    @DisplayName("TC-Sec-07 Step 2: Uploading an executable file (.exe) returns 415 Unsupported Media Type (FILE-415)")
    void testPostKyc_ExecutableFile_Returns415() throws Exception {
        byte[] exeBytes = new byte[]{'M', 'Z', 0x00, 0x00, 0x01, 0x02};
        MockMultipartFile exeFile = new MockMultipartFile(
                "files",
                "dangerous.exe",
                "application/x-msdownload",
                exeBytes
        );

        mockMvc.perform(multipart("/kyc")
                        .file(exeFile)
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("FILE-415"));
    }

    @Test
    @DisplayName("TC-KYC-01: Non-customer role is rejected with 403 Forbidden (ACCESS-403)")
    void testPostKyc_NonCustomerRole_Returns403() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(2001L, UserRole.INSURER_ADMIN, 1L));

        MockMultipartFile validPdf = new MockMultipartFile(
                "files",
                "doc.pdf",
                "application/pdf",
                "%PDF-1.4 sample document".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/kyc")
                        .file(validPdf)
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("TC-KYC-01 Step 5: A rejected customer may resubmit, and the latest record wins (Verified)")
    void testPostKyc_ResubmissionAfterRejection() throws Exception {
        MockMultipartFile validPdf = new MockMultipartFile(
                "files[]",
                "doc.pdf",
                "application/pdf",
                "%PDF-1.4 sample document".getBytes(StandardCharsets.UTF_8)
        );

        // First attempt with rejected PAN
        mockMvc.perform(multipart("/kyc")
                        .file(validPdf)
                        .param("panNumber", "ZZZZZ0000Z")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Rejected"));

        assertFalse(kycService.isVerified(777L));

        // Customer resubmits with valid PAN
        mockMvc.perform(multipart("/kyc")
                        .file(validPdf)
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Verified"));

        assertTrue(kycService.isVerified(777L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Verified"));
    }
}
