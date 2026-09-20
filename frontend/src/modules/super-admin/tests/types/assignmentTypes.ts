export type OrganizationTestStatus = "ACTIVE" | "INACTIVE";

export interface OrganizationTestResponse {
  refId: string;
  organizationRefId: string;
  organizationName: string;
  testRefId: string;
  testCode: string;
  testName: string;
  testType: string;
  status: OrganizationTestStatus;
  effectiveFrom?: string | null;
  effectiveUntil?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AssignTestRequest {
  organizationRefId: string;
  testRefId: string;
  effectiveFrom?: string;
  effectiveUntil?: string;
}

export interface UpdateOrganizationTestRequest {
  status?: OrganizationTestStatus;
  effectiveFrom?: string;
  effectiveUntil?: string;
}

export interface OrganizationTestQueryParams {
  organizationRefId: string;
  status?: string;
  page?: number;
  size?: number;
  sort?:
    | "createdAt"
    | "updatedAt"
    | "effectiveFrom"
    | "effectiveUntil"
    | "status"
    | "test.name"
    | "test.code"
    | "organization.name";
  direction?: "asc" | "desc";
}
