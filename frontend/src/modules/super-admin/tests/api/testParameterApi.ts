import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type { SpringPage } from "../types/testTypes";
import type {
  CreateTestParameterRequest,
  ParameterQueryParams,
  TestParameterResponse,
  UpdateTestParameterRequest,
} from "../types/parameterTypes";

export const testParameterApi = {
  async getTestParameters(
    testRefId: string,
    params: ParameterQueryParams = {},
  ): Promise<SpringPage<TestParameterResponse>> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: params.sort ?? "displayOrder",
      direction: params.direction ?? "asc",
    };

    if (params.status && params.status.trim()) {
      queryParams.status = params.status.trim();
    }

    const response = await apiClient.get<
      ApiResponse<SpringPage<TestParameterResponse>>
    >(`/tests/${encodeURIComponent(testRefId)}/parameters`, {
      params: queryParams,
    });
    return response.data.data;
  },

  async getParameter(parameterRefId: string): Promise<TestParameterResponse> {
    const response = await apiClient.get<ApiResponse<TestParameterResponse>>(
      `/test-parameters/${encodeURIComponent(parameterRefId)}`,
    );
    return response.data.data;
  },

  async createParameter(
    testRefId: string,
    request: CreateTestParameterRequest,
  ): Promise<TestParameterResponse> {
    const response = await apiClient.post<ApiResponse<TestParameterResponse>>(
      `/tests/${encodeURIComponent(testRefId)}/parameters`,
      request,
    );
    return response.data.data;
  },

  async updateParameter(
    parameterRefId: string,
    request: UpdateTestParameterRequest,
  ): Promise<TestParameterResponse> {
    const response = await apiClient.patch<ApiResponse<TestParameterResponse>>(
      `/test-parameters/${encodeURIComponent(parameterRefId)}`,
      request,
    );
    return response.data.data;
  },

  async deactivateParameter(parameterRefId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `/test-parameters/${encodeURIComponent(parameterRefId)}`,
    );
  },
};
