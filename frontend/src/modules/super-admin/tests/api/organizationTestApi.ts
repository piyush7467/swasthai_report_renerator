import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type { SpringPage } from "../types/testTypes";
import type {
  AssignTestRequest,
  OrganizationTestQueryParams,
  OrganizationTestResponse,
  UpdateOrganizationTestRequest,
} from "../types/assignmentTypes";

export const organizationTestApi = {
  async getAllAssignments(
    params: OrganizationTestQueryParams,
  ): Promise<SpringPage<OrganizationTestResponse>> {
    const queryParams: Record<string, string | number> = {
      organizationRefId: params.organizationRefId,
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: params.sort ?? "createdAt",
      direction: params.direction ?? "desc",
    };

    if (params.status && params.status.trim()) {
      queryParams.status = params.status.trim();
    }

    const response = await apiClient.get<
      ApiResponse<SpringPage<OrganizationTestResponse>>
    >("/organization-tests", {
      params: queryParams,
    });
    return response.data.data;
  },

  async getAssignment(refId: string): Promise<OrganizationTestResponse> {
    const response = await apiClient.get<
      ApiResponse<OrganizationTestResponse>
    >(`/organization-tests/${encodeURIComponent(refId)}`);
    return response.data.data;
  },

  async assignTest(
    request: AssignTestRequest,
  ): Promise<OrganizationTestResponse> {
    const response = await apiClient.post<
      ApiResponse<OrganizationTestResponse>
    >("/organization-tests", request);
    return response.data.data;
  },

  async updateAssignment(
    refId: string,
    request: UpdateOrganizationTestRequest,
  ): Promise<OrganizationTestResponse> {
    const response = await apiClient.patch<
      ApiResponse<OrganizationTestResponse>
    >(`/organization-tests/${encodeURIComponent(refId)}`, request);
    return response.data.data;
  },

  async deactivateAssignment(refId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `/organization-tests/${encodeURIComponent(refId)}`,
    );
  },
};
