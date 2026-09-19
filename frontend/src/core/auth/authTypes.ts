export type UserRole =
  | "SUPER_ADMIN"
  | "ORG_ADMIN"
  | "LAB_STAFF";

export type UserStatus =
  | "ACTIVE"
  | "INACTIVE";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresAt: string;

  userRefId: string;
  name: string;
  email: string;
  role: UserRole;
  organizationRefId: string | null;
}

export interface AuthUser {
  userRefId: string;
  refId: string;
  name: string;
  email: string;
  role: UserRole;
  organizationRefId: string | null;
}

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

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface ApiErrorResponse {
  success: false;
  status: number;
  code: string;
  message: string;
  path: string;
  timestamp: string;
  errors?: Record<string, string>;
}