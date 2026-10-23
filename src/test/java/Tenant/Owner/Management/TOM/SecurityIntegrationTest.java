package Tenant.Owner.Management.TOM;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@SpringBootTest(properties = "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationRequiresPhoneNumberForBothRoles() throws Exception {
        for (String role : new String[] { "OWNER", "TENANT" }) {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "name", "Registration Test User",
                            "email", "missing-phone-" + role.toLowerCase() + "-" + UUID.randomUUID() + "@example.test",
                            "password", "SecurePass123!",
                            "role", role))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("phone must not be blank"));
        }
    }

    @Test
    void tenantDetailsShowOwnerScopedLeaseAndMaintenanceInformation() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String password = "SecurePass123!";
        String ownerEmail = "tenant-details-owner-" + suffix + "@example.test";
        String otherOwnerEmail = "tenant-details-other-owner-" + suffix + "@example.test";
        String tenantEmail = "tenant-details-tenant-" + suffix + "@example.test";
        register(ownerEmail, password, "OWNER");
        register(otherOwnerEmail, password, "OWNER");
        long tenantId = register(tenantEmail, password, "TENANT");
        User tenant = userRepository.findById(tenantId).orElseThrow();
        String ownerToken = login(ownerEmail, password);
        String otherOwnerToken = login(otherOwnerEmail, password);
        String tenantToken = login(tenantEmail, password);

        String propertyResponse = mockMvc.perform(post("/api/properties")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Tenant details home",
                        "address", "2 Tenant Details Road",
                        "type", "FLAT",
                        "rentAmount", 1200,
                        "description", "Tenant details test property"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long propertyId = objectMapper.readTree(propertyResponse).get("id").asLong();
        String startDate = java.time.LocalDate.now().toString();

        mockMvc.perform(post("/api/leases")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "propertyId", propertyId,
                        "tenantCode", tenant.getTenantCode(),
                        "startDate", startDate,
                        "monthlyRent", 1200,
                        "fineGraceDays", 5,
                        "finePerDay", 0))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/maintenance")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "propertyId", propertyId,
                        "title", "Leaking tap",
                        "description", "The kitchen tap is leaking.",
                        "priority", "MEDIUM"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tenants/{tenantId}", tenantId)
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Security Test User"))
                .andExpect(jsonPath("$.email").value(tenantEmail))
                .andExpect(jsonPath("$.phone").value("5550100100"))
                .andExpect(jsonPath("$.tenantCode").value(tenant.getTenantCode()))
                .andExpect(jsonPath("$.maintenanceRequestCount").value(1))
                .andExpect(jsonPath("$.maintenanceRequestsByProperty." + propertyId).value(1))
                .andExpect(jsonPath("$.leases[0].propertyTitle").value("Tenant details home"))
                .andExpect(jsonPath("$.leases[0].startDate").value(startDate));

        mockMvc.perform(get("/api/maintenance/page")
                .param("propertyId", String.valueOf(propertyId))
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tenantId").value(tenantId));

        mockMvc.perform(get("/api/tenants/{tenantId}", tenantId)
                .header("Authorization", "Bearer " + otherOwnerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tenants/{tenantId}", tenantId)
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownersAndTenantsCanEditOnlyProfileContactFields() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String password = "SecurePass123!";
        String ownerEmail = "profile-owner-" + suffix + "@example.test";
        String tenantEmail = "profile-tenant-" + suffix + "@example.test";
        register(ownerEmail, password, "OWNER");
        long tenantId = register(tenantEmail, password, "TENANT");
        String ownerToken = login(ownerEmail, password);
        String tenantToken = login(tenantEmail, password);
        User tenant = userRepository.findById(tenantId).orElseThrow();
        String originalTenantCode = tenant.getTenantCode();

        mockMvc.perform(get("/api/profile")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.phone").value("5550100100"));
        String profileBeforeUpdate = mockMvc.perform(get("/api/profile")
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.tenantCode").value(originalTenantCode))
                .andReturn().getResponse().getContentAsString();
        String originalCreatedAt = objectMapper.readTree(profileBeforeUpdate).get("createdAt").asText();

        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Updated Tenant Name",
                        "email", "updated-" + tenantEmail,
                        "phone", "5550100200"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Tenant Name"))
                .andExpect(jsonPath("$.email").value("updated-" + tenantEmail))
                .andExpect(jsonPath("$.phone").value("5550100200"))
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.tenantCode").value(originalTenantCode))
                .andExpect(jsonPath("$.createdAt").value(originalCreatedAt));

        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Updated Owner Name",
                        "email", "updated-" + ownerEmail,
                        "phone", "5550100300"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Owner Name"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void registrationLoginAndRoleScopedEndpointsUseJwtIdentity() throws Exception {
        mockMvc.perform(get("/api/properties").param("ownerId", "999"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/properties")
                .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());

        String suffix = UUID.randomUUID().toString();
        String ownerEmail = "security-owner-" + suffix + "@example.test";
        String password = "SecurePass123!";
        long ownerId = register(ownerEmail, password, "OWNER");
        User savedOwner = userRepository.findByEmail(ownerEmail).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(password, savedOwner.getPassword()));
        org.junit.jupiter.api.Assertions.assertNotEquals(password, savedOwner.getPassword());

        String ownerToken = login(ownerEmail, password);
        mockMvc.perform(post("/api/properties")
                .param("ownerId", "999")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "JWT scoped property",
                        "address", "1 Secure Road",
                        "type", "FLAT",
                        "rentAmount", 1000,
                        "description", "Security integration test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ownerId));

        mockMvc.perform(get("/api/properties")
                .param("ownerId", "999")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerId").value(ownerId));

        mockMvc.perform(get("/api/dashboard/owner")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        String tenantEmail = "security-tenant-" + suffix + "@example.test";
        long tenantId = register(tenantEmail, password, "TENANT");
        String tenantToken = login(tenantEmail, password);
        User tenant = userRepository.findById(tenantId).orElseThrow();

        mockMvc.perform(get("/api/tenants/search")
                .param("code", tenant.getTenantCode())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/tenants/search")
                .param("code", tenant.getTenantCode())
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/me/rents")
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDue").value(0))
                .andExpect(jsonPath("$.breakdown").isEmpty())
                .andExpect(jsonPath("$.history").isEmpty());

        mockMvc.perform(get("/api/maintenance/page")
                .param("size", "20")
                .param("search", "leak")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/maintenance/page")
                .param("size", "20")
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(put("/api/maintenance/999")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Edited request",
                        "description", "Not permitted",
                        "priority", "LOW"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/maintenance/999")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Edited request",
                        "description", "Tenant scoped request",
                        "priority", "LOW"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/maintenance/999/cancel")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/maintenance/999/cancel")
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/me/rents")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard/owner")
                .param("ownerId", Long.toString(ownerId))
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/properties")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", ownerEmail,
                        "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        String legacyEmail = "legacy-owner-" + suffix + "@example.test";
        User legacyUser = new User();
        legacyUser.setName("Legacy Owner");
        legacyUser.setEmail(legacyEmail);
        legacyUser.setPassword(password);
        legacyUser.setPhone("5550100100");
        legacyUser.setRole("OWNER");
        legacyUser.setCreatedAt(java.time.LocalDateTime.now());
        legacyUser = userRepository.saveAndFlush(legacyUser);

        String legacyToken = login(legacyEmail, password);
        User migratedUser = userRepository.findById(legacyUser.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(password, migratedUser.getPassword()));
        org.junit.jupiter.api.Assertions.assertNotEquals(password, migratedUser.getPassword());
        mockMvc.perform(get("/api/properties")
                .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk());
    }

    @Test
    void ownerNoticeEndpointReturnsFiveItemPagesAndFiltersOnServer() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "notice-page-owner-" + suffix + "@example.test";
        String password = "SecurePass123!";
        register(email, password, "OWNER");
        String token = login(email, password);

        for (int index = 1; index <= 6; index++) {
            mockMvc.perform(post("/api/notices")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "title", "Notice " + index,
                            "message", "Notice page test"))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/notices/page")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));

        mockMvc.perform(get("/api/notices/page")
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(get("/api/notices/page")
                .header("Authorization", "Bearer " + token)
                .param("search", "NOTICE 6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Notice 6"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void onlyOwnerCanUpdateTheirNotice() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String password = "SecurePass123!";
        String ownerEmail = "notice-update-owner-" + suffix + "@example.test";
        register(ownerEmail, password, "OWNER");
        String ownerToken = login(ownerEmail, password);

        String createdNotice = mockMvc.perform(post("/api/notices")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Original title",
                        "message", "Original message"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long noticeId = objectMapper.readTree(createdNotice).get("id").asLong();

        mockMvc.perform(put("/api/notices/" + noticeId)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Updated title",
                        "message", "Updated message"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.message").value("Updated message"));

        String tenantEmail = "notice-update-tenant-" + suffix + "@example.test";
        register(tenantEmail, password, "TENANT");
        String tenantToken = login(tenantEmail, password);
        mockMvc.perform(put("/api/notices/" + noticeId)
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Unauthorized title",
                        "message", "Unauthorized message"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantNoticePageAcceptsSearchAndPropertyFilters() throws Exception {
        String email = "tenant-notice-filter-" + UUID.randomUUID() + "@example.test";
        String password = "SecurePass123!";
        register(email, password, "TENANT");
        String tenantToken = login(email, password);

        mockMvc.perform(get("/api/me/notices/page")
                .header("Authorization", "Bearer " + tenantToken)
                .param("page", "0")
                .param("size", "5")
                .param("search", "water")
                .param("propertyId", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private long register(String email, String password, String role) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Security Test User",
                "email", email,
                "password", password,
                "phone", "5550100100",
                "role", role));
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode user = objectMapper.readTree(response);
        return user.get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}