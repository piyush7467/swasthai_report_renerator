import { Navigate, Outlet } from "react-router-dom";

import { useAuth } from "../auth/AuthContext";
import type { UserRole } from "../auth/authTypes";

interface RoleRouteProps {
  allowedRoles: UserRole[];
}

export function RoleRoute({
  allowedRoles,
}: RoleRouteProps) {
  const { user, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="text-center">
          <div className="mx-auto mb-3 h-8 w-8 animate-spin rounded-full border-4 border-slate-300 border-t-slate-700" />

          <p className="text-sm text-slate-600">
            Loading...
          </p>
        </div>
      </div>
    );
  }

  /*
   * This is NOT a security boundary.
   *
   * The backend must independently enforce authorization.
   * This route only prevents users from seeing navigation
   * that does not belong to their current backend-provided role.
   */
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}