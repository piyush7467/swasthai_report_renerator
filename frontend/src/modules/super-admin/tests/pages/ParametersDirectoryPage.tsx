import { useState, useMemo } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import {
  AlertCircle,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Edit,
  ExternalLink,
  Eye,
  FlaskConical,
  Plus,
  RotateCcw,
  Search,
  Sliders,
  Trash2,
  MoreVertical,
  Layers,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

import { useTestsQuery } from "../hooks/useTests";
import {
  useTestParametersQuery,
  useDeactivateParameterMutation,
  useUpdateParameterMutation,
} from "../hooks/useParameters";

import { TestStatusBadge } from "../components/TestStatusBadge";
import { TestTypeBadge } from "../components/TestTypeBadge";
import { ParameterDataTypeBadge } from "../components/ParameterDataTypeBadge";
import { ConfirmDeactivateDialog } from "../components/ConfirmDeactivateDialog";
import { ParameterDetailsDialog } from "../components/ParameterDetailsDialog";
import type { TestParameterResponse } from "../types/parameterTypes";

export default function ParametersDirectoryPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Test Selection
  const selectedTestRefId = searchParams.get("testRefId") || "";

  const handleSelectTest = (testRefId: string) => {
    if (testRefId) {
      setSearchParams({ testRefId });
    } else {
      setSearchParams({});
    }
  };

  // Test list query
  const {
    data: testsData,
    isLoading: isTestsLoading,
    isError: isTestsError,
    error: testsError,
  } = useTestsQuery({
    page: 0,
    size: 150,
    sort: "name",
    direction: "asc",
  });

  const testsList = testsData?.content ?? [];
  const currentTest = useMemo(
    () => testsList.find((t) => t.refId === selectedTestRefId),
    [testsList, selectedTestRefId],
  );

  // Parameters Query Filters
  const [paramStatus, setParamStatus] = useState<string>("ALL");
  const [paramSort, setParamSort] = useState<"displayOrder" | "name" | "code" | "dataType">("displayOrder");
  const [paramDirection, setParamDirection] = useState<"asc" | "desc">("asc");
  const [paramPage, setParamPage] = useState<number>(0);
  const [searchTerm, setSearchTerm] = useState<string>("");

  // Dialog targets
  const [selectedParamForDetails, setSelectedParamForDetails] = useState<TestParameterResponse | null>(null);
  const [deactivateParamTarget, setDeactivateParamTarget] = useState<TestParameterResponse | null>(null);

  // Parameters Query
  const {
    data: paramsData,
    isLoading: isParamsLoading,
    isError: isParamsError,
    error: paramsError,
  } = useTestParametersQuery(selectedTestRefId || undefined, {
    status: paramStatus === "ALL" ? undefined : paramStatus,
    page: paramPage,
    size: 50,
    sort: paramSort,
    direction: paramDirection,
  });

  // Mutations
  const deactivateParamMutation = useDeactivateParameterMutation();
  const updateParamMutation = useUpdateParameterMutation();

  const handleDeactivateParam = async () => {
    if (!deactivateParamTarget) return;
    try {
      await deactivateParamMutation.mutateAsync(deactivateParamTarget.refId);
      setDeactivateParamTarget(null);
    } catch {
      // Error handled by react-query / global toast
    }
  };

  const handleReactivateParam = async (param: TestParameterResponse) => {
    try {
      await updateParamMutation.mutateAsync({
        parameterRefId: param.refId,
        request: { status: "ACTIVE" },
      });
    } catch {
      // Error handled by react-query
    }
  };

  // Client-side search filtering on loaded parameters
  const filteredParameters = useMemo(() => {
    const items = paramsData?.content ?? [];
    if (!searchTerm.trim()) return items;
    const q = searchTerm.toLowerCase().trim();
    return items.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.code.toLowerCase().includes(q) ||
        (p.unit && p.unit.toLowerCase().includes(q)),
    );
  }, [paramsData?.content, searchTerm]);

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Sliders className="h-6 w-6 text-blue-600" />
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Test Parameters Directory
            </h1>
          </div>
          <p className="mt-1 text-sm text-slate-600">
            Clinical measurement components, reference intervals, biological units, and calculation formulas.
          </p>
        </div>

        {selectedTestRefId && currentTest && (
          <div className="flex items-center gap-2">
            <Button
              asChild
              variant="outline"
              size="sm"
              className="gap-1.5 text-xs text-slate-700"
            >
              <Link to={`/super-admin/tests/${currentTest.refId}`}>
                <ExternalLink className="h-3.5 w-3.5 text-slate-500" />
                View Full Test Details
              </Link>
            </Button>
            <Button
              size="sm"
              onClick={() =>
                navigate(`/super-admin/tests/${currentTest.refId}/parameters/new`)
              }
              className="gap-1.5 text-xs bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-xs"
            >
              <Plus className="h-3.5 w-3.5" />
              Add Parameter
            </Button>
          </div>
        )}
      </div>

      {/* Test Selector Card */}
      <Card className="border-slate-200 bg-white shadow-xs">
        <CardContent className="p-4 sm:p-5">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1 flex-1">
              <label
                htmlFor="test-select"
                className="text-xs font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1.5"
              >
                <FlaskConical className="h-3.5 w-3.5 text-blue-600" />
                Select Master Diagnostic Test
              </label>
              <div className="w-full md:max-w-md">
                <Select
                  value={selectedTestRefId}
                  onValueChange={(val) => {
                    handleSelectTest(val);
                    setParamPage(0);
                    setSearchTerm("");
                  }}
                  disabled={isTestsLoading}
                >
                  <SelectTrigger id="test-select" className="w-full h-9 text-xs">
                    <SelectValue placeholder={isTestsLoading ? "Loading tests catalog..." : "Choose a test to view parameters"} />
                  </SelectTrigger>
                  <SelectContent className="max-h-72">
                    {testsList.map((t) => (
                      <SelectItem key={t.refId} value={t.refId}>
                        {t.name} ({t.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {currentTest && (
              <div className="flex flex-wrap items-center gap-3 pt-2 md:pt-0 border-t md:border-t-0 border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-semibold uppercase tracking-wider text-slate-600 bg-slate-100 px-2 py-1 rounded">
                    {currentTest.code}
                  </span>
                  <TestTypeBadge type={currentTest.testType} />
                  <TestStatusBadge status={currentTest.status} />
                </div>
                <div className="text-xs text-slate-500">
                  Category: <span className="font-medium text-slate-800">{currentTest.categoryName}</span>
                </div>
                {currentTest.sampleType && (
                  <div className="text-xs text-slate-500">
                    Sample: <span className="font-medium text-slate-800">{currentTest.sampleType}</span>
                  </div>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* When no test is selected */}
      {!selectedTestRefId && (
        <Card className="border-dashed border-slate-300 bg-slate-50/50">
          <CardHeader className="text-center py-10">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-blue-50 text-blue-600 mb-3">
              <Layers className="h-6 w-6" />
            </div>
            <CardTitle className="text-lg font-semibold text-slate-900">
              No Test Selected
            </CardTitle>
            <CardDescription className="max-w-md mx-auto text-xs text-slate-500 mt-1">
              Select any diagnostic test from the dropdown above or pick one of the frequently accessed tests below to review and configure its parameters.
            </CardDescription>

            {isTestsLoading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 max-w-3xl mx-auto mt-6">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-20 w-full" />
                ))}
              </div>
            ) : isTestsError ? (
              <Alert variant="destructive" className="max-w-md mx-auto mt-4 text-left">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Failed to load catalog</AlertTitle>
                <AlertDescription className="text-xs">
                  {testsError?.message || "Could not fetch tests list."}
                </AlertDescription>
              </Alert>
            ) : testsList.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 max-w-3xl mx-auto mt-6 text-left">
                {testsList.slice(0, 6).map((test) => (
                  <button
                    key={test.refId}
                    type="button"
                    onClick={() => handleSelectTest(test.refId)}
                    className="p-3.5 rounded-lg border border-slate-200 bg-white hover:border-blue-300 hover:bg-blue-50/40 transition-all text-left group shadow-2xs"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-mono text-[11px] font-semibold text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
                        {test.code}
                      </span>
                      <TestTypeBadge type={test.testType} />
                    </div>
                    <p className="text-xs font-semibold text-slate-900 group-hover:text-blue-700 truncate">
                      {test.name}
                    </p>
                    <p className="text-[11px] text-slate-500 mt-0.5 truncate">
                      {test.categoryName || "General"} &bull; {test.sampleType || "Standard"}
                    </p>
                  </button>
                ))}
              </div>
            ) : null}
          </CardHeader>
        </Card>
      )}

      {/* When a test is selected, show its parameters */}
      {selectedTestRefId && (
        <div className="space-y-4">
          {/* Filter Toolbar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-3 rounded-lg border border-slate-200 shadow-2xs">
            {/* Search Input */}
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
              <Input
                type="search"
                placeholder="Filter by parameter name, code, or unit..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-8 h-8 text-xs bg-slate-50/50"
              />
            </div>

            {/* Controls */}
            <div className="flex flex-wrap items-center gap-2">
              <div className="w-36">
                <Select
                  value={paramStatus}
                  onValueChange={(val) => {
                    setParamStatus(val);
                    setParamPage(0);
                  }}
                >
                  <SelectTrigger className="h-8 text-xs">
                    <SelectValue placeholder="All Status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All Status</SelectItem>
                    <SelectItem value="ACTIVE">Active</SelectItem>
                    <SelectItem value="INACTIVE">Inactive</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="w-40">
                <Select
                  value={paramSort}
                  onValueChange={(val) => {
                    setParamSort(val as "displayOrder" | "name" | "code" | "dataType");
                    setParamPage(0);
                  }}
                >
                  <SelectTrigger className="h-8 text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="displayOrder">Display Order</SelectItem>
                    <SelectItem value="name">Parameter Name</SelectItem>
                    <SelectItem value="code">Parameter Code</SelectItem>
                    <SelectItem value="dataType">Data Type</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  setParamDirection((prev) => (prev === "asc" ? "desc" : "asc"))
                }
                className="h-8 text-xs px-2.5"
                title={`Sort Direction: ${paramDirection.toUpperCase()}`}
              >
                <ArrowUpDown className="h-3.5 w-3.5 mr-1" />
                {paramDirection.toUpperCase()}
              </Button>
            </div>
          </div>

          {/* Parameters Table Card */}
          <Card className="border-slate-200 bg-white shadow-xs overflow-hidden">
            {isParamsLoading ? (
              <div className="p-6 space-y-3">
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : isParamsError ? (
              <div className="p-6">
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>Error loading parameters</AlertTitle>
                  <AlertDescription className="text-xs">
                    {paramsError?.message || "Failed to retrieve diagnostic parameters."}
                  </AlertDescription>
                </Alert>
              </div>
            ) : filteredParameters.length === 0 ? (
              <div className="text-center py-12 px-4">
                <Sliders className="mx-auto h-8 w-8 text-slate-300 mb-2" />
                <h3 className="text-sm font-semibold text-slate-900">
                  No parameters found
                </h3>
                <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                  {searchTerm
                    ? `No parameters matched "${searchTerm}". Try resetting search filter.`
                    : "This test does not have any diagnostic parameters configured yet."}
                </p>
                {currentTest && !searchTerm && (
                  <Button
                    size="sm"
                    onClick={() =>
                      navigate(`/super-admin/tests/${currentTest.refId}/parameters/new`)
                    }
                    className="mt-4 text-xs bg-blue-600 hover:bg-blue-700 text-white"
                  >
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    Add First Parameter
                  </Button>
                )}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-700 border-collapse">
                  <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                    <tr>
                      <th scope="col" className="px-4 py-3 w-16">
                        #
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Parameter Name
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Code
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Data Type
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Input / Calc
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Unit
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Reference Limits
                      </th>
                      <th scope="col" className="px-4 py-3">
                        Status
                      </th>
                      <th scope="col" className="px-4 py-3 text-right">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredParameters.map((param) => (
                      <tr
                        key={param.refId}
                        className="hover:bg-slate-50/75 transition-colors"
                      >
                        <td className="px-4 py-3 font-mono text-slate-500">
                          #{param.displayOrder}
                        </td>
                        <td className="px-4 py-3">
                          <button
                            type="button"
                            onClick={() => setSelectedParamForDetails(param)}
                            className="font-semibold text-slate-900 hover:text-blue-600 transition-colors text-left block"
                          >
                            {param.name}
                          </button>
                          {param.description && (
                            <span className="text-[11px] text-slate-500 truncate block max-w-xs">
                              {param.description}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-mono font-medium text-slate-600">
                          {param.code}
                        </td>
                        <td className="px-4 py-3">
                          <ParameterDataTypeBadge dataType={param.dataType} />
                        </td>
                        <td className="px-4 py-3 text-xs">
                          {param.inputType === "CALCULATED" ? (
                            <Badge
                              variant="outline"
                              className="border-indigo-200 bg-indigo-50 text-indigo-700 font-mono text-[11px]"
                            >
                              CALC: {param.calculationType}
                            </Badge>
                          ) : (
                            <span className="text-slate-500">Manual</span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-mono text-slate-600">
                          {param.unit || "—"}
                        </td>
                        <td className="px-4 py-3 text-slate-700">
                          {param.referenceMin != null || param.referenceMax != null ? (
                            <span>
                              {param.referenceMin ?? 0} &ndash; {param.referenceMax ?? "∞"}
                            </span>
                          ) : (
                            "—"
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <TestStatusBadge status={param.status} />
                        </td>
                        <td className="px-4 py-3 text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 text-slate-500 hover:text-slate-900"
                              >
                                <MoreVertical className="h-3.5 w-3.5" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-44 text-xs">
                              <DropdownMenuItem
                                onClick={() => setSelectedParamForDetails(param)}
                              >
                                <Eye className="mr-2 h-3.5 w-3.5 text-slate-500" />
                                View Details
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(
                                    `/super-admin/tests/${selectedTestRefId}/parameters/${param.refId}/edit`,
                                  )
                                }
                              >
                                <Edit className="mr-2 h-3.5 w-3.5 text-slate-500" />
                                Edit Parameter
                              </DropdownMenuItem>
                              {param.status === "ACTIVE" ? (
                                <DropdownMenuItem
                                  className="text-amber-600 focus:text-amber-700"
                                  onClick={() => setDeactivateParamTarget(param)}
                                >
                                  <Trash2 className="mr-2 h-3.5 w-3.5" />
                                  Deactivate
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem
                                  className="text-emerald-600 focus:text-emerald-700"
                                  onClick={() => void handleReactivateParam(param)}
                                >
                                  <RotateCcw className="mr-2 h-3.5 w-3.5" />
                                  Reactivate
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination */}
            {paramsData && paramsData.totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs">
                <span className="text-slate-500">
                  Page {paramsData.number + 1} of {paramsData.totalPages} ({paramsData.totalElements} parameters)
                </span>

                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={paramPage === 0}
                    onClick={() => setParamPage((p) => Math.max(0, p - 1))}
                    className="h-7 px-2 text-xs"
                  >
                    <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={paramsData.last}
                    onClick={() => setParamPage((p) => p + 1)}
                    className="h-7 px-2 text-xs"
                  >
                    Next
                    <ChevronRight className="h-3.5 w-3.5 ml-1" />
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </div>
      )}

      {/* Modals & Dialogs */}
      <ParameterDetailsDialog
        open={Boolean(selectedParamForDetails)}
        onOpenChange={(open) => !open && setSelectedParamForDetails(null)}
        parameter={selectedParamForDetails}
        testRefId={selectedTestRefId}
      />

      <ConfirmDeactivateDialog
        open={Boolean(deactivateParamTarget)}
        onOpenChange={(open) => !open && setDeactivateParamTarget(null)}
        title="Deactivate Parameter"
        description={`Are you sure you want to deactivate parameter "${deactivateParamTarget?.name}" (${deactivateParamTarget?.code})? Tests referencing this parameter will no longer capture this value.`}
        confirmLabel="Deactivate"
        isLoading={deactivateParamMutation.isPending}
        onConfirm={handleDeactivateParam}
      />
    </div>
  );
}
