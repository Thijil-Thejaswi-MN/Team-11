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
}
