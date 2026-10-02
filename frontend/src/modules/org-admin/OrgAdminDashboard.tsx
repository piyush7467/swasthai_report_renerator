import { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Activity,
  ArrowRight,
  Building2,
  CheckCircle2,
  Clock,
  FileCheck2,
  FileText,
  FlaskConical,
  Plus,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  UserPlus,
  Users,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { useAuth } from "@/core/auth/AuthContext";

import { useReportsQuery } from "./reports/hooks/useReports";
import { usePatientsQuery } from "./patients/hooks/usePatients";
import { useLabStaffSummaryQuery } from "./staff/hooks/useLabStaff";
import { useMyOrganizationProfileQuery } from "./settings/hooks/useOrgProfile";
import { useLicenseOverview } from "./licensing/hooks/useOrgLicense";

import {
  useOrgOverviewStats,
  useOrgReportTrend,
  useOrgCategoryUsage,
  useOrgPatientTrend,
  useOrgRecentActivity,
} from "./analytics/hooks/useOrgAnalytics";

import { OrgReportActivityChart } from "./analytics/components/charts/OrgReportActivityChart";
import { OrgReportStatusDonut } from "./analytics/components/charts/OrgReportStatusDonut";
import { OrgCategoryUsageChart } from "./analytics/components/charts/OrgCategoryUsageChart";
import { OrgPatientTrendChart } from "./analytics/components/charts/OrgPatientTrendChart";

// Brand Asset
import heroLabImg from "@/assets/dashboard/hero_lab_illustration.jpg";

export function OrgAdminDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [trendDays, setTrendDays] = useState<7 | 14 | 30>(30);
  const [reportFilter, setReportFilter] = useState<"ALL" | "FINALIZED" | "DRAFT">("ALL");
  const [isRefreshing, setIsRefreshing] = useState(false);

  // 1. Overview KPIs
  const {
    data: overviewStats,
    isLoading: isStatsLoading,
    refetch: refetchStats,
  } = useOrgOverviewStats();

  // 2. Trend Analytics
  const {
    data: trendData,
    isLoading: isTrendLoading,
    refetch: refetchTrend,
  } = useOrgReportTrend(trendDays);

  // 3. Category Usage
  const {
    data: categoryUsageData,
    isLoading: isCategoryLoading,
    refetch: refetchCategory,
  } = useOrgCategoryUsage(6);

  // 4. Patient Trend
  const {
    data: patientTrendData,
    isLoading: isPatientTrendLoading,
    refetch: refetchPatientTrend,
  } = useOrgPatientTrend(30);

  // 5. Recent Activity Feed
  const {
    data: activityFeed,
    isLoading: isActivityLoading,
    refetch: refetchActivity,
  } = useOrgRecentActivity();

  // 6. Reports Query
  const {
    data: reportsData,
    isLoading: isReportsLoading,
    refetch: refetchReports,
  } = useReportsQuery({
    page: 0,
    size: 5,
    sort: "createdAt",
    direction: "desc",
    status: reportFilter === "ALL" ? undefined : reportFilter,
  });

  const { data: draftReportsData } = useReportsQuery({
    status: "DRAFT",
    size: 1,
  });

  // 7. Patients Query
  const {
    data: patientsData,
    isLoading: isPatientsLoading,
    refetch: refetchPatients,
  } = usePatientsQuery({
    page: 0,
    size: 5,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  // 8. Lab Staff & Capacity
  const {
    data: staffSummary,
    isLoading: isStaffLoading,
    refetch: refetchStaff,
  } = useLabStaffSummaryQuery();

  const {
    data: licenseData,
    refetch: refetchLicense,
  } = useLicenseOverview();

  // 9. Organization Profile & Letterhead
  const {
    data: profile,
    refetch: refetchProfile,
  } = useMyOrganizationProfileQuery();

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await Promise.all([
        refetchStats(),
        refetchTrend(),
        refetchCategory(),
        refetchPatientTrend(),
        refetchActivity(),
        refetchReports(),
        refetchPatients(),
        refetchStaff(),
        refetchLicense(),
        refetchProfile(),
      ]);
    } finally {
      setIsRefreshing(false);
    }
  };

  // Letterhead completion check
  const letterheadChecks = useMemo(() => {
    const hasName = Boolean(profile?.organizationName);
    const hasAddress = Boolean(profile?.addressLine1);
    const hasLogo = Boolean(profile?.logoConfigured);
    const hasSignature = Boolean(profile?.signatureConfigured);

    const completed = [hasName, hasAddress, hasLogo, hasSignature].filter(Boolean).length;
    return { hasName, hasAddress, hasLogo, hasSignature, completed, total: 4 };
  }, [profile]);

  const totalReportsCount = overviewStats?.totalReports ?? reportsData?.totalElements ?? 0;
  const reportsTodayCount = overviewStats?.reportsToday ?? 0;
  const finalizedCount = overviewStats?.finalizedReports ?? 0;
  const draftCount = draftReportsData?.totalElements ?? 0;
  const totalPatientsCount = overviewStats?.totalPatients ?? patientsData?.totalElements ?? 0;
  const activeTestsCount = overviewStats?.activeTests ?? 0;

  const recentReports = reportsData?.content ?? [];
  const recentPatients = patientsData?.content ?? [];

  if (!user) {
    return null;
  }

  return (
    <div className="space-y-6 pb-12">
      {/* 1. Hero Header Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-teal-50/70 via-slate-50/40 to-cyan-50/60 border border-teal-100/60 p-6 sm:p-8">
        {/* Soft background laboratory illustration seamlessly blending on right */}
        <div className="absolute top-0 right-0 h-full w-full sm:w-1/2 lg:w-2/5 pointer-events-none select-none overflow-hidden hidden md:block">
          <img
            src={heroLabImg}
            alt="Medical Laboratory"
            className="w-full h-full object-cover object-center opacity-85 mix-blend-multiply"
          />
          {/* Subtle multi-directional fade masks to blend naturally into background */}
          <div className="absolute inset-0 bg-gradient-to-r from-teal-50/90 via-teal-50/40 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-t from-slate-50/80 via-transparent to-transparent" />
        </div>

        {/* Right subtle clinical tagline */}
        <div className="absolute top-8 right-8 text-right hidden xl:block select-none pointer-events-none z-10">
          <p className="text-xs font-medium text-slate-400 leading-tight">
            Accurate<br />Diagnostics<br />for Healthier<br />Lives
          </p>
          <div className="h-0.5 w-7 bg-teal-600 ml-auto mt-2 rounded-full" />
        </div>

        {/* Content sits naturally on background */}
        <div className="relative z-10 max-w-xl space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-100/80 text-teal-800 border border-teal-200">
              <Activity className="size-3.5 text-teal-600" />
              Laboratory Operations Console
            </span>
            <span className="text-xs text-slate-500 font-medium font-mono">
              {profile?.organizationName || user.organizationRefId}
            </span>
          </div>

          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-slate-900 leading-tight">
            Good morning, {user.name}
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 leading-relaxed max-w-lg">
            Diagnostic Operations Console · Real-time Overview & Clinical Insights
          </p>
        </div>

        {/* Action Controls */}
        <div className="relative z-10 flex flex-wrap items-center gap-2.5 mt-5">
          <Button
            asChild
            size="sm"
            className="bg-teal-600 hover:bg-teal-700 text-white font-semibold text-xs shadow-xs gap-1.5 px-4 h-9 cursor-pointer transition-colors"
          >
            <Link to="/org-admin/reports/new">
              <Plus className="size-4 stroke-[2.5]" />
              New Report
            </Link>
          </Button>

          <Button
            asChild
            size="sm"
            variant="outline"
            className="text-xs gap-1.5 border-slate-200 bg-white text-slate-700 hover:bg-slate-50 hover:text-slate-900 cursor-pointer h-9 px-3.5 shadow-xs"
          >
            <Link to="/org-admin/patients">
              <UserPlus className="size-3.5 text-teal-600" />
              Register Patient
            </Link>
          </Button>

          <Button
            asChild
            size="sm"
            variant="outline"
            className="text-xs gap-1.5 border-slate-200 bg-white text-slate-700 hover:bg-slate-50 hover:text-slate-900 cursor-pointer h-9 px-3.5 shadow-xs"
          >
            <Link to="/org-admin/staff">
              <Users className="size-3.5 text-teal-600" />
              Lab Team
            </Link>
          </Button>

          <Button
            size="sm"
            variant="outline"
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="text-xs border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900 cursor-pointer h-9 w-9 p-0 shadow-xs"
            title="Refresh Analytics Data"
          >
            <RefreshCw className={`size-3.5 ${isRefreshing ? "animate-spin text-teal-600" : ""}`} />
          </Button>
        </div>
      </div>

      {/* 2. Continuous 5-metric KPI Strip */}
      <div className="rounded-2xl border border-slate-200/80 bg-white shadow-xs divide-y sm:divide-y-0 sm:divide-x divide-slate-100 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5">
        {/* Total Reports */}
        <div className="p-4 sm:p-5 flex flex-col justify-between hover:bg-slate-50/50 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Total Reports
            </span>
            <div className="p-1.5 rounded-lg bg-teal-50 text-teal-600">
              <FileText className="size-4" />
            </div>
          </div>
          <div className="mt-3">
            {isStatsLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight font-mono">
                {totalReportsCount.toLocaleString()}
              </div>
            )}
            <div className="flex items-center gap-1.5 mt-1.5">
              <Badge
                variant="outline"
                className="text-[10px] px-1.5 py-0 h-4 border-teal-200 bg-teal-50 text-teal-700 font-semibold"
              >
                {overviewStats?.reportsGrowthRate != null && overviewStats.reportsGrowthRate > 0
                  ? `+${overviewStats.reportsGrowthRate}%`
                  : "All-time"}
              </Badge>
              <span className="text-[11px] text-slate-400">Total generated</span>
            </div>
          </div>
        </div>

        {/* Reports Today */}
        <div className="p-4 sm:p-5 flex flex-col justify-between hover:bg-slate-50/50 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Reports Today
            </span>
            <div className="p-1.5 rounded-lg bg-sky-50 text-sky-600">
              <Clock className="size-4" />
            </div>
          </div>
          <div className="mt-3">
            {isStatsLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight font-mono">
                {reportsTodayCount.toLocaleString()}
              </div>
            )}
            <div className="flex items-center gap-1.5 mt-1.5">
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-sky-700 bg-sky-50 px-1.5 py-0 rounded border border-sky-200">
                <span className="size-1.5 rounded-full bg-sky-500 animate-ping" />
                Live count
              </span>
              <span className="text-[11px] text-slate-400">Since midnight</span>
            </div>
          </div>
        </div>

        {/* Finalized Reports */}
        <div className="p-4 sm:p-5 flex flex-col justify-between hover:bg-slate-50/50 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Finalized Reports
            </span>
            <div className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600">
              <FileCheck2 className="size-4" />
            </div>
          </div>
          <div className="mt-3">
            {isStatsLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl sm:text-3xl font-extrabold text-emerald-700 tracking-tight font-mono">
                {finalizedCount.toLocaleString()}
              </div>
            )}
            <div className="flex items-center gap-1.5 mt-1.5">
              <Badge
                variant="outline"
                className="text-[10px] px-1.5 py-0 h-4 border-emerald-200 bg-emerald-50 text-emerald-700 font-semibold"
              >
                {overviewStats?.finalizedRate != null ? `${overviewStats.finalizedRate}%` : "100%"} rate
              </Badge>
              <span className="text-[11px] text-slate-400">Doctor verified</span>
            </div>
          </div>
        </div>

        {/* Total Patients */}
        <div className="p-4 sm:p-5 flex flex-col justify-between hover:bg-slate-50/50 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Total Patients
            </span>
            <div className="p-1.5 rounded-lg bg-indigo-50 text-indigo-600">
              <Users className="size-4" />
            </div>
          </div>
          <div className="mt-3">
            {isStatsLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight font-mono">
                {totalPatientsCount.toLocaleString()}
              </div>
            )}
            <div className="flex items-center gap-1.5 mt-1.5">
              <Badge
                variant="outline"
                className="text-[10px] px-1.5 py-0 h-4 border-indigo-200 bg-indigo-50 text-indigo-700 font-semibold"
              >
                {overviewStats?.patientsGrowthRate != null && overviewStats.patientsGrowthRate > 0
                  ? `+${overviewStats.patientsGrowthRate}%`
                  : "Active"}
              </Badge>
              <span className="text-[11px] text-slate-400">Admitted</span>
            </div>
          </div>
        </div>

        {/* Active Tests */}
        <div className="p-4 sm:p-5 flex flex-col justify-between hover:bg-slate-50/50 transition-colors col-span-2 sm:col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Active Tests
            </span>
            <div className="p-1.5 rounded-lg bg-purple-50 text-purple-600">
              <FlaskConical className="size-4" />
            </div>
          </div>
          <div className="mt-3">
            {isStatsLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl sm:text-3xl font-extrabold text-purple-700 tracking-tight font-mono">
                {activeTestsCount.toLocaleString()}
              </div>
            )}
            <div className="flex items-center gap-1.5 mt-1.5">
              <Badge
                variant="outline"
                className="text-[10px] px-1.5 py-0 h-4 border-purple-200 bg-purple-50 text-purple-700 font-semibold"
              >
                Catalog
              </Badge>
              <span className="text-[11px] text-slate-400">Assigned tests</span>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Analytics Grid Row 1: Dual-bar Report Activity & Donut Status */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left (7 cols): Report Activity Dual-Bar Histogram */}
        <div className="lg:col-span-7 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 gap-3">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <Activity className="size-4 text-teal-600" />
                  Report Activity & Daily Output
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Comparison between total reports initiated vs finalized & issued.
                </p>
              </div>

              {/* Time Range Pills */}
              <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg self-start sm:self-auto">
                {([7, 14, 30] as const).map((d) => (
                  <button
                    key={d}
                    type="button"
                    onClick={() => setTrendDays(d)}
                    className={`px-2.5 py-1 text-[11px] font-medium rounded-md transition-all cursor-pointer ${
                      trendDays === d
                        ? "bg-white text-slate-900 shadow-xs font-semibold"
                        : "text-slate-500 hover:text-slate-900"
                    }`}
                  >
                    {d}d
                  </button>
                ))}
              </div>
            </div>

            {/* Legend */}
            <div className="flex items-center gap-4 py-3 text-xs">
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-xs bg-sky-500" />
                <span className="text-slate-600 font-medium">Total Generated</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-xs bg-emerald-500" />
                <span className="text-slate-600 font-medium">Finalized & Issued</span>
              </div>
            </div>
          </div>

          <div className="pt-2">
            {isTrendLoading ? (
              <Skeleton className="h-56 w-full rounded-lg" />
            ) : (
              <OrgReportActivityChart data={trendData || []} height={210} />
            )}
          </div>
        </div>

        {/* Right (5 cols): Reports by Status Donut */}
        <div className="lg:col-span-5 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div className="pb-3 border-b border-slate-100">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <FileCheck2 className="size-4 text-emerald-600" />
              Reports by Status
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Live status distribution of diagnostic documentation.
            </p>
          </div>

          <div className="py-4 my-auto">
            {isStatsLoading ? (
              <Skeleton className="h-56 w-full rounded-lg" />
            ) : (
              <OrgReportStatusDonut
                total={totalReportsCount}
                finalized={finalizedCount}
                draft={draftCount}
                height={210}
              />
            )}
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Issuance integrity</span>
            <span className="font-semibold text-emerald-700">
              {overviewStats?.finalizedRate != null ? `${overviewStats.finalizedRate}% issued` : "100% issued"}
            </span>
          </div>
        </div>
      </div>

      {/* 4. Analytics Grid Row 2: Category Usage & Patient Trend */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left (6 cols): Reports by Test Category */}
        <div className="lg:col-span-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <FlaskConical className="size-4 text-indigo-600" />
                  Reports by Test Category
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Volume distribution across active laboratory departments.
                </p>
              </div>
              <Button asChild variant="ghost" size="sm" className="text-xs text-indigo-700 hover:text-indigo-800 h-8">
                <Link to="/org-admin/tests" className="flex items-center gap-1">
                  Catalog
                  <ArrowRight className="size-3" />
                </Link>
              </Button>
            </div>
          </div>

          <div className="py-2">
            {isCategoryLoading ? (
              <div className="space-y-3 py-4">
                {[1, 2, 3, 4].map((i) => (
                  <Skeleton key={i} className="h-8 w-full rounded-lg" />
                ))}
              </div>
            ) : (
              <OrgCategoryUsageChart data={categoryUsageData || []} height={200} />
            )}
          </div>

          <div className="pt-3 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
            <span>Aggregated by ordered report tests</span>
            <span>Real-time DB query</span>
          </div>
        </div>

        {/* Right (6 cols): Registered Patients Trend */}
        <div className="lg:col-span-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <TrendingUp className="size-4 text-indigo-600" />
                  Registered Patients Trend
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Daily patient registrations and intake curve over the last 30 days.
                </p>
              </div>
              <Button asChild variant="ghost" size="sm" className="text-xs text-indigo-700 hover:text-indigo-800 h-8">
                <Link to="/org-admin/patients" className="flex items-center gap-1">
                  Directory
                  <ArrowRight className="size-3" />
                </Link>
              </Button>
            </div>
          </div>

          <div className="py-2">
            {isPatientTrendLoading ? (
              <Skeleton className="h-52 w-full rounded-lg" />
            ) : (
              <OrgPatientTrendChart data={patientTrendData || []} height={200} />
            )}
          </div>

          <div className="pt-3 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
            <span>Total admitted: {totalPatientsCount}</span>
            <span>Monthly trend</span>
          </div>
        </div>
      </div>

      {/* 5. Diagnostic Reports Table & Lab Status Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (8 cols): Recent Diagnostic Reports */}
        <div className="lg:col-span-8 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 gap-3">
              <div>
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <FileText className="size-4 text-teal-600" />
                  Recent Diagnostic Reports
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Latest pathology and biochemistry reports generated by your team.
                </p>
              </div>

              {/* Status Filter Tabs */}
              <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg self-start sm:self-auto">
                {(["ALL", "FINALIZED", "DRAFT"] as const).map((tab) => (
                  <button
                    key={tab}
                    type="button"
                    onClick={() => setReportFilter(tab)}
                    className={`px-3 py-1 text-[11px] font-medium rounded-md transition-all cursor-pointer ${
                      reportFilter === tab
                        ? "bg-white text-slate-900 shadow-xs font-semibold"
                        : "text-slate-500 hover:text-slate-900"
                    }`}
                  >
                    {tab === "ALL" ? "All" : tab === "FINALIZED" ? "Finalized" : "Draft"}
                  </button>
                ))}
              </div>
            </div>

            {/* Reports List / Table */}
            <div className="pt-2">
              {isReportsLoading ? (
                <div className="space-y-3 py-4">
                  {[1, 2, 3, 4].map((i) => (
                    <div key={i} className="flex justify-between items-center py-2.5">
                      <div className="space-y-1.5">
                        <Skeleton className="h-4 w-44" />
                        <Skeleton className="h-3 w-28" />
                      </div>
                      <Skeleton className="h-6 w-20 rounded-full" />
                    </div>
                  ))}
                </div>
              ) : recentReports.length === 0 ? (
                <div className="py-10">
                  <EmptyState
                    title="No reports match this criteria"
                    description="Select a different filter or create a new diagnostic report for a registered patient."
                    action={{
                      label: "Create New Report",
                      onClick: () => navigate("/org-admin/reports/new"),
                    }}
                  />
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {recentReports.map((report) => {
                    const isFinalized = report.status === "FINALIZED";
                    return (
                      <div
                        key={report.refId}
                        onClick={() => navigate(`/org-admin/reports/${report.refId}`)}
                        className="py-3 px-2 hover:bg-slate-50/80 rounded-xl transition-colors flex items-center justify-between cursor-pointer group"
                      >
                        <div className="flex items-center gap-3.5">
                          <div
                            className={`size-10 rounded-xl flex flex-col items-center justify-center font-bold text-[11px] shrink-0 ${
                              isFinalized
                                ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                                : "bg-amber-50 text-amber-700 border border-amber-200"
                            }`}
                          >
                            <FileText className="size-4 mb-0.5" />
                          </div>

                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-semibold text-slate-900 group-hover:text-teal-700 transition-colors text-sm">
                                {report.patientName || "Patient"}
                              </span>
                              {report.patientGender && (
                                <span className="text-xs text-slate-400">
                                  ({report.patientGender.charAt(0)}
                                  {report.patientAgeAtReportingValue
                                    ? `, ${report.patientAgeAtReportingValue}y`
                                    : ""}
                                  )
                                </span>
                              )}
                            </div>
                            <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-500">
                              <span className="font-mono text-[11px] text-slate-400">
                                {report.refId}
                              </span>
                              <span>•</span>
                              <span>
                                {report.tests?.length ?? 1} {(report.tests?.length ?? 1) === 1 ? "Test" : "Tests"}
                              </span>
                              <span>•</span>
                              <span>
                                {new Date(report.createdAt).toLocaleDateString()}
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-3">
                          <Badge
                            variant="outline"
                            className={
                              isFinalized
                                ? "border-emerald-200 bg-emerald-50 text-emerald-700 text-[10px] font-semibold"
                                : "border-amber-200 bg-amber-50 text-amber-700 text-[10px] font-semibold"
                            }
                          >
                            {report.status}
                          </Badge>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="text-xs text-teal-700 hover:text-teal-900 hover:bg-teal-50 h-8 px-2.5 font-medium hidden sm:flex items-center gap-1"
                          >
                            View
                            <ArrowRight className="size-3.5" />
                          </Button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-slate-500">
              Showing recent {recentReports.length} of {totalReportsCount} reports
            </span>
            <Button asChild variant="ghost" size="sm" className="text-xs text-teal-700 hover:text-teal-800">
              <Link to="/org-admin/reports" className="flex items-center gap-1 font-semibold">
                View All Reports ({totalReportsCount})
                <ArrowRight className="size-3.5" />
              </Link>
            </Button>
          </div>
        </div>

        {/* Right Column (4 cols): Lab Setup & Staff Capacity */}
        <div className="lg:col-span-4 space-y-6 flex flex-col justify-between">
          {/* Lab Letterhead & Branding Card */}
          <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Building2 className="size-4 text-teal-600" />
                Lab Letterhead & Branding
              </h3>
              <Badge
                variant="outline"
                className={
                  letterheadChecks.completed === 4
                    ? "border-emerald-200 bg-emerald-50 text-emerald-700 text-[10px] font-bold"
                    : "border-amber-200 bg-amber-50 text-amber-700 text-[10px] font-bold"
                }
              >
                {letterheadChecks.completed} / {letterheadChecks.total} Setup
              </Badge>
            </div>

            <p className="text-xs text-slate-500 mt-2">
              Mandatory credentials for doctor-signed diagnostic reports.
            </p>

            {/* Checklist */}
            <div className="mt-3.5 space-y-2 text-xs">
              <div className="flex items-center justify-between p-2 rounded-lg bg-slate-50 border border-slate-100">
                <span className="text-slate-600 font-medium">Lab Details & NABL Info</span>
                {letterheadChecks.hasName ? (
                  <CheckCircle2 className="size-4 text-emerald-600" />
                ) : (
                  <span className="text-amber-600 font-medium">Pending</span>
                )}
              </div>

              <div className="flex items-center justify-between p-2 rounded-lg bg-slate-50 border border-slate-100">
                <span className="text-slate-600 font-medium">Physical Lab Address</span>
                {letterheadChecks.hasAddress ? (
                  <CheckCircle2 className="size-4 text-emerald-600" />
                ) : (
                  <span className="text-amber-600 font-medium">Pending</span>
                )}
              </div>

              <div className="flex items-center justify-between p-2 rounded-lg bg-slate-50 border border-slate-100">
                <span className="text-slate-600 font-medium">Laboratory Logo</span>
                {letterheadChecks.hasLogo ? (
                  <CheckCircle2 className="size-4 text-emerald-600" />
                ) : (
                  <span className="text-slate-400 font-medium">Optional</span>
                )}
              </div>

              <div className="flex items-center justify-between p-2 rounded-lg bg-slate-50 border border-slate-100">
                <span className="text-slate-600 font-medium">Doctor Signature Stamp</span>
                {letterheadChecks.hasSignature ? (
                  <CheckCircle2 className="size-4 text-emerald-600" />
                ) : (
                  <span className="text-rose-600 font-medium">Required</span>
                )}
              </div>
            </div>

            <Button
              asChild
              variant="outline"
              size="sm"
              className="w-full mt-4 text-xs border-teal-200 text-teal-800 hover:bg-teal-50 cursor-pointer h-9 font-medium"
            >
              <Link to="/org-admin/settings" className="flex items-center justify-center gap-1.5">
                <Sparkles className="size-3.5 text-teal-600" />
                Configure Letterhead & Signature
              </Link>
            </Button>
          </div>

          {/* License & Staff Capacity Card */}
          <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <ShieldCheck className="size-4 text-teal-600" />
                Staff Capacity & Plan
              </h3>
              <Badge variant="outline" className="border-slate-200 text-slate-700 text-[10px] font-mono">
                {licenseData?.license?.planName || staffSummary?.planName || "Standard Lab"}
              </Badge>
            </div>

            <div className="mt-4 space-y-3">
              {isStaffLoading ? (
                <Skeleton className="h-14 w-full" />
              ) : (
                <div>
                  <div className="flex justify-between text-xs mb-1.5 font-medium">
                    <span className="text-slate-600">Lab Staff Seats</span>
                    <span className="text-slate-900 font-bold font-mono">
                      {staffSummary?.activeStaff ?? 1} / {staffSummary?.maxLabStaff ?? 3} Active
                    </span>
                  </div>

                  <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                    <div
                      className="bg-teal-600 h-full rounded-full transition-all"
                      style={{
                        width: `${Math.min(
                          100,
                          (((staffSummary?.activeStaff ?? 1) / (staffSummary?.maxLabStaff ?? 3)) * 100)
                        )}%`,
                      }}
                    />
                  </div>

                  <p className="text-[11px] text-slate-500 mt-2">
                    {staffSummary?.remainingSlots ?? 2} staff seats remaining under current plan tier.
                  </p>
                </div>
              )}

              <Button
                asChild
                size="sm"
                variant="ghost"
                className="w-full text-xs text-slate-600 hover:text-slate-900 justify-between h-8 px-1 pt-2 border-t border-slate-100"
              >
                <Link to="/org-admin/license">
                  View License & Upgrade
                  <ArrowRight className="size-3.5" />
                </Link>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* 6. Quick Access Navigation Tiles (3 Columns) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Patient Directory */}
        <Link to="/org-admin/patients" className="block group">
          <div className="rounded-2xl border border-slate-200/80 bg-white p-5 group-hover:border-teal-300 group-hover:shadow-xs transition-all h-full flex items-start gap-4">
            <div className="p-3 rounded-xl bg-teal-50 text-teal-600 group-hover:bg-teal-600 group-hover:text-white transition-colors shrink-0">
              <UserPlus className="size-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-bold text-slate-900 group-hover:text-teal-700 transition-colors">
                  Patient Directory
                </h4>
                <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4 border-slate-200 text-slate-600 font-mono">
                  {totalPatientsCount}
                </Badge>
              </div>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                Admit new patients, inspect medical histories, and retrieve past lab results.
              </p>
            </div>
          </div>
        </Link>

        {/* Assigned Tests */}
        <Link to="/org-admin/tests" className="block group">
          <div className="rounded-2xl border border-slate-200/80 bg-white p-5 group-hover:border-indigo-300 group-hover:shadow-xs transition-all h-full flex items-start gap-4">
            <div className="p-3 rounded-xl bg-indigo-50 text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white transition-colors shrink-0">
              <FlaskConical className="size-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-bold text-slate-900 group-hover:text-indigo-700 transition-colors">
                  Assigned Tests & Ranges
                </h4>
                <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4 border-slate-200 text-slate-600 font-mono">
                  {activeTestsCount}
                </Badge>
              </div>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                Inspect biological reference intervals, test units, and assign laboratory tests.
              </p>
            </div>
          </div>
        </Link>

        {/* Lab Staff */}
        <Link to="/org-admin/staff" className="block group">
          <div className="rounded-2xl border border-slate-200/80 bg-white p-5 group-hover:border-purple-300 group-hover:shadow-xs transition-all h-full flex items-start gap-4">
            <div className="p-3 rounded-xl bg-purple-50 text-purple-600 group-hover:bg-purple-600 group-hover:text-white transition-colors shrink-0">
              <Users className="size-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-bold text-slate-900 group-hover:text-purple-700 transition-colors">
                  Lab Staff Members
                </h4>
                <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4 border-slate-200 text-slate-600 font-mono">
                  {staffSummary?.activeStaff ?? 1}
                </Badge>
              </div>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                Manage pathologists, technicians, and configure permission roles.
              </p>
            </div>
          </div>
        </Link>
      </div>

      {/* 7. Bottom Section: Recent Patients & Recent Lab Activity Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (6 cols): Recent Patients */}
        <div className="lg:col-span-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <Users className="size-4 text-teal-600" />
                  Recent Patients
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Newly registered patients admitted for diagnostic testing.
                </p>
              </div>
              <Button asChild variant="ghost" size="sm" className="text-xs text-teal-700 hover:text-teal-800 h-8">
                <Link to="/org-admin/patients" className="flex items-center gap-1">
                  View All
                  <ArrowRight className="size-3" />
                </Link>
              </Button>
            </div>

            <div className="pt-2">
              {isPatientsLoading ? (
                <div className="space-y-3 py-4">
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-10 w-full rounded-lg" />
                  ))}
                </div>
              ) : recentPatients.length === 0 ? (
                <div className="py-8">
                  <EmptyState
                    title="No patients enrolled"
                    description="Register your first patient to begin ordering and generating reports."
                    action={{
                      label: "Register Patient",
                      onClick: () => navigate("/org-admin/patients"),
                    }}
                  />
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {recentPatients.map((patient) => (
                    <div
                      key={patient.refId}
                      onClick={() => navigate(`/org-admin/patients`)}
                      className="py-3 px-2 hover:bg-slate-50/80 rounded-xl transition-colors flex items-center justify-between cursor-pointer group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="size-9 rounded-full bg-slate-100 text-slate-700 flex items-center justify-center font-bold text-xs">
                          {patient.name?.charAt(0)?.toUpperCase() || "P"}
                        </div>
                        <div>
                          <div className="font-semibold text-slate-900 text-sm group-hover:text-teal-700 transition-colors">
                            {patient.name}
                          </div>
                          <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5 font-mono">
                            <span>{patient.refId}</span>
                            {patient.ageValue && (
                              <>
                                <span>•</span>
                                <span>Age {patient.ageValue}</span>
                              </>
                            )}
                            {patient.gender && (
                              <>
                                <span>•</span>
                                <span className="capitalize">{patient.gender.toLowerCase()}</span>
                              </>
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="text-[11px] text-slate-400">
                          {patient.createdAt ? new Date(patient.createdAt).toLocaleDateString() : ""}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Enrolled patient records</span>
            <span className="font-semibold text-slate-700">{totalPatientsCount} Total</span>
          </div>
        </div>

        {/* Right Column (6 cols): Recent Lab Activity Timeline Feed */}
        <div className="lg:col-span-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <Activity className="size-4 text-teal-600" />
                  Recent Lab Activity
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Audit trail of operational events across reports and patient registrations.
                </p>
              </div>
              <Badge variant="outline" className="border-teal-200 bg-teal-50 text-teal-700 text-[10px]">
                Real-time
              </Badge>
            </div>

            <div className="pt-2">
              {isActivityLoading ? (
                <div className="space-y-3 py-4">
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-10 w-full rounded-lg" />
                  ))}
                </div>
              ) : !activityFeed || activityFeed.length === 0 ? (
                <div className="py-8 text-center text-slate-400 text-xs">
                  No recent operational events recorded.
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {activityFeed.map((item) => {
                    const isFinalized = item.type === "REPORT_FINALIZED";
                    const isCreated = item.type === "REPORT_CREATED";

                    return (
                      <div key={item.id} className="py-3 px-2 flex items-start gap-3 text-xs">
                        <div
                          className={`size-7 rounded-lg flex items-center justify-center shrink-0 mt-0.5 ${
                            isFinalized
                              ? "bg-emerald-50 text-emerald-600"
                              : isCreated
                              ? "bg-sky-50 text-sky-600"
                              : "bg-indigo-50 text-indigo-600"
                          }`}
                        >
                          {isFinalized ? (
                            <FileCheck2 className="size-3.5" />
                          ) : isCreated ? (
                            <FileText className="size-3.5" />
                          ) : (
                            <UserPlus className="size-3.5" />
                          )}
                        </div>

                        <div className="flex-1 min-w-0">
                          <p className="text-slate-800 font-medium leading-snug truncate">
                            {item.description}
                          </p>
                          <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-0.5">
                            <span className="font-medium text-slate-600">{item.userName}</span>
                            <span>•</span>
                            <span className="font-mono">{item.time}</span>
                          </div>
                        </div>

                        <span className="font-mono text-[10px] text-slate-400 shrink-0">
                          {item.id}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Automated event logging</span>
            <span className="font-medium text-teal-700">Active</span>
          </div>
        </div>
      </div>
    </div>
  );
}
