import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type { SpringPage } from "@/modules/super-admin/tests/types/testTypes";
import type { OrganizationTestResponse } from "@/modules/super-admin/tests/types/assignmentTypes";

export interface TenantTestQueryParams {
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
}

export const tenantTestApi = {
  async getMyOrganizationTests(
    params: TenantTestQueryParams = {},
  ): Promise<SpringPage<OrganizationTestResponse>> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 50,
      sort: params.sort ?? "test.name",
      direction: params.direction ?? "asc",
    };

    if (params.status && params.status.trim()) {
      queryParams.status = params.status.trim();
    }

    const response = await apiClient.get<
      ApiResponse<SpringPage<OrganizationTestResponse>>
    >("/organization-tests/my", {
      params: queryParams,
    });
    return response.data.data;
  },
};
