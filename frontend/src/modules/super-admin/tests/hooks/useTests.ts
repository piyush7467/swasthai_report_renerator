import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";

import { testApi } from "../api/testApi";
import type {
  CreateTestRequest,
  SpringPage,
  TestQueryParams,
  TestResponse,
  UpdateTestRequest,
} from "../types/testTypes";

export const TEST_QUERY_KEYS = {
  all: ["tests"] as const,
  list: (params: TestQueryParams) => ["tests", "list", params] as const,
  detail: (refId: string) => ["tests", "detail", refId] as const,
};

export function useTestsQuery(
  params: TestQueryParams = {},
): UseQueryResult<SpringPage<TestResponse>, Error> {
  return useQuery({
    queryKey: TEST_QUERY_KEYS.list(params),
    queryFn: () => testApi.getTests(params),
    placeholderData: (previousData) => previousData,
  });
}

export function useTestQuery(
  refId: string | undefined,
): UseQueryResult<TestResponse, Error> {
  return useQuery({
    queryKey: TEST_QUERY_KEYS.detail(refId || ""),
    queryFn: () => testApi.getTest(refId!),
    enabled: Boolean(refId),
  });
}

export function useCreateTestMutation(): UseMutationResult<
  TestResponse,
  Error,
  CreateTestRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateTestRequest) => testApi.createTest(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TEST_QUERY_KEYS.all });
    },
  });
}

export function useUpdateTestMutation(): UseMutationResult<
  TestResponse,
  Error,
  { refId: string; request: UpdateTestRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ refId, request }) => testApi.updateTest(refId, request),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({ queryKey: TEST_QUERY_KEYS.all });
      void queryClient.invalidateQueries({
        queryKey: TEST_QUERY_KEYS.detail(data.refId),
      });
    },
  });
}

export function useDeleteTestMutation(): UseMutationResult<
  void,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => testApi.deleteTest(refId),
    onSuccess: (_, refId) => {
      void queryClient.invalidateQueries({ queryKey: TEST_QUERY_KEYS.all });
      void queryClient.invalidateQueries({
        queryKey: TEST_QUERY_KEYS.detail(refId),
      });
    },
  });
}
