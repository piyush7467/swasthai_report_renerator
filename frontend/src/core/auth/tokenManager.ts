import type { AuthUser } from "./authTypes";

const ACCESS_TOKEN_KEY = "swasthai_access_token";
const REFRESH_TOKEN_KEY = "swasthai_refresh_token";
const USER_KEY = "swasthai_user";
const USER_REF_ID_KEY = "swasthai_user_ref_id";

export const tokenManager = {
  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  },

  getUser(): AuthUser | null {
    const raw = sessionStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  },

  getUserRefId(): string | null {
    const user = this.getUser();
    if (user?.userRefId) {
      return user.userRefId;
    }
    return sessionStorage.getItem(USER_REF_ID_KEY);
  },

  setTokens(
    accessToken: string,
    refreshToken: string,
  ): void {
    sessionStorage.setItem(
      ACCESS_TOKEN_KEY,
      accessToken,
    );

    sessionStorage.setItem(
      REFRESH_TOKEN_KEY,
      refreshToken,
    );
  },

  setUser(user: AuthUser): void {
    sessionStorage.setItem(
      USER_KEY,
      JSON.stringify(user),
    );
    sessionStorage.setItem(
      USER_REF_ID_KEY,
      user.userRefId,
    );
  },

  setUserRefId(userRefId: string): void {
    sessionStorage.setItem(
      USER_REF_ID_KEY,
      userRefId,
    );
  },

  clearTokens(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(USER_REF_ID_KEY);
  },

  hasTokens(): boolean {
    return (
      this.getAccessToken() !== null &&
      this.getRefreshToken() !== null
    );
  },
};