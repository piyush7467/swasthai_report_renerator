import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";
import { orgProfileApi } from "../api/orgProfileApi";
import type {
  OrganizationProfileResponse,
  UpdateOrganizationProfileRequest,
} from "@/modules/super-admin/types/organizationTypes";

export const ORG_PROFILE_QUERY_KEYS = {
  me: ["organization-profile", "me"] as const,
  logo: ["organization-profile", "me", "logo"] as const,
  signature: ["organization-profile", "me", "signature"] as const,
};

export function useMyOrganizationProfileQuery(): UseQueryResult<
  OrganizationProfileResponse,
  Error
> {
  return useQuery({
    queryKey: ORG_PROFILE_QUERY_KEYS.me,
    queryFn: () => orgProfileApi.getMyProfile(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useMyLogoBlobQuery(enabled: boolean) {
  return useQuery({
    queryKey: ORG_PROFILE_QUERY_KEYS.logo,
    queryFn: () => orgProfileApi.getLogoBlob(),
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useMySignatureBlobQuery(enabled: boolean) {
  return useQuery({
    queryKey: ORG_PROFILE_QUERY_KEYS.signature,
    queryFn: () => orgProfileApi.getSignatureBlob(),
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUpdateMyOrganizationProfileMutation(): UseMutationResult<
  OrganizationProfileResponse,
  Error,
  UpdateOrganizationProfileRequest
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateOrganizationProfileRequest) =>
      orgProfileApi.updateMyProfile(data),
    onSuccess: (updated) => {
      queryClient.setQueryData(ORG_PROFILE_QUERY_KEYS.me, updated);
      queryClient.invalidateQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.me });
    },
  });
}

export function useUploadMyLogoMutation(): UseMutationResult<
  OrganizationProfileResponse,
  Error,
  File
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => orgProfileApi.uploadLogo(file),
    onSuccess: (updated) => {
      queryClient.setQueryData(ORG_PROFILE_QUERY_KEYS.me, updated);
      queryClient.invalidateQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.me });
      queryClient.invalidateQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.logo });
    },
  });
}

export function useDeleteMyLogoMutation(): UseMutationResult<
  OrganizationProfileResponse,
  Error,
  void
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => orgProfileApi.deleteLogo(),
    onSuccess: (updated) => {
      queryClient.setQueryData(ORG_PROFILE_QUERY_KEYS.me, updated);
      queryClient.invalidateQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.me });
      queryClient.removeQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.logo });
    },
  });
}

export function useUploadMySignatureMutation(): UseMutationResult<
  OrganizationProfileResponse,
  Error,
  File
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => orgProfileApi.uploadSignature(file),
    onSuccess: (updated) => {
      queryClient.setQueryData(ORG_PROFILE_QUERY_KEYS.me, updated);
      queryClient.invalidateQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.me });
      queryClient.invalidateQueries({
        queryKey: ORG_PROFILE_QUERY_KEYS.signature,
      });
    },
  });
}

export function useDeleteMySignatureMutation(): UseMutationResult<
  OrganizationProfileResponse,
  Error,
  void
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => orgProfileApi.deleteSignature(),
    onSuccess: (updated) => {
      queryClient.setQueryData(ORG_PROFILE_QUERY_KEYS.me, updated);
      queryClient.invalidateQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.me });
      queryClient.removeQueries({ queryKey: ORG_PROFILE_QUERY_KEYS.signature });
    },
  });
}
