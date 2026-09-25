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

import TestCatalogPage from "@/modules/super-admin/tests/pages/TestCatalogPage";
import CreateTestPage from "@/modules/super-admin/tests/pages/CreateTestPage";
import EditTestPage from "@/modules/super-admin/tests/pages/EditTestPage";
import TestDetailsPage from "@/modules/super-admin/tests/pages/TestDetailsPage";
import CreateParameterPage from "@/modules/super-admin/tests/pages/CreateParameterPage";
import EditParameterPage from "@/modules/super-admin/tests/pages/EditParameterPage";
import CategoriesPage from "@/modules/super-admin/tests/pages/CategoriesPage";
import AssignmentsPage from "@/modules/super-admin/tests/pages/AssignmentsPage";
import AssignTestsPage from "@/modules/super-admin/tests/pages/AssignTestsPage";
import ParametersDirectoryPage from "@/modules/super-admin/tests/pages/ParametersDirectoryPage";

import PlansPage from "@/modules/super-admin/licensing/pages/PlansPage";
import CreatePlanPage from "@/modules/super-admin/licensing/pages/CreatePlanPage";
import EditPlanPage from "@/modules/super-admin/licensing/pages/EditPlanPage";
import OrganizationLicensesPage from "@/modules/super-admin/licensing/pages/OrganizationLicensesPage";
import AdminUpgradeRequestsPage from "@/modules/super-admin/licensing/pages/AdminUpgradeRequestsPage";
import OrganizationLicensePage from "@/modules/super-admin/organizations/OrganizationLicensePage";

import ReportOverviewPage from "@/modules/super-admin/reports/pages/ReportOverviewPage";
import ReportActivityPage from "@/modules/super-admin/reports/pages/ReportActivityPage";
import ReportAuditPage from "@/modules/super-admin/reports/pages/ReportAuditPage";
import SettingsPage from "@/modules/super-admin/settings/pages/SettingsPage";

import ReportsListPage from "@/modules/org-admin/reports/pages/ReportsListPage";
import ReportWorkspacePage from "@/modules/org-admin/reports/pages/ReportWorkspacePage";
import PatientsListPage from "@/modules/org-admin/patients/pages/PatientsListPage";
import PatientProfilePage from "@/modules/org-admin/patients/pages/PatientProfilePage";
import LabStaffPage from "@/modules/org-admin/staff/pages/LabStaffPage";
import OrgAdminLicensePage from "@/modules/org-admin/licensing/pages/OrganizationLicensePage";
import SharedReportViewerPage from "@/modules/public/pages/SharedReportViewerPage";

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

        <Route
          path="/shared/reports/:shareToken"
          element={<SharedReportViewerPage />}
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
                path="/super-admin/organizations/:refId/license"
                element={<OrganizationLicensePage />}
              />

              <Route
                path="/super-admin/users"
                element={<UsersPage />}
              />

              {/* REPORT ANALYTICS & AUDIT MODULE */}
              <Route
                path="/super-admin/reports"
                element={<ReportOverviewPage />}
              />
              <Route
                path="/super-admin/reports/activity"
                element={<ReportActivityPage />}
              />
              <Route
                path="/super-admin/reports/audit"
                element={<ReportAuditPage />}
              />

              {/* LICENSING MODULE */}
              <Route
                path="/super-admin/licensing/plans"
                element={<PlansPage />}
              />
              <Route
                path="/super-admin/licensing/plans/new"
                element={<CreatePlanPage />}
              />
              <Route
                path="/super-admin/licensing/plans/:refId/edit"
                element={<EditPlanPage />}
              />
              <Route
                path="/super-admin/licensing/licenses"
                element={<OrganizationLicensesPage />}
              />
              <Route
                path="/super-admin/licensing/upgrade-requests"
                element={<AdminUpgradeRequestsPage />}
              />

              {/* TEST SYSTEM */}
              <Route
                path="/super-admin/tests"
                element={<TestCatalogPage />}
              />
              <Route
                path="/super-admin/tests/new"
                element={<CreateTestPage />}
              />
              <Route
                path="/super-admin/tests/categories"
                element={<CategoriesPage />}
              />
              <Route
                path="/super-admin/tests/parameters"
                element={<ParametersDirectoryPage />}
              />
              <Route
                path="/super-admin/tests/assignments"
                element={<AssignmentsPage />}
              />
              <Route
                path="/super-admin/tests/assignments/new"
                element={<AssignTestsPage />}
              />
              <Route
                path="/super-admin/tests/:refId"
                element={<TestDetailsPage />}
              />
              <Route
                path="/super-admin/tests/:refId/edit"
                element={<EditTestPage />}
              />
              <Route
                path="/super-admin/tests/:refId/parameters/new"
                element={<CreateParameterPage />}
              />
              <Route
                path="/super-admin/tests/:refId/parameters/:parameterRefId/edit"
                element={<EditParameterPage />}
              />

              {/* SETTINGS */}
              <Route
                path="/super-admin/settings"
                element={<SettingsPage />}
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
              <Route
                path="/org-admin/patients"
                element={<PatientsListPage />}
              />
              <Route
                path="/org-admin/patients/:patientRefId"
                element={<PatientProfilePage />}
              />
              <Route
                path="/org-admin/reports"
                element={<ReportsListPage />}
              />
              <Route
                path="/org-admin/reports/new"
                element={<ReportWorkspacePage />}
              />
              <Route
                path="/org-admin/reports/:reportRefId"
                element={<ReportWorkspacePage />}
              />
              <Route
                path="/org-admin/staff"
                element={<LabStaffPage />}
              />
              <Route
                path="/org-admin/license"
                element={<OrgAdminLicensePage />}
              />
              <Route
                path="/organization/license"
                element={<OrgAdminLicensePage />}
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
              <Route
                path="/lab-staff/patients"
                element={<PatientsListPage />}
              />
              <Route
                path="/lab-staff/patients/:patientRefId"
                element={<PatientProfilePage />}
              />
              <Route
                path="/lab-staff/reports"
                element={<ReportsListPage />}
              />
              <Route
                path="/lab-staff/reports/new"
                element={<ReportWorkspacePage />}
              />
              <Route
                path="/lab-staff/reports/:reportRefId"
                element={<ReportWorkspacePage />}
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