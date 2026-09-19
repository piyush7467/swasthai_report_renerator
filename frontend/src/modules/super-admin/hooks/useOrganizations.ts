import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import { organizationApi } from "../api/organizationApi";
import type {
  CreateOrganizationRequest,
  OrganizationQueryParams,
  UpdateOrganizationProfileRequest,
  UpdateOrganizationRequest,
  UpdateOrganizationStatusRequest,
} from "../types/organizationTypes";

export const organizationKeys = {
  all: ["organizations"] as const,
  lists: () => [...organizationKeys.all, "list"] as const,
  list: (params: OrganizationQueryParams) =>
    [...organizationKeys.lists(), params] as const,
  details: () => [...organizationKeys.all, "detail"] as const,
  detail: (refId: string) => [...organizationKeys.details(), refId] as const,
  profiles: () => [...organizationKeys.all, "profile"] as const,
  profile: (refId: string) => [...organizationKeys.profiles(), refId] as const,
};

export function useOrganizationsQuery(params: OrganizationQueryParams = {}) {
  return useQuery({
    queryKey: organizationKeys.list(params),
    queryFn: () => organizationApi.getOrganizations(params),
  });
}

export function useOrganizationQuery(refId: string, enabled = true) {
  return useQuery({
    queryKey: organizationKeys.detail(refId),
    queryFn: () => organizationApi.getOrganization(refId),
    enabled: Boolean(refId) && enabled,
  });
}

export function useOrganizationProfileQuery(
  organizationRefId: string,
  enabled = true,
) {
  return useQuery({
    queryKey: organizationKeys.profile(organizationRefId),
    queryFn: () => organizationApi.getOrganizationProfile(organizationRefId),
    enabled: Boolean(organizationRefId) && enabled,
  });
}

export function useCreateOrganizationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateOrganizationRequest) =>
      organizationApi.createOrganization(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.lists(),
      });
    },
  });
}

export function useUpdateOrganizationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      data,
    }: {
      refId: string;
      data: UpdateOrganizationRequest;
    }) => organizationApi.updateOrganization(refId, data),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.lists(),
      });
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.detail(variables.refId),
      });
    },
  });
}

export function useUpdateOrganizationStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      data,
    }: {
      refId: string;
      data: UpdateOrganizationStatusRequest;
    }) => organizationApi.updateOrganizationStatus(refId, data),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.lists(),
      });
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.detail(variables.refId),
      });
    },
  });
}

export function useUpdateOrganizationProfileMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      data,
    }: {
      refId: string;
      data: UpdateOrganizationProfileRequest;
    }) => organizationApi.updateOrganizationProfile(refId, data),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.profile(variables.refId),
      });
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.detail(variables.refId),
      });
    },
  });
}

export function useUploadOrganizationLogoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      file,
    }: {
      refId: string;
      file: File;
    }) => organizationApi.uploadOrganizationLogo(refId, file),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.profile(variables.refId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", variables.refId, "logo"],
      });
    },
  });
}

export function useDeleteOrganizationLogoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => organizationApi.deleteOrganizationLogo(refId),
    onSuccess: (_, refId) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.profile(refId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", refId, "logo"],
      });
    },
  });
}

export function useUploadOrganizationSignatureMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      refId,
      file,
    }: {
      refId: string;
      file: File;
    }) => organizationApi.uploadOrganizationSignature(refId, file),
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.profile(variables.refId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", variables.refId, "signature"],
      });
    },
  });
}

export function useDeleteOrganizationSignatureMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (refId: string) => organizationApi.deleteOrganizationSignature(refId),
    onSuccess: (_, refId) => {
      void queryClient.invalidateQueries({
        queryKey: organizationKeys.profile(refId),
      });
      void queryClient.invalidateQueries({
        queryKey: ["organizations", refId, "signature"],
      });
    },
  });
}
