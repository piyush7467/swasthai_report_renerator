import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";

import { testParameterApi } from "../api/testParameterApi";
import type { SpringPage } from "../types/testTypes";
import type {
  CreateTestParameterRequest,
  ParameterQueryParams,
  TestParameterResponse,
  UpdateTestParameterRequest,
} from "../types/parameterTypes";

export const PARAMETER_QUERY_KEYS = {
  all: ["test-parameters"] as const,
  list: (testRefId: string, params: ParameterQueryParams) =>
    ["test-parameters", "list", testRefId, params] as const,
  detail: (parameterRefId: string) =>
    ["test-parameters", "detail", parameterRefId] as const,
};

export function useTestParametersQuery(
  testRefId: string | undefined,
  params: ParameterQueryParams = {},
): UseQueryResult<SpringPage<TestParameterResponse>, Error> {
  return useQuery({
    queryKey: PARAMETER_QUERY_KEYS.list(testRefId || "", params),
    queryFn: () => testParameterApi.getTestParameters(testRefId!, params),
    enabled: Boolean(testRefId),
    placeholderData: (previousData) => previousData,
  });
}

export function useTestParameterQuery(
  parameterRefId: string | undefined,
): UseQueryResult<TestParameterResponse, Error> {
  return useQuery({
    queryKey: PARAMETER_QUERY_KEYS.detail(parameterRefId || ""),
    queryFn: () => testParameterApi.getParameter(parameterRefId!),
    enabled: Boolean(parameterRefId),
  });
}

export function useCreateParameterMutation(testRefId: string): UseMutationResult<
  TestParameterResponse,
  Error,
  CreateTestParameterRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateTestParameterRequest) =>
      testParameterApi.createParameter(testRefId, request),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: PARAMETER_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: ["tests"],
      });
    },
  });
}

export function useUpdateParameterMutation(): UseMutationResult<
  TestParameterResponse,
  Error,
  { parameterRefId: string; request: UpdateTestParameterRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ parameterRefId, request }) =>
      testParameterApi.updateParameter(parameterRefId, request),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({
        queryKey: PARAMETER_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: PARAMETER_QUERY_KEYS.detail(data.refId),
      });
    },
  });
}

export function useDeactivateParameterMutation(): UseMutationResult<
  void,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (parameterRefId: string) =>
      testParameterApi.deactivateParameter(parameterRefId),
    onSuccess: (_, parameterRefId) => {
      void queryClient.invalidateQueries({
        queryKey: PARAMETER_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: PARAMETER_QUERY_KEYS.detail(parameterRefId),
      });
    },
  });
}
