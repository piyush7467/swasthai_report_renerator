export type OrganizationStatus = "ACTIVE" | "SUSPENDED" | "DISABLED";

export interface OrganizationResponse {
  refId: string;
  name: string;
  code: string;
  status: OrganizationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrganizationPageResponse {
  content: OrganizationResponse[];
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

export type OrganizationSortField =
  | "createdAt"
  | "updatedAt"
  | "name"
  | "code"
  | "status";

export type SortDirection = "ASC" | "DESC";

export interface OrganizationQueryParams {
  page?: number;
  size?: number;
  sortBy?: OrganizationSortField;
  sortDirection?: SortDirection;
}

export interface OrganizationProfileResponse {
  organizationRefId: string;
  organizationName: string;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  country?: string | null;
  phone?: string | null;
  alternatePhone?: string | null;
  email?: string | null;
  website?: string | null;
  logoConfigured: boolean;
  signatureConfigured: boolean;
  signatureOwnerRefId?: string | null;
  signatureOwnerName?: string | null;
  signatureOwnerEmail?: string | null;
  reportFooterText?: string | null;
  reportDisclaimer?: string | null;
  version?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateOrganizationProfileRequest {
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  country?: string | null;
  phone?: string | null;
  alternatePhone?: string | null;
  email?: string | null;
  website?: string | null;
  reportFooterText?: string | null;
  reportDisclaimer?: string | null;
}
