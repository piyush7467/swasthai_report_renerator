import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { authApi } from "./authApi";
import { tokenManager } from "./tokenManager";

import type {
  AuthUser,
  LoginRequest,
} from "./authTypes";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isInitializing: boolean;

  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<
  AuthContextValue | undefined
>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({
  children,
}: AuthProviderProps) {
  const [user, setUser] =
    useState<AuthUser | null>(null);

  const [isInitializing, setIsInitializing] =
    useState(true);

  /*
   * Restore the authenticated session when the application
   * starts or the browser page is reloaded.
   *
   * Stored user profile and tokens in sessionStorage provide
   * immediate restoration. The backend auth refresh flow handles
   * rotation or validation if the access token has expired.
   */
  useEffect(() => {
    let cancelled = false;

    const initializeAuth = async () => {
      const accessToken =
        tokenManager.getAccessToken();

      const refreshToken =
        tokenManager.getRefreshToken();

      const storedUser =
        tokenManager.getUser();

      /*
       * If we do not have valid tokens, clear any orphan state.
       */
      if (!accessToken || !refreshToken) {
        if (!cancelled) {
          tokenManager.clearTokens();
          setUser(null);
          setIsInitializing(false);
        }
        return;
      }

      /*
       * Fast-path session restoration:
       * Stored user profile from backend login/refresh is already available.
       */
      if (storedUser) {
        if (!cancelled) {
          setUser(storedUser);
          setIsInitializing(false);
        }
        return;
      }

      /*
       * Fallback restoration:
       * If tokens exist but the user profile was missing from storage,
       * execute token refresh to obtain the authoritative user identity from the backend.
       */
      try {
        const authResponse =
          await authApi.refresh({ refreshToken });

        if (cancelled) {
          return;
        }

        tokenManager.setTokens(
          authResponse.accessToken,
          authResponse.refreshToken,
        );

        const restoredUser: AuthUser = {
          userRefId: authResponse.userRefId,
          refId: authResponse.userRefId,
          name: authResponse.name,
          email: authResponse.email,
          role: authResponse.role,
          organizationRefId: authResponse.organizationRefId,
        };

        tokenManager.setUser(restoredUser);
        setUser(restoredUser);
      } catch {
        if (cancelled) {
          return;
        }

        tokenManager.clearTokens();
        setUser(null);
      } finally {
        if (!cancelled) {
          setIsInitializing(false);
        }
      }
    };

    void initializeAuth();

    return () => {
      cancelled = true;
    };
  }, []);

  /*
   * Login through the backend.
   *
   * The backend's LoginResponse directly provides the authoritative
   * identity, role, and organization reference for SUPER_ADMIN, ORG_ADMIN,
   * and LAB_STAFF without making an unauthorized secondary user lookup.
   */
  const login = async (
    request: LoginRequest,
  ): Promise<void> => {
    const authResponse =
      await authApi.login(request);

    tokenManager.setTokens(
      authResponse.accessToken,
      authResponse.refreshToken,
    );

    const authUser: AuthUser = {
      userRefId: authResponse.userRefId,
      refId: authResponse.userRefId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
      organizationRefId: authResponse.organizationRefId,
    };

    tokenManager.setUser(authUser);
    setUser(authUser);
  };

  /*
   * Logout from backend and clear local state.
   */
  const logout = async (): Promise<void> => {
    const refreshToken =
      tokenManager.getRefreshToken();

    try {
      if (refreshToken) {
        await authApi.logout({
          refreshToken,
        });
      }
    } finally {
      /*
       * Always clear local authentication state,
       * even if backend logout fails.
       */
      tokenManager.clearTokens();
      setUser(null);
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isInitializing,
      login,
      logout,
    }),
    [
      user,
      isInitializing,
    ],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/*
 * Access authentication state from React components.
 */
export function useAuth(): AuthContextValue {
  const context =
    useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside an AuthProvider",
    );
  }

  return context;
}