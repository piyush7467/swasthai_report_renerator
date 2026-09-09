package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.test.dto.request.CreateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.response.TestCategoryResponse;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.service.TestCategoryService;
import com.swasthai.report_generator.test.service.impl.TestCategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCategoryServiceTest {

    @Mock
    private TestCategoryRepository testCategoryRepository;

    private TestCategoryService testCategoryService;

    @BeforeEach
    void setUp() {
        testCategoryService = new TestCategoryServiceImpl(testCategoryRepository);
    }

    @Test
    @DisplayName("Create Category: Successfully creates category with TC- prefixed refId")
    void testCreateCategory_Success() {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM")
                .name("Hematology")
                .description("Blood and related tissues")
                .build();

        when(testCategoryRepository.existsByCode("HEM")).thenReturn(false);
        when(testCategoryRepository.existsByNameIgnoreCase("Hematology")).thenReturn(false);

        when(testCategoryRepository.save(any(TestCategory.class))).thenAnswer(invocation -> {
            TestCategory tc = invocation.getArgument(0);
            tc.setId(UUID.randomUUID());
            tc.setCreatedAt(Instant.now());
            tc.setUpdatedAt(Instant.now());
            return tc;
        });

        TestCategoryResponse response = testCategoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("HEM");
        assertThat(response.getName()).isEqualTo("Hematology");
        assertThat(response.getDescription()).isEqualTo("Blood and related tissues");
        assertThat(response.getStatus()).isEqualTo(TestCategoryStatus.ACTIVE);
        assertThat(response.getRefId()).startsWith("TC-");

        ArgumentCaptor<TestCategory> captor = ArgumentCaptor.forClass(TestCategory.class);
        verify(testCategoryRepository).save(captor.capture());
        assertThat(captor.getValue().getRefId()).startsWith("TC-");
    }

    @Test
    @DisplayName("Create Category: Duplicate code throws ResourceAlreadyExistsException")
    void testCreateCategory_DuplicateCodeThrows() {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM")
                .name("Hematology")
                .build();

        when(testCategoryRepository.existsByCode("HEM")).thenReturn(true);

        assertThatThrownBy(() -> testCategoryService.createCategory(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Test category with code 'HEM' already exists");

        verify(testCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Category: Duplicate name throws ResourceAlreadyExistsException")
    void testCreateCategory_DuplicateNameThrows() {
        CreateTestCategoryRequest request = CreateTestCategoryRequest.builder()
                .code("HEM2")
                .name("Hematology")
                .build();

        when(testCategoryRepository.existsByCode("HEM2")).thenReturn(false);
        when(testCategoryRepository.existsByNameIgnoreCase("Hematology")).thenReturn(true);

        assertThatThrownBy(() -> testCategoryService.createCategory(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Test category with name 'Hematology' already exists");

        verify(testCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get Category: Found by refId")
    void testGetCategory_Success() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-1234567890ab")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(testCategoryRepository.findByRefId("TC-1234567890ab")).thenReturn(Optional.of(category));

        TestCategoryResponse response = testCategoryService.getCategory("TC-1234567890ab");

        assertThat(response).isNotNull();
        assertThat(response.getRefId()).isEqualTo("TC-1234567890ab");
        assertThat(response.getCode()).isEqualTo("BIO");
    }

    @Test
    @DisplayName("Get Category: Non-existent refId throws ResourceNotFoundException")
    void testGetCategory_NotFoundThrows() {
        when(testCategoryRepository.findByRefId("TC-notfound")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testCategoryService.getCategory("TC-notfound"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Test category not found");
    }

    @Test
    @DisplayName("Get Categories (unpaged): Returns active categories")
    void testGetCategories_ReturnsActiveList() {
        TestCategory cat1 = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-01")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryRepository.findAllByStatusOrderByNameAsc(TestCategoryStatus.ACTIVE))
                .thenReturn(List.of(cat1));

        List<TestCategoryResponse> list = testCategoryService.getCategories();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Biochemistry");
    }

    @Test
    @DisplayName("Get Categories (paginated): Successfully filters by status, search, and sorts")
    void testGetCategories_PaginatedWithFilters() {
        TestCategory cat1 = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-01")
                .code("HEM")
                .name("Hematology")
                .status(TestCategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Page<TestCategory> page = new PageImpl<>(List.of(cat1));
        when(testCategoryRepository.findWithFilters(eq(TestCategoryStatus.ACTIVE), eq("hem"), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<TestCategoryResponse> response = testCategoryService.getCategories(
                TestCategoryStatus.ACTIVE, "hem", 0, 10, "name", "asc"
        );

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCode()).isEqualTo("HEM");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get Categories (paginated): Empty results returns empty PagedResponse cleanly")
    void testGetCategories_EmptyResults() {
        Page<TestCategory> emptyPage = new PageImpl<>(Collections.emptyList());
        when(testCategoryRepository.findWithFilters(any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        PagedResponse<TestCategoryResponse> response = testCategoryService.getCategories(
                null, null, 0, 20, "createdAt", "desc"
        );

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Get Categories (paginated): Invalid page index (< 0) throws IllegalArgumentException")
    void testGetCategories_InvalidPageThrows() {
        assertThatThrownBy(() -> testCategoryService.getCategories(null, null, -1, 20, "name", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page index must not be less than zero.");
    }

    @Test
    @DisplayName("Get Categories (paginated): Invalid page size (< 1 or > 100) throws IllegalArgumentException")
    void testGetCategories_InvalidSizeThrows() {
        assertThatThrownBy(() -> testCategoryService.getCategories(null, null, 0, 0, "name", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be between 1 and 100.");

        assertThatThrownBy(() -> testCategoryService.getCategories(null, null, 0, 101, "name", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be between 1 and 100.");
    }

    @Test
    @DisplayName("Get Categories (paginated): Invalid sort field throws IllegalArgumentException")
    void testGetCategories_InvalidSortFieldThrows() {
        assertThatThrownBy(() -> testCategoryService.getCategories(null, null, 0, 20, "sql_injection", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field: 'sql_injection'");
    }

    @Test
    @DisplayName("Get Categories (paginated): Invalid sort direction throws IllegalArgumentException")
    void testGetCategories_InvalidSortDirectionThrows() {
        assertThatThrownBy(() -> testCategoryService.getCategories(null, null, 0, 20, "name", "sideways"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort direction: 'sideways'");
    }

    @Test
    @DisplayName("Update Category: Updates code, name, description, and status cleanly")
    void testUpdateCategory_Success() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-update")
                .code("OLD")
                .name("Old Name")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-update")).thenReturn(Optional.of(category));
        when(testCategoryRepository.existsByCode("NEW")).thenReturn(false);
        when(testCategoryRepository.existsByNameIgnoreCase("New Name")).thenReturn(false);
        when(testCategoryRepository.save(any(TestCategory.class))).thenReturn(category);

        UpdateTestCategoryRequest updateRequest = UpdateTestCategoryRequest.builder()
                .code("NEW")
                .name("New Name")
                .description("Updated description")
                .status(TestCategoryStatus.INACTIVE)
                .build();

        TestCategoryResponse response = testCategoryService.updateCategory("TC-update", updateRequest);

        assertThat(response).isNotNull();
        assertThat(category.getCode()).isEqualTo("NEW");
        assertThat(category.getName()).isEqualTo("New Name");
        assertThat(category.getDescription()).isEqualTo("Updated description");
        assertThat(category.getStatus()).isEqualTo(TestCategoryStatus.INACTIVE);
    }

    @Test
    @DisplayName("Update Category: Blank code or name throws IllegalArgumentException")
    void testUpdateCategory_BlankCodeOrNameThrows() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-update")
                .code("OLD")
                .name("Old Name")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-update")).thenReturn(Optional.of(category));

        UpdateTestCategoryRequest blankCodeRequest = UpdateTestCategoryRequest.builder()
                .code("   ")
                .build();

        assertThatThrownBy(() -> testCategoryService.updateCategory("TC-update", blankCodeRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category code cannot be blank.");

        UpdateTestCategoryRequest blankNameRequest = UpdateTestCategoryRequest.builder()
                .name("   ")
                .build();

        assertThatThrownBy(() -> testCategoryService.updateCategory("TC-update", blankNameRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category name cannot be blank.");
    }

    @Test
    @DisplayName("Update Category: Duplicate code or name throws ResourceAlreadyExistsException")
    void testUpdateCategory_DuplicateCodeOrNameThrows() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-update")
                .code("OLD")
                .name("Old Name")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-update")).thenReturn(Optional.of(category));
        when(testCategoryRepository.existsByCode("EXISTING_CODE")).thenReturn(true);

        UpdateTestCategoryRequest duplicateCodeRequest = UpdateTestCategoryRequest.builder()
                .code("EXISTING_CODE")
                .build();

        assertThatThrownBy(() -> testCategoryService.updateCategory("TC-update", duplicateCodeRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Test category with code 'EXISTING_CODE' already exists");

        when(testCategoryRepository.existsByNameIgnoreCase("Existing Name")).thenReturn(true);

        UpdateTestCategoryRequest duplicateNameRequest = UpdateTestCategoryRequest.builder()
                .name("Existing Name")
                .build();

        assertThatThrownBy(() -> testCategoryService.updateCategory("TC-update", duplicateNameRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Test category with name 'Existing Name' already exists");
    }

    @Test
    @DisplayName("Delete Category: Soft deletes category from ACTIVE to INACTIVE")
    void testDeleteCategory_Success() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-del")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-del")).thenReturn(Optional.of(category));
        when(testCategoryRepository.save(any(TestCategory.class))).thenReturn(category);

        testCategoryService.deleteCategory("TC-del");

        assertThat(category.getStatus()).isEqualTo(TestCategoryStatus.INACTIVE);
        verify(testCategoryRepository).save(category);
    }

    @Test
    @DisplayName("Delete Category: Already INACTIVE category throws IllegalStateException")
    void testDeleteCategory_AlreadyInactiveThrows() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-del")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.INACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-del")).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> testCategoryService.deleteCategory("TC-del"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Test category is already inactive");

        verify(testCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivate Category: Successfully reactivates INACTIVE to ACTIVE")
    void testReactivateCategory_Success() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-reactivate")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.INACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-reactivate")).thenReturn(Optional.of(category));
        when(testCategoryRepository.save(any(TestCategory.class))).thenReturn(category);

        TestCategoryResponse response = testCategoryService.reactivateCategory("TC-reactivate");

        assertThat(response).isNotNull();
        assertThat(category.getStatus()).isEqualTo(TestCategoryStatus.ACTIVE);
        verify(testCategoryRepository).save(category);
    }

    @Test
    @DisplayName("Reactivate Category: Already ACTIVE category throws IllegalStateException")
    void testReactivateCategory_AlreadyActiveThrows() {
        TestCategory category = TestCategory.builder()
                .id(UUID.randomUUID())
                .refId("TC-reactivate")
                .code("BIO")
                .name("Biochemistry")
                .status(TestCategoryStatus.ACTIVE)
                .build();

        when(testCategoryRepository.findByRefId("TC-reactivate")).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> testCategoryService.reactivateCategory("TC-reactivate"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Test category is already active");

        verify(testCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivate Category: Non-existent refId throws ResourceNotFoundException")
    void testReactivateCategory_NotFoundThrows() {
        when(testCategoryRepository.findByRefId("TC-none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testCategoryService.reactivateCategory("TC-none"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Test category not found");
    }
}
