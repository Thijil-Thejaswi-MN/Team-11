package com.team11.insurance.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.dto.CreateInsurerRequest;
import com.team11.insurance.admin.dto.CreateUserRequest;
import com.team11.insurance.admin.entity.Insurer;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.auth.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2-05 / SAD §4.3.6 AdminController REST Integration Tests")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InsurerRepository insurerRepository;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_SYSTEM_ADMIN);
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN creates insurer successfully (201 Created)")
    void testCreateInsurerSuccess() throws Exception {
        CreateInsurerRequest req = new CreateInsurerRequest("Apex Mutual Life");

        mockMvc.perform(post("/admin/insurers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("Duplicate insurer name returns 409 Conflict")
    void testDuplicateInsurerReturns409() throws Exception {
        CreateInsurerRequest req = new CreateInsurerRequest("Unique Mutual");
        mockMvc.perform(post("/admin/insurers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/insurers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE-409"));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN creates underwriter staff with valid insurerId (201 Created)")
    void testCreateUnderwriterSuccess() throws Exception {
        Insurer insurer = insurerRepository.save(new Insurer("United Shield"));

        CreateUserRequest staffReq = new CreateUserRequest(
                "Staff Underwriter",
                "uw.staff@united.invalid",
                "1231231234",
                "StaffPassword123!",
                UserRole.UNDERWRITER,
                insurer.getId()
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("Staff user without insurerId is rejected with 422 Unprocessable Entity (SAD §4.3.6)")
    void testStaffWithoutInsurerRejected422() throws Exception {
        CreateUserRequest invalidStaff = new CreateUserRequest(
                "No Insurer Staff",
                "noinsurer@test.invalid",
                "1231231234",
                "StaffPassword123!",
                UserRole.UNDERWRITER,
                null // Missing insurerId for staff role!
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStaff)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }

    @Test
    @DisplayName("Non-admin caller (CUSTOMER) is denied access with 403 Forbidden")
    void testCustomerDeniedAdminAccess() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        CreateInsurerRequest req = new CreateInsurerRequest("Denied Insurer");

        mockMvc.perform(post("/admin/insurers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("TC-Acc-02: Duplicate user email returns 409 Conflict")
    void testDuplicateUserEmailReturns409() throws Exception {
        Insurer insurer = insurerRepository.save(new Insurer("Dupe Shield"));

        CreateUserRequest req1 = new CreateUserRequest(
                "First Staff",
                "dupe.staff@shield.invalid",
                "9876543210",
                "StaffPass123!",
                UserRole.UNDERWRITER,
                insurer.getId()
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateUserRequest req2 = new CreateUserRequest(
                "Second Staff",
                "dupe.staff@shield.invalid",
                "9876543211",
                "StaffPass123!",
                UserRole.CLAIMS_OFFICER,
                insurer.getId()
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE-409"));
    }

    @Test
    @DisplayName("TC-Acc-02: Staff with non-existent insurerId is rejected with 422 Unprocessable Entity")
    void testStaffWithNonExistentInsurerIdRejected422() throws Exception {
        CreateUserRequest invalidStaff = new CreateUserRequest(
                "Bad Insurer Staff",
                "badinsurer@test.invalid",
                "1231231234",
                "StaffPassword123!",
                UserRole.INSURER_ADMIN,
                999999L // Non-existent insurerId
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStaff)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }

    @Test
    @DisplayName("Non-staff user (CUSTOMER) can be created by SYSTEM_ADMIN without insurerId (201 Created)")
    void testNonStaffUserCreatedWithoutInsurerIdSuccess() throws Exception {
        CreateUserRequest customerReq = new CreateUserRequest(
                "Admin Created Customer",
                "admin.created.cust@test.invalid",
                "1234567890",
                "CustomerPass123!",
                UserRole.CUSTOMER,
                null // Non-staff does not require insurerId
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("TC-Acc-02: Non-admin caller (CUSTOMER) is denied POST /admin/users with 403 Forbidden")
    void testCustomerDeniedUserCreationAccess() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        CreateUserRequest req = new CreateUserRequest(
                "Unauthorized User",
                "unauth@test.invalid",
                "1112223333",
                "Password123!",
                UserRole.CUSTOMER,
                null
        );

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }
}
