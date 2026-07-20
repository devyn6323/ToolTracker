package com.tooltrack.tooltrackbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.tooltrack.tooltrackbackend.service.GoogleIdentityService;
import com.tooltrack.tooltrackbackend.service.PasswordResetMailService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest
@AutoConfigureMockMvc
class ToolWorkflowIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleIdentityService googleIdentityService;

    @MockitoBean
    private PasswordResetMailService passwordResetMailService;

    @Test
    void ownerCanAddCheckoutReturnAndReviewDrillHistory() throws Exception {
        String registerBody = """
                {"companyName":"Demo Construction","name":"Alex Owner","email":"owner@demo.test","password":"TestPass1!"}
                """;
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        String token = value(registerResponse, "token");

        MockMultipartFile photo = new MockMultipartFile("photo", "drill.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9});
        String uploadResponse = mockMvc.perform(multipart("/api/uploads/tool-photo")
                        .file(photo).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/uploads/")))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get(value(uploadResponse, "url")))
                .andExpect(status().isOk());

        String drillBody = """
                {"assetNumber":"DRILL-001","name":"Cordless Drill","category":"Power Tools",
                 "manufacturer":"DeWalt","model":"DCD800","serialNumber":"SN-1001",
                 "condition":"GOOD","currentLocation":"Main Shop"}
                """;
        String toolResponse = mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(drillBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.qrCodeValue").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String toolId = value(toolResponse, "id");

        mockMvc.perform(post("/api/tools/{id}/checkout", toolId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobName\":\"Warehouse Remodel\",\"location\":\"Job 42\",\"expectedReturnAt\":\"2099-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("CHECKOUT"))
                .andExpect(jsonPath("$.returnedAt").doesNotExist());

        mockMvc.perform(get("/api/tools/my-tools").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(toolId))
                .andExpect(jsonPath("$[0].checkedOutTo.email").value("owner@demo.test"));

        String employeeResponse = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jamie Tech\",\"email\":\"jamie@demo.test\",\"password\":\"TestPass1!\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String employeeId = value(employeeResponse, "id");

        mockMvc.perform(post("/api/tools/{id}/transfer", toolId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":\"" + employeeId + "\",\"location\":\"Job 42 - East Wing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.user.email").value("jamie@demo.test"));

        mockMvc.perform(get("/api/tools/{id}", toolId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedOutTo.email").value("jamie@demo.test"));

        mockMvc.perform(post("/api/tools/{id}/return", toolId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conditionAtReturn\":\"GOOD\",\"location\":\"Main Shop\",\"notes\":\"Battery included\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").isNotEmpty())
                .andExpect(jsonPath("$.conditionAtReturn").value("GOOD"));

        mockMvc.perform(get("/api/tools/{id}/history", toolId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobName").value("Warehouse Remodel"))
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$[0].returnedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].notes").value("Return: Battery included"));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.AVAILABLE").value(1))
                .andExpect(jsonPath("$.recentActivity[0].toolName").value("Cordless Drill"));

        mockMvc.perform(get("/api/activity").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$[0].assetNumber").value("DRILL-001"));
    }

    @Test
    void ownerCanCheckoutMultipleToolsWithSharedJobDetails() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Batch Company\",\"name\":\"Batch Owner\",\"email\":\"batch-owner@demo.test\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = value(registerResponse, "token");

        String first = mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"BATCH-001\",\"name\":\"First Tool\",\"condition\":\"GOOD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"BATCH-002\",\"name\":\"Second Tool\",\"condition\":\"FAIR\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String body = "{\"toolIds\":[\"" + value(first, "id") + "\",\"" + value(second, "id")
                + "\"],\"jobName\":\"Shared Job\",\"location\":\"North Site\",\"expectedReturnAt\":\"2099-01-01T00:00:00Z\"}";
        mockMvc.perform(post("/api/tools/checkout/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jobName").value("Shared Job"))
                .andExpect(jsonPath("$[1].location").value("North Site"));

        mockMvc.perform(get("/api/tools/my-tools").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void batchCheckoutDoesNotPartiallyCheckoutWhenOneToolIsUnavailable() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Atomic Batch Company\",\"name\":\"Batch Owner\",\"email\":\"atomic-batch@demo.test\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = value(registerResponse, "token");

        String available = mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"ATOMIC-001\",\"name\":\"Available Tool\",\"condition\":\"GOOD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String unavailable = mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"ATOMIC-002\",\"name\":\"Checked Out Tool\",\"condition\":\"GOOD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String availableId = value(available, "id");
        String unavailableId = value(unavailable, "id");

        mockMvc.perform(post("/api/tools/{id}/checkout", unavailableId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobName\":\"Existing Job\",\"expectedReturnAt\":\"2099-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk());

        String batchBody = "{\"toolIds\":[\"" + availableId + "\",\"" + unavailableId
                + "\"],\"jobName\":\"New Job\",\"expectedReturnAt\":\"2099-01-02T00:00:00Z\"}";
        mockMvc.perform(post("/api/tools/checkout/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(batchBody))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/tools/{id}", availableId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.checkedOutTo").doesNotExist());
    }

    @Test
    void registrationRequiresStrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Weak Password Co\",\"name\":\"Owner\",\"email\":\"weak@demo.test\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(org.hamcrest.Matchers.containsString("uppercase")));
    }

    @Test
    void verifiedGoogleIdentityLinksInvitedEmployeeOrCreatesOwnerCompany() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Google Link Company\",\"name\":\"Owner\",\"email\":\"google-link-owner@demo.test\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ownerToken = value(registerResponse, "token");
        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Google Employee\",\"email\":\"google-employee@demo.test\",\"password\":\"TestPass1!\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated());

        when(googleIdentityService.verify("employee-google-token")).thenReturn(
                new GoogleIdentityService.GoogleIdentity("google-subject-employee", "google-employee@demo.test", "Google Employee"));
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"employee-google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingRequired").value(false))
                .andExpect(jsonPath("$.session.user.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.session.passwordLoginEnabled").value(false))
                .andExpect(jsonPath("$.session.passwordChangeRequired").value(false));

        when(googleIdentityService.verify("owner-google-token")).thenReturn(
                new GoogleIdentityService.GoogleIdentity("google-subject-owner", "new-google-owner@demo.test", "New Google Owner"));
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"owner-google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingRequired").value(true))
                .andExpect(jsonPath("$.email").value("new-google-owner@demo.test"));
        String googleOwnerResponse = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"owner-google-token\",\"companyName\":\"Google Created Company\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingRequired").value(false))
                .andExpect(jsonPath("$.session.companyName").value("Google Created Company"))
                .andExpect(jsonPath("$.session.user.role").value("OWNER"))
                .andExpect(jsonPath("$.session.passwordLoginEnabled").value(false))
                .andReturn().getResponse().getContentAsString();

        when(googleIdentityService.verify("owner-deletion-token")).thenReturn(
                new GoogleIdentityService.GoogleIdentity("google-subject-owner", "new-google-owner@demo.test", "New Google Owner"));
        mockMvc.perform(delete("/api/auth/account")
                        .header("Authorization", "Bearer " + value(googleOwnerResponse, "token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"googleIdToken\":\"owner-deletion-token\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void employeeMustReplaceTemporaryPasswordAndOldSessionIsRevoked() throws Exception {
        String ownerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Password Change Company\",\"name\":\"Owner\",\"email\":\"password-owner@demo.test\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + value(ownerResponse, "token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Employee\",\"email\":\"temporary-employee@demo.test\",\"password\":\"Temporary1!\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"temporary-employee@demo.test\",\"password\":\"Temporary1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andReturn().getResponse().getContentAsString();
        String oldToken = value(loginResponse, "token");
        mockMvc.perform(get("/api/tools").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Temporary1!\",\"newPassword\":\"PrivatePass2!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false));
        mockMvc.perform(get("/api/tools").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"temporary-employee@demo.test\",\"password\":\"PrivatePass2!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordResetCodeIsSingleUseAndRevokesExistingSessions() throws Exception {
        String email = "reset-owner@demo.test";
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Reset Company\",\"name\":\"Reset Owner\",\"email\":\"" + email + "\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String oldToken = value(registerResponse, "token");
        AtomicReference<String> resetCode = new AtomicReference<>();
        doAnswer(invocation -> { resetCode.set(invocation.getArgument(2)); return null; })
                .when(passwordResetMailService).sendResetCode(org.mockito.ArgumentMatchers.eq(email),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
        org.assertj.core.api.Assertions.assertThat(resetCode.get()).matches("\\d{8}");
        String resetBody = "{\"email\":\"" + email + "\",\"code\":\"" + resetCode.get()
                + "\",\"newPassword\":\"ResetPass2!\"}";
        mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(resetBody))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON).content(resetBody))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tools").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"ResetPass2!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanTransferCompanyOwnershipWithReauthentication() throws Exception {
        String ownerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Transfer Owner Company\",\"name\":\"Old Owner\",\"email\":\"old-owner@demo.test\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ownerToken = value(ownerResponse, "token");
        String employeeResponse = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Future Owner\",\"email\":\"future-owner@demo.test\",\"password\":\"Temporary1!\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        mockMvc.perform(put("/api/auth/ownership/{id}", value(employeeResponse, "id"))
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"TestPass1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("MANAGER"));
        mockMvc.perform(get("/api/tools").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"future-owner@demo.test\",\"password\":\"Temporary1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("OWNER"));
    }

    @Test
    void toolPhotosAreCompanyScopedAndRemovedAfterReplacementOrCancellation() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Photo Lifecycle Company\",\"name\":\"Photo Owner\",\"email\":\"photo-owner@demo.test\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = value(registerResponse, "token");
        MockMultipartFile photo = new MockMultipartFile("photo", "tool.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9});
        String uploadResponse = mockMvc.perform(multipart("/api/uploads/tool-photo").file(photo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String photoUrl = value(uploadResponse, "url");

        String toolResponse = mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"PHOTO-001\",\"name\":\"Photo Tool\",\"condition\":\"GOOD\",\"photoUrl\":\"" + photoUrl + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").value(photoUrl))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get(photoUrl)).andExpect(status().isOk());
        mockMvc.perform(put("/api/tools/{id}", value(toolResponse, "id"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"PHOTO-001\",\"name\":\"Photo Tool\",\"condition\":\"GOOD\",\"photoUrl\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
        mockMvc.perform(get(photoUrl)).andExpect(status().isNotFound());

        String unusedResponse = mockMvc.perform(multipart("/api/uploads/tool-photo").file(photo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String unusedUrl = value(unusedResponse, "url");
        mockMvc.perform(delete("/api/uploads/tool-photo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + unusedUrl + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(unusedUrl)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/tools")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetNumber\":\"PHOTO-002\",\"name\":\"Foreign Photo\",\"condition\":\"GOOD\",\"photoUrl\":\"/uploads/not-this-company.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCanPermanentlyDeleteCompanyAccount() throws Exception {
        String email = "delete-owner@demo.test";
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Delete Me\",\"name\":\"Owner\",\"email\":\"" + email + "\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = value(registerResponse, "token");

        mockMvc.perform(delete("/api/auth/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"TestPass1!\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tools").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailedLoginsTemporarilyLockAccount() throws Exception {
        String email = "lockout-owner@demo.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Lockout Test\",\"name\":\"Owner\",\"email\":\"" + email + "\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isCreated());

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"TestPass1!\"}"))
                .andExpect(status().isTooManyRequests());
    }

    private String value(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        return json.substring(start, json.indexOf('"', start));
    }
}
