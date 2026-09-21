import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Calendar,
  CalendarDays,
  CheckCircle2,
  FileClock,
  TrendingUp,
  Building2,
  ShieldAlert,
  Trash2,
  ArrowRight,
  Activity,
  AlertCircle,
  RefreshCw,
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
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  useAdminOverviewStats,
  useAdminReportTrend,
} from "../hooks/useAdminAnalytics";
import { ReportTrendChart } from "../components/charts/ReportTrendChart";
import { StatusDistributionChart } from "../components/charts/StatusDistributionChart";
import { BreakGlassDialog } from "../components/BreakGlassDialog";

export default function ReportOverviewPage() {
  const [trendDays, setTrendDays] = useState<7 | 30>(30);
  const [isBreakGlassOpen, setIsBreakGlassOpen] = useState<boolean>(false);

  const {
    data: stats,
    isLoading: isStatsLoading,
    isError: isStatsError,
    error: statsError,
    refetch: refetchStats,
  } = useAdminOverviewStats();

  const {
    data: trendData,
    isLoading: isTrendLoading,
  } = useAdminReportTrend({ days: trendDays });

  if (isStatsError) {
    return (
      <div className="space-y-4">
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Error loading report analytics</AlertTitle>
          <AlertDescription>
            {statsError instanceof Error
              ? statsError.message
              : "Failed to connect to admin analytics service."}
          </AlertDescription>
        </Alert>
        <Button
          onClick={() => refetchStats()}
          variant="outline"
          size="sm"
          className="gap-2"
        >
          <RefreshCw className="h-4 w-4" />
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Report Operations & Analytics
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Platform-wide report volume, lifecycle distribution, and security monitoring.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={() => setIsBreakGlassOpen(true)}
            size="sm"
            className="gap-1.5 text-xs bg-rose-600 hover:bg-rose-700 text-white font-semibold shadow-xs"
          >
            <ShieldAlert className="h-3.5 w-3.5" />
            Emergency Break-Glass
          </Button>
          <Button asChild variant="outline" size="sm" className="gap-1.5 text-xs">
            <Link to="/super-admin/reports/activity">
              <Activity className="h-3.5 w-3.5 text-blue-600" />
              Activity Breakdown
            </Link>
          </Button>
          <Button asChild variant="outline" size="sm" className="gap-1.5 text-xs">
            <Link to="/super-admin/reports/audit">
              <ShieldAlert className="h-3.5 w-3.5 text-indigo-600" />
              Security Audit
            </Link>
          </Button>
        </div>
      </div>

      {/* Top KPI Cards Grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        {/* Reports Today */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Reports Today
            </CardTitle>
            <Calendar className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-20" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.reportsToday.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Calendar day (IST)</p>
          </CardContent>
        </Card>

        {/* Reports This Week */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              This Week
            </CardTitle>
            <CalendarDays className="h-4 w-4 text-indigo-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-20" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.reportsThisWeek.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Since Monday</p>
          </CardContent>
        </Card>

        {/* Reports This Month */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              This Month
            </CardTitle>
            <TrendingUp className="h-4 w-4 text-purple-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-20" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.reportsThisMonth.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Current calendar month</p>
          </CardContent>
        </Card>

        {/* Finalized Reports */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Finalized
            </CardTitle>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-20" />
            ) : (
              <div className="text-2xl font-bold text-emerald-700">
                {stats?.finalizedReports.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Active finalized</p>
          </CardContent>
        </Card>

        {/* Draft Reports */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Drafts / In-Progress
            </CardTitle>
            <FileClock className="h-4 w-4 text-amber-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-20" />
            ) : (
              <div className="text-2xl font-bold text-amber-700">
                {stats?.draftReports.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Unfinalized drafts</p>
          </CardContent>
        </Card>

        {/* Active Organizations with Reports */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Active Tenants
            </CardTitle>
            <Building2 className="h-4 w-4 text-sky-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-20" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.organizationsWithReportsCount}
                <span className="text-xs font-normal text-slate-500 ml-1">
                  / {stats?.activeOrganizationsCount}
                </span>
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Generating reports</p>
          </CardContent>
        </Card>
      </div>

      {/* Main Visualizations Grid */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Report Trend Chart Card (2 cols) */}
        <Card className="border-slate-200 bg-white lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <div>
              <CardTitle className="text-base font-semibold text-slate-900">
                Platform Report Volume Trend
              </CardTitle>
              <CardDescription className="text-xs text-slate-500">
                Daily report generation activity across all tenant diagnostic centers.
              </CardDescription>
            </div>
            <div className="flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 p-1">
              <button
                type="button"
                onClick={() => setTrendDays(7)}
                className={`rounded px-2.5 py-1 text-xs font-medium transition-colors ${
                  trendDays === 7
                    ? "bg-white text-slate-900 shadow-xs"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                7 Days
              </button>
              <button
                type="button"
                onClick={() => setTrendDays(30)}
                className={`rounded px-2.5 py-1 text-xs font-medium transition-colors ${
                  trendDays === 30
                    ? "bg-white text-slate-900 shadow-xs"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                30 Days
              </button>
            </div>
          </CardHeader>
          <CardContent className="pt-2">
            {isTrendLoading ? (
              <div className="h-64 flex items-center justify-center">
                <Skeleton className="h-56 w-full" />
              </div>
            ) : (
              <ReportTrendChart data={trendData || []} height={260} />
            )}
          </CardContent>
        </Card>

        {/* Status Distribution Card (1 col) */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold text-slate-900">
              Lifecycle Distribution
            </CardTitle>
            <CardDescription className="text-xs text-slate-500">
              Breakdown of non-deleted reports across active statuses.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            {isStatsLoading ? (
              <div className="h-64 flex items-center justify-center">
                <Skeleton className="h-44 w-44 rounded-full" />
              </div>
            ) : (
              <StatusDistributionChart
                draftCount={stats?.draftReports || 0}
                calculatedCount={stats?.calculatedReports || 0}
                finalizedCount={stats?.finalizedReports || 0}
              />
            )}
          </CardContent>
        </Card>
      </div>

      {/* Operational Highlights & Security Summary Grid */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {/* Soft-Delete and Data Retention Metrics */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Trash2 className="h-4 w-4 text-slate-500" />
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Soft-Deleted Reports & Retention
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-500">
                Retention Compliant
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500">
              Reports soft-deleted by tenant administrators, pending automated purge schedule.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                <p className="text-[10px] uppercase font-medium text-slate-400">Deleted Today</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  {stats?.deletedReportsToday.toLocaleString()}
                </p>
              </div>
              <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                <p className="text-[10px] uppercase font-medium text-slate-400">This Month</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  {stats?.deletedReportsThisMonth.toLocaleString()}
                </p>
              </div>
              <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                <p className="text-[10px] uppercase font-medium text-slate-400">Total Retained</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  {stats?.totalDeletedReports.toLocaleString()}
                </p>
              </div>
            </div>
            <p className="mt-3 text-[11px] text-slate-500">
              Retained reports are automatically and permanently purged after the configured retention threshold.
            </p>
          </CardContent>
        </Card>

        {/* Break-Glass Security Monitoring */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldAlert className="h-4 w-4 text-indigo-600" />
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Security & Break-Glass Activity
                </CardTitle>
              </div>
              <Badge className="bg-indigo-50 text-indigo-700 border-indigo-200 text-[10px]">
                Audited
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500">
              Emergency break-glass access attempts recorded across all tenant organizations.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between rounded-lg border border-indigo-100 bg-indigo-50/30 p-4">
              <div>
                <p className="text-xs font-medium text-indigo-950">
                  Break-Glass Access (Last 30 Days)
                </p>
                <p className="text-2xl font-extrabold text-indigo-900 mt-0.5">
                  {stats?.breakGlassAccessCount30Days.toLocaleString()}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setIsBreakGlassOpen(true)}
                  className="gap-1.5 text-xs border-rose-200 text-rose-700 hover:bg-rose-50"
                >
                  <ShieldAlert className="h-3 w-3 text-rose-600" />
                  Break-Glass Access
                </Button>
                <Button asChild size="sm" variant="outline" className="gap-1.5 text-xs border-indigo-200 text-indigo-900 hover:bg-indigo-100">
                  <Link to="/super-admin/reports/audit">
                    View Audit Logs
                    <ArrowRight className="h-3 w-3" />
                  </Link>
                </Button>
              </div>
            </div>
            <p className="mt-3 text-[11px] text-slate-500">
              All break-glass occurrences require explicit clinical justification and record client IP addresses.
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Break-Glass Emergency Dialog */}
      <BreakGlassDialog
        open={isBreakGlassOpen}
        onOpenChange={setIsBreakGlassOpen}
      />
    </div>
  );
}
