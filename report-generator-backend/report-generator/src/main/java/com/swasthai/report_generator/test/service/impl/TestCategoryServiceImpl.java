package com.swasthai.report_generator.test.service.impl;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.test.dto.request.CreateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.response.TestCategoryResponse;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.service.TestCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TestCategoryServiceImpl implements TestCategoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "code", "status", "createdAt", "updatedAt"
    );
    private static final int MAX_PAGE_SIZE = 100;

    private final TestCategoryRepository testCategoryRepository;

    @Override
    public TestCategoryResponse createCategory(CreateTestCategoryRequest request) {

        String code = request.getCode().trim().toUpperCase();
        String name = request.getName().trim();

        if (testCategoryRepository.existsByCode(code)) {
            throw new ResourceAlreadyExistsException(
                    "Test category with code '" + code + "' already exists"
            );
        }

        if (testCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceAlreadyExistsException(
                    "Test category with name '" + name + "' already exists"
            );
        }

        TestCategory category = TestCategory.builder()
                .refId(RefIdGenerator.generate("TC"))
                .code(code)
                .name(name)
                .description(
                        request.getDescription() != null && !request.getDescription().trim().isEmpty()
                                ? request.getDescription().trim()
                                : null
                )
                .status(TestCategoryStatus.ACTIVE)
                .build();

        TestCategory savedCategory = testCategoryRepository.save(category);

        return mapToResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public TestCategoryResponse getCategory(String categoryRefId) {

        TestCategory category = testCategoryRepository.findByRefId(categoryRefId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test category not found"
                ));

        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCategoryResponse> getCategories() {

        return testCategoryRepository
                .findAllByStatusOrderByNameAsc(TestCategoryStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TestCategoryResponse> getCategories(
            TestCategoryStatus status,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        if (sortBy != null && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: '" + sortBy + "'. Allowed sort fields: " + ALLOWED_SORT_FIELDS);
        }

        if (sortDirection != null && !"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
            throw new IllegalArgumentException("Invalid sort direction: '" + sortDirection + "'. Allowed values: 'asc', 'desc'.");
        }

        String resolvedSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));

        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<TestCategory> categoryPage = testCategoryRepository.findWithFilters(status, sanitizedSearch, pageable);

        List<TestCategoryResponse> content = categoryPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<TestCategoryResponse>builder()
                .content(content)
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .last(categoryPage.isLast())
                .build();
    }

    @Override
    public TestCategoryResponse updateCategory(
            String categoryRefId,
            UpdateTestCategoryRequest request
    ) {

        TestCategory category = testCategoryRepository.findByRefId(categoryRefId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test category not found"
                ));

        /*
         * Update code
         */
        if (request.getCode() != null) {

            String newCode = request.getCode().trim().toUpperCase();

            if (newCode.isBlank()) {
                throw new IllegalArgumentException("Category code cannot be blank.");
            }

            if (!newCode.equals(category.getCode())
                    && testCategoryRepository.existsByCode(newCode)) {

                throw new ResourceAlreadyExistsException(
                        "Test category with code '" + newCode + "' already exists"
                );
            }

            category.setCode(newCode);
        }

        /*
         * Update name
         */
        if (request.getName() != null) {

            String newName = request.getName().trim();

            if (newName.isBlank()) {
                throw new IllegalArgumentException("Category name cannot be blank.");
            }

            if (!newName.equalsIgnoreCase(category.getName())
                    && testCategoryRepository.existsByNameIgnoreCase(newName)) {

                throw new ResourceAlreadyExistsException(
                        "Test category with name '" + newName + "' already exists"
                );
            }

            category.setName(newName);
        }

        /*
         * Update description
         */
        if (request.getDescription() != null) {
            String trimmed = request.getDescription().trim();
            category.setDescription(trimmed.isEmpty() ? null : trimmed);
        }

        /*
         * Update status
         */
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        TestCategory updatedCategory =
                testCategoryRepository.save(category);

        return mapToResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(String categoryRefId) {

        TestCategory category = testCategoryRepository.findByRefId(categoryRefId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test category not found"
                ));

        if (category.getStatus() == TestCategoryStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Test category is already inactive"
            );
        }

        category.setStatus(TestCategoryStatus.INACTIVE);

        testCategoryRepository.save(category);
    }

    @Override
    public TestCategoryResponse reactivateCategory(String categoryRefId) {

        TestCategory category = testCategoryRepository.findByRefId(categoryRefId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test category not found"
                ));

        if (category.getStatus() == TestCategoryStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Test category is already active"
            );
        }

        category.setStatus(TestCategoryStatus.ACTIVE);

        TestCategory reactivatedCategory = testCategoryRepository.save(category);

        return mapToResponse(reactivatedCategory);
    }

    private TestCategoryResponse mapToResponse(TestCategory category) {

        return TestCategoryResponse.builder()
                .refId(category.getRefId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}