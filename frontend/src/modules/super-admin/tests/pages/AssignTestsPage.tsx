import { useState, useMemo } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  Calendar,
  Check,
  CheckCircle2,
  CheckSquare,
  Clock,
  Filter,
  FlaskConical,
  Loader2,
  RefreshCw,
  Search,
  Sparkles,
  Square,
  X,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import type { ApiErrorResponse } from "@/core/auth/authTypes";

import { useOrganizationsQuery } from "../../hooks/useOrganizations";
import { useTestsQuery } from "../hooks/useTests";
import { useActiveCategoriesQuery } from "../hooks/useCategories";
import { useAssignmentsQuery } from "../hooks/useAssignments";
import { organizationTestApi } from "../api/organizationTestApi";
import { TestTypeBadge } from "../components/TestTypeBadge";
import type { TestType } from "../types/testTypes";

export function AssignTestsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const urlOrgRefId = searchParams.get("organizationRefId") || "";

  // Filter state for test catalog
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState<string>("ALL");

  // Multi-selection state
  const [selectedTestRefIds, setSelectedTestRefIds] = useState<string[]>([]);

  // Optional batch dates
  const [effectiveFrom, setEffectiveFrom] = useState("");
  const [effectiveUntil, setEffectiveUntil] = useState("");
  const [dateError, setDateError] = useState<string | null>(null);

  // Submitting / progress state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [progress, setProgress] = useState({ current: 0, total: 0 });
  const [errorSummary, setErrorSummary] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // 1. Fetch Organizations
  const { data: orgsData, isLoading: orgsLoading } = useOrganizationsQuery({
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const effectiveOrgRefId =
    urlOrgRefId ||
    (orgsData?.content && orgsData.content.length > 0
      ? orgsData.content[0].refId
      : "");

  const selectedOrg = orgsData?.content.find(
    (o) => o.refId === effectiveOrgRefId,
  );

  const handleOrgChange = (newRefId: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set("organizationRefId", newRefId);
      return next;
    });
    // Clear selections when switching organization
    setSelectedTestRefIds([]);
    setErrorSummary(null);
    setSuccessMessage(null);
  };

  // 2. Fetch Active Master Tests (up to 100)
  const {
    data: testsData,
    isLoading: testsLoading,
    refetch: refetchTests,
    isFetching: isFetchingTests,
  } = useTestsQuery({
    size: 100,
    status: "ACTIVE",
    sort: "name",
    direction: "asc",
  });

  // 3. Fetch Categories for filtering
  const { data: categoriesData } = useActiveCategoriesQuery();

  // 4. Fetch Existing Assignments for target organization
  const {
    data: assignmentsData,
    isLoading: assignmentsLoading,
    refetch: refetchAssignments,
  } = useAssignmentsQuery({
    organizationRefId: effectiveOrgRefId,
    size: 100,
    sort: "createdAt",
    direction: "desc",
  });

  // Create a Set of already assigned test refIds for fast lookup
  const assignedTestRefIds = useMemo(() => {
    const set = new Set<string>();
    if (assignmentsData?.content) {
      assignmentsData.content.forEach((item) => {
        set.add(item.testRefId);
      });
    }
    return set;
  }, [assignmentsData]);

  // Filter master tests by search, category, and type
  const filteredTests = useMemo(() => {
    if (!testsData?.content) return [];
    return testsData.content.filter((test) => {
      // Search filter
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase().trim();
        const matchesName = test.name.toLowerCase().includes(query);
        const matchesCode = test.code.toLowerCase().includes(query);
        const matchesShortName = test.shortName?.toLowerCase().includes(query);
        if (!matchesName && !matchesCode && !matchesShortName) return false;
      }

      // Category filter
      if (categoryFilter !== "ALL" && test.categoryRefId !== categoryFilter) {
        return false;
      }

      // Test type filter
      if (typeFilter !== "ALL" && test.testType !== typeFilter) {
        return false;
      }

      return true;
    });
  }, [testsData, searchTerm, categoryFilter, typeFilter]);

  // Available tests that can be selected (not already assigned)
  const availableFilteredTests = useMemo(() => {
    return filteredTests.filter((t) => !assignedTestRefIds.has(t.refId));
  }, [filteredTests, assignedTestRefIds]);

  const allAvailableSelected =
    availableFilteredTests.length > 0 &&
    availableFilteredTests.every((t) => selectedTestRefIds.includes(t.refId));

  // Toggle individual test
  const toggleTest = (testRefId: string) => {
    if (assignedTestRefIds.has(testRefId) || isSubmitting) return;

    setSelectedTestRefIds((prev) =>
      prev.includes(testRefId)
        ? prev.filter((id) => id !== testRefId)
        : [...prev, testRefId],
    );
  };

  // Toggle select all available
  const handleSelectAllAvailable = () => {
    if (allAvailableSelected) {
      // Deselect available in current view
      const availableIds = new Set(availableFilteredTests.map((t) => t.refId));
      setSelectedTestRefIds((prev) => prev.filter((id) => !availableIds.has(id)));
    } else {
      // Select all available in current view
      const availableIds = availableFilteredTests.map((t) => t.refId);
      setSelectedTestRefIds((prev) =>
        Array.from(new Set([...prev, ...availableIds])),
      );
    }
  };

  const handleClearAll = () => {
    setSelectedTestRefIds([]);
  };

  // Date validation
  const validateDates = () => {
    if (effectiveFrom && effectiveUntil) {
      if (new Date(effectiveUntil) < new Date(effectiveFrom)) {
        setDateError("Effective until date cannot be before effective from date");
        return false;
      }
    }
    setDateError(null);
    return true;
  };

  // Handle batch assignment execution
  const handleBatchAssign = async () => {
    if (!effectiveOrgRefId) {
      setErrorSummary("Please select an organization first.");
      return;
    }

    if (selectedTestRefIds.length === 0) {
      setErrorSummary("Please select at least one test to assign.");
      return;
    }

    if (!validateDates()) return;

    setIsSubmitting(true);
    setErrorSummary(null);
    setSuccessMessage(null);
    setProgress({ current: 0, total: selectedTestRefIds.length });

    let successCount = 0;
    const failedMessages: string[] = [];
    const successfullyAssignedIds: string[] = [];

    for (let i = 0; i < selectedTestRefIds.length; i++) {
      const testRefId = selectedTestRefIds[i];
      const testObj = testsData?.content?.find((t) => t.refId === testRefId);
      const testDisplayName = testObj ? testObj.name : testRefId;

      try {
        await organizationTestApi.assignTest({
          organizationRefId: effectiveOrgRefId,
          testRefId,
          effectiveFrom: effectiveFrom || undefined,
          effectiveUntil: effectiveUntil || undefined,
        });
        successCount++;
        successfullyAssignedIds.push(testRefId);
      } catch (err: unknown) {
        if (axios.isAxiosError<ApiErrorResponse>(err)) {
          const apiMsg = err.response?.data?.message || err.message;
          failedMessages.push(`${testDisplayName}: ${apiMsg}`);
        } else {
          failedMessages.push(`${testDisplayName}: Failed to assign`);
        }
      }

      setProgress({ current: i + 1, total: selectedTestRefIds.length });
    }

    // Refresh assignments query cache
    void queryClient.invalidateQueries({ queryKey: ["organization-tests"] });
    void refetchAssignments();

    setIsSubmitting(false);

    if (failedMessages.length === 0) {
      // All succeeded: navigate back with state
      navigate(
        `/super-admin/tests/assignments?organizationRefId=${effectiveOrgRefId}`,
      );
    } else {
      // Partial failure: remove successfully assigned ones from selection
      setSelectedTestRefIds((prev) =>
        prev.filter((id) => !successfullyAssignedIds.includes(id)),
      );
      setErrorSummary(
        `Assigned ${successCount} test(s) successfully, but ${failedMessages.length} failed:\n• ${failedMessages.join("\n• ")}`,
      );
      if (successCount > 0) {
        setSuccessMessage(
          `Successfully assigned ${successCount} test(s) to ${selectedOrg?.name || "the organization"}.`,
        );
      }
    }
  };

  const isLoading = orgsLoading || testsLoading || assignmentsLoading;

  return (
    <div className="space-y-6 pb-24">
      {/* Top Breadcrumb & Page Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <Link
            to={`/super-admin/tests/assignments?organizationRefId=${effectiveOrgRefId}`}
            className="inline-flex items-center text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-2"
          >
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Back to Test Assignments
          </Link>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Assign Tests to Organization
            </h1>
            <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
              Bulk Assignment
            </Badge>
          </div>
          <p className="text-sm text-slate-500">
            Select one or multiple tests from the master catalog to license to the target organization tenant.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              void refetchTests();
              void refetchAssignments();
            }}
            disabled={isFetchingTests || isSubmitting}
          >
            <RefreshCw
              className={`mr-2 h-4 w-4 ${isFetchingTests ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </div>

      {/* Success Banner */}
      {successMessage && (
        <Alert className="border-emerald-200 bg-emerald-50 text-emerald-900">
          <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          <AlertTitle>Success</AlertTitle>
          <AlertDescription className="text-xs">{successMessage}</AlertDescription>
        </Alert>
      )}

      {/* Error Summary Banner */}
      {errorSummary && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Notice</AlertTitle>
          <AlertDescription className="text-xs whitespace-pre-line">
            {errorSummary}
          </AlertDescription>
        </Alert>
      )}

      {/* Target Organization & Policy Configuration */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Organization Context Card */}
        <Card className="md:col-span-2 shadow-xs border-slate-200">
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <Building2 className="h-4 w-4 text-blue-600" />
              <CardTitle className="text-sm font-semibold text-slate-900">
                Target Organization
              </CardTitle>
            </div>
            <CardDescription className="text-xs">
              Choose the tenant organization that will receive the test licenses.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
              <div className="w-full sm:w-80">
                <Select
                  value={effectiveOrgRefId}
                  onValueChange={handleOrgChange}
                  disabled={orgsLoading || isSubmitting}
                >
                  <SelectTrigger className="text-sm font-medium">
                    <SelectValue placeholder="Select target organization..." />
                  </SelectTrigger>
                  <SelectContent>
                    {orgsData?.content.map((org) => (
                      <SelectItem key={org.refId} value={org.refId}>
                        {org.name} ({org.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {selectedOrg && (
                <div className="rounded-md bg-slate-50 border border-slate-200 px-3 py-1.5 text-xs text-slate-600 flex items-center gap-3">
                  <div>
                    <span className="text-slate-400">Ref:</span>{" "}
                    <span className="font-mono font-medium text-slate-800">
                      {selectedOrg.refId}
                    </span>
                  </div>
                  <div className="h-3 w-px bg-slate-200" />
                  <div>
                    <span className="text-slate-400">Current Assigned:</span>{" "}
                    <span className="font-semibold text-blue-700">
                      {assignedTestRefIds.size} tests
                    </span>
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Optional Validity Dates Card */}
        <Card className="shadow-xs border-slate-200">
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-slate-600" />
              <CardTitle className="text-sm font-semibold text-slate-900">
                Batch Validity Period
              </CardTitle>
            </div>
            <CardDescription className="text-xs">
              Optional dates applied to all selected tests.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-2">
              <div>
                <Label htmlFor="effectiveFrom" className="text-[11px] text-slate-500 font-medium">
                  Effective From
                </Label>
                <Input
                  id="effectiveFrom"
                  type="date"
                  value={effectiveFrom}
                  onChange={(e) => {
                    setEffectiveFrom(e.target.value);
                    setDateError(null);
                  }}
                  disabled={isSubmitting}
                  className="h-8 text-xs mt-1"
                />
              </div>
              <div>
                <Label htmlFor="effectiveUntil" className="text-[11px] text-slate-500 font-medium">
                  Effective Until
                </Label>
                <Input
                  id="effectiveUntil"
                  type="date"
                  value={effectiveUntil}
                  onChange={(e) => {
                    setEffectiveUntil(e.target.value);
                    setDateError(null);
                  }}
                  disabled={isSubmitting}
                  className="h-8 text-xs mt-1"
                />
              </div>
            </div>
            {dateError && (
              <p className="text-[11px] text-red-600 font-medium">{dateError}</p>
            )}
            <p className="text-[10px] text-slate-400">
              Leave blank for immediately active and indefinite validity.
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Master Test Catalog Multi-Select Card */}
      <Card className="shadow-xs border-slate-200 overflow-hidden">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <FlaskConical className="h-4 w-4 text-blue-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Select Diagnostic Tests
                </CardTitle>
              </div>
              <CardDescription className="text-xs mt-0.5">
                Click any available test or check boxes to select. Tests already assigned to {selectedOrg?.name || "this organization"} are disabled.
              </CardDescription>
            </div>

            {/* Quick Action Buttons */}
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleSelectAllAvailable}
                disabled={availableFilteredTests.length === 0 || isSubmitting}
                className="h-8 text-xs"
              >
                {allAvailableSelected ? (
                  <>
                    <Square className="mr-1.5 h-3.5 w-3.5 text-slate-500" />
                    Deselect Shown ({availableFilteredTests.length})
                  </>
                ) : (
                  <>
                    <CheckSquare className="mr-1.5 h-3.5 w-3.5 text-blue-600" />
                    Select All Available ({availableFilteredTests.length})
                  </>
                )}
              </Button>

              {selectedTestRefIds.length > 0 && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleClearAll}
                  disabled={isSubmitting}
                  className="h-8 text-xs text-slate-500 hover:text-slate-900"
                >
                  <X className="mr-1 h-3.5 w-3.5" />
                  Clear ({selectedTestRefIds.length})
                </Button>
              )}
            </div>
          </div>

          {/* Filter Toolbar */}
          <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 pt-3">
            {/* Search */}
            <div className="sm:col-span-5 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
              <Input
                placeholder="Search test by name, code, or short name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                disabled={isSubmitting}
                className="pl-9 h-8 text-xs"
              />
              {searchTerm && (
                <button
                  type="button"
                  onClick={() => setSearchTerm("")}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  <X className="h-3 w-3" />
                </button>
              )}
            </div>

            {/* Category Filter */}
            <div className="sm:col-span-4">
              <Select
                value={categoryFilter}
                onValueChange={setCategoryFilter}
                disabled={isSubmitting}
              >
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue placeholder="All Categories" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Diagnostic Categories</SelectItem>
                  {categoriesData?.content.map((cat) => (
                    <SelectItem key={cat.refId} value={cat.refId}>
                      {cat.name} ({cat.code})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Test Type Filter */}
            <div className="sm:col-span-3">
              <Select
                value={typeFilter}
                onValueChange={setTypeFilter}
                disabled={isSubmitting}
              >
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue placeholder="All Test Types" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Types</SelectItem>
                  <SelectItem value="INDIVIDUAL">Individual</SelectItem>
                  <SelectItem value="PANEL">Panel</SelectItem>
                  <SelectItem value="PROFILE">Profile</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>

        {/* Table Content */}
        {isLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : filteredTests.length === 0 ? (
          <div className="py-16 text-center text-sm text-slate-500">
            <Filter className="mx-auto h-8 w-8 text-slate-300 mb-2" />
            No diagnostic tests matched your search or filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-700">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <tr>
                  <th scope="col" className="w-12 px-4 py-3 text-center">
                    <input
                      type="checkbox"
                      checked={allAvailableSelected}
                      onChange={handleSelectAllAvailable}
                      disabled={availableFilteredTests.length === 0 || isSubmitting}
                      className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 cursor-pointer disabled:opacity-40"
                      aria-label="Select all available tests"
                    />
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Diagnostic Test
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Code
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Category
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Type
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Sample / Specimen
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Turnaround
                  </th>
                  <th scope="col" className="px-4 py-3 text-right">
                    Assignment Status
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredTests.map((test) => {
                  const isAssigned = assignedTestRefIds.has(test.refId);
                  const isSelected = selectedTestRefIds.includes(test.refId);

                  return (
                    <tr
                      key={test.refId}
                      onClick={() => toggleTest(test.refId)}
                      className={`transition-colors ${
                        isAssigned
                          ? "bg-slate-50/60 opacity-65 cursor-not-allowed"
                          : isSelected
                            ? "bg-blue-50/70 hover:bg-blue-50 cursor-pointer"
                            : "hover:bg-slate-50 cursor-pointer"
                      }`}
                    >
                      {/* Checkbox */}
                      <td
                        className="px-4 py-3.5 text-center"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <input
                          type="checkbox"
                          checked={isSelected || isAssigned}
                          disabled={isAssigned || isSubmitting}
                          onChange={() => toggleTest(test.refId)}
                          className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 cursor-pointer disabled:cursor-not-allowed"
                        />
                      </td>

                      {/* Test Name */}
                      <td className="px-4 py-3.5">
                        <div className="font-semibold text-slate-900">
                          {test.name}
                        </div>
                        {test.shortName && (
                          <span className="text-xs text-slate-500">
                            {test.shortName}
                          </span>
                        )}
                      </td>

                      {/* Test Code */}
                      <td className="px-4 py-3.5 font-mono text-xs font-medium text-slate-600">
                        {test.code}
                      </td>

                      {/* Category */}
                      <td className="px-4 py-3.5 text-xs text-slate-600 font-medium">
                        {test.categoryName || "General"}
                      </td>

                      {/* Type Badge */}
                      <td className="px-4 py-3.5">
                        <TestTypeBadge type={test.testType as TestType} />
                      </td>

                      {/* Sample Type */}
                      <td className="px-4 py-3.5 text-xs text-slate-600">
                        {test.sampleType}
                        {test.sampleType === "OTHER" && test.customSampleType && (
                          <span className="text-slate-400"> ({test.customSampleType})</span>
                        )}
                      </td>

                      {/* Turnaround */}
                      <td className="px-4 py-3.5 text-xs text-slate-600">
                        {test.turnaroundTimeHours != null ? (
                          <span className="inline-flex items-center gap-1">
                            <Clock className="h-3 w-3 text-slate-400" />
                            {test.turnaroundTimeHours}h
                          </span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>

                      {/* Assignment Status */}
                      <td className="px-4 py-3.5 text-right">
                        {isAssigned ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                            <Check className="h-3 w-3 text-slate-500" />
                            Already Assigned
                          </span>
                        ) : isSelected ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
                            <Sparkles className="h-3 w-3 text-blue-600" />
                            Selected
                          </span>
                        ) : (
                          <span className="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700 border border-emerald-200">
                            Available
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Floating Sticky Bottom Bar for Batch Assignment */}
      <div className="fixed bottom-0 left-0 right-0 z-20 border-t border-slate-200 bg-white/95 backdrop-blur-md px-6 py-3.5 shadow-lg">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-100 text-blue-700 font-bold text-sm">
              {selectedTestRefIds.length}
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">
                {selectedTestRefIds.length === 0
                  ? "No tests selected"
                  : `${selectedTestRefIds.length} test${selectedTestRefIds.length === 1 ? "" : "s"} selected for assignment`}
              </p>
              <p className="text-xs text-slate-500">
                Target:{" "}
                <span className="font-semibold text-slate-800">
                  {selectedOrg?.name || "Select an organization"}
                </span>
                {effectiveFrom || effectiveUntil ? (
                  <span>
                    {" "}
                    • Validity: {effectiveFrom || "Immediate"} →{" "}
                    {effectiveUntil || "Indefinite"}
                  </span>
                ) : null}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {isSubmitting && (
              <div className="flex items-center gap-2 text-xs font-medium text-blue-700 mr-2">
                <Loader2 className="h-4 w-4 animate-spin text-blue-600" />
                Assigning ({progress.current}/{progress.total})...
              </div>
            )}

            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() =>
                navigate(
                  `/super-admin/tests/assignments?organizationRefId=${effectiveOrgRefId}`,
                )
              }
              disabled={isSubmitting}
            >
              Cancel
            </Button>

            <Button
              type="button"
              size="sm"
              onClick={handleBatchAssign}
              disabled={
                selectedTestRefIds.length === 0 ||
                !effectiveOrgRefId ||
                isSubmitting
              }
              className="bg-blue-600 hover:bg-blue-700 text-white min-w-36"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Assigning...
                </>
              ) : (
                <>
                  <CheckSquare className="mr-2 h-4 w-4" />
                  Assign {selectedTestRefIds.length > 0 ? `${selectedTestRefIds.length} Tests` : "Tests"}
                </>
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AssignTestsPage;
