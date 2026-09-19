import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreateOrganizationRequest,
  OrganizationPageResponse,
  OrganizationProfileResponse,
  OrganizationQueryParams,
  OrganizationResponse,
  UpdateOrganizationRequest,
  UpdateOrganizationStatusRequest,
  UpdateOrganizationProfileRequest,
} from "../types/organizationTypes";

export const organizationApi = {
  async getOrganizations(
    params: OrganizationQueryParams = {},
  ): Promise<OrganizationPageResponse> {
    const response = await apiClient.get<
      ApiResponse<OrganizationPageResponse>
    >("/organizations", {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sortBy: params.sortBy ?? "createdAt",
        sortDirection: params.sortDirection ?? "DESC",
      },
    });
    return response.data.data;
  },

  async getOrganization(refId: string): Promise<OrganizationResponse> {
    const response = await apiClient.get<
      ApiResponse<OrganizationResponse>
    >(`/organizations/${encodeURIComponent(refId)}`);
    return response.data.data;
  },

  async createOrganization(
    request: CreateOrganizationRequest,
  ): Promise<OrganizationResponse> {
    const response = await apiClient.post<
      ApiResponse<OrganizationResponse>
    >("/organizations", request);
    return response.data.data;
  },

  async updateOrganization(
    refId: string,
    request: UpdateOrganizationRequest,
  ): Promise<OrganizationResponse> {
    const response = await apiClient.put<
      ApiResponse<OrganizationResponse>
    >(`/organizations/${encodeURIComponent(refId)}`, request);
    return response.data.data;
  },

  async updateOrganizationStatus(
    refId: string,
    request: UpdateOrganizationStatusRequest,
  ): Promise<OrganizationResponse> {
    const response = await apiClient.patch<
      ApiResponse<OrganizationResponse>
    >(`/organizations/${encodeURIComponent(refId)}/status`, request);
    return response.data.data;
  },

  async getOrganizationProfile(
    organizationRefId: string,
  ): Promise<OrganizationProfileResponse> {
    const response = await apiClient.get<
      ApiResponse<OrganizationProfileResponse>
    >(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}`,
    );
    return response.data.data;
  },

  async updateOrganizationProfile(
    organizationRefId: string,
    request: UpdateOrganizationProfileRequest,
  ): Promise<OrganizationProfileResponse> {
    const response = await apiClient.put<
      ApiResponse<OrganizationProfileResponse>
    >(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}`,
      request,
    );
    return response.data.data;
  },

  async getOrganizationLogo(organizationRefId: string): Promise<Blob> {
    const response = await apiClient.get<Blob>(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}/logo`,
      {
        responseType: "blob",
      },
    );
    return response.data;
  },

  async uploadOrganizationLogo(
    organizationRefId: string,
    file: File,
  ): Promise<OrganizationProfileResponse> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await apiClient.post<
      ApiResponse<OrganizationProfileResponse>
    >(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}/logo`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );
    return response.data.data;
  },

  async deleteOrganizationLogo(
    organizationRefId: string,
  ): Promise<OrganizationProfileResponse> {
    const response = await apiClient.delete<
      ApiResponse<OrganizationProfileResponse>
    >(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}/logo`,
    );
    return response.data.data;
  },

  async getOrganizationSignature(organizationRefId: string): Promise<Blob> {
    const response = await apiClient.get<Blob>(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}/signature`,
      {
        responseType: "blob",
      },
    );
    return response.data;
  },

  async uploadOrganizationSignature(
    organizationRefId: string,
    file: File,
  ): Promise<OrganizationProfileResponse> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await apiClient.post<
      ApiResponse<OrganizationProfileResponse>
    >(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}/signature`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );
    return response.data.data;
  },

  async deleteOrganizationSignature(
    organizationRefId: string,
  ): Promise<OrganizationProfileResponse> {
    const response = await apiClient.delete<
      ApiResponse<OrganizationProfileResponse>
    >(
      `/organization-profile/organizations/${encodeURIComponent(
        organizationRefId,
      )}/signature`,
    );
    return response.data.data;
  },
};
