import { useState } from "react";
import {
  AlertCircle,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Edit,
  FolderTree,
  MoreVertical,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
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
  useCategoriesQuery,
  useDeleteCategoryMutation,
  useReactivateCategoryMutation,
} from "../hooks/useCategories";
import { TestStatusBadge } from "../components/TestStatusBadge";
import { CategoryDialog } from "../components/CategoryDialog";
import { ConfirmDeactivateDialog } from "../components/ConfirmDeactivateDialog";
import type {
  TestCategoryQueryParams,
  TestCategoryResponse,
  TestCategoryStatus,
} from "../types/categoryTypes";

export function CategoriesPage() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>("ALL");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [sortBy, setSortBy] = useState<
    "name" | "code" | "status" | "createdAt" | "updatedAt"
  >("name");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");

  const [dialogOpen, setDialogOpen] = useState(false);
  const [categoryToEdit, setCategoryToEdit] =
    useState<TestCategoryResponse | null>(null);
  const [deactivateTarget, setDeactivateTarget] =
    useState<TestCategoryResponse | null>(null);

  const queryParams: TestCategoryQueryParams = {
    page,
    size: pageSize,
    sortBy,
    sortDirection,
    search: search.trim() ? search.trim() : undefined,
    status: status !== "ALL" ? (status as TestCategoryStatus) : undefined,
  };

  const {
    data: categoriesData,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useCategoriesQuery(queryParams);

  const deleteMutation = useDeleteCategoryMutation();
  const reactivateMutation = useReactivateCategoryMutation();

  const handleDeactivate = async () => {
    if (!deactivateTarget) return;
    await deleteMutation.mutateAsync(deactivateTarget.refId);
    setDeactivateTarget(null);
  };

  const handleReactivate = async (cat: TestCategoryResponse) => {
    await reactivateMutation.mutateAsync(cat.refId);
  };

  const totalPages = categoriesData?.totalPages ?? 0;
  const totalElements = categoriesData?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Test Categories
            </h1>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">
              {totalElements} categories
            </span>
          </div>
          <p className="text-sm text-slate-500">
            Manage top-level medical and diagnostic test classifications.
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
            onClick={() => {
              setCategoryToEdit(null);
              setDialogOpen(true);
            }}
            className="bg-blue-600 hover:bg-blue-700 text-white"
          >
            <Plus className="mr-2 h-4 w-4" />
            Add Category
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
              placeholder="Search categories..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="pl-9 text-sm"
            />
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
            value={sortBy}
            onValueChange={(val) => {
              setSortBy(
                val as "name" | "code" | "status" | "createdAt" | "updatedAt",
              );
              setPage(0);
            }}
          >
            <SelectTrigger className="w-36 text-sm">
              <SelectValue placeholder="Sort by" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">Name</SelectItem>
              <SelectItem value="code">Code</SelectItem>
              <SelectItem value="status">Status</SelectItem>
              <SelectItem value="createdAt">Date Created</SelectItem>
              <SelectItem value="updatedAt">Date Updated</SelectItem>
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
          <AlertTitle>Error loading categories</AlertTitle>
          <AlertDescription className="mt-1 flex items-center justify-between">
            <span>{error?.message || "Failed to load test categories."}</span>
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

      {/* Table */}
      <div className="rounded-lg border border-slate-200 bg-white shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : categoriesData?.content && categoriesData.content.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-700">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <tr>
                  <th scope="col" className="px-6 py-3.5">
                    Category Name
                  </th>
                  <th scope="col" className="px-6 py-3.5">
                    Code
                  </th>
                  <th scope="col" className="px-6 py-3.5">
                    Description
                  </th>
                  <th scope="col" className="px-6 py-3.5">
                    Status
                  </th>
                  <th scope="col" className="px-6 py-3.5">
                    Created
                  </th>
                  <th scope="col" className="px-6 py-3.5 text-right">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {categoriesData.content.map((cat) => (
                  <tr
                    key={cat.refId}
                    className="hover:bg-slate-50/80 transition-colors"
                  >
                    <td className="px-6 py-4 font-semibold text-slate-900">
                      {cat.name}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs font-medium text-slate-600">
                      {cat.code}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-600 max-w-sm truncate">
                      {cat.description || "—"}
                    </td>
                    <td className="px-6 py-4">
                      <TestStatusBadge status={cat.status} />
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {new Date(cat.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-slate-500 hover:text-slate-900"
                          >
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-40">
                          <DropdownMenuItem
                            onClick={() => {
                              setCategoryToEdit(cat);
                              setDialogOpen(true);
                            }}
                          >
                            <Edit className="mr-2 h-4 w-4 text-slate-500" />
                            Edit Category
                          </DropdownMenuItem>

                          {cat.status === "ACTIVE" ? (
                            <DropdownMenuItem
                              className="text-amber-600 focus:text-amber-700"
                              onClick={() => setDeactivateTarget(cat)}
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              Deactivate
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem
                              className="text-emerald-600 focus:text-emerald-700"
                              onClick={() => void handleReactivate(cat)}
                            >
                              <RotateCcw className="mr-2 h-4 w-4" />
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
        ) : (
          <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400 mb-3">
              <FolderTree className="h-6 w-6" />
            </div>
            <h3 className="text-base font-semibold text-slate-900">
              No categories found
            </h3>
            <p className="mt-1 text-sm text-slate-500 max-w-sm">
              {search || status !== "ALL"
                ? "No categories match your filter criteria."
                : "Create top-level diagnostic categories like Hematology, Biochemistry, or Microbiology."}
            </p>
            <div className="mt-4">
              <Button
                size="sm"
                onClick={() => {
                  setCategoryToEdit(null);
                  setDialogOpen(true);
                }}
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Category
              </Button>
            </div>
          </div>
        )}

        {/* Pagination Bar */}
        {categoriesData && categoriesData.totalElements > 0 && (
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

      {/* Category Modal Dialog */}
      <CategoryDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        categoryToEdit={categoryToEdit}
      />

      {/* Deactivate Confirmation Dialog */}
      <ConfirmDeactivateDialog
        open={Boolean(deactivateTarget)}
        onOpenChange={(open) => {
          if (!open) setDeactivateTarget(null);
        }}
        title={`Deactivate category "${deactivateTarget?.name}"?`}
        description="Setting this category to INACTIVE will prevent new tests from being assigned to it. Existing tests will remain unaffected."
        confirmLabel="Deactivate Category"
        onConfirm={handleDeactivate}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}

export default CategoriesPage;
