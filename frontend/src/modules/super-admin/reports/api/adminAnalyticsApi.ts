import { apiClient } from "@/core/api/apiClient";
import type {
  AdminOverviewStats,
  OrganizationReportActivity,
  OrgActivityFilterParams,
  PageResponse,
  ReportTrendPoint,
  SecurityAuditFilterParams,
  SecurityAuditLog,
  TestUsageStats,
  TrendFilterParams,
} from "../types/analyticsTypes";

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export const adminAnalyticsApi = {
  getOverviewStats: async (): Promise<AdminOverviewStats> => {
    const response = await apiClient.get<ApiResponse<AdminOverviewStats>>(
      "/admin/analytics/overview"
    );
    return response.data.data;
  },

  getReportTrend: async (params?: TrendFilterParams): Promise<ReportTrendPoint[]> => {
    const response = await apiClient.get<ApiResponse<ReportTrendPoint[]>>(
      "/admin/analytics/reports/trend",
      {
        params: {
          days: params?.days,
          from: params?.from,
          to: params?.to,
          organizationRefId: params?.organizationRefId,
        },
      }
    );
    return response.data.data;
  },

  getOrganizationReportActivity: async (
    params?: OrgActivityFilterParams
  ): Promise<PageResponse<OrganizationReportActivity>> => {
    const response = await apiClient.get<
      ApiResponse<PageResponse<OrganizationReportActivity>>
    >("/admin/analytics/reports/organizations", {
      params: {
        search: params?.search || undefined,
        page: params?.page ?? 0,
        size: params?.size ?? 10,
        sort: params?.sort ?? "totalReports",
        direction: params?.direction ?? "desc",
      },
    });
    return response.data.data;
  },

  getTestUsage: async (
    limit: number = 10,
    organizationRefId?: string
  ): Promise<TestUsageStats[]> => {
    const response = await apiClient.get<ApiResponse<TestUsageStats[]>>(
      "/admin/analytics/tests/usage",
      {
        params: {
          limit,
          organizationRefId: organizationRefId || undefined,
        },
      }
    );
    return response.data.data;
  },

  getSecurityAuditLogs: async (
    params?: SecurityAuditFilterParams
  ): Promise<PageResponse<SecurityAuditLog>> => {
    const response = await apiClient.get<
      ApiResponse<PageResponse<SecurityAuditLog>>
    >("/admin/audit/security", {
      params: {
        action: params?.action || undefined,
        targetOrganizationRefId: params?.targetOrganizationRefId || undefined,
        success: params?.success !== undefined ? params.success : undefined,
        from: params?.from || undefined,
        to: params?.to || undefined,
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? "createdAt",
        direction: params?.direction ?? "desc",
      },
    });
    return response.data.data;
  },

  breakGlassAccess: async (
    reportRefId: string,
    data: { organizationRefId: string; justification: string }
  ): Promise<any> => {
    const response = await apiClient.post<ApiResponse<any>>(
      `/reports/${encodeURIComponent(reportRefId)}/break-glass`,
      data
    );
    return response.data.data;
  },
};
