import { useState, useMemo } from "react";
import { useParams } from "react-router-dom";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  Clock,
  Edit,
  Eye,
  Filter,
  MoreVertical,
  Search,
  Shield,
  ShieldAlert,
  Users,
  X,
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

import type {
  SortDirection,
  UserResponse,
  UserRole,
  UserSortField,
  UserStatus,
} from "../types/userTypes";
import { useUsersQuery } from "../hooks/useUsers";
import { useOrganizationQuery } from "../hooks/useOrganizations";
import { UserRoleBadge } from "../components/UserRoleBadge";
import { UserStatusBadge } from "../components/UserStatusBadge";
import { CreateUserDialog } from "../components/CreateUserDialog";
import { EditUserDialog } from "../components/EditUserDialog";
import { ChangeUserStatusDialog } from "../components/ChangeUserStatusDialog";
import { UserDetailsDialog } from "../components/UserDetailsDialog";
import { OrganizationHeaderNav } from "../components/OrganizationHeaderNav";

export function OrganizationUsersPage() {
  const params = useParams<{ refId?: string; orgRefId?: string }>();
  const refId = params.refId || params.orgRefId || "";

  const organizationQuery = useOrganizationQuery(refId);
  const organization = organizationQuery.data;

  // Server-side pagination & sorting
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sortBy, setSortBy] = useState<UserSortField>("createdAt");
  const [sortDirection, setSortDirection] = useState<SortDirection>("DESC");

  // Server-side filters (mutually exclusive per backend contract)
  const [roleFilter, setRoleFilter] = useState<UserRole | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<UserStatus | "ALL">("ALL");

  // Client-side quick filter on current loaded page
  const [clientSearch, setClientSearch] = useState("");

  // Dialog states
  const [selectedUserForDetails, setSelectedUserForDetails] = useState<string | null>(null);
  const [selectedUserForEdit, setSelectedUserForEdit] = useState<UserResponse | null>(null);
  const [selectedUserForStatus, setSelectedUserForStatus] = useState<UserResponse | null>(null);

  const queryParams = {
    page: 0,
    size: 100,
    sortBy,
    sortDirection,
    role: roleFilter !== "ALL" ? roleFilter : undefined,
    status: statusFilter !== "ALL" ? statusFilter : undefined,
  };

  const { data, isLoading, isError, error, refetch, isFetching } =
    useUsersQuery(queryParams);

  const handleSort = (field: UserSortField) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "ASC" ? "DESC" : "ASC"));
    } else {
      setSortBy(field);
      setSortDirection("ASC");
    }
    setPage(0);
  };

  const getSortIcon = (field: UserSortField) => {
    if (sortBy !== field) {
      return <ArrowUpDown className="ml-1.5 h-3.5 w-3.5 opacity-40" />;
    }
    return sortDirection === "ASC" ? (
      <ArrowUp className="ml-1.5 h-3.5 w-3.5 text-slate-900" />
    ) : (
      <ArrowDown className="ml-1.5 h-3.5 w-3.5 text-slate-900" />
    );
  };

  const handleRoleFilterChange = (value: string) => {
    if (value === "ALL") {
      setRoleFilter("ALL");
    } else {
      setRoleFilter(value as UserRole);
      setStatusFilter("ALL");
    }
    setPage(0);
  };

  const handleStatusFilterChange = (value: string) => {
    if (value === "ALL") {
      setStatusFilter("ALL");
    } else {
      setStatusFilter(value as UserStatus);
      setRoleFilter("ALL");
    }
    setPage(0);
  };

  const clearAllFilters = () => {
    setRoleFilter("ALL");
    setStatusFilter("ALL");
    setClientSearch("");
    setPage(0);
  };

  const hasActiveFilters =
    roleFilter !== "ALL" || statusFilter !== "ALL" || clientSearch.trim() !== "";

  // Strictly filter for users assigned to THIS organization, excluding platform SUPER_ADMIN
  const allUsers = useMemo(() => data?.content ?? [], [data?.content]);
  const orgUsers = useMemo(() => {
    return allUsers.filter(
      (u) => u.organizationRefId === refId && u.role !== "SUPER_ADMIN",
    );
  }, [allUsers, refId]);

  // Client-side search on this organization's users
  const filteredUsers = useMemo(() => {
    if (!clientSearch.trim()) return orgUsers;
    const term = clientSearch.trim().toLowerCase();
    return orgUsers.filter(
      (u) =>
        u.name.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term) ||
        u.refId.toLowerCase().includes(term),
    );
  }, [orgUsers, clientSearch]);

  const totalUsers = filteredUsers.length;
  const totalPages = Math.ceil(totalUsers / size) || 1;
  const displayedUsers = useMemo(() => {
    const start = page * size;
    return filteredUsers.slice(start, start + size);
  }, [filteredUsers, page, size]);

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "Never";
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
      {/* Context & Navigation Header */}
      <OrganizationHeaderNav
        organization={organization}
        isLoading={organizationQuery.isLoading}
        onRefresh={() => {
          void organizationQuery.refetch();
          void refetch();
        }}
        isRefreshing={organizationQuery.isFetching || isFetching}
      />

      {/* Action Banner & Primary CTA */}
      <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-xs sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-base font-semibold text-slate-900">
            Organization Personnel
          </h2>
          <p className="text-xs text-slate-500">
            Manage laboratory managers (Org Admin) and laboratory staff assigned to this facility.
          </p>
        </div>

        {organization && (
          <CreateUserDialog
            defaultOrganizationRefId={organization.refId}
          />
        )}
      </div>

      {/* Filter and Search Toolbar */}
      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-xs space-y-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          {/* Quick Search */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="Quick filter loaded page by name or email..."
              value={clientSearch}
              onChange={(e) => setClientSearch(e.target.value)}
              className="pl-9 pr-8 text-sm"
            />
            {clientSearch && (
              <button
                type="button"
                onClick={() => setClientSearch("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          {/* Filters & Sorting */}
          <div className="flex flex-wrap items-center gap-2.5">
            {/* Role Filter */}
            <div className="w-36 sm:w-40">
              <Select
                value={roleFilter}
                onValueChange={handleRoleFilterChange}
              >
                <SelectTrigger className="text-xs">
                  <span className="text-slate-500 mr-1">Role:</span>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Roles</SelectItem>
                  <SelectItem value="ORG_ADMIN">Org Admin</SelectItem>
                  <SelectItem value="LAB_STAFF">Lab Staff</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Status Filter */}
            <div className="w-36 sm:w-40">
              <Select
                value={statusFilter}
                onValueChange={handleStatusFilterChange}
              >
                <SelectTrigger className="text-xs">
                  <span className="text-slate-500 mr-1">Status:</span>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Statuses</SelectItem>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                  <SelectItem value="SUSPENDED">Suspended</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Sort Field */}
            <div className="w-36 sm:w-40">
              <Select
                value={sortBy}
                onValueChange={(val: UserSortField) => {
                  setSortBy(val);
                  setPage(0);
                }}
              >
                <SelectTrigger className="text-xs">
                  <span className="text-slate-500 mr-1">Sort:</span>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt">Created Date</SelectItem>
                  <SelectItem value="name">Name</SelectItem>
                  <SelectItem value="email">Email</SelectItem>
                  <SelectItem value="role">Role</SelectItem>
                  <SelectItem value="status">Status</SelectItem>
                  <SelectItem value="lastLoginAt">Last Sign In</SelectItem>
                  <SelectItem value="updatedAt">Updated Date</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Sort Direction Toggle */}
            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                setSortDirection((prev) => (prev === "ASC" ? "DESC" : "ASC"))
              }
              className="px-2.5 text-xs text-slate-700"
              title={`Sort direction: ${sortDirection}`}
            >
              {sortDirection === "ASC" ? (
                <>
                  <ArrowUp className="mr-1 h-3.5 w-3.5 text-slate-900" />
                  ASC
                </>
              ) : (
                <>
                  <ArrowDown className="mr-1 h-3.5 w-3.5 text-slate-900" />
                  DESC
                </>
              )}
            </Button>

            {/* Per Page */}
            <div className="w-24">
              <Select
                value={String(size)}
                onValueChange={(val) => {
                  setSize(Number(val));
                  setPage(0);
                }}
              >
                <SelectTrigger className="text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="10">10 / page</SelectItem>
                  <SelectItem value="20">20 / page</SelectItem>
                  <SelectItem value="50">50 / page</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Clear Filters */}
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={clearAllFilters}
                className="text-xs text-slate-500 hover:text-slate-900"
              >
                <X className="mr-1 h-3.5 w-3.5" />
                Clear
              </Button>
            )}
          </div>
        </div>

        {(roleFilter !== "ALL" || statusFilter !== "ALL") && (
          <p className="text-[11px] text-slate-500 italic">
            * Server filters apply by Role or Status independently per backend architecture.
          </p>
        )}
      </div>

      {/* Error State */}
      {isError && (
        <Alert variant="destructive" className="border-red-200 bg-red-50 text-red-800">
          <ShieldAlert className="h-4 w-4 text-red-600" />
          <AlertTitle className="text-sm font-semibold">Error Loading Users</AlertTitle>
          <AlertDescription className="text-xs text-red-700 mt-1 flex items-center justify-between">
            <span>
              {(error as Error)?.message ||
                "Failed to retrieve users from the backend API."}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void refetch()}
              className="ml-4 h-7 border-red-300 bg-white text-xs text-red-800 hover:bg-red-50"
            >
              Try Again
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Users Table */}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50/80">
              <TableRow className="border-b border-slate-200 hover:bg-transparent">
                <TableHead
                  onClick={() => handleSort("name")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700"
                >
                  <div className="flex items-center">
                    User / Name
                    {getSortIcon("name")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("role")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700"
                >
                  <div className="flex items-center">
                    Role
                    {getSortIcon("role")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("status")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700"
                >
                  <div className="flex items-center">
                    Status
                    {getSortIcon("status")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("lastLoginAt")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700"
                >
                  <div className="flex items-center">
                    Last Sign In
                    {getSortIcon("lastLoginAt")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("createdAt")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700"
                >
                  <div className="flex items-center">
                    Created Date
                    {getSortIcon("createdAt")}
                  </div>
                </TableHead>

                <TableHead className="w-12 text-right text-xs font-semibold text-slate-700">
                  <span className="sr-only">Actions</span>
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i} className="border-b border-slate-100">
                    <TableCell className="py-4">
                      <div className="space-y-1.5">
                        <Skeleton className="h-4 w-36" />
                        <Skeleton className="h-3 w-48" />
                      </div>
                    </TableCell>
                    <TableCell>
                      <Skeleton className="h-5 w-24 rounded-full" />
                    </TableCell>
                    <TableCell>
                      <Skeleton className="h-5 w-20 rounded-full" />
                    </TableCell>
                    <TableCell>
                      <Skeleton className="h-4 w-24" />
                    </TableCell>
                    <TableCell>
                      <Skeleton className="h-4 w-24" />
                    </TableCell>
                    <TableCell>
                      <Skeleton className="h-8 w-8 rounded-md" />
                    </TableCell>
                  </TableRow>
                ))
              ) : displayedUsers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="py-12 text-center">
                    {hasActiveFilters ? (
                      <div className="flex flex-col items-center justify-center text-slate-500">
                        <Filter className="h-10 w-10 text-slate-300 stroke-[1.5]" />
                        <p className="mt-2 text-sm font-medium text-slate-700">
                          No organization personnel match your criteria
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          Try adjusting search filters.
                        </p>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={clearAllFilters}
                          className="mt-3 text-xs"
                        >
                          Clear Filters
                        </Button>
                      </div>
                    ) : (
                      <div className="flex flex-col items-center justify-center text-slate-500">
                        <Users className="h-10 w-10 text-slate-300 stroke-[1.5]" />
                        <p className="mt-2 text-sm font-medium text-slate-700">
                          No personnel registered for this organization yet
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          Register your first Organization Admin or Lab Staff member for this laboratory.
                        </p>
                        {organization && (
                          <div className="mt-3">
                            <CreateUserDialog
                              defaultOrganizationRefId={organization.refId}
                            />
                          </div>
                        )}
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              ) : (
                displayedUsers.map((user) => {
                  return (
                    <TableRow
                      key={user.refId}
                      className="border-b border-slate-100 hover:bg-slate-50/70 transition-colors"
                    >
                      {/* Name & Email */}
                      <TableCell className="py-3.5">
                        <div>
                          <button
                            type="button"
                            onClick={() => setSelectedUserForDetails(user.refId)}
                            className="text-left font-semibold text-slate-900 hover:text-blue-600 hover:underline focus:outline-none"
                          >
                            {user.name}
                          </button>
                          <p className="text-xs text-slate-500 truncate max-w-xs">
                            {user.email}
                          </p>
                          <p className="text-[10px] font-mono text-slate-400 mt-0.5">
                            {user.refId}
                          </p>
                        </div>
                      </TableCell>

                      {/* Role */}
                      <TableCell>
                        <UserRoleBadge role={user.role} />
                      </TableCell>

                      {/* Status */}
                      <TableCell>
                        <UserStatusBadge status={user.status} />
                      </TableCell>

                      {/* Last Login */}
                      <TableCell>
                        <div className="flex items-center gap-1.5 text-xs text-slate-600">
                          <Clock className="h-3 w-3 text-slate-400 shrink-0" />
                          <span>{formatDate(user.lastLoginAt)}</span>
                        </div>
                      </TableCell>

                      {/* Created Date */}
                      <TableCell className="text-xs text-slate-600">
                        {formatDate(user.createdAt)}
                      </TableCell>

                      {/* Action Menu */}
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900"
                            >
                              <MoreVertical className="h-4 w-4" />
                              <span className="sr-only">Open menu</span>
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-48">
                            <DropdownMenuLabel className="text-xs font-semibold text-slate-500">
                              User Actions
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                              onClick={() => setSelectedUserForDetails(user.refId)}
                              className="cursor-pointer text-xs"
                            >
                              <Eye className="mr-2 h-3.5 w-3.5 text-slate-500" />
                              View Details
                            </DropdownMenuItem>

                            <DropdownMenuItem
                              onClick={() => setSelectedUserForEdit(user)}
                              className="cursor-pointer text-xs"
                            >
                              <Edit className="mr-2 h-3.5 w-3.5 text-slate-500" />
                              Edit User
                            </DropdownMenuItem>

                            <DropdownMenuItem
                              onClick={() => setSelectedUserForStatus(user)}
                              className="cursor-pointer text-xs"
                            >
                              <Shield className="mr-2 h-3.5 w-3.5 text-slate-500" />
                              Change Status
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>

        {/* Organization Personnel Pagination Bar */}
        {totalUsers > 0 && (
          <div className="flex flex-col gap-3 border-t border-slate-200 px-4 py-3 sm:flex-row sm:items-center sm:justify-between bg-white">
            <div className="text-xs text-slate-600">
              Showing{" "}
              <span className="font-semibold text-slate-900">
                {page * size + 1}
              </span>{" "}
              to{" "}
              <span className="font-semibold text-slate-900">
                {Math.min((page + 1) * size, totalUsers)}
              </span>{" "}
              of{" "}
              <span className="font-semibold text-slate-900">
                {totalUsers}
              </span>{" "}
              organization personnel
            </div>

            {totalPages > 1 && (
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  disabled={page === 0 || isLoading}
                  className="h-8 text-xs"
                >
                  Previous
                </Button>

                <div className="text-xs text-slate-600 px-2 font-medium">
                  Page {page + 1} of {totalPages}
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setPage((prev) =>
                      prev + 1 < totalPages ? prev + 1 : prev,
                    )
                  }
                  disabled={page + 1 >= totalPages || isLoading}
                  className="h-8 text-xs"
                >
                  Next
                </Button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* User Details Dialog */}
      <UserDetailsDialog
        refId={selectedUserForDetails}
        open={Boolean(selectedUserForDetails)}
        onOpenChange={(open) => {
          if (!open) setSelectedUserForDetails(null);
        }}
        onEdit={(userId) => {
          const u = orgUsers.find((item) => item.refId === userId);
          if (u) setSelectedUserForEdit(u);
        }}
        onChangeStatus={(userId) => {
          const u = orgUsers.find((item) => item.refId === userId);
          if (u) setSelectedUserForStatus(u);
        }}
      />

      {/* Edit User Dialog */}
      <EditUserDialog
        user={selectedUserForEdit}
        open={Boolean(selectedUserForEdit)}
        onOpenChange={(open) => {
          if (!open) setSelectedUserForEdit(null);
        }}
      />

      {/* Change Status Dialog */}
      <ChangeUserStatusDialog
        user={selectedUserForStatus}
        open={Boolean(selectedUserForStatus)}
        onOpenChange={(open) => {
          if (!open) setSelectedUserForStatus(null);
        }}
      />
    </div>
  );
}

export default OrganizationUsersPage;
