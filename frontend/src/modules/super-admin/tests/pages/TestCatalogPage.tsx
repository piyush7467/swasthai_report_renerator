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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";

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
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Test Catalog
            </h1>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">
              {totalElements} tests
            </span>
          </div>
          <p className="text-sm text-slate-500">
            Manage the master diagnostic test catalog and specifications.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching}
          >
            <RefreshCw
              className={`mr-2 h-4 w-4 ${isFetching ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>

          <Button
            size="sm"
            onClick={() => navigate("/super-admin/tests/new")}
            className="bg-blue-600 hover:bg-blue-700 text-white"
          >
            <Plus className="mr-2 h-4 w-4" />
            Add Test
          </Button>
        </div>
      </div>

      {/* Filters Toolbar */}
      <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 sm:flex-row sm:items-center sm:justify-between shadow-xs">
        <div className="flex flex-1 flex-col gap-3 sm:flex-row sm:items-center">
          {/* Search */}
          <div className="relative flex-1 sm:max-w-xs">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="Search tests..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="pl-9 text-sm"
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
          <div className="overflow-x-auto">
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
                        className="font-semibold text-slate-900 hover:text-blue-600 transition-colors"
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
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 px-2.5 text-xs font-medium text-slate-700 hover:text-blue-600 hover:border-blue-300"
                          onClick={() =>
                            navigate(
                              `/super-admin/tests/${test.refId}?tab=parameters`,
                            )
                          }
                        >
                          <Sliders className="mr-1.5 h-3.5 w-3.5 text-indigo-600" />
                          Parameters
                        </Button>

                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-slate-500 hover:text-slate-900"
                            >
                              <MoreVertical className="h-4 w-4" />
                              <span className="sr-only">Actions</span>
                            </Button>
                          </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48">
                          <DropdownMenuItem
                            onClick={() =>
                              navigate(`/super-admin/tests/${test.refId}`)
                            }
                          >
                            <Eye className="mr-2 h-4 w-4 text-slate-500" />
                            View Details
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() =>
                              navigate(`/super-admin/tests/${test.refId}/edit`)
                            }
                          >
                            <FileEdit className="mr-2 h-4 w-4 text-slate-500" />
                            Edit Test
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() =>
                              navigate(
                                `/super-admin/tests/${test.refId}?tab=parameters`,
                              )
                            }
                          >
                            <Sliders className="mr-2 h-4 w-4 text-indigo-500" />
                            Manage Parameters
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() =>
                              navigate(
                                `/super-admin/tests/${test.refId}?tab=assignments`,
                              )
                            }
                          >
                            <Link2 className="mr-2 h-4 w-4 text-blue-500" />
                            Manage Assignments
                          </DropdownMenuItem>
                          {test.status === "ACTIVE" ? (
                            <DropdownMenuItem
                              className="text-amber-600 focus:text-amber-700"
                              onClick={() => setDeactivateTarget(test)}
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              Deactivate
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem
                              className="text-emerald-600 focus:text-emerald-700"
                              onClick={() => void handleReactivate(test)}
                            >
                              <RotateCcw className="mr-2 h-4 w-4" />
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
        ) : (
          <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400 mb-3">
              <FlaskConical className="h-6 w-6" />
            </div>
            <h3 className="text-base font-semibold text-slate-900">
              No tests found
            </h3>
            <p className="mt-1 text-sm text-slate-500 max-w-sm">
              {search || categoryRefId !== "ALL" || status !== "ALL"
                ? "No diagnostic tests match your filter criteria. Try adjusting your search."
                : "Get started by creating your first diagnostic test specification."}
            </p>
            <div className="mt-4">
              <Button
                size="sm"
                onClick={() => navigate("/super-admin/tests/new")}
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Test
              </Button>
            </div>
          </div>
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
