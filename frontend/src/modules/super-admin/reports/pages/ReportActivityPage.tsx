import { useState } from "react";
import {
  Activity,
  Building2,
  Calendar,
  ChevronLeft,
  ChevronRight,
  FlaskConical,
  Search,
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
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAdminReportTrend,
  useAdminOrgReportActivity,
  useAdminTestUsage,
} from "../hooks/useAdminAnalytics";
import { useOrganizationsQuery } from "../../hooks/useOrganizations";
import { ReportTrendChart } from "../components/charts/ReportTrendChart";
import { TestUsageChart } from "../components/charts/TestUsageChart";

export default function ReportActivityPage() {
  const [trendDays, setTrendDays] = useState<7 | 30 | 90>(30);
  const [selectedOrgRefId, setSelectedOrgRefId] = useState<string>("");
  const [searchOrg, setSearchOrg] = useState<string>("");
  const [orgPage, setOrgPage] = useState<number>(0);

  // Organizations list for dropdown filter
  const { data: orgsData } = useOrganizationsQuery({
    page: 0,
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const organizations = orgsData?.content ?? [];

  // Trend query
  const {
    data: trendData,
    isLoading: isTrendLoading,
  } = useAdminReportTrend({
    days: trendDays,
    organizationRefId: selectedOrgRefId || undefined,
  });

  // Top test usage
  const {
    data: testUsageData,
    isLoading: isTestUsageLoading,
  } = useAdminTestUsage(10, selectedOrgRefId || undefined);

  // Organization activity table
  const {
    data: orgActivityData,
    isLoading: isOrgActivityLoading,
  } = useAdminOrgReportActivity({
    search: searchOrg || undefined,
    page: orgPage,
    size: 10,
    sort: "totalReports",
    direction: "desc",
  });

  return (
    <div className="space-y-6">
      {/* Header & Filter Controls */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Detailed Report Activity
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Granular analysis of report trends, top test utilization, and tenant organization volume.
          </p>
        </div>

        {/* Global Toolbar Filters */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Tenant Selector Dropdown */}
          <div className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs shadow-2xs">
            <Building2 className="h-3.5 w-3.5 text-slate-400" />
            <select
              value={selectedOrgRefId}
              onChange={(e) => {
                setSelectedOrgRefId(e.target.value);
                setOrgPage(0);
              }}
              className="bg-transparent font-medium text-slate-700 outline-hidden"
            >
              <option value="">All Organizations (Platform)</option>
              {organizations.map((org) => (
                <option key={org.refId} value={org.refId}>
                  {org.name}
                </option>
              ))}
            </select>
          </div>

          {/* Timeframe Toggle Buttons */}
          <div className="flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1">
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
            <button
              type="button"
              onClick={() => setTrendDays(90)}
              className={`rounded px-2.5 py-1 text-xs font-medium transition-colors ${
                trendDays === 90
                  ? "bg-white text-slate-900 shadow-xs"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              90 Days
            </button>
          </div>
        </div>
      </div>

      {/* Visual Analytics Grid: Trend Line & Test Usage */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Trend Area Chart (2 cols) */}
        <Card className="border-slate-200 bg-white lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <div>
              <div className="flex items-center gap-2">
                <Activity className="h-4 w-4 text-blue-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Report Creation Trend
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                {selectedOrgRefId
                  ? `Activity filtered to ${
                      organizations.find((o) => o.refId === selectedOrgRefId)?.name ||
                      selectedOrgRefId
                    }`
                  : `Platform report volume over the past ${trendDays} days`}
              </CardDescription>
            </div>
            <Badge variant="outline" className="text-[11px] text-slate-600">
              <Calendar className="h-3 w-3 mr-1" />
              {trendDays} Day Window
            </Badge>
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

        {/* Top Clinical Test Utilization (1 col) */}
        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FlaskConical className="h-4 w-4 text-purple-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Top Test Usage
                </CardTitle>
              </div>
              <Badge variant="outline" className="text-[10px] text-slate-500">
                Top 10 Tests
              </Badge>
            </div>
            <CardDescription className="text-xs text-slate-500">
              Most requested laboratory tests in generated reports.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            {isTestUsageLoading ? (
              <div className="space-y-3 pt-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-9 w-full" />
                ))}
              </div>
            ) : (
              <TestUsageChart data={testUsageData || []} />
            )}
          </CardContent>
        </Card>
      </div>

      {/* Organization Report Activity Breakdown Table */}
      <Card className="border-slate-200 bg-white">
        <CardHeader className="pb-3">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <Building2 className="h-4 w-4 text-slate-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Tenant Organization Activity Breakdown
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Volume and lifecycle status metrics aggregated per tenant organization.
              </CardDescription>
            </div>

            {/* Search Input */}
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
              <Input
                type="text"
                placeholder="Search organization or code..."
                value={searchOrg}
                onChange={(e) => {
                  setSearchOrg(e.target.value);
                  setOrgPage(0);
                }}
                className="h-8 pl-8 text-xs bg-slate-50 border-slate-200"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-y border-slate-200 bg-slate-50/75 text-slate-600 font-semibold">
                  <th className="py-2.5 px-4">Organization</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Total Reports</th>
                  <th className="py-2.5 px-3 text-right">Today</th>
                  <th className="py-2.5 px-3 text-right">This Week</th>
                  <th className="py-2.5 px-3 text-right">This Month</th>
                  <th className="py-2.5 px-3 text-center">Finalized / Draft</th>
                  <th className="py-2.5 px-4 text-right">Last Generated</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {isOrgActivityLoading ? (
                  Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i}>
                      <td colSpan={8} className="py-3 px-4">
                        <Skeleton className="h-5 w-full" />
                      </td>
                    </tr>
                  ))
                ) : !orgActivityData?.content || orgActivityData.content.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-slate-500">
                      No organizations matching your search criteria.
                    </td>
                  </tr>
                ) : (
                  orgActivityData.content.map((org) => (
                    <tr key={org.organizationRefId} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-900">{org.organizationName}</div>
                        <div className="text-[11px] font-mono text-slate-400">
                          {org.organizationCode} • {org.organizationRefId}
                        </div>
                      </td>

                      <td className="py-3 px-3">
                        <Badge
                          variant="outline"
                          className={
                            org.organizationStatus === "ACTIVE"
                              ? "bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px]"
                              : "bg-slate-100 text-slate-600 border-slate-200 text-[10px]"
                          }
                        >
                          {org.organizationStatus}
                        </Badge>
                      </td>

                      <td className="py-3 px-3 text-right font-bold text-slate-900">
                        {org.totalReports.toLocaleString()}
                      </td>

                      <td className="py-3 px-3 text-right font-medium text-slate-700">
                        {org.reportsToday > 0 ? (
                          <span className="text-blue-600 font-semibold">{org.reportsToday}</span>
                        ) : (
                          "0"
                        )}
                      </td>

                      <td className="py-3 px-3 text-right font-medium text-slate-700">
                        {org.reportsThisWeek}
                      </td>

                      <td className="py-3 px-3 text-right font-medium text-slate-700">
                        {org.reportsThisMonth}
                      </td>

                      <td className="py-3 px-3 text-center">
                        <div className="inline-flex items-center gap-1.5 text-[11px]">
                          <span className="text-emerald-700 font-medium">
                            {org.finalizedReports}
                          </span>
                          <span className="text-slate-300">/</span>
                          <span className="text-amber-700 font-medium">
                            {org.draftReports}
                          </span>
                        </div>
                      </td>

                      <td className="py-3 px-4 text-right text-slate-500 font-mono text-[11px]">
                        {org.lastReportCreatedAt
                          ? new Date(org.lastReportCreatedAt).toLocaleDateString()
                          : "Never"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Bar */}
          {orgActivityData && orgActivityData.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs">
              <span className="text-slate-500">
                Page {orgActivityData.page + 1} of {orgActivityData.totalPages} ({orgActivityData.totalElements} organizations)
              </span>

              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={orgPage === 0}
                  onClick={() => setOrgPage((p) => Math.max(0, p - 1))}
                  className="h-7 px-2 text-xs"
                >
                  <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={orgActivityData.last}
                  onClick={() => setOrgPage((p) => p + 1)}
                  className="h-7 px-2 text-xs"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
