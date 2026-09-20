export type UserRole = "SUPER_ADMIN" | "ORG_ADMIN" | "LAB_STAFF";

export type UserStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED";

export type UserSortField =
  | "name"
  | "email"
  | "role"
  | "status"
  | "createdAt"
  | "updatedAt"
  | "lastLoginAt";

export type SortDirection = "ASC" | "DESC";

export interface UserResponse {
  refId: string;
  name: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  organizationRefId: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UserPageResponse {
  content: UserResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface UserQueryParams {
  page?: number;
  size?: number;
  sortBy?: UserSortField;
  sortDirection?: SortDirection;
  role?: UserRole;
  status?: UserStatus;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
  organizationRefId?: string | null;
}

export interface UpdateUserRequest {
  name: string;
  email: string;
  role?: UserRole;
  organizationRefId?: string | null;
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
}
