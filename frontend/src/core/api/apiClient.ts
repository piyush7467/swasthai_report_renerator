import axios from "axios";

import type {
  AxiosError,
  InternalAxiosRequestConfig,
} from "axios";

import { tokenManager } from "../auth/tokenManager";

import type {
  ApiErrorResponse,
  LoginResponse,
} from "../auth/authTypes";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  "http://localhost:8087/api/v1";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

/*
 * Only one refresh operation may run at a time.
 *
 * This is especially important because the backend rotates
 * refresh tokens. Multiple simultaneous refresh requests could
 * otherwise cause refresh-token reuse detection.
 */
let refreshPromise: Promise<string> | null = null;

interface RetryableRequestConfig
  extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/*
 * Refresh the access token using the current refresh token.
 */
const refreshAccessToken =
  async (): Promise<string> => {
    const refreshToken =
      tokenManager.getRefreshToken();

    if (!refreshToken) {
      throw new Error(
        "No refresh token available.",
      );
    }

    const response = await axios.post<{
      success: boolean;
      message: string;
      data: LoginResponse;
    }>(
      `${API_BASE_URL}/auth/refresh`,
      {
        refreshToken,
      },
      {
        headers: {
          "Content-Type": "application/json",
        },
        timeout: 15000,
      },
    );

    const authResponse =
      response.data.data;

    /*
     * Store the newly rotated token pair.
     *
     * The old refresh token must never be reused.
     */
    tokenManager.setTokens(
      authResponse.accessToken,
      authResponse.refreshToken,
    );

    tokenManager.setUser({
      userRefId: authResponse.userRefId,
      refId: authResponse.userRefId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
      organizationRefId: authResponse.organizationRefId,
    });

    return authResponse.accessToken;
  };

/*
 * Attach the current access token to protected requests.
 * Do not attach Authorization unnecessarily to public auth requests (login/refresh).
 */
apiClient.interceptors.request.use(
  (config) => {
    const isAuthPublic =
      config.url?.includes("/auth/login") ||
      config.url?.includes("/auth/refresh");

    const accessToken =
      tokenManager.getAccessToken();

    if (accessToken && !isAuthPublic) {
      config.headers.Authorization =
        `Bearer ${accessToken}`;
    }

    return config;
  },
  (error) =>
    Promise.reject(error),
);

/*
 * Centralized authentication failure handling.
 */
apiClient.interceptors.response.use(
  (response) => response,

  async (
    error: AxiosError<ApiErrorResponse>,
  ) => {
    const originalRequest =
      error.config as
        | RetryableRequestConfig
        | undefined;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    const status =
      error.response?.status;

    /*
     * Never attempt to refresh on refresh endpoint itself or on login endpoint.
     *
     * 401 on /auth/login means invalid credentials.
     * 401 on /auth/refresh means refresh token invalid/expired/reused.
     */
    const isRefreshRequest =
      originalRequest.url?.includes(
        "/auth/refresh",
      );

    const isLoginRequest =
      originalRequest.url?.includes(
        "/auth/login",
      );

    if (
      status !== 401 ||
      originalRequest._retry ||
      isRefreshRequest ||
      isLoginRequest
    ) {
      return Promise.reject(error);
    }

    const refreshToken =
      tokenManager.getRefreshToken();

    /*
     * There is nothing to refresh with.
     */
    if (!refreshToken) {
      tokenManager.clearTokens();

      return Promise.reject(error);
    }

    /*
     * Mark this exact request so it can never be retried
     * more than once.
     */
    originalRequest._retry = true;

    try {
      /*
       * Single-flight refresh.
       *
       * If another request is already refreshing the token,
       * wait for that same operation instead of sending another
       * refresh request.
       */
      if (!refreshPromise) {
        refreshPromise =
          refreshAccessToken().finally(
            () => {
              refreshPromise = null;
            },
          );
      }

      const newAccessToken =
        await refreshPromise;

      /*
       * Retry the original request with the newly issued
       * access token.
       */
      originalRequest.headers.Authorization =
        `Bearer ${newAccessToken}`;

      return apiClient(
        originalRequest,
      );
    } catch (refreshError) {
      /*
       * Refresh failed.
       *
       * This normally means the refresh token is invalid,
       * expired, revoked, or detected as reused.
       *
       * The backend should be treated as authoritative.
       */
      tokenManager.clearTokens();

      return Promise.reject(
        refreshError,
      );
    }
  },
);