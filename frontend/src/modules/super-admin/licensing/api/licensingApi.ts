import { apiClient } from "@/core/api/apiClient";
import type {
  ActivateLicenseRequest,
  CreatePlanRequest,
  LicenseResponse,
  PaginatedUpgradeRequestsResponse,
  PlanResponse,
  PlanUpgradeRequestResponse,
  RenewLicenseRequest,
  UpdatePlanRequest,
  UpdateUpgradeRequestStatusRequest,
  UpgradeRequestStatus,
} from "../types/licensingTypes";

export const licensingApi = {
  // ============================================================
  // PLAN ENDPOINTS (SUPER_ADMIN)
  // ============================================================

  async getPlans(): Promise<PlanResponse[]> {
    const response = await apiClient.get<PlanResponse[] | { data: PlanResponse[] }>(
      "/plans",
    );
    if (Array.isArray(response.data)) {
      return response.data;
    }
    return (response.data as { data: PlanResponse[] })?.data ?? [];
  },

  async getPlan(planRefId: string): Promise<PlanResponse> {
    const response = await apiClient.get<PlanResponse | { data: PlanResponse }>(
      `/plans/${encodeURIComponent(planRefId)}`,
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as PlanResponse;
    }
    return response.data as PlanResponse;
  },

  async createPlan(request: CreatePlanRequest): Promise<PlanResponse> {
    const response = await apiClient.post<PlanResponse | { data: PlanResponse }>(
      "/plans",
      request,
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as PlanResponse;
    }
    return response.data as PlanResponse;
  },

  async updatePlan(
    planRefId: string,
    request: UpdatePlanRequest,
  ): Promise<PlanResponse> {
    const response = await apiClient.put<PlanResponse | { data: PlanResponse }>(
      `/plans/${encodeURIComponent(planRefId)}`,
      request,
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as PlanResponse;
    }
    return response.data as PlanResponse;
  },

  // ============================================================
  // LICENSE ENDPOINTS (SUPER_ADMIN)
  // ============================================================

  async getOrganizationLicense(
    organizationRefId: string,
  ): Promise<LicenseResponse> {
    const response = await apiClient.get<
      LicenseResponse | { data: LicenseResponse }
    >(`/organizations/${encodeURIComponent(organizationRefId)}/license`);

    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as LicenseResponse;
    }
    return response.data as LicenseResponse;
  },

  async activateLicense(
    organizationRefId: string,
    request: ActivateLicenseRequest,
  ): Promise<LicenseResponse> {
    const response = await apiClient.post<
      LicenseResponse | { data: LicenseResponse }
    >(`/organizations/${encodeURIComponent(organizationRefId)}/license`, request);

    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as LicenseResponse;
    }
    return response.data as LicenseResponse;
  },

  async renewLicense(
    organizationRefId: string,
    request: RenewLicenseRequest,
  ): Promise<LicenseResponse> {
    const response = await apiClient.post<
      LicenseResponse | { data: LicenseResponse }
    >(
      `/organizations/${encodeURIComponent(organizationRefId)}/license/renew`,
      request,
    );

    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as LicenseResponse;
    }
    return response.data as LicenseResponse;
  },

  async deactivateLicense(
    organizationRefId: string,
  ): Promise<LicenseResponse> {
    const response = await apiClient.post<
      LicenseResponse | { data: LicenseResponse }
    >(
      `/organizations/${encodeURIComponent(organizationRefId)}/license/deactivate`,
    );

    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as LicenseResponse;
    }
    return response.data as LicenseResponse;
  },

  async reactivateLicense(
    organizationRefId: string,
  ): Promise<LicenseResponse> {
    const response = await apiClient.post<
      LicenseResponse | { data: LicenseResponse }
    >(
      `/organizations/${encodeURIComponent(organizationRefId)}/license/reactivate`,
    );

    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as LicenseResponse;
    }
    return response.data as LicenseResponse;
  },

  // ============================================================
  // UPGRADE REQUEST ENDPOINTS (SUPER_ADMIN)
  // ============================================================

  async getUpgradeRequests(params?: {
    status?: UpgradeRequestStatus | string;
    organizationRefId?: string;
    page?: number;
    size?: number;
  }): Promise<PaginatedUpgradeRequestsResponse> {
    const query = new URLSearchParams();
    if (params?.status && params.status !== "ALL") {
      query.append("status", params.status);
    }
    if (params?.organizationRefId) {
      query.append("organizationRefId", params.organizationRefId);
    }
    if (params?.page !== undefined) {
      query.append("page", params.page.toString());
    }
    if (params?.size !== undefined) {
      query.append("size", params.size.toString());
    }

    const queryString = query.toString();
    const endpoint = `/admin/license/upgrade-requests${queryString ? `?${queryString}` : ""}`;
    const response = await apiClient.get<
      PaginatedUpgradeRequestsResponse | { data: PaginatedUpgradeRequestsResponse }
    >(endpoint);

    const data = response.data as unknown as Record<string, unknown>;
    if (
      data &&
      "data" in data &&
      typeof data.data === "object" &&
      data.data !== null &&
      "content" in data.data
    ) {
      return data.data as PaginatedUpgradeRequestsResponse;
    }
    return response.data as PaginatedUpgradeRequestsResponse;
  },

  async getUpgradeRequest(refId: string): Promise<PlanUpgradeRequestResponse> {
    const response = await apiClient.get<
      PlanUpgradeRequestResponse | { data: PlanUpgradeRequestResponse }
    >(`/admin/license/upgrade-requests/${encodeURIComponent(refId)}`);

    const data = response.data as unknown as Record<string, unknown>;
    if (
      data &&
      "data" in data &&
      typeof data.data === "object" &&
      data.data !== null &&
      "refId" in data.data
    ) {
      return data.data as PlanUpgradeRequestResponse;
    }
    return response.data as PlanUpgradeRequestResponse;
  },

  async updateUpgradeRequestStatus(
    refId: string,
    request: UpdateUpgradeRequestStatusRequest
  ): Promise<PlanUpgradeRequestResponse> {
    const response = await apiClient.patch<
      PlanUpgradeRequestResponse | { data: PlanUpgradeRequestResponse }
    >(
      `/admin/license/upgrade-requests/${encodeURIComponent(refId)}/status`,
      request
    );

    const data = response.data as unknown as Record<string, unknown>;
    if (
      data &&
      "data" in data &&
      typeof data.data === "object" &&
      data.data !== null &&
      "refId" in data.data
    ) {
      return data.data as PlanUpgradeRequestResponse;
    }
    return response.data as PlanUpgradeRequestResponse;
  },
};
