import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { orgProfileApi } from "../api/orgProfileApi";
import type { OrganizationProfileResponse } from "@/modules/super-admin/types/organizationTypes";

export const ORG_PROFILE_QUERY_KEYS = {
  me: ["organization-profile", "me"] as const,
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
