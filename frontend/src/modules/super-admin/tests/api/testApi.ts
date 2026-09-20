import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreateTestRequest,
  SpringPage,
  TestQueryParams,
  TestResponse,
  UpdateTestRequest,
} from "../types/testTypes";

export const testApi = {
  async getTests(params: TestQueryParams = {}): Promise<SpringPage<TestResponse>> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: params.sort ?? "name",
      direction: params.direction ?? "asc",
    };

    if (params.search && params.search.trim()) {
      queryParams.search = params.search.trim();
    }
    if (params.categoryRefId && params.categoryRefId.trim()) {
      queryParams.categoryRefId = params.categoryRefId.trim();
    }
    if (params.status && params.status.trim()) {
      queryParams.status = params.status.trim();
    }

    const response = await apiClient.get<ApiResponse<SpringPage<TestResponse>>>(
      "/tests",
      { params: queryParams },
    );
    return response.data.data;
  },

  async getTest(refId: string): Promise<TestResponse> {
    const response = await apiClient.get<ApiResponse<TestResponse>>(
      `/tests/${encodeURIComponent(refId)}`,
    );
    return response.data.data;
  },

  async createTest(request: CreateTestRequest): Promise<TestResponse> {
    const response = await apiClient.post<ApiResponse<TestResponse>>(
      "/tests",
      request,
    );
    return response.data.data;
  },

  async updateTest(
    refId: string,
    request: UpdateTestRequest,
  ): Promise<TestResponse> {
    const response = await apiClient.patch<ApiResponse<TestResponse>>(
      `/tests/${encodeURIComponent(refId)}`,
      request,
    );
    return response.data.data;
  },

  async deleteTest(refId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `/tests/${encodeURIComponent(refId)}`,
    );
  },
};
