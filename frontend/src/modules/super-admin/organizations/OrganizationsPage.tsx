import { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  Building2,
  Edit,
  Eye,
  Filter,
  MoreVertical,
  RefreshCw,
  Search,
  ShieldAlert,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { EmptyState } from "@/components/ui/empty-state";

import type {
  OrganizationResponse,
  OrganizationSortField,
  SortDirection,
} from "../types/organizationTypes";
import { useOrganizationsQuery } from "../hooks/useOrganizations";
import { OrganizationStatusBadge } from "../components/OrganizationStatusBadge";
import { CreateOrganizationDialog } from "../components/CreateOrganizationDialog";
import { EditOrganizationDialog } from "../components/EditOrganizationDialog";
import { ChangeOrganizationStatusDialog } from "../components/ChangeOrganizationStatusDialog";

export function OrganizationsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sortBy, setSortBy] = useState<OrganizationSortField>("createdAt");
  const [sortDirection, setSortDirection] = useState<SortDirection>("DESC");

  // Client-side quick filter on current page
  const [clientSearch, setClientSearch] = useState("");

  // Dialog states
  const [selectedOrgForEdit, setSelectedOrgForEdit] =
    useState<OrganizationResponse | null>(null);
  const [selectedOrgForStatus, setSelectedOrgForStatus] =
    useState<OrganizationResponse | null>(null);

  const { data, isLoading, isError, error, refetch, isFetching } =
    useOrganizationsQuery({
      page,
      size,
      sortBy,
      sortDirection,
    });

  const handleSort = (field: OrganizationSortField) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "ASC" ? "DESC" : "ASC"));
    } else {
      setSortBy(field);
      setSortDirection("ASC");
    }
    setPage(0);
  };

  const getSortIcon = (field: OrganizationSortField) => {
    if (sortBy !== field) {
      return <ArrowUpDown className="ml-1.5 h-3.5 w-3.5 opacity-40" />;
    }
    return sortDirection === "ASC" ? (
      <ArrowUp className="ml-1.5 h-3.5 w-3.5 text-slate-900" />
    ) : (
      <ArrowDown className="ml-1.5 h-3.5 w-3.5 text-slate-900" />
    );
  };

  const organizations = useMemo(
    () => data?.content ?? [],
    [data?.content],
  );

  const filteredOrganizations = useMemo(() => {
    if (!clientSearch.trim()) return organizations;
    const term = clientSearch.trim().toLowerCase();
    return organizations.filter(
      (org) =>
        org.name.toLowerCase().includes(term) ||
        org.code.toLowerCase().includes(term) ||
        org.refId.toLowerCase().includes(term),
    );
  }, [organizations, clientSearch]);

  const formatDate = (isoString: string) => {
    try {
      return new Date(isoString).toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Primary Actions */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200/70 pb-5">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-xl bg-teal-50 text-[#0F766E] border border-teal-100 shadow-2xs shrink-0">
            <Building2 className="size-5" />
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900">
                Organizations
              </h1>
              {data && (
                <span className="rounded-full bg-teal-50 border border-teal-200/60 px-2.5 py-0.5 font-mono text-xs font-semibold text-[#0F766E]">
                  {data.totalElements} Total
                </span>
              )}
            </div>
            <p className="mt-0.5 text-xs text-slate-500">
              Manage clinical laboratory tenants, operational status, and licensing across the platform.
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

          <div className="flex-1 sm:flex-none">
            <CreateOrganizationDialog />
          </div>
        </div>
      </div>

      {/* Filter and Control Bar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-xl border border-slate-200/80 bg-white p-3 shadow-xs">
        {/* Quick Filter Search */}
        <div className="relative w-full sm:max-w-xs md:max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
          <Input
            value={clientSearch}
            onChange={(e) => setClientSearch(e.target.value)}
            placeholder="Quick search loaded organizations..."
            className="pl-9 text-xs h-9 border-slate-200 focus-visible:ring-teal-500"
          />
          {clientSearch && (
            <button
              onClick={() => setClientSearch("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600"
            >
              Clear
            </button>
          )}
        </div>

        {/* Sort & Page Size Controls */}
        <div className="flex flex-wrap items-center justify-between sm:justify-end gap-2.5">
          <div className="flex items-center gap-1.5">
            <Filter className="h-3.5 w-3.5 text-slate-400" />
            <span className="text-xs font-medium text-slate-600">Sort:</span>
            <Select
              value={sortBy}
              onValueChange={(val) => {
                setSortBy(val as OrganizationSortField);
                setPage(0);
              }}
            >
              <SelectTrigger className="h-8 w-28 text-xs border-slate-200">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="createdAt">Created Date</SelectItem>
                <SelectItem value="updatedAt">Updated Date</SelectItem>
                <SelectItem value="name">Name</SelectItem>
                <SelectItem value="code">Code</SelectItem>
                <SelectItem value="status">Status</SelectItem>
              </SelectContent>
            </Select>

            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setSortDirection((prev) => (prev === "ASC" ? "DESC" : "ASC"))
              }
              className="h-8 px-2 text-xs border-slate-200"
              title={`Sort direction: ${sortDirection}`}
            >
              {sortDirection === "ASC" ? (
                <span className="flex items-center gap-1">
                  <ArrowUp className="h-3 w-3" /> ASC
                </span>
              ) : (
                <span className="flex items-center gap-1">
                  <ArrowDown className="h-3 w-3" /> DESC
                </span>
              )}
            </Button>
          </div>

          <div className="flex items-center gap-1.5">
            <span className="text-xs font-medium text-slate-600">Rows:</span>
            <Select
              value={String(size)}
              onValueChange={(val) => {
                setSize(Number(val));
                setPage(0);
              }}
            >
              <SelectTrigger className="h-8 w-18 text-xs border-slate-200">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">10</SelectItem>
                <SelectItem value="20">20</SelectItem>
                <SelectItem value="50">50</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      {/* Main Table Container */}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
        {isLoading ? (
          <div className="p-6 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <Skeleton className="h-5 w-40" />
              <Skeleton className="h-5 w-24" />
            </div>
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="flex items-center justify-between py-2">
                <div className="space-y-2">
                  <Skeleton className="h-4 w-48" />
                  <Skeleton className="h-3 w-32" />
                </div>
                <Skeleton className="h-6 w-20 rounded-full" />
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-8 w-8 rounded-md" />
              </div>
            ))}
          </div>
        ) : isError ? (
          <div className="p-8 text-center space-y-3">
            <Alert variant="destructive" className="max-w-md mx-auto">
              <AlertTitle>Failed to load organizations</AlertTitle>
              <AlertDescription className="text-xs">
                {error instanceof Error
                  ? error.message
                  : "An unexpected error occurred while communicating with the server."}
              </AlertDescription>
            </Alert>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void refetch()}
              className="mt-2"
            >
              <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
              Retry Query
            </Button>
          </div>
        ) : organizations.length === 0 ? (
          /* Empty Database State */
          <EmptyState
            title="No Organizations Found"
            description="No laboratory tenants have been registered in SwasthAI yet. Create your first organization to configure diagnostic workflows."
            action={<CreateOrganizationDialog />}
          />
        ) : filteredOrganizations.length === 0 ? (
          /* Empty Filter State */
          <EmptyState
            compact
            title="No Matching Organizations"
            description={`No loaded organizations match the filter "${clientSearch}".`}
            action={{
              label: "Clear Search Filter",
              onClick: () => setClientSearch(""),
              variant: "outline",
            }}
          />
        ) : (
          /* Populated Table / Cards View */
          <>
            {/* Desktop Table View */}
            <div className="hidden md:block overflow-x-auto">
              <Table>
                <TableHeader className="bg-slate-50/80">
                  <TableRow>
                    <TableHead
                      className="cursor-pointer select-none font-semibold text-slate-900 hover:text-slate-700"
                      onClick={() => handleSort("name")}
                    >
                      <span className="flex items-center">
                        Organization Name {getSortIcon("name")}
                      </span>
                    </TableHead>

                    <TableHead
                      className="cursor-pointer select-none font-semibold text-slate-900 hover:text-slate-700"
                      onClick={() => handleSort("code")}
                    >
                      <span className="flex items-center">
                        Code {getSortIcon("code")}
                      </span>
                    </TableHead>

                    <TableHead
                      className="cursor-pointer select-none font-semibold text-slate-900 hover:text-slate-700"
                      onClick={() => handleSort("status")}
                    >
                      <span className="flex items-center">
                        Status {getSortIcon("status")}
                      </span>
                    </TableHead>

                    <TableHead
                      className="cursor-pointer select-none font-semibold text-slate-900 hover:text-slate-700"
                      onClick={() => handleSort("createdAt")}
                    >
                      <span className="flex items-center">
                        Created Date {getSortIcon("createdAt")}
                      </span>
                    </TableHead>

                    <TableHead className="w-16 text-right font-semibold text-slate-900">
                      Actions
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredOrganizations.map((org) => {
                    const isDisabled = org.status === "DISABLED";
                    return (
                      <TableRow
                        key={org.refId}
                        className="hover:bg-slate-50/60 transition-colors"
                      >
                        <TableCell className="font-medium text-slate-900">
                          <div className="flex flex-col">
                            <Link
                              to={`/super-admin/organizations/${org.refId}`}
                              className="font-semibold text-slate-900 hover:text-teal-700 transition-colors"
                            >
                              {org.name}
                            </Link>
                            <span className="font-mono text-[11px] text-slate-400 truncate max-w-xs">
                              {org.refId}
                            </span>
                          </div>
                        </TableCell>

                        <TableCell>
                          <span className="rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 font-mono text-xs font-semibold text-slate-800">
                            {org.code}
                          </span>
                        </TableCell>

                        <TableCell>
                          <OrganizationStatusBadge status={org.status} />
                        </TableCell>

                        <TableCell className="text-xs text-slate-600">
                          {formatDate(org.createdAt)}
                        </TableCell>

                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 text-slate-500 hover:text-slate-900 rounded-md"
                                aria-label={`Actions for ${org.name}`}
                              >
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-48 text-xs">
                              <DropdownMenuLabel className="text-xs text-slate-500 font-mono truncate">
                                {org.code}
                              </DropdownMenuLabel>
                              <DropdownMenuSeparator />

                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(`/super-admin/organizations/${org.refId}`)
                                }
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <Eye className="h-3.5 w-3.5 text-slate-500" />
                                View Details
                              </DropdownMenuItem>

                              <DropdownMenuItem
                                onClick={() => setSelectedOrgForEdit(org)}
                                disabled={isDisabled}
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <Edit className="h-3.5 w-3.5 text-slate-500" />
                                Edit Organization
                              </DropdownMenuItem>

                              <DropdownMenuSeparator />

                              <DropdownMenuItem
                                onClick={() => setSelectedOrgForStatus(org)}
                                className="cursor-pointer flex items-center gap-2 text-amber-700 focus:text-amber-800 focus:bg-amber-50"
                              >
                                <ShieldAlert className="h-3.5 w-3.5 text-amber-600" />
                                Change Status
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>

            {/* Mobile Card View */}
            <div className="md:hidden divide-y divide-slate-100">
              {filteredOrganizations.map((org) => {
                const isDisabled = org.status === "DISABLED";
                return (
                  <div key={org.refId} className="p-4 space-y-3 hover:bg-slate-50/50 transition-colors">
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1">
                        <Link
                          to={`/super-admin/organizations/${org.refId}`}
                          className="font-bold text-sm text-slate-900 hover:text-teal-700 transition-colors block"
                        >
                          {org.name}
                        </Link>
                        <div className="flex items-center gap-2">
                          <span className="rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 font-mono text-[11px] font-semibold text-slate-800">
                            {org.code}
                          </span>
                          <span className="font-mono text-[10px] text-slate-400">
                            {org.refId}
                          </span>
                        </div>
                      </div>

                      <OrganizationStatusBadge status={org.status} />
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs text-slate-500">
                      <span>Created {formatDate(org.createdAt)}</span>

                      <div className="flex items-center gap-1.5">
                        <Button
                          size="sm"
                          onClick={() => navigate(`/super-admin/organizations/${org.refId}`)}
                          className="h-7 px-2.5 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-medium"
                        >
                          View
                        </Button>

                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 w-7 p-0 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-md"
                              aria-label={`Actions for ${org.name}`}
                            >
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-48 text-xs">
                            <DropdownMenuLabel className="text-xs text-slate-500 font-mono truncate">
                              {org.code}
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => navigate(`/super-admin/organizations/${org.refId}`)}
                              className="cursor-pointer flex items-center gap-2"
                            >
                              <Eye className="h-3.5 w-3.5 text-slate-500" />
                              View Details
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => setSelectedOrgForEdit(org)}
                              disabled={isDisabled}
                              className="cursor-pointer flex items-center gap-2"
                            >
                              <Edit className="h-3.5 w-3.5 text-slate-500" />
                              Edit Organization
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => setSelectedOrgForStatus(org)}
                              className="cursor-pointer flex items-center gap-2 text-amber-700 focus:text-amber-800 focus:bg-amber-50"
                            >
                              <ShieldAlert className="h-3.5 w-3.5 text-amber-600" />
                              Change Status
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}

        {/* Pagination Footer */}
        {data && data.totalPages > 0 && (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-t border-slate-200 bg-slate-50/50 px-4 py-3 text-xs text-slate-600">
            <div>
              Showing{" "}
              <span className="font-semibold text-slate-900">
                {data.totalElements === 0 ? 0 : data.page * data.size + 1}
              </span>{" "}
              to{" "}
              <span className="font-semibold text-slate-900">
                {Math.min((data.page + 1) * data.size, data.totalElements)}
              </span>{" "}
              of{" "}
              <span className="font-semibold text-slate-900">
                {data.totalElements}
              </span>{" "}
              organizations
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={data.first || isFetching}
                className="h-8 px-3 text-xs"
              >
                Previous
              </Button>

              <span className="text-xs font-medium text-slate-700 px-1">
                Page {data.page + 1} of {Math.max(1, data.totalPages)}
              </span>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.last || isFetching}
                className="h-8 px-3 text-xs"
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}

      <EditOrganizationDialog
        organization={selectedOrgForEdit}
        open={Boolean(selectedOrgForEdit)}
        onOpenChange={(val) => !val && setSelectedOrgForEdit(null)}
      />

      <ChangeOrganizationStatusDialog
        organization={selectedOrgForStatus}
        open={Boolean(selectedOrgForStatus)}
        onOpenChange={(val) => !val && setSelectedOrgForStatus(null)}
      />
    </div>
  );
}

export default OrganizationsPage;