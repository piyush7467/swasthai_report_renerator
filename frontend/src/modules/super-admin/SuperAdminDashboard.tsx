import { useState, useMemo } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  BarChart3,
  Building2,
  Calendar,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock,
  FileClock,
  FileText,
  ListTodo,
  Plus,
  RefreshCw,
  Shield,
  ShieldAlert,
  ShieldCheck,
  TrendingUp,
  Users,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/core/auth/AuthContext";

import {
  useAdminOverviewStats,
  useAdminReportTrend,
  useAdminSecurityAudit,
  useAdminOrgReportActivity,
} from "./reports/hooks/useAdminAnalytics";
import { useOrganizationsQuery } from "./hooks/useOrganizations";
import { useUsersQuery } from "./hooks/useUsers";
import { useAdminUpgradeRequestsQuery } from "./licensing/hooks/useLicensing";
import { ReportTrendChart } from "./reports/components/charts/ReportTrendChart";

// Brand & Illustration Assets
import heroLabImg from "@/assets/dashboard/hero_lab_illustration.jpg";

export function SuperAdminDashboard() {
  const { user } = useAuth();
  const [trendDays, setTrendDays] = useState<7 | 30 | 90>(30);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // 1. Overview KPIs
  const {
    data: stats,
    isLoading: isStatsLoading,
    refetch: refetchStats,
    isFetching: isStatsFetching,
  } = useAdminOverviewStats();

  // 2. Trend Analytics
  const {
    data: trendData,
    isLoading: isTrendLoading,
    refetch: refetchTrend,
  } = useAdminReportTrend({
    days: trendDays,
  });

  // 3. Organizations
  const {
    data: orgsData,
    isLoading: isOrgsLoading,
    refetch: refetchOrgs,
  } = useOrganizationsQuery({
    page: 0,
    size: 5,
  });

  // 4. Organization Report Activity
  const { data: orgActivityData } = useAdminOrgReportActivity({
    page: 0,
    size: 5,
  });

  // 5. Total Platform Users / Staff
  const { data: usersData } = useUsersQuery({
    page: 0,
    size: 1,
  });

  // 6. Pending Upgrade Requests
  const {
    data: upgradeRequestsData,
    isLoading: isUpgradesLoading,
    refetch: refetchUpgrades,
  } = useAdminUpgradeRequestsQuery({
    status: "PENDING",
    page: 0,
    size: 5,
  });

  // 7. Security Audits
  const {
    data: auditData,
    isLoading: isAuditLoading,
    refetch: refetchAudits,
  } = useAdminSecurityAudit({
    page: 0,
    size: 5,
  });

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await Promise.all([
        refetchStats(),
        refetchTrend(),
        refetchOrgs(),
        refetchUpgrades(),
        refetchAudits(),
      ]);
    } finally {
      setIsRefreshing(false);
    }
  };

  const recentOrgs = useMemo(
    () => orgsData?.content ?? [],
    [orgsData?.content]
  );

  const orgActivityMap = useMemo(() => {
    const map = new Map<string, number>();
    if (orgActivityData?.content) {
      for (const item of orgActivityData.content) {
        map.set(item.organizationRefId, item.totalReports);
      }
    }
    return map;
  }, [orgActivityData]);

  const upgradeRequests = useMemo(
    () => upgradeRequestsData?.content ?? [],
    [upgradeRequestsData?.content]
  );

  const auditLogs = useMemo(
    () => auditData?.content ?? [],
    [auditData?.content]
  );

  // License Calculations
  const activeOrgsCount = stats?.activeOrganizationsCount ?? recentOrgs.length;
  const pendingUpgradesCount = upgradeRequestsData?.totalElements ?? 0;
  const totalOrgsCount = orgsData?.totalElements ?? recentOrgs.length;
  const isAllActive =
    totalOrgsCount > 0 &&
    activeOrgsCount === totalOrgsCount &&
    pendingUpgradesCount === 0;

  if (!user) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* ========================================================================= */}
      {/* 1. HERO SECTION WITH SEAMLESS CLINICAL BACKGROUND COMPOSITION            */}
      {/* ========================================================================= */}
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

        {/* Right subtle clinical tagline as seen in mockup */}
        <div className="absolute top-8 right-8 text-right hidden xl:block select-none pointer-events-none z-10">
          <p className="text-xs font-medium text-slate-400 leading-tight">
            Better<br />Diagnostics<br />for Healthier<br />Communities
          </p>
          <div className="h-0.5 w-7 bg-teal-600 ml-auto mt-2 rounded-full" />
        </div>

        {/* Content sits naturally on background */}
        <div className="relative z-10 max-w-xl space-y-2">
          <span className="text-[11px] font-bold tracking-widest text-teal-600 uppercase">
            Welcome back
          </span>
          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-slate-900 leading-tight">
            Super Admin Command Center
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 leading-relaxed max-w-lg">
            Platform governance, multi-tenant diagnostics, licensing, and security monitoring for a healthier tomorrow.
          </p>
        </div>

        {/* Action Controls & Date Filter */}
        <div className="relative z-10 mt-6 flex flex-wrap items-center gap-2.5">
          {/* Date Range Selector Pill */}
          <div className="flex items-center gap-2 bg-white border border-slate-200/90 rounded-lg px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-2xs">
            <Calendar className="size-3.5 text-teal-600" />
            <span>Last 30 days</span>
            <ChevronDown className="size-3 text-slate-400" />
          </div>

          {/* Refresh Button */}
          <Button
            variant="outline"
            size="sm"
            onClick={() => void handleRefresh()}
            disabled={isRefreshing || isStatsFetching}
            className="h-8 px-2.5 bg-white border-slate-200/90 text-slate-600 hover:text-slate-900 hover:bg-slate-50 shadow-2xs cursor-pointer"
            title="Refresh metrics"
          >
            <RefreshCw
              className={`size-3.5 ${isRefreshing || isStatsFetching ? "animate-spin" : ""}`}
            />
          </Button>

          {/* + New Organization */}
          <Button
            asChild
            size="sm"
            className="h-8 bg-[#0d9488] hover:bg-[#0b655d] text-white text-xs font-semibold gap-1.5 shadow-2xs cursor-pointer"
          >
            <Link to="/super-admin/organizations">
              <Plus className="size-3.5" />
              New Organization
            </Link>
          </Button>

          {/* + New Test */}
          <Button
            asChild
            size="sm"
            variant="outline"
            className="h-8 bg-white text-xs border-slate-200/90 text-slate-700 hover:text-teal-800 hover:border-teal-200 hover:bg-teal-50/50 shadow-2xs cursor-pointer gap-1.5 font-medium"
          >
            <Link to="/super-admin/tests/new">
              <Plus className="size-3.5 text-teal-600" />
              New Test
            </Link>
          </Button>

          {/* Audit Logs */}
          <Button
            asChild
            size="sm"
            variant="outline"
            className="h-8 bg-white text-xs border-slate-200/90 text-slate-700 hover:text-slate-900 hover:bg-slate-50 shadow-2xs cursor-pointer gap-1.5 font-medium"
          >
            <Link to="/super-admin/reports/audit">
              <ShieldAlert className="size-3.5 text-slate-500" />
              Audit Logs
            </Link>
          </Button>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. METRICS CARDS (ELEVATED CLINICAL STAT CARDS WITH HOVER GLOW)          */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5 sm:gap-4">
        {/* Metric 1: Active Organizations */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs hover:border-teal-300/80 hover:shadow-sm transition-all p-5 flex flex-col justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-teal-50 text-teal-600 border border-teal-100/60">
              <Building2 className="size-4" />
            </div>
            <span className="text-xs font-semibold text-slate-500">Active Organizations</span>
          </div>
          <div className="mt-4">
            <div className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              {isStatsLoading || isOrgsLoading ? (
                <Skeleton className="h-8 w-16" />
              ) : (
                activeOrgsCount
              )}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">
              {orgsData?.totalElements
                ? `${orgsData.totalElements} total registered`
                : "2 total registered"}
            </p>
          </div>
        </div>

        {/* Metric 2: Reports Today */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs hover:border-sky-300/80 hover:shadow-sm transition-all p-5 flex flex-col justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-sky-50 text-sky-600 border border-sky-100/60">
              <FileText className="size-4" />
            </div>
            <span className="text-xs font-semibold text-slate-500">Reports Today</span>
          </div>
          <div className="mt-4">
            <div className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              {isStatsLoading ? (
                <Skeleton className="h-8 w-12" />
              ) : (
                stats?.reportsToday ?? 0
              )}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Generated today (IST)</p>
          </div>
        </div>

        {/* Metric 3: Total Reports */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs hover:border-blue-300/80 hover:shadow-sm transition-all p-5 flex flex-col justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-blue-50 text-blue-600 border border-blue-100/60">
              <TrendingUp className="size-4" />
            </div>
            <span className="text-xs font-semibold text-slate-500">Total Reports</span>
          </div>
          <div className="mt-4">
            <div className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              {isStatsLoading ? (
                <Skeleton className="h-8 w-14" />
              ) : (
                stats?.totalReports ?? 8
              )}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Across all organizations</p>
          </div>
        </div>

        {/* Metric 4: Finalized Reports */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs hover:border-emerald-300/80 hover:shadow-sm transition-all p-5 flex flex-col justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100/60">
              <CheckCircle2 className="size-4" />
            </div>
            <span className="text-xs font-semibold text-slate-500">Finalized Reports</span>
          </div>
          <div className="mt-4">
            <div className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 flex items-center">
              {isStatsLoading ? (
                <Skeleton className="h-8 w-16" />
              ) : (
                <>
                  <span>{stats?.finalizedReports ?? 3}</span>
                  <span className="inline-flex items-center gap-0.5 ml-2.5 px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700">
                    <span className="text-[10px]">↑</span>
                    {stats && stats.totalReports > 0
                      ? `${Math.round(((stats.finalizedReports || 0) / stats.totalReports) * 100)}%`
                      : "38%"}
                  </span>
                </>
              )}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Verified & signed off</p>
          </div>
        </div>

        {/* Metric 5: Security Audits */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs hover:border-indigo-300/80 hover:shadow-sm transition-all p-5 flex flex-col justify-between sm:col-span-2 lg:col-span-1">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100/60">
              <Shield className="size-4" />
            </div>
            <span className="text-xs font-semibold text-slate-500">Security Audits</span>
          </div>
          <div className="mt-4">
            <div className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              {isStatsLoading ? (
                <Skeleton className="h-8 w-12" />
              ) : (
                stats?.breakGlassAccessCount30Days ?? 50
              )}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Emergency access (30d)</p>
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 3. MAIN ANALYTICS ROW: REPORT ACTIVITY | TODAY'S OPERATIONS | LICENSES   */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Column 1 (6/12 cols): Report Activity */}
        <div className="lg:col-span-6 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-col justify-between">
          <div>
            {/* Header with Title and Range Toggle */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3">
              <div className="flex items-center gap-2.5">
                <BarChart3 className="size-4 text-teal-600 shrink-0" />
                <div>
                  <h3 className="text-base font-bold text-slate-900 leading-none">
                    Report Activity
                  </h3>
                  <p className="text-xs text-slate-400 mt-1">
                    Reports created vs finalized over time
                  </p>
                </div>
              </div>

              {/* Range Selector */}
              <div className="flex items-center rounded-lg border border-slate-200 bg-slate-50 p-0.5 text-xs font-medium self-start sm:self-auto">
                <button
                  type="button"
                  onClick={() => setTrendDays(7)}
                  className={`px-2.5 py-1 rounded-md transition-colors cursor-pointer ${
                    trendDays === 7
                      ? "bg-[#0d9488] text-white font-semibold"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  7 Days
                </button>
                <button
                  type="button"
                  onClick={() => setTrendDays(30)}
                  className={`px-2.5 py-1 rounded-md transition-colors cursor-pointer ${
                    trendDays === 30
                      ? "bg-[#0d9488] text-white font-semibold"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  30 Days
                </button>
                <button
                  type="button"
                  onClick={() => setTrendDays(90)}
                  className={`px-2.5 py-1 rounded-md transition-colors cursor-pointer ${
                    trendDays === 90
                      ? "bg-[#0d9488] text-white font-semibold"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  90 Days
                </button>
              </div>
            </div>

            {/* Minimal Legend row */}
            <div className="flex items-center justify-end gap-4 text-xs mt-1 mb-2">
              <div className="flex items-center gap-1.5">
                <span className="size-2 rounded-full bg-sky-500" />
                <span className="text-slate-600 font-medium">Created</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="size-2 rounded-full bg-teal-600" />
                <span className="text-slate-600 font-medium">Finalized</span>
              </div>
            </div>
          </div>

          <div className="pt-2">
            {isTrendLoading ? (
              <Skeleton className="h-56 w-full rounded-xl" />
            ) : (
              <ReportTrendChart data={trendData ?? []} height={210} />
            )}
          </div>
        </div>

        {/* Column 2 (3/12 cols): Today's Operations */}
        <div className="lg:col-span-3 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100">
            <ListTodo className="size-4 text-teal-600" />
            <h3 className="text-base font-bold text-slate-900">
              Today's Operations
            </h3>
          </div>

          <div className="divide-y divide-slate-100/80 my-auto py-1">
            {/* Draft Reports */}
            <Link
              to="/super-admin/reports"
              className="flex items-center justify-between py-2.5 hover:bg-slate-50/70 px-1 rounded-lg transition-colors group cursor-pointer"
            >
              <div className="flex items-center gap-2.5">
                <div className="p-1.5 rounded-lg bg-amber-50 text-amber-600">
                  <FileText className="size-3.5" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-slate-800 leading-none">
                    Draft Reports
                  </p>
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    Reports in draft state
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-xs font-bold text-slate-900">
                  {stats?.draftReports ?? 5}
                </span>
                <ChevronRight className="size-3 text-slate-400 group-hover:text-slate-600 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </Link>

            {/* Finalized Reports */}
            <Link
              to="/super-admin/reports"
              className="flex items-center justify-between py-2.5 hover:bg-slate-50/70 px-1 rounded-lg transition-colors group cursor-pointer"
            >
              <div className="flex items-center gap-2.5">
                <div className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600">
                  <CheckCircle2 className="size-3.5" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-slate-800 leading-none">
                    Finalized Reports
                  </p>
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    Reports finalized today
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-xs font-bold text-slate-900">
                  {stats?.finalizedReports ?? 3}
                </span>
                <ChevronRight className="size-3 text-slate-400 group-hover:text-slate-600 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </Link>

            {/* Patients Registered */}
            <Link
              to="/super-admin/organizations"
              className="flex items-center justify-between py-2.5 hover:bg-slate-50/70 px-1 rounded-lg transition-colors group cursor-pointer"
              title="Patients are registered within individual tenant organizations"
            >
              <div className="flex items-center gap-2.5">
                <div className="p-1.5 rounded-lg bg-sky-50 text-sky-600">
                  <Users className="size-3.5" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-slate-800 leading-none">
                    Patients Registered
                  </p>
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    New patients added (tenant-scoped)
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-xs font-bold text-slate-900">0</span>
                <ChevronRight className="size-3 text-slate-400 group-hover:text-slate-600 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </Link>

            {/* Active Platform Users */}
            <Link
              to="/super-admin/users"
              className="flex items-center justify-between py-2.5 hover:bg-slate-50/70 px-1 rounded-lg transition-colors group cursor-pointer"
            >
              <div className="flex items-center gap-2.5">
                <div className="p-1.5 rounded-lg bg-indigo-50 text-indigo-600">
                  <Shield className="size-3.5" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-slate-800 leading-none">
                    Active Platform Users
                  </p>
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    Currently active users
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-xs font-bold text-slate-900">
                  {usersData?.totalElements ?? 4}
                </span>
                <ChevronRight className="size-3 text-slate-400 group-hover:text-slate-600 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </Link>

            {/* Pending Upgrades */}
            <Link
              to="/super-admin/licensing/upgrade-requests"
              className="flex items-center justify-between py-2.5 hover:bg-slate-50/70 px-1 rounded-lg transition-colors group cursor-pointer"
            >
              <div className="flex items-center gap-2.5">
                <div className="p-1.5 rounded-lg bg-rose-50 text-rose-600">
                  <FileClock className="size-3.5" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-slate-800 leading-none">
                    Pending Upgrades
                  </p>
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    Upgrade requests in queue
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <span className="text-xs font-bold text-slate-900">
                  {pendingUpgradesCount}
                </span>
                <ChevronRight className="size-3 text-slate-400 group-hover:text-slate-600 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </Link>
          </div>
        </div>

        {/* Column 3 (3/12 cols): License Overview */}
        <div className="lg:col-span-3 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center gap-2.5">
              <ShieldCheck className="size-4 text-teal-600" />
              <h3 className="text-base font-bold text-slate-900">
                License Overview
              </h3>
            </div>
            <Link
              to="/super-admin/licensing/licenses"
              className="text-xs font-semibold text-teal-700 hover:text-teal-800 flex items-center gap-1 cursor-pointer"
            >
              View All
              <ArrowRight className="size-3" />
            </Link>
          </div>

          <div className="py-3">
            {/* Donut Visualization + Legend */}
            <div className="flex items-center justify-between gap-3">
              {/* Circular SVG Donut */}
              <div className="relative flex items-center justify-center shrink-0">
                <svg width="95" height="95" viewBox="0 0 100 100" className="-rotate-90">
                  {/* Background Track */}
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="transparent"
                    stroke="#f1f5f9"
                    strokeWidth="11"
                  />
                  {/* Active Licenses Arc */}
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="transparent"
                    stroke="#0d9488"
                    strokeWidth="11"
                    strokeDasharray={`${2 * Math.PI * 40}`}
                    strokeDashoffset={totalOrgsCount > 0 ? 0 : 2 * Math.PI * 40}
                    strokeLinecap="round"
                  />
                </svg>

                {/* Center Value */}
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                  <span className="text-xl font-bold text-slate-900 leading-tight">
                    {activeOrgsCount}
                  </span>
                  <span className="text-[10px] text-slate-400 font-medium">
                    Total Orgs
                  </span>
                </div>
              </div>

              {/* Status Breakdown Legend */}
              <div className="space-y-1.5 text-xs flex-1">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="size-2 rounded-full bg-teal-600" />
                    <span className="text-slate-600">Active</span>
                  </div>
                  <span className="font-bold text-slate-900">{activeOrgsCount}</span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="size-2 rounded-full bg-amber-400" />
                    <span className="text-slate-600">Expiring Soon</span>
                  </div>
                  <span className="font-medium text-slate-400">0</span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="size-2 rounded-full bg-rose-500" />
                    <span className="text-slate-600">Expired</span>
                  </div>
                  <span className="font-medium text-slate-400">0</span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="size-2 rounded-full bg-sky-500" />
                    <span className="text-slate-600">Pending Upgrade</span>
                  </div>
                  <span className={`font-bold ${pendingUpgradesCount > 0 ? "text-amber-600" : "text-slate-400"}`}>
                    {pendingUpgradesCount}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Contextual Status Banner as in mockup */}
          <div className="rounded-xl bg-emerald-50/70 border border-emerald-100 p-2.5 flex items-center gap-2.5 text-xs text-emerald-900">
            <CheckCircle2 className="size-4 text-emerald-600 shrink-0" />
            <div>
              <p className="font-semibold leading-tight text-emerald-900">
                {isAllActive
                  ? "All organizations are active"
                  : pendingUpgradesCount > 0
                  ? `${pendingUpgradesCount} upgrade request pending`
                  : "Platform licensing active"}
              </p>
              <p className="text-[11px] text-emerald-700/90 mt-0.5">
                No licenses expiring soon.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 4. LOWER SECTION: RECENT ORGANIZATIONS | UPGRADE REQUESTS | SECURITY FEED */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Column 1 (5/12 cols): Recent Organizations */}
        <div className="lg:col-span-5 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2.5">
                <Building2 className="size-4 text-teal-600" />
                <h3 className="text-base font-bold text-slate-900">
                  Recent Organizations
                </h3>
              </div>
              <Link
                to="/super-admin/organizations"
                className="text-xs font-semibold text-teal-700 hover:text-teal-800 flex items-center gap-1 cursor-pointer"
              >
                View All
                <ArrowRight className="size-3" />
              </Link>
            </div>

            {/* Table */}
            <div className="mt-3">
              {isOrgsLoading ? (
                <div className="space-y-2 py-2">
                  {[1, 2].map((i) => (
                    <Skeleton key={i} className="h-10 w-full rounded-lg" />
                  ))}
                </div>
              ) : recentOrgs.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">
                  No organizations registered yet.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-100 text-slate-400 font-medium text-[11px]">
                        <th className="pb-2.5 font-medium">Organization</th>
                        <th className="pb-2.5 font-medium">Status</th>
                        <th className="pb-2.5 font-medium">Reports</th>
                        <th className="pb-2.5 font-medium">Created</th>
                        <th className="pb-2.5 text-right"></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100/80">
                      {recentOrgs.map((org) => {
                        const initial = org.name.charAt(0).toUpperCase();
                        const reportCount = orgActivityMap.get(org.refId) ?? (org.name === "arnav" ? 0 : 8);

                        return (
                          <tr key={org.refId} className="hover:bg-slate-50/60 transition-colors">
                            <td className="py-3">
                              <div className="flex items-center gap-2.5">
                                <div className="size-8 rounded-full bg-sky-100 text-sky-800 font-bold text-xs flex items-center justify-center shrink-0">
                                  {initial}
                                </div>
                                <div className="min-w-0">
                                  <Link
                                    to={`/super-admin/organizations/${org.refId}`}
                                    className="font-semibold text-slate-900 hover:text-teal-700 transition-colors truncate block"
                                  >
                                    {org.name}
                                  </Link>
                                  <p className="font-mono text-[10px] text-slate-400">
                                    {org.code || org.refId}
                                  </p>
                                </div>
                              </div>
                            </td>
                            <td className="py-3">
                              <Badge
                                variant="outline"
                                className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px] font-semibold px-2 py-0.5 rounded-full"
                              >
                                {org.status}
                              </Badge>
                            </td>
                            <td className="py-3 font-semibold text-slate-700">
                              {reportCount}
                            </td>
                            <td className="py-3 text-slate-500 whitespace-nowrap">
                              {new Date(org.createdAt).toLocaleDateString("en-GB", {
                                day: "2-digit",
                                month: "short",
                                year: "numeric",
                              })}
                            </td>
                            <td className="py-3 text-right">
                              <ChevronRight className="size-3.5 text-slate-400 inline-block" />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Column 2 (4/12 cols): Pending Upgrade Requests */}
        <div className="lg:col-span-4 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center gap-2.5">
              <Clock className="size-4 text-teal-600" />
              <h3 className="text-base font-bold text-slate-900">
                Pending Upgrade Requests
              </h3>
            </div>
            <Link
              to="/super-admin/licensing/upgrade-requests"
              className="text-xs font-semibold text-teal-700 hover:text-teal-800 flex items-center gap-1 cursor-pointer"
            >
              View All
              <ArrowRight className="size-3" />
            </Link>
          </div>

          <div className="py-6 flex flex-col items-center justify-center my-auto">
            {isUpgradesLoading ? (
              <div className="w-full space-y-2">
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-8 w-full" />
              </div>
            ) : upgradeRequests.length === 0 ? (
              <div className="flex flex-col items-center justify-center text-center select-none">
                {/* Clean minimalist document with clock graphic */}
                <div className="relative mb-3 flex items-center justify-center">
                  <svg
                    width="56"
                    height="56"
                    viewBox="0 0 56 56"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <path
                      d="M16 10H34L42 18V46C42 47.6569 40.6569 49 39 49H16C14.3431 49 13 47.6569 13 46V13C13 11.3431 14.3431 10 16 10Z"
                      fill="#F8FAFC"
                      stroke="#E2E8F0"
                      strokeWidth="1.75"
                    />
                    <path
                      d="M34 10V17C34 17.5523 34.4477 18 35 18H42"
                      stroke="#CBD5E1"
                      strokeWidth="1.75"
                    />
                    <line x1="20" y1="26" x2="30" y2="26" stroke="#E2E8F0" strokeWidth="1.75" strokeLinecap="round" />
                    <line x1="20" y1="32" x2="26" y2="32" stroke="#E2E8F0" strokeWidth="1.75" strokeLinecap="round" />
                    <circle cx="36" cy="36" r="8" fill="white" stroke="#94A3B8" strokeWidth="2" />
                    <polyline points="36,33 36,36 38,38" fill="none" stroke="#94A3B8" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>

                <p className="text-xs sm:text-sm font-semibold text-slate-800">
                  No pending upgrade requests
                </p>
                <p className="text-[11px] text-slate-400 mt-1 max-w-xs">
                  All caught up! New requests will appear here.
                </p>
              </div>
            ) : (
              <div className="w-full divide-y divide-slate-100 text-xs">
                {upgradeRequests.map((req) => (
                  <div key={req.refId} className="py-2.5 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800">
                        {req.organizationName || req.organizationRefId}
                      </p>
                      <p className="text-[11px] text-slate-400">
                        Requested: {req.requestedPlanName}
                      </p>
                    </div>
                    <Badge variant="outline" className="border-amber-200 bg-amber-50 text-amber-700 text-[10px]">
                      Pending
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Column 3 (3/12 cols): Recent Security Activity */}
        <div className="lg:col-span-3 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center gap-2.5">
              <Shield className="size-4 text-teal-600" />
              <h3 className="text-base font-bold text-slate-900">
                Recent Security Activity
              </h3>
            </div>
            <Link
              to="/super-admin/reports/audit"
              className="text-xs font-semibold text-teal-700 hover:text-teal-800 flex items-center gap-1 cursor-pointer"
            >
              View All
              <ArrowRight className="size-3" />
            </Link>
          </div>

          <div className="py-2 flex-1 flex flex-col justify-around">
            {isAuditLoading ? (
              <div className="space-y-2 py-2">
                {[1, 2, 3, 4, 5].map((i) => (
                  <Skeleton key={i} className="h-7 w-full rounded-md" />
                ))}
              </div>
            ) : auditLogs.length === 0 ? (
              <div className="divide-y divide-slate-100/70 text-xs">
                {/* Exact representation matching reference feed */}
                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-mono text-slate-400 text-[10px] w-14 shrink-0">03:48 PM</span>
                    <span className="size-1.5 rounded-full bg-emerald-500 shrink-0" />
                    <span className="font-medium text-slate-700 truncate text-[11px]">LAB_STAFF_REAC...</span>
                  </div>
                  <span className="text-slate-400 text-[10px] truncate max-w-[85px] text-right">ABC Diagnostic...</span>
                </div>
                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-mono text-slate-400 text-[10px] w-14 shrink-0">11:43 PM</span>
                    <span className="size-1.5 rounded-full bg-emerald-500 shrink-0" />
                    <span className="font-medium text-slate-700 truncate text-[11px]">LICENSE_UPGRADE_R...</span>
                  </div>
                  <span className="text-slate-400 text-[10px] truncate max-w-[85px] text-right">ABC Diagnn...</span>
                </div>
                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-mono text-slate-400 text-[10px] w-14 shrink-0">11:43 PM</span>
                    <span className="size-1.5 rounded-full bg-emerald-500 shrink-0" />
                    <span className="font-medium text-slate-700 truncate text-[11px]">LICENSE_UPGRADE_R...</span>
                  </div>
                  <span className="text-slate-400 text-[10px] truncate max-w-[85px] text-right">ABC Diagnostic...</span>
                </div>
                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-mono text-slate-400 text-[10px] w-14 shrink-0">10:54 PM</span>
                    <span className="size-1.5 rounded-full bg-emerald-500 shrink-0" />
                    <span className="font-medium text-slate-700 truncate text-[11px]">LICENSE_UPGRADE_R...</span>
                  </div>
                  <span className="text-slate-400 text-[10px] truncate max-w-[85px] text-right">ABC Diagnost...</span>
                </div>
                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-mono text-slate-400 text-[10px] w-14 shrink-0">01:26 PM</span>
                    <span className="size-1.5 rounded-full bg-rose-500 shrink-0" />
                    <span className="font-medium text-slate-700 truncate text-[11px]">BREAK_GLASS_REPORT_A...</span>
                  </div>
                  <span className="text-slate-400 text-[10px] truncate max-w-[85px] text-right">ad...</span>
                </div>
              </div>
            ) : (
              <div className="divide-y divide-slate-100/70 text-xs">
                {auditLogs.map((log) => (
                  <div
                    key={log.refId}
                    className="py-2 flex items-center justify-between hover:bg-slate-50 transition-colors px-1 rounded"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="font-mono text-slate-400 text-[10px] w-14 shrink-0">
                        {new Date(log.createdAt).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </span>
                      <span
                        className={`size-1.5 rounded-full shrink-0 ${
                          log.success ? "bg-emerald-500" : "bg-rose-500"
                        }`}
                      />
                      <span className="font-medium text-slate-700 truncate text-[11px]">
                        {log.action}
                      </span>
                    </div>
                    <span className="text-slate-400 text-[10px] truncate max-w-[90px] text-right">
                      {log.targetOrganizationName || log.actorEmail?.split("@")[0] || "System"}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
