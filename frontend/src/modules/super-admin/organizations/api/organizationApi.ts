import { apiClient } from "@/core/api/apiClient";
import type {
  CreateOrganizationRequest,
  Organization,
  OrganizationPageResponse,
  UpdateOrganizationRequest,
  UpdateOrganizationStatusRequest,
} from "../types";

interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp?: string;
}

export const organizationApi = {
  async getOrganizations(params: {
    page: number;
    size: number;
    sortBy: string;
    sortDirection: "ASC" | "DESC";
  }): Promise<OrganizationPageResponse> {
    const response = await apiClient.get<
      ApiResponse<OrganizationPageResponse>
    >("/organizations", {
      params,
    });

    return response.data.data;
  },

  async getOrganization(refId: string): Promise<Organization> {
    const response = await apiClient.get<ApiResponse<Organization>>(
      `/organizations/${refId}`,
    );

    return response.data.data;
  },

  async createOrganization(
    request: CreateOrganizationRequest,
  ): Promise<Organization> {
    const response = await apiClient.post<ApiResponse<Organization>>(
      "/organizations",
      request,
    );

    return response.data.data;
  },

  async updateOrganization(
    refId: string,
    request: UpdateOrganizationRequest,
  ): Promise<Organization> {
    const response = await apiClient.put<ApiResponse<Organization>>(
      `/organizations/${refId}`,
      request,
    );

    return response.data.data;
  },

  async updateOrganizationStatus(
    refId: string,
    request: UpdateOrganizationStatusRequest,
  ): Promise<Organization> {
    const response = await apiClient.patch<ApiResponse<Organization>>(
      `/organizations/${refId}/status`,
      request,
    );

    return response.data.data;
  },
};