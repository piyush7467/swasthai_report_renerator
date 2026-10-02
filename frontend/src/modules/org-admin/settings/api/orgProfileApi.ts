import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  OrganizationProfileResponse,
  UpdateOrganizationProfileRequest,
} from "@/modules/super-admin/types/organizationTypes";

export const orgProfileApi = {
  async getMyProfile(): Promise<OrganizationProfileResponse> {
    const response = await apiClient.get<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me",
    );
    return response.data.data;
  },

  async updateMyProfile(
    request: UpdateOrganizationProfileRequest,
  ): Promise<OrganizationProfileResponse> {
    const response = await apiClient.put<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me",
      request,
    );
    return response.data.data;
  },

  async uploadLogo(file: File): Promise<OrganizationProfileResponse> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await apiClient.post<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me/logo",
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );
    return response.data.data;
  },

  async deleteLogo(): Promise<OrganizationProfileResponse> {
    const response = await apiClient.delete<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me/logo",
    );
    return response.data.data;
  },

  async uploadSignature(file: File): Promise<OrganizationProfileResponse> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await apiClient.post<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me/signature",
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );
    return response.data.data;
  },

  async deleteSignature(): Promise<OrganizationProfileResponse> {
    const response = await apiClient.delete<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me/signature",
    );
    return response.data.data;
  },

  async getLogoBlob(): Promise<Blob> {
    const response = await apiClient.get<Blob>("/organization-profile/me/logo", {
      responseType: "blob",
    });
    return response.data;
  },

  async getSignatureBlob(): Promise<Blob> {
    const response = await apiClient.get<Blob>(
      "/organization-profile/me/signature",
      {
        responseType: "blob",
      },
    );
    return response.data;
  },
};
