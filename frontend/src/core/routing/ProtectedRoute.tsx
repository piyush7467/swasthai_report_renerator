import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuth } from "../auth/AuthContext";

export function ProtectedRoute() {
  const {
    isAuthenticated,
    isInitializing,
  } = useAuth();

  const location = useLocation();

  /*
   * Do not make a routing decision while authentication
   * restoration is still in progress.
   */
  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="text-center">
          <div className="mx-auto mb-3 h-8 w-8 animate-spin rounded-full border-4 border-slate-300 border-t-slate-700" />

          <p className="text-sm text-slate-600">
            Checking your session...
          </p>
        </div>
      </div>
    );
  }

  /*
   * Frontend authentication state is only used for navigation.
   * The backend still validates every protected API request.
   */
  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location,
        }}
      />
    );
  }

  return <Outlet />;
}