import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { licensingApi } from "../api/licensingApi";
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

export const licensingKeys = {
  allPlans: ["plans"] as const,
  planDetail: (refId: string) => ["plans", refId] as const,
  organizationLicense: (orgRefId: string) =>
    ["organizations", orgRefId, "license"] as const,
  upgradeRequests: (params?: unknown) =>
    ["admin", "upgrade-requests", params] as const,
  upgradeRequestDetail: (refId: string) =>
    ["admin", "upgrade-requests", "detail", refId] as const,
};

// ============================================================
// PLAN HOOKS
// ============================================================

export function usePlansQuery() {
  return useQuery<PlanResponse[], Error>({
    queryKey: licensingKeys.allPlans,
    queryFn: () => licensingApi.getPlans(),
    staleTime: 30 * 1000,
  });
}

export function usePlanQuery(planRefId?: string) {
  return useQuery<PlanResponse, Error>({
    queryKey: licensingKeys.planDetail(planRefId ?? ""),
    queryFn: () => licensingApi.getPlan(planRefId!),
    enabled: Boolean(planRefId && planRefId.trim() !== ""),
    staleTime: 30 * 1000,
  });
}

export function useCreatePlanMutation() {
  const queryClient = useQueryClient();
  return useMutation<PlanResponse, Error, CreatePlanRequest>({
    mutationFn: (request) => licensingApi.createPlan(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: licensingKeys.allPlans });
    },
  });
}

export function useUpdatePlanMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    PlanResponse,
    Error,
    { planRefId: string; request: UpdatePlanRequest }
  >({
    mutationFn: ({ planRefId, request }) =>
      licensingApi.updatePlan(planRefId, request),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: licensingKeys.allPlans });
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.planDetail(variables.planRefId),
      });
    },
  });
}

// ============================================================
// LICENSE HOOKS
// ============================================================

export function useOrganizationLicenseQuery(organizationRefId?: string) {
  return useQuery<LicenseResponse | null, Error>({
    queryKey: licensingKeys.organizationLicense(organizationRefId ?? ""),
    queryFn: async () => {
      if (!organizationRefId) return null;
      try {
        return await licensingApi.getOrganizationLicense(organizationRefId);
      } catch (err: unknown) {
        if (axios.isAxiosError(err) && err.response?.status === 404) {
          return null; // Clean empty license state when 404
        }
        throw err;
      }
    },
    enabled: Boolean(organizationRefId && organizationRefId.trim() !== ""),
    retry: (failureCount, error) => {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return false;
      }
      return failureCount < 2;
    },
    staleTime: 30 * 1000,
  });
}

export function useActivateLicenseMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    LicenseResponse,
    Error,
    { organizationRefId: string; request: ActivateLicenseRequest }
  >({
    mutationFn: ({ organizationRefId, request }) =>
      licensingApi.activateLicense(organizationRefId, request),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.organizationLicense(variables.organizationRefId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", variables.organizationRefId],
      });
    },
  });
}

export function useRenewLicenseMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    LicenseResponse,
    Error,
    { organizationRefId: string; request: RenewLicenseRequest }
  >({
    mutationFn: ({ organizationRefId, request }) =>
      licensingApi.renewLicense(organizationRefId, request),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.organizationLicense(variables.organizationRefId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", variables.organizationRefId],
      });
    },
  });
}

export function useDeactivateLicenseMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    LicenseResponse,
    Error,
    string // organizationRefId
  >({
    mutationFn: (organizationRefId) =>
      licensingApi.deactivateLicense(organizationRefId),
    onSuccess: (_, organizationRefId) => {
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.organizationLicense(organizationRefId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", organizationRefId],
      });
    },
  });
}

export function useReactivateLicenseMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    LicenseResponse,
    Error,
    string // organizationRefId
  >({
    mutationFn: (organizationRefId) =>
      licensingApi.reactivateLicense(organizationRefId),
    onSuccess: (_, organizationRefId) => {
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.organizationLicense(organizationRefId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", organizationRefId],
      });
    },
  });
}

// ============================================================
// UPGRADE REQUEST HOOKS (SUPER_ADMIN)
// ============================================================

export function useAdminUpgradeRequestsQuery(params?: {
  status?: UpgradeRequestStatus | string;
  organizationRefId?: string;
  page?: number;
  size?: number;
}) {
  return useQuery<PaginatedUpgradeRequestsResponse, Error>({
    queryKey: licensingKeys.upgradeRequests(params),
    queryFn: () => licensingApi.getUpgradeRequests(params),
    staleTime: 15 * 1000,
  });
}

export function useAdminUpgradeRequestQuery(refId?: string) {
  return useQuery<PlanUpgradeRequestResponse, Error>({
    queryKey: licensingKeys.upgradeRequestDetail(refId ?? ""),
    queryFn: () => licensingApi.getUpgradeRequest(refId!),
    enabled: Boolean(refId && refId.trim() !== ""),
    staleTime: 15 * 1000,
  });
}

export function useUpdateUpgradeRequestStatusMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    PlanUpgradeRequestResponse,
    Error,
    { refId: string; request: UpdateUpgradeRequestStatusRequest }
  >({
    mutationFn: ({ refId, request }) =>
      licensingApi.updateUpgradeRequestStatus(refId, request),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({
        queryKey: ["admin", "upgrade-requests"],
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", data.organizationRefId],
      });
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.organizationLicense(data.organizationRefId),
      });
      void queryClient.invalidateQueries({
        queryKey: licensingKeys.allPlans,
      });
    },
  });
}

