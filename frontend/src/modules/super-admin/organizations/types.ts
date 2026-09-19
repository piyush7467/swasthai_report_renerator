export type OrganizationStatus =
  | "ACTIVE"
  | "SUSPENDED"
  | "DISABLED";

export interface Organization {
  refId: string;
  name: string;
  code: string;
  status: OrganizationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrganizationPageResponse {
  content: Organization[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CreateOrganizationRequest {
  name: string;
  code: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  code: string;
}

export interface UpdateOrganizationStatusRequest {
  status: OrganizationStatus;
}