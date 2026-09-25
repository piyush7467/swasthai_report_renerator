import { useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eye,
  LogIn,
  Mail,
  Plus,
  PowerOff,
  RefreshCw,
  Search,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  useLabStaffListQuery,
  useLabStaffSummaryQuery,
} from "../hooks/useLabStaff";
import { LabStaffSummaryCard } from "../components/LabStaffSummaryCard";
import { AddLabStaffModal } from "../components/AddLabStaffModal";
import { LabStaffDetailsModal } from "../components/LabStaffDetailsModal";
import { DeactivateStaffDialog } from "../components/DeactivateStaffDialog";
import type {
  LabStaffResponse,
  UserStatus,
} from "../types/labStaffTypes";

export default function LabStaffPage() {
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<UserStatus | "ALL">("ALL");

  // Modals state
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [detailsRefId, setDetailsRefId] = useState<string | null>(null);
  const [staffToDeactivate, setStaffToDeactivate] =
    useState<LabStaffResponse | null>(null);

  // Queries
  const {
    data: summary,
    isLoading: isSummaryLoading,
    refetch: refetchSummary,
  } = useLabStaffSummaryQuery();

  const {
    data: pagedStaff,
    isLoading: isListLoading,
    isError,
    error,
    refetch: refetchList,
  } = useLabStaffListQuery({
    page,
    size: 15,
    search: searchTerm.trim() || undefined,
    status: selectedStatus === "ALL" ? undefined : selectedStatus,
    sortBy: "createdAt",
    sortDirection: "DESC",
  });

  const staffList = pagedStaff?.content ?? [];

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
    setPage(0);
  };

  const handleStatusChange = (status: UserStatus | "ALL") => {
    setSelectedStatus(status);
    setPage(0);
  };

  const handleRefresh = () => {
    void refetchSummary();
    void refetchList();
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "Never";
    try {
      return new Date(isoString).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      });
    } catch {
      return isoString;
    }
  };

  const isLimitReached = summary?.limitReached ?? false;

  return (
    <div className="space-y-6 pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-teal-50 text-[#0F766E] border border-teal-100 shadow-2xs">
            <Users className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Lab Staff
            </h1>
            <p className="mt-0.5 text-xs text-slate-500">
              Manage your laboratory team and access permissions.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            className="text-xs text-slate-600 border-slate-200 hover:bg-slate-50 gap-1.5 cursor-pointer"
            title="Refresh list"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </Button>

          <Button
            onClick={() => setIsAddModalOpen(true)}
            size="sm"
            disabled={isLimitReached}
            title={
              isLimitReached
                ? `License limit reached (${summary?.activeStaff}/${summary?.maxLabStaff}). Deactivate an active staff member or upgrade subscription.`
                : "Add a new lab staff member"
            }
            className="gap-1.5 text-xs font-semibold bg-[#0F766E] hover:bg-[#115E59] text-white shadow-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Plus className="h-4 w-4" />
            Add Lab Staff
          </Button>
        </div>
      </div>

      {/* Plan License Summary Card */}
      <LabStaffSummaryCard summary={summary} isLoading={isSummaryLoading} />

      {/* Search & Filter Bar */}
      <Card className="p-4 border-slate-200 bg-white shadow-xs">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3">
          {/* Search Box */}
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              type="text"
              placeholder="Search by name or email..."
              value={searchTerm}
              onChange={handleSearchChange}
              className="pl-9 text-xs h-9 bg-slate-50/50 border-slate-200 focus:bg-white"
            />
          </div>

          {/* Status Tabs Filter */}
          <div className="flex items-center gap-1 w-full md:w-auto bg-slate-100/80 p-1 rounded-lg border border-slate-200/60">
            <button
              type="button"
              onClick={() => handleStatusChange("ALL")}
              className={`px-3 py-1 text-xs font-medium rounded-md transition-all ${
                selectedStatus === "ALL"
                  ? "bg-white text-slate-900 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              All Staff
            </button>
            <button
              type="button"
              onClick={() => handleStatusChange("ACTIVE")}
              className={`px-3 py-1 text-xs font-medium rounded-md transition-all ${
                selectedStatus === "ACTIVE"
                  ? "bg-white text-teal-800 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              Active
            </button>
            <button
              type="button"
              onClick={() => handleStatusChange("INACTIVE")}
              className={`px-3 py-1 text-xs font-medium rounded-md transition-all ${
                selectedStatus === "INACTIVE"
                  ? "bg-white text-slate-800 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              Inactive
            </button>
          </div>
        </div>
      </Card>

      {/* Staff Roster View */}
      {isListLoading ? (
        <Card className="p-6 border-slate-200 bg-white shadow-xs space-y-4">
          <Skeleton className="h-6 w-1/4" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </Card>
      ) : isError ? (
        <Card className="p-8 border-rose-200 bg-rose-50/30 text-center">
          <AlertCircle className="h-8 w-8 text-rose-500 mx-auto mb-2" />
          <h3 className="text-sm font-semibold text-rose-900">
            Error Loading Lab Staff
          </h3>
          <p className="text-xs text-rose-600 mt-1">
            {error?.message || "An unexpected error occurred while fetching staff."}
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            className="mt-4 text-xs"
          >
            Try Again
          </Button>
        </Card>
      ) : staffList.length === 0 ? (
        <Card className="p-12 border-slate-200 bg-white text-center shadow-xs">
          <div className="w-12 h-12 rounded-full bg-teal-50 text-[#0F766E] border border-teal-100 flex items-center justify-center mx-auto mb-3">
            <Users className="h-6 w-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900">
            {searchTerm || selectedStatus !== "ALL"
              ? "No Staff Members Found"
              : "No Lab Staff Accounts Yet"}
          </h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1 mb-5">
            {searchTerm || selectedStatus !== "ALL"
              ? "No staff members matched your current filter or search criteria."
              : "Add your laboratory technicians and assistants to give them access to register patients and prepare diagnostic reports."}
          </p>
          {searchTerm || selectedStatus !== "ALL" ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setSearchTerm("");
                setSelectedStatus("ALL");
              }}
              className="text-xs"
            >
              Clear Filters
            </Button>
          ) : (
            <Button
              onClick={() => setIsAddModalOpen(true)}
              size="sm"
              disabled={isLimitReached}
              className="bg-[#0F766E] hover:bg-[#115E59] text-white text-xs font-semibold"
            >
              <Plus className="h-4 w-4 mr-1" />
              Add First Staff Member
            </Button>
          )}
        </Card>
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="hidden md:block rounded-xl border border-slate-200 bg-white shadow-xs overflow-hidden">
            <Table>
              <TableHeader className="bg-slate-50/80">
                <TableRow className="border-b border-slate-200 hover:bg-transparent">
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5">
                    Staff Member
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5">
                    Email
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5">
                    Status
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5">
                    Last Activity
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5">
                    Created On
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5 text-center">
                    Reports Created
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 py-3.5 text-center">
                    Reports Finalized
                  </TableHead>
                  <TableHead className="text-xs font-semibold text-slate-700 text-right py-3.5 pr-4">
                    Actions
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {staffList.map((staff) => (
                  <TableRow
                    key={staff.refId}
                    className="border-b border-slate-100 hover:bg-slate-50/60 transition-colors"
                  >
                    <TableCell className="py-3.5">
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-teal-50 border border-teal-100 text-[#0F766E] flex items-center justify-center font-bold text-xs shrink-0">
                          {staff.name.charAt(0).toUpperCase()}
                        </div>
                        <p className="text-sm font-semibold text-slate-900 leading-tight">
                          {staff.name}
                        </p>
                      </div>
                    </TableCell>

                    <TableCell className="py-3.5 text-xs text-slate-600">
                      <div className="flex items-center gap-1.5">
                        <Mail className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                        <span className="truncate max-w-[200px]">{staff.email}</span>
                      </div>
                    </TableCell>

                    <TableCell className="py-3.5">
                      {staff.status === "ACTIVE" ? (
                        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                          Active
                        </span>
                      ) : (
                        <div className="flex flex-col gap-0.5">
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600 border border-slate-200 w-fit">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400" />
                            Inactive
                          </span>
                          {staff.cleanupEligible ? (
                            <span className="text-[10px] font-semibold text-rose-600">
                              Eligible for cleanup
                            </span>
                          ) : staff.eligibleForCleanupAt ? (
                            <span className="text-[10px] text-slate-500">
                              Cleanup: {formatDate(staff.eligibleForCleanupAt)}
                            </span>
                          ) : null}
                        </div>
                      )}
                    </TableCell>

                    <TableCell className="py-3.5 text-xs text-slate-500">
                      <div className="flex items-center gap-1">
                        <LogIn className="h-3.5 w-3.5 text-slate-400" />
                        <span>{formatDate(staff.lastLoginAt)}</span>
                      </div>
                    </TableCell>

                    <TableCell className="py-3.5 text-xs text-slate-500">
                      <div>{formatDate(staff.createdAt)}</div>
                      {staff.inactiveAt && (
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          Inactive since: {formatDate(staff.inactiveAt)}
                        </div>
                      )}
                    </TableCell>

                    <TableCell className="py-3.5 text-xs text-center font-medium text-slate-700">
                      <span className="inline-block px-2 py-0.5 rounded bg-slate-50 border border-slate-100">
                        {staff.reportsCreated ?? 0}
                      </span>
                    </TableCell>

                    <TableCell className="py-3.5 text-xs text-center font-medium text-[#0F766E]">
                      <span className="inline-block px-2 py-0.5 rounded bg-teal-50 border border-teal-100">
                        {staff.reportsFinalized ?? 0}
                      </span>
                    </TableCell>

                    <TableCell className="py-3.5 text-right pr-4">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setDetailsRefId(staff.refId)}
                          className="h-8 px-2.5 text-xs text-slate-600 hover:text-teal-700 hover:bg-teal-50 gap-1"
                          title="View activity and report details"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          Details
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setStaffToDeactivate(staff)}
                          className={`h-8 px-2.5 text-xs gap-1 ${
                            staff.status === "ACTIVE"
                              ? "text-slate-600 hover:text-amber-700 hover:bg-amber-50"
                              : "text-[#0F766E] hover:text-[#115E59] hover:bg-teal-50 font-medium"
                          }`}
                        >
                          {staff.status === "ACTIVE" ? (
                            <>
                              <PowerOff className="h-3.5 w-3.5" />
                              Deactivate
                            </>
                          ) : (
                            <>
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Reactivate
                            </>
                          )}
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Mobile Responsive Cards */}
          <div className="block md:hidden space-y-3">
            {staffList.map((staff) => (
              <Card
                key={staff.refId}
                className="p-4 border-slate-200 bg-white shadow-xs space-y-3"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-full bg-teal-50 border border-teal-100 text-[#0F766E] flex items-center justify-center font-bold text-sm shrink-0">
                      {staff.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-900">
                        {staff.name}
                      </p>
                      <p className="text-xs text-slate-500">{staff.email}</p>
                    </div>
                  </div>
                  <div>
                    {staff.status === "ACTIVE" ? (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                        Active
                      </span>
                    ) : (
                      <div className="flex flex-col items-end gap-0.5">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium bg-slate-100 text-slate-600 border border-slate-200">
                          Inactive
                        </span>
                        {staff.cleanupEligible ? (
                          <span className="text-[10px] font-semibold text-rose-600">
                            Eligible for cleanup
                          </span>
                        ) : staff.eligibleForCleanupAt ? (
                          <span className="text-[10px] text-slate-500">
                            Cleanup: {formatDate(staff.eligibleForCleanupAt)}
                          </span>
                        ) : null}
                      </div>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs pt-2 border-t border-slate-100 text-slate-500">
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-slate-400 block">
                      Last Activity
                    </span>
                    <span>{formatDate(staff.lastLoginAt)}</span>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-slate-400 block">
                      Created
                    </span>
                    <span>{formatDate(staff.createdAt)}</span>
                    {staff.inactiveAt && (
                      <div className="text-[10px] text-slate-400">
                        Inactive since: {formatDate(staff.inactiveAt)}
                      </div>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs pt-2 border-t border-slate-100 text-slate-600">
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-slate-400 block">
                      Reports Created
                    </span>
                    <span className="font-semibold text-slate-800">{staff.reportsCreated ?? 0}</span>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-slate-400 block">
                      Reports Finalized
                    </span>
                    <span className="font-semibold text-[#0F766E]">{staff.reportsFinalized ?? 0}</span>
                  </div>
                </div>

                <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setDetailsRefId(staff.refId)}
                    className="h-8 text-xs text-slate-600"
                  >
                    <Eye className="h-3.5 w-3.5 mr-1" />
                    Details
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setStaffToDeactivate(staff)}
                    className={`h-8 text-xs ${
                      staff.status === "ACTIVE"
                        ? "text-amber-700 hover:bg-amber-50"
                        : "text-[#0F766E] hover:bg-teal-50"
                    }`}
                  >
                    {staff.status === "ACTIVE" ? (
                      <>
                        <PowerOff className="h-3.5 w-3.5 mr-1" />
                        Deactivate
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                        Reactivate
                      </>
                    )}
                  </Button>
                </div>
              </Card>
            ))}
          </div>

          {/* Pagination Controls */}
          {pagedStaff && pagedStaff.totalPages > 1 && (
            <div className="flex items-center justify-between text-xs text-slate-500 pt-2 px-1">
              <span>
                Showing page {pagedStaff.page + 1} of {pagedStaff.totalPages} (
                {pagedStaff.totalElements} staff members)
              </span>
              <div className="flex items-center gap-1.5">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  disabled={page === 0}
                  className="h-8 px-2.5 text-xs"
                >
                  <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((prev) => prev + 1)}
                  disabled={pagedStaff.last}
                  className="h-8 px-2.5 text-xs"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Modals & Dialogs */}
      <AddLabStaffModal
        open={isAddModalOpen}
        onOpenChange={setIsAddModalOpen}
        summary={summary}
        onSuccess={() => {
          void refetchSummary();
          void refetchList();
        }}
      />

      <LabStaffDetailsModal
        staffRefId={detailsRefId}
        open={Boolean(detailsRefId)}
        onOpenChange={(open) => !open && setDetailsRefId(null)}
      />

      <DeactivateStaffDialog
        staff={staffToDeactivate}
        open={Boolean(staffToDeactivate)}
        onOpenChange={(open) => !open && setStaffToDeactivate(null)}
        summary={summary}
        onSuccess={() => {
          void refetchSummary();
          void refetchList();
        }}
      />
    </div>
  );
}
