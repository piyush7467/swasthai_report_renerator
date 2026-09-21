import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { tenantTestApi, type TenantTestQueryParams } from "../api/tenantTestApi";
import type { SpringPage } from "@/modules/super-admin/tests/types/testTypes";
import type { OrganizationTestResponse } from "@/modules/super-admin/tests/types/assignmentTypes";

export const TENANT_TEST_QUERY_KEYS = {
  all: ["tenant-tests"] as const,
  list: (params: TenantTestQueryParams) => ["tenant-tests", "list", params] as const,
};

export function useTenantTestsQuery(
  params: TenantTestQueryParams = { status: "ACTIVE" },
): UseQueryResult<SpringPage<OrganizationTestResponse>, Error> {
  return useQuery({
    queryKey: TENANT_TEST_QUERY_KEYS.list(params),
    queryFn: () => tenantTestApi.getMyOrganizationTests(params),
    placeholderData: (previousData) => previousData,
  });
}
