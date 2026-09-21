import { Link } from "react-router-dom";
import {
  Building2,
  Calendar,
  CheckCircle2,
  FileText,
  FlaskConical,
  Plus,
  Shield,
  ShieldAlert,
  TrendingUp,
  User,
  Users,
  ArrowRight,
  Activity,
  Layers,
} from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/core/auth/AuthContext";
import { useAdminOverviewStats } from "./reports/hooks/useAdminAnalytics";
import { useOrganizationsQuery } from "./hooks/useOrganizations";

export function SuperAdminDashboard() {
  const { user } = useAuth();

  const { data: stats, isLoading: isStatsLoading } = useAdminOverviewStats();
  const { data: orgsData, isLoading: isOrgsLoading } = useOrganizationsQuery({
    page: 0,
    size: 1,
  });

  if (!user) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* Welcome & Command Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Super Admin Command Center
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Platform governance, multi-tenant diagnostics, licensing, and security monitoring.
          </p>
        </div>

        {/* Quick Actions Dropdown / Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <Button asChild size="sm" className="bg-blue-600 hover:bg-blue-700 text-white text-xs gap-1.5 shadow-2xs">
            <Link to="/super-admin/organizations">
              <Plus className="h-3.5 w-3.5" />
              New Organization
            </Link>
          </Button>

          <Button asChild size="sm" variant="outline" className="text-xs gap-1.5 border-slate-300">
            <Link to="/super-admin/tests/new">
              <Plus className="h-3.5 w-3.5 text-purple-600" />
              New Test
            </Link>
          </Button>

          <Button asChild size="sm" variant="outline" className="text-xs gap-1.5 border-slate-300">
            <Link to="/super-admin/reports/audit">
              <ShieldAlert className="h-3.5 w-3.5 text-indigo-600" />
              Audit Logs
            </Link>
          </Button>
        </div>
      </div>

      {/* Live Operational Metric KPI Strip */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {/* Total Organizations */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Active Organizations
            </CardTitle>
            <Building2 className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading || isOrgsLoading ? (
              <Skeleton className="h-7 w-16" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.activeOrganizationsCount.toLocaleString()}
                <span className="text-xs font-normal text-slate-400 ml-1">
                  / {orgsData?.totalElements ?? stats?.activeOrganizationsCount} total
                </span>
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Tenant diagnostic centers</p>
          </CardContent>
        </Card>

        {/* Reports Generated Today */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Reports Today
            </CardTitle>
            <Calendar className="h-4 w-4 text-emerald-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-16" />
            ) : (
              <div className="text-2xl font-bold text-emerald-700">
                {stats?.reportsToday.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Generated today (IST)</p>
          </CardContent>
        </Card>

        {/* Total Platform Reports */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Total Reports
            </CardTitle>
            <TrendingUp className="h-4 w-4 text-purple-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-16" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.totalReports.toLocaleString()}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Active patient reports</p>
          </CardContent>
        </Card>

        {/* Finalized Percentage */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Finalized Reports
            </CardTitle>
            <CheckCircle2 className="h-4 w-4 text-teal-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-16" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">
                {stats?.finalizedReports.toLocaleString()}
                <span className="text-xs font-normal text-emerald-600 ml-1.5 font-medium">
                  ({stats?.totalReports ? Math.round((stats.finalizedReports / stats.totalReports) * 100) : 0}%)
                </span>
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Verified & signed off</p>
          </CardContent>
        </Card>

        {/* Break-Glass Security Audits */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-slate-500">
              Security Audits
            </CardTitle>
            <ShieldAlert className="h-4 w-4 text-indigo-600" />
          </CardHeader>
          <CardContent>
            {isStatsLoading ? (
              <Skeleton className="h-7 w-16" />
            ) : (
              <div className="text-2xl font-bold text-indigo-900">
                {stats?.breakGlassAccessCount30Days}
              </div>
            )}
            <p className="text-[11px] text-slate-500 mt-1">Emergency access (30d)</p>
          </CardContent>
        </Card>
      </div>

      {/* Authenticated Identity Strip */}
      <Card className="border-slate-200 bg-slate-50/50">
        <CardContent className="p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-900 text-white font-bold shrink-0">
              <User className="h-4 w-4" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-semibold text-slate-900 text-sm">{user.name}</span>
                <Badge className="bg-slate-900 text-white text-[10px] py-0 h-4">{user.role}</Badge>
              </div>
              <p className="text-slate-500 text-[11px]">{user.email} • Authorization: Platform Super Admin</p>
            </div>
          </div>

          <div className="flex items-center gap-2 text-[11px] text-slate-500 font-mono">
            <Shield className="h-3.5 w-3.5 text-emerald-600" />
            <span>JWT RS256 Asymmetric Session Active</span>
          </div>
        </CardContent>
      </Card>

      {/* Comprehensive Module Grid (All 6 core modules) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* Module 1: Organizations */}
        <Card className="border-slate-200 bg-white flex flex-col justify-between hover:border-blue-300 transition-colors">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-md bg-blue-50 text-blue-600">
                  <Building2 className="h-4 w-4" />
                </div>
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Organizations
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-600">
                Multi-Tenant
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500 pt-1">
              Onboard diagnostic labs, update corporate statuses, customize lab logos, report headers, footers, and signatures.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <Button variant="outline" size="sm" asChild className="w-full text-xs justify-between">
              <Link to="/super-admin/organizations">
                Manage Organizations
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Module 2: Global Users */}
        <Card className="border-slate-200 bg-white flex flex-col justify-between hover:border-blue-300 transition-colors">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-md bg-indigo-50 text-indigo-600">
                  <Users className="h-4 w-4" />
                </div>
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Global Users
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-600">
                RBAC
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500 pt-1">
              Administer user accounts across all tenant organizations, assign roles (ORG_ADMIN, LAB_STAFF), and manage access.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <Button variant="outline" size="sm" asChild className="w-full text-xs justify-between">
              <Link to="/super-admin/users">
                Manage User Directory
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Module 3: Test System */}
        <Card className="border-slate-200 bg-white flex flex-col justify-between hover:border-purple-300 transition-colors">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-md bg-purple-50 text-purple-600">
                  <FlaskConical className="h-4 w-4" />
                </div>
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Test Catalog & Parameters
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-600">
                Clinical
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500 pt-1">
              Master diagnostic catalog, clinical categories, panic limits, auto-calculation formulas (MCV, MCH, MCHC), and tenant assignments.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2 space-y-1.5">
            <Button variant="outline" size="sm" asChild className="w-full text-xs justify-between">
              <Link to="/super-admin/tests">
                Explore Test Catalog
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
            <Button variant="ghost" size="sm" asChild className="w-full text-xs justify-between text-slate-600 hover:text-purple-700">
              <Link to="/super-admin/tests/parameters">
                Parameters Directory
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Module 4: Licensing */}
        <Card className="border-slate-200 bg-white flex flex-col justify-between hover:border-emerald-300 transition-colors">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-md bg-emerald-50 text-emerald-600">
                  <Layers className="h-4 w-4" />
                </div>
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Licensing & Subscriptions
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-600">
                SaaS Billing
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500 pt-1">
              Subscription tiers, annual price definitions, license activations, 2-step renewals, and fair license freeze/pause management.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <Button variant="outline" size="sm" asChild className="w-full text-xs justify-between">
              <Link to="/super-admin/licensing/plans">
                Manage Plans & Licenses
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Module 5: Report Operations & Analytics */}
        <Card className="border-slate-200 bg-white flex flex-col justify-between hover:border-sky-300 transition-colors">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-md bg-sky-50 text-sky-600">
                  <Activity className="h-4 w-4" />
                </div>
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Operational Analytics
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-600">
                Platform
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500 pt-1">
              Time-series report volume trends, top laboratory test rankings, tenant activity breakdowns, and lifecycle metrics.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <Button variant="outline" size="sm" asChild className="w-full text-xs justify-between">
              <Link to="/super-admin/reports">
                View Report Analytics
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Module 6: Security & Audit Trail */}
        <Card className="border-slate-200 bg-white flex flex-col justify-between hover:border-indigo-300 transition-colors">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-md bg-indigo-50 text-indigo-600">
                  <FileText className="h-4 w-4" />
                </div>
                <CardTitle className="text-sm font-semibold text-slate-900">
                  Audit & Security Trail
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-600">
                Immutable
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500 pt-1">
              System-wide audit trail, break-glass review, client IP tracking, justification compliance, and tamper-proof logs.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <Button variant="outline" size="sm" asChild className="w-full text-xs justify-between">
              <Link to="/super-admin/reports/audit">
                Inspect Security Audit
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
