import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import { userApi } from "../api/userApi";
import type {
  CreateUserRequest,
  UpdateUserRequest,
  UpdateUserStatusRequest,
  UserQueryParams,
} from "../types/userTypes";

export const userKeys = {
  all: ["users"] as const,
  lists: () => [...userKeys.all, "list"] as const,
  list: (params: UserQueryParams) => [...userKeys.lists(), params] as const,
  details: () => [...userKeys.all, "detail"] as const,
  detail: (refId: string) => [...userKeys.details(), refId] as const,
};

export function useUsersQuery(params: UserQueryParams = {}) {
  return useQuery({
    queryKey: userKeys.list(params),
    queryFn: () => userApi.getUsers(params),
  });
}

export function useUserQuery(refId: string | null, enabled = true) {
  return useQuery({
    queryKey: userKeys.detail(refId ?? ""),
    queryFn: () => userApi.getUser(refId!),
    enabled: Boolean(refId) && enabled,
  });
}

export function useCreateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateUserRequest) => userApi.createUser(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: userKeys.lists(),
      });
    },
  });
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      data,
    }: {
      refId: string;
      data: UpdateUserRequest;
    }) => userApi.updateUser(refId, data),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: userKeys.lists(),
      });
      void queryClient.invalidateQueries({
        queryKey: userKeys.detail(variables.refId),
      });
    },
  });
}

export function useUpdateUserStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      data,
    }: {
      refId: string;
      data: UpdateUserStatusRequest;
    }) => userApi.updateUserStatus(refId, data),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: userKeys.lists(),
      });
      void queryClient.invalidateQueries({
        queryKey: userKeys.detail(variables.refId),
      });
    },
  });
}
