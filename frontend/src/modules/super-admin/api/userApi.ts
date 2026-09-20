import { apiClient } from "@/core/api/apiClient";
import type { ApiResponse } from "@/core/auth/authTypes";
import type {
  CreateUserRequest,
  UpdateUserRequest,
  UpdateUserStatusRequest,
  UserPageResponse,
  UserQueryParams,
  UserResponse,
} from "../types/userTypes";

export const userApi = {
  async getUsers(params: UserQueryParams = {}): Promise<UserPageResponse> {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sortBy: params.sortBy ?? "createdAt",
      sortDirection: params.sortDirection ?? "DESC",
    };

    if (params.role) {
      queryParams.role = params.role;
    }

    if (params.status) {
      queryParams.status = params.status;
    }

    const response = await apiClient.get<ApiResponse<UserPageResponse>>(
      "/users",
      {
        params: queryParams,
      },
    );
    return response.data.data;
  },

  async getUser(refId: string): Promise<UserResponse> {
    const response = await apiClient.get<ApiResponse<UserResponse>>(
      `/users/${encodeURIComponent(refId)}`,
    );
    return response.data.data;
  },

  async createUser(request: CreateUserRequest): Promise<UserResponse> {
    const response = await apiClient.post<ApiResponse<UserResponse>>(
      "/users",
      request,
    );
    return response.data.data;
  },

  async updateUser(
    refId: string,
    request: UpdateUserRequest,
  ): Promise<UserResponse> {
    const response = await apiClient.put<ApiResponse<UserResponse>>(
      `/users/${encodeURIComponent(refId)}`,
      request,
    );
    return response.data.data;
  },

  async updateUserStatus(
    refId: string,
    request: UpdateUserStatusRequest,
  ): Promise<UserResponse> {
    const response = await apiClient.patch<ApiResponse<UserResponse>>(
      `/users/${encodeURIComponent(refId)}/status`,
      request,
    );
    return response.data.data;
  },
};
