import { useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  CheckCircle2,
  Clock,
  FileEdit,
  FilePlus,
  FlaskConical,
  UserPlus,
  Users,
} from "lucide-react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { useAuth } from "@/core/auth/AuthContext";

import { useReportsQuery } from "@/modules/org-admin/reports/hooks/useReports";
import { usePatientsQuery } from "@/modules/org-admin/patients/hooks/usePatients";
import { useTenantTestsQuery } from "@/modules/org-admin/tests/hooks/useTenantTests";

export function LabStaffDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  // 1. Pending Drafts Query (Worklist)
  const { data: draftReportsData, isLoading: isDraftsLoading } = useReportsQuery({
    status: "DRAFT",
    page: 0,
    size: 5,
    sort: "createdAt",
    direction: "desc",
  });

  // 2. Finalized Reports Query
  const { data: finalizedReportsData, isLoading: isFinalizedLoading } = useReportsQuery({
    status: "FINALIZED",
    page: 0,
    size: 5,
    sort: "createdAt",
    direction: "desc",
  });

  // 3. Patients Count
  const { data: patientsData, isLoading: isPatientsLoading } = usePatientsQuery({
    page: 0,
    size: 1,
  });

  // 5. Assigned Tests Count
  const { data: testsData, isLoading: isTestsLoading } = useTenantTestsQuery({
    status: "ACTIVE",
    size: 1,
  });

  const pendingDrafts = useMemo(
    () => draftReportsData?.content ?? [],
    [draftReportsData?.content]
  );

  const recentFinalized = useMemo(
    () => finalizedReportsData?.content ?? [],
    [finalizedReportsData?.content]
  );

  const draftCount = draftReportsData?.totalElements ?? 0;
  const finalizedCount = finalizedReportsData?.totalElements ?? 0;
  const totalPatientsCount = patientsData?.totalElements ?? 0;
  const activeTestsCount = testsData?.totalElements ?? 0;

  if (!user) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* Welcome & Station Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between bg-gradient-to-r from-emerald-900 to-slate-900 text-white p-6 rounded-2xl shadow-sm">
        <div className="space-y-1.5">
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
              Laboratory Station
            </span>
            <span className="text-xs text-slate-400">
              Staff ID: {user.name} ({user.role})
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
            Welcome, {user.name}
          </h1>
          <p className="text-sm text-slate-300 max-w-2xl">
            Specimen tracking, test parameter result entry, and clinical diagnostic report generation.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <Button
            asChild
            size="sm"
            className="bg-emerald-500 hover:bg-emerald-600 text-white text-xs gap-1.5 font-semibold shadow-xs cursor-pointer"
          >
            <Link to="/lab-staff/reports/new">
              <FilePlus className="h-4 w-4" />
              New Report
            </Link>
          </Button>

          <Button
            asChild
            size="sm"
            variant="outline"
            className="text-xs gap-1.5 border-slate-700 bg-slate-800/80 text-white hover:bg-slate-700 cursor-pointer"
          >
            <Link to="/lab-staff/patients">
              <UserPlus className="h-4 w-4 text-emerald-400" />
              Patients
            </Link>
          </Button>

          <Button
            asChild
            size="sm"
            variant="outline"
            className="text-xs gap-1.5 border-slate-700 bg-slate-800/80 text-white hover:bg-slate-700 cursor-pointer"
          >
            <Link to="/lab-staff/tests">
              <FlaskConical className="h-4 w-4 text-emerald-400" />
              Test Ranges
            </Link>
          </Button>
        </div>
      </div>

      {/* Clinical KPI Metric Cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {/* Pending Drafts */}
        <Card className="border-slate-200/80 shadow-xs bg-white hover:border-amber-200 transition-colors">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600">
              <Clock className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Pending Drafts</p>
              <div className="text-2xl font-bold text-amber-700 mt-0.5">
                {isDraftsLoading ? <Skeleton className="h-7 w-12" /> : draftCount}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Finalized Reports */}
        <Card className="border-slate-200/80 shadow-xs bg-white hover:border-emerald-200 transition-colors">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
              <CheckCircle2 className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Finalized Reports</p>
              <div className="text-2xl font-bold text-emerald-700 mt-0.5">
                {isFinalizedLoading ? <Skeleton className="h-7 w-12" /> : finalizedCount}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Registered Patients */}
        <Card className="border-slate-200/80 shadow-xs bg-white hover:border-blue-200 transition-colors">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600">
              <Users className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Registered Patients</p>
              <div className="text-2xl font-bold text-slate-900 mt-0.5">
                {isPatientsLoading ? <Skeleton className="h-7 w-12" /> : totalPatientsCount}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Available Tests */}
        <Card className="border-slate-200/80 shadow-xs bg-white hover:border-purple-200 transition-colors">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-purple-50 text-purple-600">
              <FlaskConical className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Available Tests</p>
              <div className="text-2xl font-bold text-purple-700 mt-0.5">
                {isTestsLoading ? <Skeleton className="h-7 w-12" /> : activeTestsCount}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Grid: Worklist & Finalized Results */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Clinical Worklist (Drafts Needing Attention) */}
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardHeader className="p-5 pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
            <div>
              <CardTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Clock className="size-4 text-amber-600" />
                Active Worklist — Pending Drafts
              </CardTitle>
              <CardDescription className="text-xs text-slate-500 mt-0.5">
                Reports awaiting specimen results entry and pathologist review.
              </CardDescription>
            </div>

            <Button asChild variant="ghost" size="sm" className="text-xs text-amber-700 hover:text-amber-800">
              <Link to="/lab-staff/reports" className="flex items-center gap-1">
                View All
                <ArrowRight className="size-3.5" />
              </Link>
            </Button>
          </CardHeader>

          <CardContent className="p-0">
            {isDraftsLoading ? (
              <div className="p-6 space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex justify-between items-center py-2">
                    <div className="space-y-1">
                      <Skeleton className="h-4 w-40" />
                      <Skeleton className="h-3 w-24" />
                    </div>
                    <Skeleton className="h-6 w-20 rounded-full" />
                  </div>
                ))}
              </div>
            ) : pendingDrafts.length === 0 ? (
              <div className="py-8">
                <EmptyState
                  title="Worklist is clear"
                  description="No reports are currently pending results. Create a new patient report to start testing."
                  action={{
                    label: "New Report",
                    onClick: () => navigate("/lab-staff/reports/new"),
                  }}
                />
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {pendingDrafts.map((report) => (
                  <div
                    key={report.refId}
                    onClick={() => navigate(`/lab-staff/reports/${report.refId}`)}
                    className="p-4 hover:bg-amber-50/20 transition-colors flex items-center justify-between cursor-pointer group"
                  >
                    <div className="flex items-center gap-3">
                      <div className="size-9 rounded-lg bg-amber-50 text-amber-700 border border-amber-200 flex items-center justify-center font-bold text-xs">
                        {report.tests?.length ?? 1}T
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-900 group-hover:text-amber-700 transition-colors text-sm">
                            {report.patientName || "Patient"}
                          </span>
                          {report.patientGender && (
                            <span className="text-xs text-slate-400">
                              ({report.patientGender.charAt(0)}
                              {report.patientAgeAtReportingValue
                                ? `, ${report.patientAgeAtReportingValue}`
                                : ""}
                              )
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-slate-400 mt-0.5 font-mono">
                          ID: {report.refId} • {new Date(report.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>

                    <Button
                      size="sm"
                      variant="outline"
                      className="h-8 text-xs border-amber-200 text-amber-800 hover:bg-amber-50 flex items-center gap-1.5"
                    >
                      <FileEdit className="size-3.5" />
                      Enter Results
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Right Column: Recent Finalized Diagnostic Reports */}
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardHeader className="p-5 pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
            <div>
              <CardTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
                <CheckCircle2 className="size-4 text-emerald-600" />
                Recent Finalized Reports
              </CardTitle>
              <CardDescription className="text-xs text-slate-500 mt-0.5">
                Completed, verified reports ready for patient delivery.
              </CardDescription>
            </div>

            <Button asChild variant="ghost" size="sm" className="text-xs text-emerald-700 hover:text-emerald-800">
              <Link to="/lab-staff/reports" className="flex items-center gap-1">
                View All
                <ArrowRight className="size-3.5" />
              </Link>
            </Button>
          </CardHeader>

          <CardContent className="p-0">
            {isFinalizedLoading ? (
              <div className="p-6 space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex justify-between items-center py-2">
                    <div className="space-y-1">
                      <Skeleton className="h-4 w-40" />
                      <Skeleton className="h-3 w-24" />
                    </div>
                    <Skeleton className="h-6 w-20 rounded-full" />
                  </div>
                ))}
              </div>
            ) : recentFinalized.length === 0 ? (
              <div className="py-8">
                <EmptyState
                  title="No finalized reports yet"
                  description="Complete parameter entry on pending drafts and finalize to generate official reports."
                />
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {recentFinalized.map((report) => (
                  <div
                    key={report.refId}
                    onClick={() => navigate(`/lab-staff/reports/${report.refId}`)}
                    className="p-4 hover:bg-emerald-50/20 transition-colors flex items-center justify-between cursor-pointer group"
                  >
                    <div className="flex items-center gap-3">
                      <div className="size-9 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center justify-center font-bold text-xs">
                        {report.tests?.length ?? 1}T
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-900 group-hover:text-emerald-700 transition-colors text-sm">
                            {report.patientName || "Patient"}
                          </span>
                          <Badge
                            variant="outline"
                            className="border-emerald-200 bg-emerald-50 text-emerald-700 text-[10px]"
                          >
                            Finalized
                          </Badge>
                        </div>
                        <p className="text-xs text-slate-400 mt-0.5 font-mono">
                          ID: {report.refId} • {new Date(report.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>

                    <ArrowRight className="size-4 text-slate-300 group-hover:text-slate-600 transition-colors" />
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Quick Diagnostic Station Shortcuts */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Link to="/lab-staff/reports/new" className="block group">
          <Card className="border-slate-200/80 bg-white h-full group-hover:border-emerald-300 group-hover:shadow-xs transition-all">
            <CardContent className="p-4 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600 group-hover:text-white transition-colors">
                <FilePlus className="size-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900 group-hover:text-emerald-700 transition-colors">
                  Create Diagnostic Report
                </h4>
                <p className="text-xs text-slate-500 mt-0.5">
                  Select an enrolled patient, choose assigned tests, and enter specimen results.
                </p>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link to="/lab-staff/patients" className="block group">
          <Card className="border-slate-200/80 bg-white h-full group-hover:border-emerald-300 group-hover:shadow-xs transition-all">
            <CardContent className="p-4 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white transition-colors">
                <UserPlus className="size-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900 group-hover:text-blue-700 transition-colors">
                  Patient Records & Admissions
                </h4>
                <p className="text-xs text-slate-500 mt-0.5">
                  Search patient database, register walk-ins, and inspect historical diagnostic visits.
                </p>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link to="/lab-staff/tests" className="block group">
          <Card className="border-slate-200/80 bg-white h-full group-hover:border-emerald-300 group-hover:shadow-xs transition-all">
            <CardContent className="p-4 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-purple-50 text-purple-600 group-hover:bg-purple-600 group-hover:text-white transition-colors">
                <FlaskConical className="size-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900 group-hover:text-purple-700 transition-colors">
                  Test Catalog & Normal Ranges
                </h4>
                <p className="text-xs text-slate-500 mt-0.5">
                  Quick reference for biological reference intervals, units, and critical alarm limits.
                </p>
              </div>
            </CardContent>
          </Card>
        </Link>
      </div>
    </div>
  );
}
