package com.swasthai.report_generator.test.service;

import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.test.dto.request.CreateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestCategoryRequest;
import com.swasthai.report_generator.test.dto.response.TestCategoryResponse;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;

import java.util.List;

public interface TestCategoryService {

    TestCategoryResponse createCategory(
            CreateTestCategoryRequest request
    );

    TestCategoryResponse getCategory(
            String categoryRefId
    );

    List<TestCategoryResponse> getCategories();

    PagedResponse<TestCategoryResponse> getCategories(
            TestCategoryStatus status,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    TestCategoryResponse updateCategory(
            String categoryRefId,
            UpdateTestCategoryRequest request
    );

    void deleteCategory(String categoryRefId);

    TestCategoryResponse reactivateCategory(String categoryRefId);
}