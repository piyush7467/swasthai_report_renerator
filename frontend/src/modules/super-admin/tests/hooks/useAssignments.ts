import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";

import { organizationTestApi } from "../api/organizationTestApi";
import type { SpringPage } from "../types/testTypes";
import type {
  AssignTestRequest,
  OrganizationTestQueryParams,
  OrganizationTestResponse,
  UpdateOrganizationTestRequest,
} from "../types/assignmentTypes";

export const ASSIGNMENT_QUERY_KEYS = {
  all: ["organization-tests"] as const,
  list: (params: OrganizationTestQueryParams) =>
    ["organization-tests", "list", params] as const,
  detail: (refId: string) => ["organization-tests", "detail", refId] as const,
};

export function useAssignmentsQuery(
  params: OrganizationTestQueryParams,
): UseQueryResult<SpringPage<OrganizationTestResponse>, Error> {
  return useQuery({
    queryKey: ASSIGNMENT_QUERY_KEYS.list(params),
    queryFn: () => organizationTestApi.getAllAssignments(params),
    enabled: Boolean(params.organizationRefId),
    placeholderData: (previousData) => previousData,
  });
}

export function useAssignmentQuery(
  refId: string | undefined,
): UseQueryResult<OrganizationTestResponse, Error> {
  return useQuery({
    queryKey: ASSIGNMENT_QUERY_KEYS.detail(refId || ""),
    queryFn: () => organizationTestApi.getAssignment(refId!),
    enabled: Boolean(refId),
  });
}

export function useAssignTestMutation(): UseMutationResult<
  OrganizationTestResponse,
  Error,
  AssignTestRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AssignTestRequest) =>
      organizationTestApi.assignTest(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ASSIGNMENT_QUERY_KEYS.all,
      });
    },
  });
}

export function useUpdateAssignmentMutation(): UseMutationResult<
  OrganizationTestResponse,
  Error,
  { refId: string; request: UpdateOrganizationTestRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ refId, request }) =>
      organizationTestApi.updateAssignment(refId, request),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({
        queryKey: ASSIGNMENT_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: ASSIGNMENT_QUERY_KEYS.detail(data.refId),
      });
    },
  });
}

export function useDeactivateAssignmentMutation(): UseMutationResult<
  void,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) =>
      organizationTestApi.deactivateAssignment(refId),
    onSuccess: (_, refId) => {
      void queryClient.invalidateQueries({
        queryKey: ASSIGNMENT_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: ASSIGNMENT_QUERY_KEYS.detail(refId),
      });
    },
  });
}
