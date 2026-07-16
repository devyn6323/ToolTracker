package com.tooltrack.tooltrackbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ToolWorkflowIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

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
