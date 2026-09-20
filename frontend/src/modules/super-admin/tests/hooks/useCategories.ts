import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";

import { testCategoryApi } from "../api/testCategoryApi";
import type {
  CreateTestCategoryRequest,
  PagedResponse,
  TestCategoryQueryParams,
  TestCategoryResponse,
  UpdateTestCategoryRequest,
} from "../types/categoryTypes";

export const CATEGORY_QUERY_KEYS = {
  all: ["test-categories"] as const,
  list: (params: TestCategoryQueryParams) =>
    ["test-categories", "list", params] as const,
  activeList: () => ["test-categories", "active-list"] as const,
  detail: (refId: string) => ["test-categories", "detail", refId] as const,
};

export function useCategoriesQuery(
  params: TestCategoryQueryParams = {},
): UseQueryResult<PagedResponse<TestCategoryResponse>, Error> {
  return useQuery({
    queryKey: CATEGORY_QUERY_KEYS.list(params),
    queryFn: () => testCategoryApi.getCategories(params),
    placeholderData: (previousData) => previousData,
  });
}

/**
 * Hook for dropdowns to fetch all active categories
 */
export function useActiveCategoriesQuery(): UseQueryResult<
  PagedResponse<TestCategoryResponse>,
  Error
> {
  return useQuery({
    queryKey: CATEGORY_QUERY_KEYS.activeList(),
    queryFn: () =>
      testCategoryApi.getCategories({
        status: "ACTIVE",
        size: 100,
        sortBy: "name",
        sortDirection: "asc",
      }),
  });
}

export function useCategoryQuery(
  refId: string | undefined,
): UseQueryResult<TestCategoryResponse, Error> {
  return useQuery({
    queryKey: CATEGORY_QUERY_KEYS.detail(refId || ""),
    queryFn: () => testCategoryApi.getCategory(refId!),
    enabled: Boolean(refId),
  });
}

export function useCreateCategoryMutation(): UseMutationResult<
  TestCategoryResponse,
  Error,
  CreateTestCategoryRequest
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateTestCategoryRequest) =>
      testCategoryApi.createCategory(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.all,
      });
    },
  });
}

export function useUpdateCategoryMutation(): UseMutationResult<
  TestCategoryResponse,
  Error,
  { refId: string; request: UpdateTestCategoryRequest }
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ refId, request }) =>
      testCategoryApi.updateCategory(refId, request),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.detail(data.refId),
      });
    },
  });
}

export function useDeleteCategoryMutation(): UseMutationResult<
  void,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => testCategoryApi.deleteCategory(refId),
    onSuccess: (_, refId) => {
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.detail(refId),
      });
    },
  });
}

export function useReactivateCategoryMutation(): UseMutationResult<
  TestCategoryResponse,
  Error,
  string
> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => testCategoryApi.reactivateCategory(refId),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.all,
      });
      void queryClient.invalidateQueries({
        queryKey: CATEGORY_QUERY_KEYS.detail(data.refId),
      });
    },
  });
}
