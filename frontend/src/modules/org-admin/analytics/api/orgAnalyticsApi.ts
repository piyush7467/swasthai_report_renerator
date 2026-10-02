import { apiClient } from "@/core/api/apiClient";
import type {
  OrgOverviewStats,
  OrgReportTrendPoint,
  OrgCategoryUsageStats,
  OrgPatientTrendPoint,
  OrgActivityItem,
} from "../types/orgAnalyticsTypes";

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export const orgAnalyticsApi = {
  getOverviewStats: async (): Promise<OrgOverviewStats> => {
    const response = await apiClient.get<ApiResponse<OrgOverviewStats>>(
      "/organization/analytics/overview"
    );
    return response.data.data;
  },

  getReportTrend: async (days: number = 30): Promise<OrgReportTrendPoint[]> => {
    const response = await apiClient.get<ApiResponse<OrgReportTrendPoint[]>>(
      "/organization/analytics/reports/trend",
      {
        params: { days },
      }
    );
    return response.data.data;
  },

  getCategoryUsage: async (limit: number = 6): Promise<OrgCategoryUsageStats[]> => {
    const response = await apiClient.get<ApiResponse<OrgCategoryUsageStats[]>>(
      "/organization/analytics/tests/category-usage",
      {
        params: { limit },
      }
    );
    return response.data.data;
  },

  getPatientTrend: async (days: number = 30): Promise<OrgPatientTrendPoint[]> => {
    const response = await apiClient.get<ApiResponse<OrgPatientTrendPoint[]>>(
      "/organization/analytics/patients/trend",
      {
        params: { days },
      }
    );
    return response.data.data;
  },

  getRecentActivity: async (): Promise<OrgActivityItem[]> => {
    const response = await apiClient.get<ApiResponse<OrgActivityItem[]>>(
      "/organization/analytics/activity"
    );
    return response.data.data;
  },
};
