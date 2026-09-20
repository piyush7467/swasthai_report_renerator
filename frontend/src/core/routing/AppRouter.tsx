import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

import { LoginPage } from "../auth/pages/LoginPage";
import { UnauthorizedPage } from "../components/UnauthorizedPage";
import { AppShell } from "../layouts/AppShell";
import { SuperAdminDashboard } from "@/modules/super-admin/SuperAdminDashboard";
import { OrgAdminDashboard } from "@/modules/org-admin/OrgAdminDashboard";
import { LabStaffDashboard } from "@/modules/lab-staff/LabStaffDashboard";

import { ProtectedRoute } from "./ProtectedRoute";
import { RoleRoute } from "./RoleRoute";
import OrganizationsPage from "@/modules/super-admin/organizations/OrganizationsPage";
import OrganizationDetailsPage from "@/modules/super-admin/organizations/OrganizationDetailsPage";
import OrganizationProfilePage from "@/modules/super-admin/pages/OrganizationProfilePage";
import OrganizationUsersPage from "@/modules/super-admin/pages/OrganizationUsersPage";
import UsersPage from "@/modules/super-admin/users/UsersPage";

function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <section className="text-center">
        <p className="text-5xl font-bold text-slate-800">
          404
        </p>

        <h1 className="mt-3 text-xl font-semibold text-slate-900">
          Page not found
        </h1>

        <p className="mt-2 text-sm text-slate-600">
          The page you requested does not exist.
        </p>
      </section>
    </main>
  );
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>

        {/* =========================
            PUBLIC ROUTES
        ========================== */}

        <Route
          path="/login"
          element={<LoginPage />}
        />

        <Route
          path="/unauthorized"
          element={<UnauthorizedPage />}
        />

        {/* =========================
            AUTHENTICATED ROUTES
        ========================== */}

        <Route element={<ProtectedRoute />}>

          {/* Common authenticated application shell */}
          <Route element={<AppShell />}>

            {/* SUPER ADMIN */}
            <Route
              element={
                <RoleRoute
                  allowedRoles={["SUPER_ADMIN"]}
                />
              }
            >
              <Route
                path="/super-admin"
                element={<SuperAdminDashboard />}
              />

              <Route
                path="/super-admin/organizations"
                element={<OrganizationsPage />}
              />

              <Route
                path="/super-admin/organizations/:refId"
                element={<OrganizationDetailsPage />}
              />

              <Route
                path="/super-admin/organizations/:orgRefId"
                element={<OrganizationDetailsPage />}
              />

              <Route
                path="/super-admin/organizations/:refId/users"
                element={<OrganizationUsersPage />}
              />

              <Route
                path="/super-admin/organizations/:refId/profile"
                element={<OrganizationProfilePage />}
              />

              <Route
                path="/super-admin/users"
                element={<UsersPage />}
              />

            </Route>

            {/* ORGANIZATION ADMIN */}
            <Route
              element={
                <RoleRoute
                  allowedRoles={["ORG_ADMIN"]}
                />
              }
            >
              <Route
                path="/org-admin"
                element={<OrgAdminDashboard />}
              />
            </Route>

            {/* LAB STAFF */}
            <Route
              element={
                <RoleRoute
                  allowedRoles={["LAB_STAFF"]}
                />
              }
            >
              <Route
                path="/lab-staff"
                element={<LabStaffDashboard />}
              />
            </Route>

          </Route>
        </Route>

        {/* =========================
            FALLBACK
        ========================== */}

        <Route
          path="/"
          element={
            <Navigate
              to="/login"
              replace
            />
          }
        />

        <Route
          path="*"
          element={<NotFoundPage />}
        />

      </Routes>
    </BrowserRouter>
  );
}