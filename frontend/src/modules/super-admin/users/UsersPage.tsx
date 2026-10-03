import { useState, useMemo } from "react";
import { Link } from "react-router-dom";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  Clock,
  Edit,
  Eye,
  MoreVertical,
  RefreshCw,
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
import { EmptyState } from "@/components/ui/empty-state";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

import type {
  SortDirection,
  UserResponse,
  UserRole,
  UserSortField,
  UserStatus,
} from "../types/userTypes";
import { useUsersQuery } from "../hooks/useUsers";
import { UserRoleBadge } from "../components/UserRoleBadge";
import { UserStatusBadge } from "../components/UserStatusBadge";
import { CreateUserDialog } from "../components/CreateUserDialog";
import { EditUserDialog } from "../components/EditUserDialog";
import { ChangeUserStatusDialog } from "../components/ChangeUserStatusDialog";
import { UserDetailsDialog } from "../components/UserDetailsDialog";

export function UsersPage() {
  // Server-side pagination & sorting
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sortBy, setSortBy] = useState<UserSortField>("createdAt");
  const [sortDirection, setSortDirection] = useState<SortDirection>("DESC");

  // Server-side filters (mutually exclusive as backend explicitly rejects both together)
  const [roleFilter, setRoleFilter] = useState<UserRole | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<UserStatus | "ALL">("ALL");

  // Client-side quick filter on current loaded page
  const [clientSearch, setClientSearch] = useState("");

  // Dialog states
  const [selectedUserForDetails, setSelectedUserForDetails] = useState<string | null>(null);
  const [selectedUserForEdit, setSelectedUserForEdit] = useState<UserResponse | null>(null);
  const [selectedUserForStatus, setSelectedUserForStatus] = useState<UserResponse | null>(null);

  const queryParams = {
    page,
    size,
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

  // Handle mutual exclusivity of backend filters
  const handleRoleFilterChange = (value: string) => {
    if (value === "ALL") {
      setRoleFilter("ALL");
    } else {
      setRoleFilter(value as UserRole);
      setStatusFilter("ALL"); // Reset status to satisfy backend single-filter contract
    }
    setPage(0);
  };

  const handleStatusFilterChange = (value: string) => {
    if (value === "ALL") {
      setStatusFilter("ALL");
    } else {
      setStatusFilter(value as UserStatus);
      setRoleFilter("ALL"); // Reset role to satisfy backend single-filter contract
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

  const users = useMemo(() => data?.content ?? [], [data?.content]);

  // Client-side quick filter on current loaded page
  const filteredUsers = useMemo(() => {
    if (!clientSearch.trim()) return users;
    const term = clientSearch.trim().toLowerCase();
    return users.filter(
      (u) =>
        u.name.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term) ||
        u.refId.toLowerCase().includes(term) ||
        (u.organizationRefId && u.organizationRefId.toLowerCase().includes(term)),
    );
  }, [users, clientSearch]);

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
      {/* Header & Primary Actions */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200/70 pb-5">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-xl bg-teal-50 text-[#0F766E] border border-teal-100 shadow-2xs shrink-0">
            <Users className="size-5" />
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900">
                Global User Directory
              </h1>
              {data && (
                <span className="rounded-full bg-teal-50 border border-teal-200/60 px-2.5 py-0.5 font-mono text-xs font-semibold text-[#0F766E]">
                  {data.totalElements} Total
                </span>
              )}
            </div>
            <p className="mt-0.5 text-xs text-slate-500">
              Search, inspect, and audit user accounts across all clinical laboratory organizations and platform operators.
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
            <CreateUserDialog />
          </div>
        </div>
      </div>

      {/* Filter and Search Toolbar */}
      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          {/* Quick Search */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="Quick filter loaded page by name, email, or refId..."
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
                  <SelectItem value="SUPER_ADMIN">Super Admin</SelectItem>
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
                  <SelectItem value="lastLoginAt">Last Login</SelectItem>
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

            {/* Per Page Selection */}
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

        {/* Backend Invariant Guidance Note */}
        {(roleFilter !== "ALL" || statusFilter !== "ALL") && (
          <p className="text-[11px] text-slate-500 italic">
            * Server filters apply by Role or Status independently to ensure deterministic backend pagination.
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
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        {/* Desktop Table View */}
        <div className="hidden md:block overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50/80">
              <TableRow className="border-b border-slate-200 hover:bg-transparent">
                <TableHead
                  onClick={() => handleSort("name")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700 hover:text-slate-900"
                >
                  <div className="flex items-center">
                    User / Name
                    {getSortIcon("name")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("role")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700 hover:text-slate-900"
                >
                  <div className="flex items-center">
                    Role
                    {getSortIcon("role")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("status")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700 hover:text-slate-900"
                >
                  <div className="flex items-center">
                    Status
                    {getSortIcon("status")}
                  </div>
                </TableHead>

                <TableHead className="text-xs font-semibold text-slate-700">
                  Organization Ref
                </TableHead>

                <TableHead
                  onClick={() => handleSort("lastLoginAt")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700 hover:text-slate-900"
                >
                  <div className="flex items-center">
                    Last Sign In
                    {getSortIcon("lastLoginAt")}
                  </div>
                </TableHead>

                <TableHead
                  onClick={() => handleSort("createdAt")}
                  className="cursor-pointer select-none text-xs font-semibold text-slate-700 hover:text-slate-900"
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
                      <Skeleton className="h-4 w-28" />
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
              ) : filteredUsers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="py-6 text-center">
                    {hasActiveFilters ? (
                      <EmptyState
                        compact
                        title="No Matching Users Found"
                        description="No users matched your current role, status, or search filters. Try adjusting your search query."
                        action={{
                          label: "Clear Filters",
                          onClick: clearAllFilters,
                          variant: "outline",
                        }}
                      />
                    ) : (
                      <EmptyState
                        title="No Personnel Found in Directory"
                        description="Get started by registering a new organization administrator or lab staff member in the system."
                        action={<CreateUserDialog />}
                      />
                    )}
                  </TableCell>
                </TableRow>
              ) : (
                filteredUsers.map((user) => {
                  const isProtectedSuperAdmin = user.role === "SUPER_ADMIN";

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
                            className="text-left font-semibold text-slate-900 hover:text-teal-700 transition-colors focus:outline-none"
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

                      {/* Organization Reference */}
                      <TableCell>
                        {user.organizationRefId ? (
                          <Link
                            to={`/super-admin/organizations/${encodeURIComponent(
                              user.organizationRefId,
                            )}`}
                            className="font-mono text-xs text-teal-700 hover:text-teal-900 hover:underline bg-teal-50 px-2 py-0.5 rounded border border-teal-200/60 inline-flex items-center transition-colors"
                            title={`Navigate to organization ${user.organizationRefId}`}
                          >
                            {user.organizationRefId}
                          </Link>
                        ) : (
                          <span className="text-xs text-slate-400 italic">
                            Platform Wide
                          </span>
                        )}
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
                              className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900 rounded-md"
                            >
                              <MoreVertical className="h-4 w-4" />
                              <span className="sr-only">Open menu</span>
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-48 text-xs">
                            <DropdownMenuLabel className="text-xs font-semibold text-slate-500">
                              User Actions
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                              onClick={() => setSelectedUserForDetails(user.refId)}
                              className="cursor-pointer flex items-center gap-2"
                            >
                              <Eye className="h-3.5 w-3.5 text-slate-500" />
                              View Details
                            </DropdownMenuItem>

                            {isProtectedSuperAdmin ? (
                              <DropdownMenuItem
                                disabled
                                className="text-slate-400 cursor-not-allowed flex items-center gap-2"
                                title="SUPER_ADMIN account cannot be modified"
                              >
                                <Edit className="h-3.5 w-3.5 text-slate-400" />
                                Edit Protected User
                              </DropdownMenuItem>
                            ) : (
                              <DropdownMenuItem
                                onClick={() => setSelectedUserForEdit(user)}
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <Edit className="h-3.5 w-3.5 text-slate-500" />
                                Edit User
                              </DropdownMenuItem>
                            )}

                            {isProtectedSuperAdmin ? (
                              <DropdownMenuItem
                                disabled
                                className="text-slate-400 cursor-not-allowed flex items-center gap-2"
                                title="SUPER_ADMIN status cannot be modified"
                              >
                                <Shield className="h-3.5 w-3.5 text-slate-400" />
                                Status Locked
                              </DropdownMenuItem>
                            ) : (
                              <DropdownMenuItem
                                onClick={() => setSelectedUserForStatus(user)}
                                className="cursor-pointer flex items-center gap-2 text-amber-700 focus:text-amber-800 focus:bg-amber-50"
                              >
                                <Shield className="h-3.5 w-3.5 text-amber-600" />
                                Change Status
                              </DropdownMenuItem>
                            )}
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

        {/* Mobile Cards View */}
        <div className="md:hidden divide-y divide-slate-100">
          {isLoading ? (
            Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="p-4 space-y-3">
                <Skeleton className="h-10 w-full rounded-md" />
                <Skeleton className="h-6 w-3/4 rounded-md" />
              </div>
            ))
          ) : filteredUsers.length === 0 ? (
            <div className="p-6 text-center">
              <EmptyState
                compact
                title="No Matching Users Found"
                description="No users matched your current role, status, or search filters."
                action={{
                  label: "Clear Filters",
                  onClick: clearAllFilters,
                  variant: "outline",
                }}
              />
            </div>
          ) : (
            filteredUsers.map((user) => {
              const isProtectedSuperAdmin = user.role === "SUPER_ADMIN";
              return (
                <div key={user.refId} className="p-4 space-y-3 hover:bg-slate-50/50 transition-colors">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-full bg-teal-50 border border-teal-100 text-[#0F766E] flex items-center justify-center font-bold text-xs shrink-0">
                        {user.name.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <button
                          type="button"
                          onClick={() => setSelectedUserForDetails(user.refId)}
                          className="text-left font-bold text-sm text-slate-900 hover:text-teal-700 transition-colors block"
                        >
                          {user.name}
                        </button>
                        <p className="text-xs text-slate-500 truncate max-w-[190px]">{user.email}</p>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-1">
                      <UserRoleBadge role={user.role} />
                      <UserStatusBadge status={user.status} />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs pt-1 border-t border-slate-100 text-slate-500">
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Organization</span>
                      {user.organizationRefId ? (
                        <Link
                          to={`/super-admin/organizations/${encodeURIComponent(user.organizationRefId)}`}
                          className="font-mono text-[11px] text-teal-700 hover:underline truncate block"
                        >
                          {user.organizationRefId}
                        </Link>
                      ) : (
                        <span className="text-slate-400 italic">Platform Wide</span>
                      )}
                    </div>
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Last Active</span>
                      <span>{formatDate(user.lastLoginAt)}</span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs text-slate-500">
                    <span className="font-mono text-[10px] text-slate-400">{user.refId}</span>

                    <div className="flex items-center gap-1.5">
                      <Button
                        size="sm"
                        onClick={() => setSelectedUserForDetails(user.refId)}
                        className="h-7 px-2.5 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-medium"
                      >
                        Details
                      </Button>

                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-7 w-7 p-0 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-md"
                            aria-label={`Actions for ${user.name}`}
                          >
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48 text-xs">
                          <DropdownMenuLabel className="text-xs font-semibold text-slate-500">
                            User Actions
                          </DropdownMenuLabel>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            onClick={() => setSelectedUserForDetails(user.refId)}
                            className="cursor-pointer flex items-center gap-2"
                          >
                            <Eye className="h-3.5 w-3.5 text-slate-500" />
                            View Details
                          </DropdownMenuItem>
                          {!isProtectedSuperAdmin && (
                            <DropdownMenuItem
                              onClick={() => setSelectedUserForEdit(user)}
                              className="cursor-pointer flex items-center gap-2"
                            >
                              <Edit className="h-3.5 w-3.5 text-slate-500" />
                              Edit User
                            </DropdownMenuItem>
                          )}
                          {!isProtectedSuperAdmin && (
                            <DropdownMenuItem
                              onClick={() => setSelectedUserForStatus(user)}
                              className="cursor-pointer flex items-center gap-2 text-amber-700 focus:text-amber-800 focus:bg-amber-50"
                            >
                              <Shield className="h-3.5 w-3.5 text-amber-600" />
                              Change Status
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Server-Side Pagination Bar */}
        {data && data.totalPages > 0 && (
          <div className="flex flex-col gap-3 border-t border-slate-200 px-4 py-3 sm:flex-row sm:items-center sm:justify-between bg-white">
            <div className="text-xs text-slate-600">
              Showing{" "}
              <span className="font-semibold text-slate-900">
                {data.totalElements > 0 ? page * size + 1 : 0}
              </span>{" "}
              to{" "}
              <span className="font-semibold text-slate-900">
                {Math.min((page + 1) * size, data.totalElements)}
              </span>{" "}
              of{" "}
              <span className="font-semibold text-slate-900">
                {data.totalElements}
              </span>{" "}
              users
            </div>

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
                Page {page + 1} of {Math.max(1, data.totalPages)}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() =>
                  setPage((prev) =>
                    prev + 1 < data.totalPages ? prev + 1 : prev,
                  )
                }
                disabled={page + 1 >= data.totalPages || isLoading}
                className="h-8 text-xs"
              >
                Next
              </Button>
            </div>
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
        onEdit={(refId) => {
          const u = users.find((item) => item.refId === refId);
          if (u) setSelectedUserForEdit(u);
        }}
        onChangeStatus={(refId) => {
          const u = users.find((item) => item.refId === refId);
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

export default UsersPage;
