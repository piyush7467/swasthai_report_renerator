import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";
import { labStaffApi } from "../api/labStaffApi";
import type {
  CreateLabStaffRequest,
  LabStaffDetailsResponse,
  LabStaffQueryParams,
  LabStaffResponse,
  LabStaffSummaryResponse,
  PagedLabStaffResponse,
  UpdateLabStaffStatusRequest,
} from "../types/labStaffTypes";

export const LAB_STAFF_QUERY_KEYS = {
  all: ["lab-staff"] as const,
  summary: () => ["lab-staff", "summary"] as const,
  list: (params: LabStaffQueryParams) => ["lab-staff", "list", params] as const,
  detail: (refId: string) => ["lab-staff", "detail", refId] as const,
};

export function useLabStaffSummaryQuery(): UseQueryResult<
  LabStaffSummaryResponse,
  Error
> {
  return useQuery({
    queryKey: LAB_STAFF_QUERY_KEYS.summary(),
    queryFn: () => labStaffApi.getSummary(),
  });
}

export function useLabStaffListQuery(
  params: LabStaffQueryParams = {}
): UseQueryResult<PagedLabStaffResponse, Error> {
  return useQuery({
    queryKey: LAB_STAFF_QUERY_KEYS.list(params),
    queryFn: () => labStaffApi.getStaffList(params),
    placeholderData: (previousData) => previousData,
  });
}

export function useLabStaffDetailsQuery(
  refId: string | undefined
): UseQueryResult<LabStaffDetailsResponse, Error> {
  return useQuery({
    queryKey: LAB_STAFF_QUERY_KEYS.detail(refId || ""),
    queryFn: () => labStaffApi.getStaffDetails(refId!),
    enabled: Boolean(refId),
  });
}

export function useCreateLabStaffMutation(): UseMutationResult<
  LabStaffResponse,
  Error,
  CreateLabStaffRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateLabStaffRequest) =>
      labStaffApi.createStaff(request),
    onSuccess: (newStaff) => {
      queryClient.setQueryData(
        LAB_STAFF_QUERY_KEYS.detail(newStaff.refId),
        newStaff
      );
      void queryClient.invalidateQueries({
        queryKey: LAB_STAFF_QUERY_KEYS.all,
      });
    },
  });
}

export function useUpdateLabStaffStatusMutation(): UseMutationResult<
  LabStaffResponse,
  Error,
  { refId: string; request: UpdateLabStaffStatusRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ refId, request }) =>
      labStaffApi.updateStaffStatus(refId, request),
    onSuccess: (updated) => {
      queryClient.setQueryData(
        LAB_STAFF_QUERY_KEYS.detail(updated.refId),
        updated
      );
      void queryClient.invalidateQueries({
        queryKey: LAB_STAFF_QUERY_KEYS.all,
      });
    },
  });
}

export function useDeactivateLabStaffMutation(): UseMutationResult<
  LabStaffResponse,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => labStaffApi.deactivateStaff(refId),
    onSuccess: (updated) => {
      queryClient.setQueryData(
        LAB_STAFF_QUERY_KEYS.detail(updated.refId),
        updated
      );
      void queryClient.invalidateQueries({
        queryKey: LAB_STAFF_QUERY_KEYS.all,
      });
    },
  });
}

export function useDeleteLabStaffMutation(): UseMutationResult<
  void,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => labStaffApi.deleteStaff(refId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: LAB_STAFF_QUERY_KEYS.all,
      });
    },
  });
}
