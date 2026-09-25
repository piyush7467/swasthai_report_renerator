import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orgLicensingApi } from "../api/orgLicensingApi";
import type {
  CreateUpgradeRequest,
  UpgradeRequestStatus,
} from "../types/licenseTypes";

export const LICENSE_QUERY_KEYS = {
  overview: ["org-license", "overview"] as const,
  availablePlans: ["org-license", "available-plans"] as const,
  requests: (page: number, size: number, status?: UpgradeRequestStatus) =>
    ["org-license", "requests", { page, size, status }] as const,
  requestDetails: (refId: string) =>
    ["org-license", "request-details", refId] as const,
};

export function useLicenseOverview() {
  return useQuery({
    queryKey: LICENSE_QUERY_KEYS.overview,
    queryFn: () => orgLicensingApi.getOverview(),
    staleTime: 30_000,
  });
}

export function useAvailablePlans() {
  return useQuery({
    queryKey: LICENSE_QUERY_KEYS.availablePlans,
    queryFn: () => orgLicensingApi.getAvailablePlans(),
    staleTime: 60_000,
  });
}

export function useUpgradeRequests(
  page: number = 0,
  size: number = 10,
  status?: UpgradeRequestStatus
) {
  return useQuery({
    queryKey: LICENSE_QUERY_KEYS.requests(page, size, status),
    queryFn: () => orgLicensingApi.getMyRequests(page, size, status),
    staleTime: 15_000,
  });
}

export function useUpgradeRequestDetails(refId?: string) {
  return useQuery({
    queryKey: LICENSE_QUERY_KEYS.requestDetails(refId ?? ""),
    queryFn: () => orgLicensingApi.getRequestDetails(refId!),
    enabled: Boolean(refId),
  });
}

export function useCreateUpgradeRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateUpgradeRequest) =>
      orgLicensingApi.createUpgradeRequest(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["org-license"] });
    },
  });
}

export function useCancelUpgradeRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (refId: string) => orgLicensingApi.cancelRequest(refId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["org-license"] });
    },
  });
}
