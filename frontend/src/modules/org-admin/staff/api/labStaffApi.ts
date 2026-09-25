import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreateLabStaffRequest,
  LabStaffDetailsResponse,
  LabStaffQueryParams,
  LabStaffResponse,
  LabStaffSummaryResponse,
  PagedLabStaffResponse,
  UpdateLabStaffStatusRequest,
} from "../types/labStaffTypes";

export const labStaffApi = {
  async getSummary(): Promise<LabStaffSummaryResponse> {
    const response = await apiClient.get<ApiResponse<LabStaffSummaryResponse>>(
      "/users/lab-staff/summary"
    );
    return response.data.data;
  },

  async getStaffList(
    params: LabStaffQueryParams = {}
  ): Promise<PagedLabStaffResponse> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 15,
      sortBy: params.sortBy ?? "createdAt",
      sortDirection: params.sortDirection ?? "DESC",
    };

    if (params.status) {
      queryParams.status = params.status;
    }

    if (params.search && params.search.trim()) {
      queryParams.search = params.search.trim();
    }

    const response = await apiClient.get<ApiResponse<PagedLabStaffResponse>>(
      "/users/lab-staff",
      { params: queryParams }
    );
    return response.data.data;
  },

  async getStaffDetails(refId: string): Promise<LabStaffDetailsResponse> {
    const response = await apiClient.get<ApiResponse<LabStaffDetailsResponse>>(
      `/users/lab-staff/${encodeURIComponent(refId)}`
    );
    return response.data.data;
  },

  async createStaff(request: CreateLabStaffRequest): Promise<LabStaffResponse> {
    const response = await apiClient.post<ApiResponse<LabStaffResponse>>(
      "/users/lab-staff",
      request
    );
    return response.data.data;
  },

  async updateStaffStatus(
    refId: string,
    request: UpdateLabStaffStatusRequest
  ): Promise<LabStaffResponse> {
    const response = await apiClient.patch<ApiResponse<LabStaffResponse>>(
      `/users/lab-staff/${encodeURIComponent(refId)}/status`,
      request
    );
    return response.data.data;
  },

  async deactivateStaff(refId: string): Promise<LabStaffResponse> {
    const response = await apiClient.patch<ApiResponse<LabStaffResponse>>(
      `/users/lab-staff/${encodeURIComponent(refId)}/deactivate`
    );
    return response.data.data;
  },

  async deleteStaff(refId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(
      `/users/lab-staff/${encodeURIComponent(refId)}`
    );
  },
};
