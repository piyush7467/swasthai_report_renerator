import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreateTestCategoryRequest,
  PagedResponse,
  TestCategoryQueryParams,
  TestCategoryResponse,
  UpdateTestCategoryRequest,
} from "../types/categoryTypes";

export const testCategoryApi = {
  async getCategories(
    params: TestCategoryQueryParams = {},
  ): Promise<PagedResponse<TestCategoryResponse>> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sortBy: params.sortBy ?? "name",
      sortDirection: params.sortDirection ?? "asc",
    };

    if (params.search && params.search.trim()) {
      queryParams.search = params.search.trim();
    }
    if (params.status) {
      queryParams.status = params.status;
    }

    const response = await apiClient.get<
      ApiResponse<PagedResponse<TestCategoryResponse>>
    >("/test-categories", { params: queryParams });
    return response.data.data;
  },

  async getCategory(refId: string): Promise<TestCategoryResponse> {
    const response = await apiClient.get<ApiResponse<TestCategoryResponse>>(
      `/test-categories/${encodeURIComponent(refId)}`,
    );
    return response.data.data;
  },

  async createCategory(
    request: CreateTestCategoryRequest,
  ): Promise<TestCategoryResponse> {
    const response = await apiClient.post<ApiResponse<TestCategoryResponse>>(
      "/test-categories",
      request,
    );
    return response.data.data;
  },

  async updateCategory(
    refId: string,
    request: UpdateTestCategoryRequest,
  ): Promise<TestCategoryResponse> {
    const response = await apiClient.patch<ApiResponse<TestCategoryResponse>>(
      `/test-categories/${encodeURIComponent(refId)}`,
      request,
    );
    return response.data.data;
  },

  async deleteCategory(refId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `/test-categories/${encodeURIComponent(refId)}`,
    );
  },

  async reactivateCategory(refId: string): Promise<TestCategoryResponse> {
    const response = await apiClient.post<ApiResponse<TestCategoryResponse>>(
      `/test-categories/${encodeURIComponent(refId)}/reactivate`,
    );
    return response.data.data;
  },
};
