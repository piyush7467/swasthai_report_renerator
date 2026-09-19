import { apiClient } from "../api/apiClient";

import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  UserResponse,
} from "./authTypes";

export const authApi = {
  async login(
    request: LoginRequest,
  ): Promise<LoginResponse> {
    const response = await apiClient.post<
      ApiResponse<LoginResponse>
    >("/auth/login", request);

    return response.data.data;
  },

  async refresh(
    request: RefreshTokenRequest,
  ): Promise<LoginResponse> {
    const response = await apiClient.post<
      ApiResponse<LoginResponse>
    >("/auth/refresh", request);

    return response.data.data;
  },

  async logout(
    request: RefreshTokenRequest,
  ): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      "/auth/logout",
      request,
    );
  },

  async getUser(
    refId: string,
  ): Promise<UserResponse> {
    const response = await apiClient.get<
      ApiResponse<UserResponse>
    >(
      `/users/${encodeURIComponent(refId)}`,
    );

    return response.data.data;
  },
};