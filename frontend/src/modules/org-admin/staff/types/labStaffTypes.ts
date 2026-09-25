export type UserRole = "SUPER_ADMIN" | "ORG_ADMIN" | "LAB_STAFF";
export type UserStatus = "ACTIVE" | "INACTIVE";

export interface LabStaffResponse {
  refId: string;
  name: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  organizationRefId: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
  inactiveAt?: string | null;
  eligibleForCleanupAt?: string | null;
  cleanupEligible?: boolean;
  deactivatedByName?: string | null;
  reportsCreated?: number;
  reportsFinalized?: number;
}

export interface LabStaffSummaryResponse {
  totalStaff: number;
  activeStaff: number;
  maxLabStaff: number;
  remainingSlots: number;
  limitReached: boolean;
  planCode: string | null;
  planName: string | null;
}

export interface LabStaffDetailsResponse extends LabStaffResponse {
  organizationName: string | null;
  reportsCreated: number;
  reportsFinalized: number;
}

export interface CreateLabStaffRequest {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface UpdateLabStaffStatusRequest {
  status: UserStatus;
}

export interface PagedLabStaffResponse {
  content: LabStaffResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface LabStaffQueryParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: "ASC" | "DESC";
  status?: UserStatus;
  search?: string;
}
