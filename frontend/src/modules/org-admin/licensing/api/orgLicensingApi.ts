import { apiClient } from "@/core/api/apiClient";
import type {
  AvailablePlanResponse,
  CreateUpgradeRequest,
  OrganizationLicenseOverviewResponse,
  PaginatedResponse,
  PlanUpgradeRequestResponse,
  UpgradeRequestStatus,
} from "../types/licenseTypes";

export const orgLicensingApi = {
  async getOverview(): Promise<OrganizationLicenseOverviewResponse> {
    const response = await apiClient.get<OrganizationLicenseOverviewResponse>(
      "/license/overview"
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "license" in data.data) {
      return data.data as OrganizationLicenseOverviewResponse;
    }
    return response.data;
  },

  async getAvailablePlans(): Promise<AvailablePlanResponse[]> {
    const response = await apiClient.get<AvailablePlanResponse[] | { data: AvailablePlanResponse[] }>(
      "/license/available-plans"
    );
    if (Array.isArray(response.data)) {
      return response.data;
    }
    return (response.data as { data: AvailablePlanResponse[] })?.data ?? [];
  },

  async createUpgradeRequest(
    request: CreateUpgradeRequest
  ): Promise<PlanUpgradeRequestResponse> {
    const response = await apiClient.post<PlanUpgradeRequestResponse | { data: PlanUpgradeRequestResponse }>(
      "/license/upgrade-requests",
      request
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as PlanUpgradeRequestResponse;
    }
    return response.data as PlanUpgradeRequestResponse;
  },

  async getMyRequests(
    page: number = 0,
    size: number = 10,
    status?: UpgradeRequestStatus
  ): Promise<PaginatedResponse<PlanUpgradeRequestResponse>> {
    const params: Record<string, string | number> = { page, size };
    if (status) {
      params.status = status;
    }
    const response = await apiClient.get<PaginatedResponse<PlanUpgradeRequestResponse> | { data: PaginatedResponse<PlanUpgradeRequestResponse> }>(
      "/license/upgrade-requests/my",
      { params }
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "content" in data.data) {
      return data.data as PaginatedResponse<PlanUpgradeRequestResponse>;
    }
    return response.data as PaginatedResponse<PlanUpgradeRequestResponse>;
  },

  async getRequestDetails(refId: string): Promise<PlanUpgradeRequestResponse> {
    const response = await apiClient.get<PlanUpgradeRequestResponse | { data: PlanUpgradeRequestResponse }>(
      `/license/upgrade-requests/${encodeURIComponent(refId)}`
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as PlanUpgradeRequestResponse;
    }
    return response.data as PlanUpgradeRequestResponse;
  },

  async cancelRequest(refId: string): Promise<PlanUpgradeRequestResponse> {
    const response = await apiClient.post<PlanUpgradeRequestResponse | { data: PlanUpgradeRequestResponse }>(
      `/license/upgrade-requests/${encodeURIComponent(refId)}/cancel`
    );
    const data = response.data as unknown as Record<string, unknown>;
    if (data && "data" in data && typeof data.data === "object" && data.data !== null && "refId" in data.data) {
      return data.data as PlanUpgradeRequestResponse;
    }
    return response.data as PlanUpgradeRequestResponse;
  },
};
