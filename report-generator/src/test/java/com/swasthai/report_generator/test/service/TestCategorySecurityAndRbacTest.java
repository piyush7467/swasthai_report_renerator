package com.swasthai.report_generator.test.service;

import tools.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.test.dto.request.CreateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.response.TestCategoryResponse;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import com.swasthai.report_generator.test.service.TestCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TestCategorySecurityAndRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestCategoryService testCategoryService;

    // -------------------------------------------------------------
    // 1. Authentication & RBAC (401, 403, 200/201)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Create Category: Unauthenticated request returns 401 Unauthorized")
    void testCreateCategory_Unauthenticated_Returns401() throws Exception {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM")
                .name("Hematology")
                .build();

        mockMvc.perform(post("/api/v1/test-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(testCategoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("Create Category: Non-SUPER_ADMIN (e.g. ORG_ADMIN) returns 403 Forbidden")
    void testCreateCategory_OrgAdmin_Returns403() throws Exception {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM")
                .name("Hematology")
                .build();

        mockMvc.perform(post("/api/v1/test-categories")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(testCategoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("Create Category: SUPER_ADMIN succeeds with 201 Created and consistent ApiResponse")
    void testCreateCategory_SuperAdmin_Returns201() throws Exception {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM")
                .name("Hematology")
                .description("Blood and related tissues")
                .build();

        TestCategoryResponse response = TestCategoryResponse.builder()
                .refId("TC-abc1234567")
                .code("HEM")
                .name("Hematology")
                .description("Blood and related tissues")
                .status(TestCategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(testCategoryService.createCategory(any(CreateTestCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/test-categories")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test category created successfully"))
                .andExpect(jsonPath("$.data.refId").value("TC-abc1234567"))
                .andExpect(jsonPath("$.data.code").value("HEM"))
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    // -------------------------------------------------------------
    // 2. Validation & Conflicts (400, 409)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Create Category: Validation error (blank code/name) returns 400 VALIDATION_ERROR")
    void testCreateCategory_ValidationError_Returns400() throws Exception {
        CreateTestCategoryRequest invalidRequest = CreateTestCategoryRequest.builder()
                .code("")
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/test-categories")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(testCategoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("Create Category: Duplicate code returns 409 RESOURCE_ALREADY_EXISTS")
    void testCreateCategory_DuplicateCode_Returns409() throws Exception {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM")
                .name("Hematology")
                .build();

        when(testCategoryService.createCategory(any())).thenThrow(
                new ResourceAlreadyExistsException("Test category with code 'HEM' already exists")
        );

        mockMvc.perform(post("/api/v1/test-categories")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(containsString("code 'HEM' already exists")));
    }

    // -------------------------------------------------------------
    // 3. Get All & Filtering & Pagination (401, 200, 400)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Get Categories: Unauthenticated returns 401 Unauthorized")
    void testGetCategories_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/test-categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Get Categories: Any authenticated role (LAB_STAFF) can view paginated categories")
    void testGetCategories_LabStaff_Returns200WithPagination() throws Exception {
        TestCategoryResponse item = TestCategoryResponse.builder()
                .refId("TC-123")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        PagedResponse<TestCategoryResponse> pagedResponse = PagedResponse.<TestCategoryResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(testCategoryService.getCategories(eq(TestCategoryStatus.ACTIVE), eq("bio"), eq(0), eq(20), eq("name"), eq("asc")))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/test-categories")
                        .with(user("labstaff").roles("LAB_STAFF"))
                        .param("status", "ACTIVE")
                        .param("search", "bio")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].code").value("BIO"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Get Categories: Invalid sort field returns 400 BAD_REQUEST")
    void testGetCategories_InvalidSortField_Returns400() throws Exception {
        when(testCategoryService.getCategories(any(), any(), anyInt(), anyInt(), eq("malicious_column"), any()))
                .thenThrow(new IllegalArgumentException("Invalid sort field: 'malicious_column'. Allowed sort fields: [name, code, status, createdAt, updatedAt]"));

        mockMvc.perform(get("/api/v1/test-categories")
                        .with(user("labstaff").roles("LAB_STAFF"))
                        .param("sortBy", "malicious_column"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort field")));
    }

    // -------------------------------------------------------------
    // 4. Get By refId (401, 200, 404)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Get Category By refId: Authenticated user succeeds")
    void testGetCategoryByRefId_Success() throws Exception {
        TestCategoryResponse response = TestCategoryResponse.builder()
                .refId("TC-123")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryService.getCategory("TC-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/test-categories/TC-123")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.refId").value("TC-123"))
                .andExpect(jsonPath("$.data.code").value("BIO"));
    }

    @Test
    @DisplayName("Get Category By refId: Nonexistent refId returns 404 RESOURCE_NOT_FOUND")
    void testGetCategoryByRefId_NotFound_Returns404() throws Exception {
        when(testCategoryService.getCategory("TC-missing")).thenThrow(
                new ResourceNotFoundException("Test category not found")
        );

        mockMvc.perform(get("/api/v1/test-categories/TC-missing")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // -------------------------------------------------------------
    // 5. Update (401, 403, 200, 400)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Update Category: ORG_ADMIN returns 403 Forbidden")
    void testUpdateCategory_OrgAdmin_Returns403() throws Exception {
        UpdateTestCategoryRequest request = UpdateTestCategoryRequest.builder()
                .name("Updated Name")
                .build();

        mockMvc.perform(patch("/api/v1/test-categories/TC-123")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Update Category: SUPER_ADMIN succeeds with 200 OK")
    void testUpdateCategory_SuperAdmin_Returns200() throws Exception {
        UpdateTestCategoryRequest request = UpdateTestCategoryRequest.builder()
                .name("Updated Name")
                .build();

        TestCategoryResponse response = TestCategoryResponse.builder()
                .refId("TC-123")
                .code("BIO")
                .name("Updated Name")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryService.updateCategory(eq("TC-123"), any(UpdateTestCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/test-categories/TC-123")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    // -------------------------------------------------------------
    // 6. Soft Delete & Reactivate (401, 403, 200, 400 ILLEGAL_STATE)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Delete Category: Non-SUPER_ADMIN returns 403 Forbidden")
    void testDeleteCategory_LabStaff_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/test-categories/TC-123")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Delete Category: SUPER_ADMIN succeeds with 200 OK")
    void testDeleteCategory_SuperAdmin_Returns200() throws Exception {
        doNothing().when(testCategoryService).deleteCategory("TC-123");

        mockMvc.perform(delete("/api/v1/test-categories/TC-123")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test category deleted successfully"));
    }

    @Test
    @DisplayName("Delete Category: Already inactive throws 400 ILLEGAL_STATE")
    void testDeleteCategory_AlreadyInactive_Returns400() throws Exception {
        doThrow(new IllegalStateException("Test category is already inactive"))
                .when(testCategoryService).deleteCategory("TC-123");

        mockMvc.perform(delete("/api/v1/test-categories/TC-123")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
                .andExpect(jsonPath("$.message").value("Test category is already inactive"));
    }

    @Test
    @DisplayName("Reactivate Category: Non-SUPER_ADMIN returns 403 Forbidden")
    void testReactivateCategory_OrgAdmin_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/test-categories/TC-123/reactivate")
                        .with(user("orgadmin").roles("ORG_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Reactivate Category: SUPER_ADMIN succeeds with 200 OK")
    void testReactivateCategory_SuperAdmin_Returns200() throws Exception {
        TestCategoryResponse response = TestCategoryResponse.builder()
                .refId("TC-123")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryService.reactivateCategory("TC-123")).thenReturn(response);

        mockMvc.perform(post("/api/v1/test-categories/TC-123/reactivate")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Test category reactivated successfully"));
    }

    @Test
    @DisplayName("Reactivate Category: Already active throws 400 ILLEGAL_STATE")
    void testReactivateCategory_AlreadyActive_Returns400() throws Exception {
        when(testCategoryService.reactivateCategory("TC-123"))
                .thenThrow(new IllegalStateException("Test category is already active"));

        mockMvc.perform(post("/api/v1/test-categories/TC-123/reactivate")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
                .andExpect(jsonPath("$.message").value("Test category is already active"));
    }
}
