import { useQuery } from "@tanstack/react-query";
import { orgAnalyticsApi } from "../api/orgAnalyticsApi";

export const ORG_ANALYTICS_QUERY_KEYS = {
  overview: ["org", "analytics", "overview"] as const,
  trend: (days?: number) => ["org", "analytics", "trend", days] as const,
  categoryUsage: (limit?: number) => ["org", "analytics", "category-usage", limit] as const,
  patientTrend: (days?: number) => ["org", "analytics", "patient-trend", days] as const,
  activity: ["org", "analytics", "activity"] as const,
};

export function useOrgOverviewStats() {
  return useQuery({
    queryKey: ORG_ANALYTICS_QUERY_KEYS.overview,
    queryFn: orgAnalyticsApi.getOverviewStats,
    staleTime: 60 * 1000,
  });
}

export function useOrgReportTrend(days: number = 30) {
  return useQuery({
    queryKey: ORG_ANALYTICS_QUERY_KEYS.trend(days),
    queryFn: () => orgAnalyticsApi.getReportTrend(days),
    staleTime: 60 * 1000,
  });
}

export function useOrgCategoryUsage(limit: number = 6) {
  return useQuery({
    queryKey: ORG_ANALYTICS_QUERY_KEYS.categoryUsage(limit),
    queryFn: () => orgAnalyticsApi.getCategoryUsage(limit),
    staleTime: 60 * 1000,
  });
}

export function useOrgPatientTrend(days: number = 30) {
  return useQuery({
    queryKey: ORG_ANALYTICS_QUERY_KEYS.patientTrend(days),
    queryFn: () => orgAnalyticsApi.getPatientTrend(days),
    staleTime: 60 * 1000,
  });
}

export function useOrgRecentActivity() {
  return useQuery({
    queryKey: ORG_ANALYTICS_QUERY_KEYS.activity,
    queryFn: orgAnalyticsApi.getRecentActivity,
    staleTime: 30 * 1000,
  });
}
