import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type { OrganizationProfileResponse } from "@/modules/super-admin/types/organizationTypes";

export const orgProfileApi = {
  async getMyProfile(): Promise<OrganizationProfileResponse> {
    const response = await apiClient.get<ApiResponse<OrganizationProfileResponse>>(
      "/organization-profile/me",
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
