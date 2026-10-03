import { useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import {
  AlertCircle,
  ArrowRight,
  Building2,
  ChevronLeft,
  ChevronRight,
  Edit,
  FlaskConical,
  MoreVertical,
  Plus,
  RefreshCw,
  Trash2,
} from "lucide-react";

import { Button } from "@/components/ui/button";
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
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";

import { useOrganizationsQuery } from "../../hooks/useOrganizations";
import {
  useAssignmentsQuery,
  useDeactivateAssignmentMutation,
} from "../hooks/useAssignments";
import { TestStatusBadge } from "../components/TestStatusBadge";
import { TestTypeBadge } from "../components/TestTypeBadge";
import { UpdateAssignmentDialog } from "../components/UpdateAssignmentDialog";
import { ConfirmDeactivateDialog } from "../components/ConfirmDeactivateDialog";
import type {
  OrganizationTestQueryParams,
  OrganizationTestResponse,
} from "../types/assignmentTypes";

export function AssignmentsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const urlOrgRefId = searchParams.get("organizationRefId") || "";

  const [status, setStatus] = useState<string>("ALL");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const [assignmentToEdit, setAssignmentToEdit] =
    useState<OrganizationTestResponse | null>(null);
  const [deactivateTarget, setDeactivateTarget] =
    useState<OrganizationTestResponse | null>(null);

  // Load organizations
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

  const handleOrgChange = (newRefId: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set("organizationRefId", newRefId);
      return next;
    });
    setPage(0);
  };

  const queryParams: OrganizationTestQueryParams = {
    organizationRefId: effectiveOrgRefId,
    status: status !== "ALL" ? status : undefined,
    page,
    size: pageSize,
    sort: "createdAt",
    direction: "desc",
  };

  const {
    data: assignmentsData,
    isLoading: assignmentsLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useAssignmentsQuery(queryParams);

  const deactivateMutation = useDeactivateAssignmentMutation();

  const handleDeactivate = async () => {
    if (!deactivateTarget) return;
    await deactivateMutation.mutateAsync(deactivateTarget.refId);
    setDeactivateTarget(null);
  };

  const selectedOrg = orgsData?.content.find(
    (o) => o.refId === effectiveOrgRefId,
  );
  const totalPages = assignmentsData?.totalPages ?? 0;
  const totalElements = assignmentsData?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Organization Test Assignments
            </h1>
            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">
              {totalElements} assigned
            </span>
          </div>
          <p className="text-sm text-slate-500">
            Control which diagnostic tests from the master catalog are licensed to each organization.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching || !effectiveOrgRefId}
          >
            <RefreshCw
              className={`mr-2 h-4 w-4 ${isFetching ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>

          <Button
            size="sm"
            onClick={() =>
              navigate(
                `/super-admin/tests/assignments/new?organizationRefId=${effectiveOrgRefId}`,
              )
            }
            className="bg-[#0F766E] hover:bg-[#115E59] text-white shadow-xs"
          >
            <Plus className="mr-2 h-4 w-4" />
            Assign Tests
          </Button>
        </div>
      </div>

      {/* Visual Hierarchy Architecture Card */}
      <Card className="border-teal-100 bg-teal-50/30 shadow-xs">
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-slate-600">
            <div className="flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded bg-teal-100 text-teal-800 font-semibold">
                <FlaskConical className="h-4 w-4" />
              </div>
              <span className="font-semibold text-slate-900">Master Test Catalog</span>
            </div>

            <div className="flex items-center gap-1 text-slate-400 font-medium">
              <ArrowRight className="h-4 w-4" />
              <span>Assigned via Policy</span>
              <ArrowRight className="h-4 w-4" />
            </div>

            <div className="flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded bg-emerald-100 text-emerald-700 font-semibold">
                <Building2 className="h-4 w-4" />
              </div>
              <span className="font-semibold text-slate-900">Organization Tenant</span>
            </div>

            <div className="flex items-center gap-1 text-slate-400 font-medium">
              <ArrowRight className="h-4 w-4" />
              <span>Licensed for Reports</span>
            </div>

            <div className="flex items-center gap-2">
              <span className="rounded bg-emerald-100 px-2 py-1 font-semibold text-emerald-800">
                Staff Reporting Active
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Organization Selection & Filter Toolbar */}
      <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 sm:flex-row sm:items-center sm:justify-between shadow-xs">
        <div className="flex flex-1 flex-col gap-3 sm:flex-row sm:items-center">
          {/* Organization Selector */}
          <div className="w-full sm:w-72">
            <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-1">
              Select Target Organization
            </label>
            <Select
              value={effectiveOrgRefId}
              onValueChange={handleOrgChange}
              disabled={orgsLoading}
            >
              <SelectTrigger className="text-sm font-medium">
                <SelectValue placeholder="Choose organization..." />
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

          {/* Status Filter */}
          <div className="w-full sm:w-40 sm:self-end">
            <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-1">
              Assignment Status
            </label>
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

        {selectedOrg && (
          <div className="text-xs text-slate-500 sm:text-right pt-2 sm:pt-0 sm:self-end">
            <span className="font-semibold text-slate-800">{selectedOrg.name}</span>
            <span className="block font-mono text-[11px]">Ref: {selectedOrg.refId}</span>
          </div>
        )}
      </div>

      {/* Error state */}
      {isError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Error loading test assignments</AlertTitle>
          <AlertDescription className="mt-1 flex items-center justify-between">
            <span>{error?.message || "Failed to load assignments."}</span>
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
        {assignmentsLoading || orgsLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : !effectiveOrgRefId ? (
          <div className="py-16 text-center text-sm text-slate-500">
            Please select an organization to view its assigned diagnostic tests.
          </div>
        ) : assignmentsData?.content && assignmentsData.content.length > 0 ? (
          <>
            {/* Mobile Cards View (< md) */}
            <div className="md:hidden divide-y divide-slate-100">
              {assignmentsData.content.map((assign) => (
                <div key={assign.refId} className="p-4 space-y-2.5">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h4 className="font-semibold text-slate-900 text-sm">
                        {assign.testName}
                      </h4>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="font-mono text-xs font-medium text-slate-500">
                          {assign.testCode}
                        </span>
                        <TestTypeBadge type={assign.testType} />
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <TestStatusBadge status={assign.status} />
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
                        <DropdownMenuContent align="end" className="w-44">
                          <DropdownMenuItem
                            onClick={() => setAssignmentToEdit(assign)}
                          >
                            <Edit className="mr-2 h-4 w-4 text-slate-500" />
                            Edit Assignment
                          </DropdownMenuItem>
                          {assign.status === "ACTIVE" && (
                            <DropdownMenuItem
                              className="text-amber-600 focus:text-amber-700"
                              onClick={() => setDeactivateTarget(assign)}
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              Deactivate
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs text-slate-500 pt-1">
                    <span>
                      {assign.effectiveFrom || assign.effectiveUntil ? (
                        <span>
                          {assign.effectiveFrom || "Start"} &rarr; {assign.effectiveUntil || "Indefinite"}
                        </span>
                      ) : (
                        <span className="text-slate-400">Always active</span>
                      )}
                    </span>
                    <span className="text-[11px] text-slate-400">
                      {new Date(assign.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Desktop Table View (>= md) */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-700">
                <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                  <tr>
                    <th scope="col" className="px-6 py-3.5">
                      Diagnostic Test
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Test Code
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Type
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Assignment Status
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Effective Period
                    </th>
                    <th scope="col" className="px-6 py-3.5">
                      Assigned Date
                    </th>
                    <th scope="col" className="px-6 py-3.5 text-right">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {assignmentsData.content.map((assign) => (
                    <tr
                      key={assign.refId}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="px-6 py-4 font-semibold text-slate-900">
                        {assign.testName}
                      </td>
                      <td className="px-6 py-4 font-mono text-xs font-medium text-slate-600">
                        {assign.testCode}
                      </td>
                      <td className="px-6 py-4">
                        <TestTypeBadge type={assign.testType} />
                      </td>
                      <td className="px-6 py-4">
                        <TestStatusBadge status={assign.status} />
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-600">
                        {assign.effectiveFrom || assign.effectiveUntil ? (
                          <span>
                            {assign.effectiveFrom || "Start"} →{" "}
                            {assign.effectiveUntil || "Indefinite"}
                          </span>
                        ) : (
                          <span className="text-slate-400">Always active</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-500">
                        {new Date(assign.createdAt).toLocaleDateString()}
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
                          <DropdownMenuContent align="end" className="w-44">
                            <DropdownMenuItem
                              onClick={() => setAssignmentToEdit(assign)}
                            >
                              <Edit className="mr-2 h-4 w-4 text-slate-500" />
                              Edit Assignment
                            </DropdownMenuItem>
                            {assign.status === "ACTIVE" && (
                              <DropdownMenuItem
                                className="text-amber-600 focus:text-amber-700"
                                onClick={() => setDeactivateTarget(assign)}
                              >
                                <Trash2 className="mr-2 h-4 w-4" />
                                Deactivate
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
          </>
        ) : (
          <EmptyState
            title="No Tests Assigned"
            description={
              selectedOrg
                ? `No diagnostic tests are currently assigned to ${selectedOrg.name}. Assign tests from the catalog to enable them for this lab.`
                : "No tests assigned. Select an organization to manage its diagnostic test offerings."
            }
            action={{
              label: "Assign Tests to Organization",
              onClick: () =>
                navigate(
                  `/super-admin/tests/assignments/new?organizationRefId=${effectiveOrgRefId}`,
                ),
              icon: Plus,
              className: "bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold text-xs h-9 px-4 rounded-lg shadow-xs",
            }}
          />
        )}

        {/* Pagination Bar */}
        {assignmentsData && assignmentsData.totalElements > 0 && (
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
                disabled={page === 0 || assignmentsLoading}
                className="h-8 px-2.5"
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || assignmentsLoading}
                className="h-8 px-2.5"
              >
                Next
                <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Edit Assignment Modal */}
      <UpdateAssignmentDialog
        open={Boolean(assignmentToEdit)}
        onOpenChange={(open) => {
          if (!open) setAssignmentToEdit(null);
        }}
        assignment={assignmentToEdit}
      />

      {/* Deactivate Confirmation */}
      <ConfirmDeactivateDialog
        open={Boolean(deactivateTarget)}
        onOpenChange={(open) => {
          if (!open) setDeactivateTarget(null);
        }}
        title={`Deactivate test assignment for "${deactivateTarget?.testName}"?`}
        description={`This will deactivate the assignment for ${deactivateTarget?.organizationName}. Staff in this organization will no longer be able to select this test in new reports.`}
        confirmLabel="Deactivate Assignment"
        onConfirm={handleDeactivate}
        isLoading={deactivateMutation.isPending}
      />
    </div>
  );
}

export default AssignmentsPage;
