import { useQuery } from "@tanstack/react-query";
import { adminAnalyticsApi } from "../api/adminAnalyticsApi";
import type {
  OrgActivityFilterParams,
  SecurityAuditFilterParams,
  TrendFilterParams,
} from "../types/analyticsTypes";

export const ANALYTICS_QUERY_KEYS = {
  overview: ["admin", "analytics", "overview"] as const,
  trend: (params?: TrendFilterParams) =>
    ["admin", "analytics", "trend", params] as const,
  organizations: (params?: OrgActivityFilterParams) =>
    ["admin", "analytics", "organizations", params] as const,
  testUsage: (limit?: number, organizationRefId?: string) =>
    ["admin", "analytics", "test-usage", limit, organizationRefId] as const,
  audit: (params?: SecurityAuditFilterParams) =>
    ["admin", "audit", "security", params] as const,
};

export function useAdminOverviewStats() {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.overview,
    queryFn: adminAnalyticsApi.getOverviewStats,
    staleTime: 60 * 1000, // 1 minute
  });
}

export function useAdminReportTrend(params?: TrendFilterParams) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.trend(params),
    queryFn: () => adminAnalyticsApi.getReportTrend(params),
    staleTime: 60 * 1000,
  });
}

export function useAdminOrgReportActivity(params?: OrgActivityFilterParams) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.organizations(params),
    queryFn: () => adminAnalyticsApi.getOrganizationReportActivity(params),
    staleTime: 60 * 1000,
  });
}

export function useAdminTestUsage(
  limit: number = 10,
  organizationRefId?: string
) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.testUsage(limit, organizationRefId),
    queryFn: () => adminAnalyticsApi.getTestUsage(limit, organizationRefId),
    staleTime: 60 * 1000,
  });
}

export function useAdminSecurityAudit(params?: SecurityAuditFilterParams) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.audit(params),
    queryFn: () => adminAnalyticsApi.getSecurityAuditLogs(params),
    staleTime: 30 * 1000,
  });
}
