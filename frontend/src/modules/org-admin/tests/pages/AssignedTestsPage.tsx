import { useState, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  Eye,
  FilePlus,
  FlaskConical,
  Layers,
  RefreshCw,
  Search,
  ShieldAlert,
  X,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import { useTenantTestsQuery } from "../hooks/useTenantTests";
import { TestParametersDrawer } from "../components/TestParametersDrawer";
import { TestTypeBadge } from "@/modules/super-admin/tests/components/TestTypeBadge";
import type { OrganizationTestResponse } from "@/modules/super-admin/tests/types/assignmentTypes";

export function AssignedTestsPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const isLabStaff = location.pathname.startsWith("/lab-staff");
  const reportNewPath = isLabStaff ? "/lab-staff/reports/new" : "/org-admin/reports/new";

  // Filter states
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("ALL");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  // Selected test for drawer preview
  const [selectedTest, setSelectedTest] = useState<OrganizationTestResponse | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  // Fetch tests assigned to current tenant organization
  const {
    data: testsPage,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useTenantTestsQuery({
    size: 100,
    status: statusFilter !== "ALL" ? statusFilter : undefined,
  });

  const allAssignedTests = useMemo(
    () => testsPage?.content ?? [],
    [testsPage?.content]
  );

  // Summary Metrics
  const metrics = useMemo(() => {
    const total = allAssignedTests.length;
    const active = allAssignedTests.filter((t) => t.status === "ACTIVE").length;
    const panelsAndProfiles = allAssignedTests.filter(
      (t) => t.testType === "PANEL" || t.testType === "PROFILE"
    ).length;
    const inactive = total - active;
    return { total, active, panelsAndProfiles, inactive };
  }, [allAssignedTests]);

  // Client-side filtering by search term and test type
  const filteredTests = useMemo(() => {
    return allAssignedTests.filter((item) => {
      // Type filter
      if (typeFilter !== "ALL" && item.testType !== typeFilter) {
        return false;
      }
      // Search term
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase();
        const matchesName = item.testName.toLowerCase().includes(query);
        const matchesCode = item.testCode.toLowerCase().includes(query);
        const matchesType = item.testType.toLowerCase().includes(query);
        if (!matchesName && !matchesCode && !matchesType) {
          return false;
        }
      }
      return true;
    });
  }, [allAssignedTests, searchTerm, typeFilter]);

  const handleOpenDrawer = (test: OrganizationTestResponse) => {
    setSelectedTest(test);
    setIsDrawerOpen(true);
  };

  const handleCreateReport = (testRefId?: string) => {
    if (testRefId) {
      // Navigate to report creation with this test pre-selected in state
      navigate(reportNewPath, {
        state: { preselectedTestRefId: testRefId },
      });
    } else {
      navigate(reportNewPath);
    }
  };

  const clearFilters = () => {
    setSearchTerm("");
    setTypeFilter("ALL");
    setStatusFilter("ALL");
  };

  const hasActiveFilters =
    Boolean(searchTerm.trim()) || typeFilter !== "ALL" || statusFilter !== "ALL";

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Assigned Diagnostic Tests
            </h1>
            <Badge
              variant="outline"
              className="border-teal-200 bg-teal-50 text-teal-700 font-semibold"
            >
              {allAssignedTests.length} Assigned
            </Badge>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Diagnostic test catalog provisioned for your laboratory. Inspect biological reference ranges, units, and order tests for patient reports.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 text-slate-700 border-slate-200 hover:bg-slate-50"
          >
            <RefreshCw
              className={`size-4 text-slate-500 ${isFetching ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>

          <Button
            size="sm"
            className="bg-teal-600 hover:bg-teal-700 text-white flex items-center gap-2 shadow-sm"
            onClick={() => handleCreateReport()}
          >
            <FilePlus className="size-4" />
            New Patient Report
          </Button>
        </div>
      </div>

      {/* KPI Metric Cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {/* Total Assigned */}
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-teal-50 text-teal-600">
              <FlaskConical className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Total Assigned</p>
              <p className="text-2xl font-bold text-slate-900 mt-0.5">
                {isLoading ? <Skeleton className="h-7 w-12" /> : metrics.total}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Active for Reporting */}
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
              <CheckCircle2 className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Active for Reports</p>
              <p className="text-2xl font-bold text-emerald-700 mt-0.5">
                {isLoading ? <Skeleton className="h-7 w-12" /> : metrics.active}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Profiles & Panels */}
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-50 text-indigo-600">
              <Layers className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Panels & Profiles</p>
              <p className="text-2xl font-bold text-indigo-700 mt-0.5">
                {isLoading ? <Skeleton className="h-7 w-12" /> : metrics.panelsAndProfiles}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Inactive / Restricted */}
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-slate-100 text-slate-500">
              <ShieldAlert className="size-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-500">Inactive / On Hold</p>
              <p className="text-2xl font-bold text-slate-700 mt-0.5">
                {isLoading ? <Skeleton className="h-7 w-12" /> : metrics.inactive}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card className="border-slate-200/80 shadow-xs bg-white">
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            {/* Search Input */}
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-slate-400" />
              <Input
                placeholder="Search by test name, code (e.g. CBC, Lipid)..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 pr-9 bg-slate-50/50 border-slate-200 focus:bg-white text-sm"
              />
              {searchTerm && (
                <button
                  type="button"
                  onClick={() => setSearchTerm("")}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  <X className="size-4" />
                </button>
              )}
            </div>

            {/* Select Dropdowns */}
            <div className="flex flex-wrap items-center gap-2">
              {/* Type Filter */}
              <Select value={typeFilter} onValueChange={setTypeFilter}>
                <SelectTrigger className="w-[140px] text-xs h-9 bg-white">
                  <SelectValue placeholder="Test Type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Types</SelectItem>
                  <SelectItem value="INDIVIDUAL">Individual</SelectItem>
                  <SelectItem value="PANEL">Panel</SelectItem>
                  <SelectItem value="PROFILE">Profile</SelectItem>
                </SelectContent>
              </Select>

              {/* Status Filter */}
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-[130px] text-xs h-9 bg-white">
                  <SelectValue placeholder="Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Status</SelectItem>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                </SelectContent>
              </Select>

              {/* Clear button if filters applied */}
              {hasActiveFilters && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={clearFilters}
                  className="h-9 text-xs text-slate-500 hover:text-slate-800"
                >
                  Clear Filters
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Error State */}
      {isError && (
        <Alert variant="destructive">
          <AlertCircle className="size-4" />
          <AlertTitle>Failed to load assigned tests</AlertTitle>
          <AlertDescription className="flex items-center justify-between">
            <span>
              {error instanceof Error ? error.message : "An unexpected network error occurred."}
            </span>
            <Button
              size="sm"
              variant="outline"
              className="bg-white text-slate-900 ml-4"
              onClick={() => void refetch()}
            >
              Try Again
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Loading Skeleton */}
      {isLoading && (
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardContent className="p-6 space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                className="flex items-center justify-between py-3 border-b border-slate-100 last:border-0"
              >
                <div className="flex items-center gap-3">
                  <Skeleton className="size-10 rounded-lg" />
                  <div className="space-y-1.5">
                    <Skeleton className="h-4 w-48" />
                    <Skeleton className="h-3 w-28" />
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Skeleton className="h-6 w-20 rounded-full" />
                  <Skeleton className="h-8 w-28 rounded-md" />
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Empty State: Zero tests assigned to organization */}
      {!isLoading && !isError && allAssignedTests.length === 0 && (
        <Card className="border-slate-200/80 shadow-xs bg-white">
          <CardContent className="p-8">
            <EmptyState
              title="No Diagnostic Tests Assigned"
              description="Your laboratory currently does not have active diagnostic test assignments. Please contact the platform Super Administrator to assign tests to your organization."
            />
          </CardContent>
        </Card>
      )}

      {/* Empty State: Search / filter yields zero results */}
      {!isLoading &&
        !isError &&
        allAssignedTests.length > 0 &&
        filteredTests.length === 0 && (
          <Card className="border-slate-200/80 shadow-xs bg-white">
            <CardContent className="p-8">
              <EmptyState
                title="No matching diagnostic tests"
                description={`No tests match your filter criteria "${searchTerm || typeFilter}". Try adjusting your keywords or clearing the active filters.`}
                action={{
                  label: "Clear Filters",
                  onClick: clearFilters,
                }}
              />
            </CardContent>
          </Card>
        )}

      {/* Tests Table */}
      {!isLoading && !isError && filteredTests.length > 0 && (
        <Card className="border-slate-200/80 shadow-xs bg-white overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-3.5">Diagnostic Test</th>
                  <th className="px-4 py-3.5">Test Type</th>
                  <th className="px-4 py-3.5">Assignment Validity</th>
                  <th className="px-4 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredTests.map((test) => {
                  const isActive = test.status === "ACTIVE";

                  return (
                    <tr
                      key={test.refId}
                      className="hover:bg-teal-50/20 transition-colors group"
                    >
                      {/* Diagnostic Test Info */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="size-10 rounded-xl bg-teal-50 text-teal-700 border border-teal-100/80 flex items-center justify-center shrink-0 shadow-2xs group-hover:bg-teal-100/70 transition-colors">
                            <FlaskConical className="size-5" />
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-semibold text-slate-900 hover:text-teal-700 transition-colors cursor-pointer"
                                onClick={() => handleOpenDrawer(test)}
                              >
                                {test.testName}
                              </span>
                              <span className="font-mono text-xs font-medium px-2 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">
                                {test.testCode}
                              </span>
                            </div>
                            <p className="text-xs text-slate-400 mt-0.5">
                              ID: {test.testRefId}
                            </p>
                          </div>
                        </div>
                      </td>

                      {/* Test Type */}
                      <td className="px-4 py-4 whitespace-nowrap">
                        <TestTypeBadge type={test.testType} />
                      </td>

                      {/* Validity Period */}
                      <td className="px-4 py-4 whitespace-nowrap text-xs text-slate-600">
                        {test.effectiveFrom || test.effectiveUntil ? (
                          <div className="flex items-center gap-1.5 text-slate-600">
                            <Clock className="size-3.5 text-slate-400" />
                            <span>
                              {test.effectiveFrom ? new Date(test.effectiveFrom).toLocaleDateString() : "Immediate"}
                              {" — "}
                              {test.effectiveUntil ? new Date(test.effectiveUntil).toLocaleDateString() : "Ongoing"}
                            </span>
                          </div>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-slate-500 font-medium">
                            <CheckCircle2 className="size-3.5 text-emerald-500" />
                            Active Indefinitely
                          </span>
                        )}
                      </td>

                      {/* Status */}
                      <td className="px-4 py-4 whitespace-nowrap">
                        {isActive ? (
                          <Badge
                            variant="outline"
                            className="border-emerald-200 bg-emerald-50 text-emerald-700 font-medium flex items-center gap-1.5 w-fit"
                          >
                            <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
                            Active
                          </Badge>
                        ) : (
                          <Badge
                            variant="outline"
                            className="border-slate-200 bg-slate-100 text-slate-600 font-medium"
                          >
                            Inactive
                          </Badge>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="px-6 py-4 whitespace-nowrap text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenDrawer(test)}
                            className="h-8 text-xs font-medium text-slate-700 border-slate-200 hover:bg-slate-50 flex items-center gap-1.5"
                          >
                            <Eye className="size-3.5 text-slate-500" />
                            Parameters & Ranges
                          </Button>

                          {isActive && (
                            <Button
                              size="sm"
                              onClick={() => handleCreateReport(test.testRefId)}
                              className="h-8 text-xs bg-teal-600 hover:bg-teal-700 text-white flex items-center gap-1.5 shadow-2xs"
                            >
                              <FilePlus className="size-3.5" />
                              Order Report
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Table Footer info */}
          <div className="p-4 bg-slate-50/60 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
            <span>
              Showing {filteredTests.length} of {allAssignedTests.length} assigned diagnostic tests
            </span>
            <span>
              Click <strong>Parameters & Ranges</strong> to review biological intervals and critical alerts.
            </span>
          </div>
        </Card>
      )}

      {/* Slide-out Parameters & Reference Range Drawer */}
      <TestParametersDrawer
        open={isDrawerOpen}
        onOpenChange={setIsDrawerOpen}
        test={selectedTest}
        onCreateReport={handleCreateReport}
      />
    </div>
  );
}
