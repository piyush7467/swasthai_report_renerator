import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  AlertCircle,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Eye,
  FileEdit,
  FlaskConical,
  Link2,
  MoreVertical,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Sliders,
  Trash2,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";

import {
  useTestsQuery,
  useDeleteTestMutation,
  useUpdateTestMutation,
} from "../hooks/useTests";
import { useActiveCategoriesQuery } from "../hooks/useCategories";
import { TestStatusBadge } from "../components/TestStatusBadge";
import { TestTypeBadge } from "../components/TestTypeBadge";
import { ConfirmDeactivateDialog } from "../components/ConfirmDeactivateDialog";
import type { TestQueryParams, TestResponse } from "../types/testTypes";

export function TestCatalogPage() {
  const navigate = useNavigate();

  const [search, setSearch] = useState("");
  const [categoryRefId, setCategoryRefId] = useState<string>("ALL");
  const [status, setStatus] = useState<string>("ALL");
  const [paramFilter, setParamFilter] = useState<"ALL" | "WITH_PARAMS" | "WITHOUT_PARAMS">("ALL");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [sortField, setSortField] = useState("name");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");

  const [deactivateTarget, setDeactivateTarget] = useState<TestResponse | null>(null);

  const queryParams: TestQueryParams = {
    page,
    size: pageSize,
    sort: sortField,
    direction: sortDirection,
    search: search.trim() ? search.trim() : undefined,
    categoryRefId: categoryRefId !== "ALL" ? categoryRefId : undefined,
    status: status !== "ALL" ? status : undefined,
    hasParameters:
      paramFilter === "WITH_PARAMS"
        ? true
        : paramFilter === "WITHOUT_PARAMS"
          ? false
          : undefined,
  };

  const {
    data: testsData,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useTestsQuery(queryParams);

  const { data: categoriesData } = useActiveCategoriesQuery();
  const deleteMutation = useDeleteTestMutation();
  const updateMutation = useUpdateTestMutation();

  const handleDeactivate = async () => {
    if (!deactivateTarget) return;
    await deleteMutation.mutateAsync(deactivateTarget.refId);
    setDeactivateTarget(null);
  };

  const handleReactivate = async (testItem: TestResponse) => {
    await updateMutation.mutateAsync({
      refId: testItem.refId,
      request: { status: "ACTIVE" },
    });
  };

  const totalPages = testsData?.totalPages ?? 0;
  const totalElements = testsData?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200/70 pb-5">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-xl bg-teal-50 text-[#0F766E] border border-teal-100 shadow-2xs shrink-0">
            <FlaskConical className="size-5" />
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900">
                Diagnostic Test Catalog
              </h1>
              <span className="rounded-full bg-teal-50 border border-teal-200/60 px-2.5 py-0.5 font-mono text-xs font-semibold text-[#0F766E]">
                {totalElements} tests
              </span>
            </div>
            <p className="mt-0.5 text-xs text-slate-500">
              Manage the clinical diagnostic test catalog, specimen requirements, and reference parameters.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching}
            className="text-xs h-9 text-slate-600 hover:text-slate-900 border-slate-200 flex-1 sm:flex-none justify-center cursor-pointer"
          >
            <RefreshCw
              className={`mr-1.5 h-3.5 w-3.5 ${isFetching ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>

          <Button
            size="sm"
            onClick={() => navigate("/super-admin/tests/new")}
            className="text-xs h-9 bg-[#0F766E] hover:bg-[#115E59] text-white flex-1 sm:flex-none justify-center font-semibold shadow-2xs cursor-pointer"
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Add Test
          </Button>
        </div>
      </div>

      {/* Filters Toolbar */}
      <div className="flex flex-col gap-3 rounded-xl border border-slate-200/80 bg-white p-3 sm:flex-row sm:items-center sm:justify-between shadow-xs">
        <div className="flex flex-1 flex-col gap-2.5 sm:flex-row sm:items-center">
          {/* Search */}
          <div className="relative flex-1 sm:max-w-xs">
            <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="Search tests by name or code..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="pl-9 text-xs h-9 border-slate-200 focus-visible:ring-teal-500"
            />
          </div>

          {/* Category Filter */}
          <div className="w-full sm:w-48">
            <Select
              value={categoryRefId}
              onValueChange={(val) => {
                setCategoryRefId(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="text-sm">
                <SelectValue placeholder="All Categories" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Categories</SelectItem>
                {categoriesData?.content.map((cat) => (
                  <SelectItem key={cat.refId} value={cat.refId}>
                    {cat.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Status Filter */}
          <div className="w-full sm:w-36">
            <Select
              value={status}
              onValueChange={(val) => {
                setStatus(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="text-sm">
                <SelectValue placeholder="All Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Status</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="INACTIVE">Inactive</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Parameter Status Filter */}
          <div className="w-full sm:w-48">
            <Select
              value={paramFilter}
              onValueChange={(val: "ALL" | "WITH_PARAMS" | "WITHOUT_PARAMS") => {
                setParamFilter(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="text-sm">
                <SelectValue placeholder="All Parameters" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Parameters</SelectItem>
                <SelectItem value="WITH_PARAMS">Configured (Has Params)</SelectItem>
                <SelectItem value="WITHOUT_PARAMS">Needs Setup (0 Params)</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Sort */}
        <div className="flex items-center gap-2">
          <Select
            value={sortField}
            onValueChange={(val) => {
              setSortField(val);
              setPage(0);
            }}
          >
            <SelectTrigger className="w-36 text-sm">
              <SelectValue placeholder="Sort by" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">Name</SelectItem>
              <SelectItem value="code">Code</SelectItem>
              <SelectItem value="shortName">Short Name</SelectItem>
              <SelectItem value="createdAt">Date Created</SelectItem>
              <SelectItem value="updatedAt">Date Updated</SelectItem>
              <SelectItem value="displayOrder">Display Order</SelectItem>
              <SelectItem value="basePrice">Base Price</SelectItem>
              <SelectItem value="turnaroundTimeHours">Turnaround Time</SelectItem>
            </SelectContent>
          </Select>

          <Button
            variant="outline"
            size="icon"
            onClick={() => {
              setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
              setPage(0);
            }}
            title={`Direction: ${sortDirection.toUpperCase()}`}
          >
            <ArrowUpDown className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Error State */}
      {isError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Error loading test catalog</AlertTitle>
          <AlertDescription className="mt-1 flex items-center justify-between">
            <span>{error?.message || "Failed to load tests."}</span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void refetch()}
              className="ml-4"
            >
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Table & Content */}
      <div className="rounded-lg border border-slate-200 bg-white shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : testsData?.content && testsData.content.length > 0 ? (
          <>
            {/* Desktop Table View */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-700">
                <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                  <tr>
                    <th scope="col" className="px-6 py-3.5">
                      Test Name
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Code
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Category
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Type
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Parameters
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Base Price
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Status
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Updated
                    </th>
                    <th scope="col" className="px-6 py-3.5 text-right">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {testsData.content.map((test) => (
                    <tr
                      key={test.refId}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="px-6 py-4">
                        <Link
                          to={`/super-admin/tests/${test.refId}`}
                          className="font-semibold text-slate-900 hover:text-teal-700 transition-colors"
                        >
                          {test.name}
                        </Link>
                        {test.shortName && (
                          <p className="text-xs text-slate-500 font-mono">
                            {test.shortName}
                          </p>
                        )}
                      </td>
                      <td className="px-6 py-4 font-mono text-xs font-medium text-slate-600">
                        {test.code}
                      </td>
                      <td className="px-6 py-4 text-xs font-medium text-slate-600">
                        {test.categoryName || test.categoryRefId}
                      </td>
                      <td className="px-6 py-4">
                        <TestTypeBadge type={test.testType} />
                      </td>
                      <td className="px-6 py-4">
                        {test.parameterCount && test.parameterCount > 0 ? (
                          <Link
                            to={`/super-admin/tests/${test.refId}?tab=parameters`}
                            className="inline-flex items-center gap-1.5 rounded-full bg-teal-50 border border-teal-200/80 px-2.5 py-1 text-xs font-semibold text-teal-800 hover:bg-teal-100/80 transition-colors shadow-2xs"
                            title="View test parameters"
                          >
                            <Sliders className="size-3 text-teal-600" />
                            {test.parameterCount} {test.parameterCount === 1 ? "param" : "params"}
                          </Link>
                        ) : (
                          <Link
                            to={`/super-admin/tests/${test.refId}/parameters/new`}
                            className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 border border-amber-200/80 px-2.5 py-1 text-xs font-medium text-amber-800 hover:bg-amber-100 transition-colors shadow-2xs"
                            title="No parameters configured yet. Click to add parameter."
                          >
                            <AlertCircle className="size-3 text-amber-600" />
                            0 params (Needs Setup)
                          </Link>
                        )}
                      </td>
                      <td className="px-6 py-4 text-xs font-medium text-slate-700">
                        {test.basePrice != null
                          ? `${test.currency || "INR"} ${Number(test.basePrice).toFixed(2)}`
                          : "—"}
                      </td>
                      <td className="px-6 py-4">
                        <TestStatusBadge status={test.status} />
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-500">
                        {new Date(test.updatedAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {test.parameterCount && test.parameterCount > 0 ? (
                            <Button
                              variant="outline"
                              size="sm"
                              className="h-8 px-2.5 text-xs font-medium text-slate-700 hover:text-teal-700 hover:border-teal-300 cursor-pointer"
                              onClick={() =>
                                navigate(
                                  `/super-admin/tests/${test.refId}?tab=parameters`,
                                )
                              }
                            >
                              <Sliders className="mr-1.5 h-3.5 w-3.5 text-indigo-600" />
                              Parameters ({test.parameterCount})
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              className="h-8 px-2.5 text-xs font-semibold bg-[#0F766E] hover:bg-[#115E59] text-white shadow-2xs cursor-pointer"
                              onClick={() =>
                                navigate(
                                  `/super-admin/tests/${test.refId}/parameters/new`,
                                )
                              }
                            >
                              <Plus className="mr-1.5 h-3.5 w-3.5" />
                              Add Parameters
                            </Button>
                          )}

                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 text-slate-500 hover:text-slate-900 rounded-md"
                              >
                                <MoreVertical className="h-4 w-4" />
                                <span className="sr-only">Actions</span>
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-48 text-xs">
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(`/super-admin/tests/${test.refId}`)
                                }
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <Eye className="h-3.5 w-3.5 text-slate-500" />
                                View Details
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(`/super-admin/tests/${test.refId}/edit`)
                                }
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <FileEdit className="h-3.5 w-3.5 text-slate-500" />
                                Edit Test
                              </DropdownMenuItem>
                              {test.parameterCount && test.parameterCount > 0 ? (
                                <DropdownMenuItem
                                  onClick={() =>
                                    navigate(
                                      `/super-admin/tests/${test.refId}?tab=parameters`,
                                    )
                                  }
                                  className="cursor-pointer flex items-center gap-2"
                                >
                                  <Sliders className="h-3.5 w-3.5 text-indigo-500" />
                                  Manage Parameters ({test.parameterCount})
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem
                                  onClick={() =>
                                    navigate(
                                      `/super-admin/tests/${test.refId}/parameters/new`,
                                    )
                                  }
                                  className="cursor-pointer flex items-center gap-2 text-teal-700 font-semibold"
                                >
                                  <Plus className="h-3.5 w-3.5 text-teal-600" />
                                  Add Parameter
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(
                                    `/super-admin/tests/${test.refId}?tab=assignments`,
                                  )
                                }
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <Link2 className="h-3.5 w-3.5 text-teal-600" />
                                Manage Assignments
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              {test.status === "ACTIVE" ? (
                                <DropdownMenuItem
                                  className="text-amber-600 focus:text-amber-700 cursor-pointer flex items-center gap-2"
                                  onClick={() => setDeactivateTarget(test)}
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                  Deactivate
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem
                                  className="text-emerald-600 focus:text-emerald-700 cursor-pointer flex items-center gap-2"
                                  onClick={() => void handleReactivate(test)}
                                >
                                  <RotateCcw className="h-3.5 w-3.5" />
                                  Reactivate Test
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Mobile Cards View */}
            <div className="md:hidden divide-y divide-slate-100">
              {testsData.content.map((test) => (
                <div key={test.refId} className="p-4 space-y-3 hover:bg-slate-50/50 transition-colors">
                  <div className="flex items-start justify-between gap-2">
                    <div className="space-y-1">
                      <Link
                        to={`/super-admin/tests/${test.refId}`}
                        className="font-bold text-sm text-slate-900 hover:text-teal-700 transition-colors block"
                      >
                        {test.name}
                      </Link>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-slate-600 bg-slate-50 border border-slate-200 px-1.5 py-0.5 rounded">
                          {test.code}
                        </span>
                        {test.shortName && (
                          <span className="text-xs text-slate-400 font-mono">
                            {test.shortName}
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-1">
                      <TestStatusBadge status={test.status} />
                      <TestTypeBadge type={test.testType} />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs pt-1 border-t border-slate-100 text-slate-500">
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Category</span>
                      <span className="text-slate-700 font-medium truncate block">{test.categoryName || test.categoryRefId}</span>
                    </div>
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Base Price</span>
                      <span className="text-slate-900 font-semibold">
                        {test.basePrice != null
                          ? `${test.currency || "INR"} ${Number(test.basePrice).toFixed(2)}`
                          : "—"}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs pt-1.5 border-t border-slate-100">
                    <span className="text-[11px] font-medium text-slate-500">Parameters:</span>
                    {test.parameterCount && test.parameterCount > 0 ? (
                      <Link
                        to={`/super-admin/tests/${test.refId}?tab=parameters`}
                        className="inline-flex items-center gap-1.5 rounded-full bg-teal-50 border border-teal-200/80 px-2.5 py-0.5 text-xs font-semibold text-teal-800"
                      >
                        <Sliders className="size-3 text-teal-600" />
                        {test.parameterCount} {test.parameterCount === 1 ? "param" : "params"}
                      </Link>
                    ) : (
                      <Link
                        to={`/super-admin/tests/${test.refId}/parameters/new`}
                        className="inline-flex items-center gap-1 rounded-full bg-amber-50 border border-amber-200/80 px-2 py-0.5 text-xs font-medium text-amber-800"
                      >
                        <AlertCircle className="size-3 text-amber-600" />
                        0 params (Needs Setup)
                      </Link>
                    )}
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs text-slate-500">
                    <span>Updated {new Date(test.updatedAt).toLocaleDateString()}</span>

                    <div className="flex items-center gap-1.5">
                      {test.parameterCount && test.parameterCount > 0 ? (
                        <Button
                          size="sm"
                          onClick={() => navigate(`/super-admin/tests/${test.refId}?tab=parameters`)}
                          className="h-7 px-2.5 text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 font-medium cursor-pointer"
                        >
                          <Sliders className="mr-1 h-3 w-3 text-indigo-600" />
                          Params ({test.parameterCount})
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          onClick={() => navigate(`/super-admin/tests/${test.refId}/parameters/new`)}
                          className="h-7 px-2.5 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-medium cursor-pointer shadow-2xs"
                        >
                          <Plus className="mr-1 h-3 w-3" />
                          Add Params
                        </Button>
                      )}

                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-7 w-7 p-0 text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-md"
                          >
                            <MoreVertical className="h-4 w-4" />
                            <span className="sr-only">Actions</span>
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48 text-xs">
                          <DropdownMenuItem
                            onClick={() => navigate(`/super-admin/tests/${test.refId}`)}
                            className="cursor-pointer flex items-center gap-2"
                          >
                            <Eye className="h-3.5 w-3.5 text-slate-500" />
                            View Details
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => navigate(`/super-admin/tests/${test.refId}/edit`)}
                            className="cursor-pointer flex items-center gap-2"
                          >
                            <FileEdit className="h-3.5 w-3.5 text-slate-500" />
                            Edit Test
                          </DropdownMenuItem>
                          {test.parameterCount && test.parameterCount > 0 ? (
                            <DropdownMenuItem
                              onClick={() => navigate(`/super-admin/tests/${test.refId}?tab=parameters`)}
                              className="cursor-pointer flex items-center gap-2"
                            >
                              <Sliders className="h-3.5 w-3.5 text-indigo-500" />
                              Manage Parameters ({test.parameterCount})
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem
                              onClick={() => navigate(`/super-admin/tests/${test.refId}/parameters/new`)}
                              className="cursor-pointer flex items-center gap-2 text-teal-700 font-semibold"
                            >
                              <Plus className="h-3.5 w-3.5 text-teal-600" />
                              Add Parameter
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuItem
                            onClick={() => navigate(`/super-admin/tests/${test.refId}?tab=assignments`)}
                            className="cursor-pointer flex items-center gap-2"
                          >
                            <Link2 className="h-3.5 w-3.5 text-teal-600" />
                            Manage Assignments
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          {test.status === "ACTIVE" ? (
                            <DropdownMenuItem
                              className="text-amber-600 focus:text-amber-700 cursor-pointer flex items-center gap-2"
                              onClick={() => setDeactivateTarget(test)}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                              Deactivate
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem
                              className="text-emerald-600 focus:text-emerald-700 cursor-pointer flex items-center gap-2"
                              onClick={() => void handleReactivate(test)}
                            >
                              <RotateCcw className="h-3.5 w-3.5" />
                              Reactivate Test
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        ) : (
          <EmptyState
            title={
              search || categoryRefId !== "ALL" || status !== "ALL" || paramFilter !== "ALL"
                ? "No Diagnostic Tests Found"
                : "No Tests in Catalog"
            }
            description={
              search || categoryRefId !== "ALL" || status !== "ALL" || paramFilter !== "ALL"
                ? "No diagnostic tests match your filter criteria. Try adjusting your search, category, or parameter status filters."
                : "Get started by creating your first diagnostic test specification in the master catalog."
            }
            action={{
              label: "Add Test",
              onClick: () => navigate("/super-admin/tests/new"),
              icon: Plus,
              className: "bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold text-xs h-9 px-4 rounded-lg shadow-xs",
            }}
            secondaryAction={
              search || categoryRefId !== "ALL" || status !== "ALL" || paramFilter !== "ALL"
                ? {
                    label: "Clear Filters",
                    onClick: () => {
                      setSearch("");
                      setCategoryRefId("ALL");
                      setStatus("ALL");
                      setParamFilter("ALL");
                      setPage(0);
                    },
                  }
                : undefined
            }
          />
        )}

        {/* Pagination Bar */}
        {testsData && testsData.totalElements > 0 && (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-t border-slate-200 px-6 py-4 text-sm text-slate-600">
            <div className="flex items-center gap-2">
              <span>Show</span>
              <Select
                value={String(pageSize)}
                onValueChange={(val) => {
                  setPageSize(Number(val));
                  setPage(0);
                }}
              >
                <SelectTrigger className="h-8 w-18 text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="10">10</SelectItem>
                  <SelectItem value="20">20</SelectItem>
                  <SelectItem value="50">50</SelectItem>
                </SelectContent>
              </Select>
              <span>per page</span>
              <span className="ml-2 text-xs text-slate-400">
                (Page {page + 1} of {totalPages || 1})
              </span>
            </div>

            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
                className="h-8 px-2.5"
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || isLoading}
                className="h-8 px-2.5"
              >
                Next
                <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Confirm Deactivate Dialog */}
      <ConfirmDeactivateDialog
        open={Boolean(deactivateTarget)}
        onOpenChange={(open) => {
          if (!open) setDeactivateTarget(null);
        }}
        title={`Deactivate "${deactivateTarget?.name}"?`}
        description="This will set the test status to INACTIVE. Organizations assigned to this test will no longer be able to submit new reports with it."
        confirmLabel="Deactivate Test"
        onConfirm={handleDeactivate}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}

export default TestCatalogPage;
