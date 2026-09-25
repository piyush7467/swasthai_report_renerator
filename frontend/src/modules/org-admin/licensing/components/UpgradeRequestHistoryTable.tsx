import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Clock,
  CheckCircle2,
  XCircle,
  PhoneCall,
  Ban,
  Eye,
  Loader2,
  ChevronLeft,
  ChevronRight,
  Inbox,
} from "lucide-react";
import {
  useUpgradeRequests,
  useCancelUpgradeRequest,
} from "../hooks/useOrgLicense";
import type {
  PlanUpgradeRequestResponse,
  UpgradeRequestStatus,
} from "../types/licenseTypes";

interface UpgradeRequestHistoryTableProps {
  onSelectRequest: (request: PlanUpgradeRequestResponse) => void;
}

export function UpgradeRequestHistoryTable({
  onSelectRequest,
}: UpgradeRequestHistoryTableProps) {
  const [page, setPage] = useState(0);
  const [selectedStatus, setSelectedStatus] = useState<
    UpgradeRequestStatus | undefined
  >(undefined);

  const { data, isLoading } = useUpgradeRequests(page, 10, selectedStatus);
  const cancelMutation = useCancelUpgradeRequest();

  const requests = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "N/A";
    try {
      return new Date(isoString).toLocaleDateString("en-IN", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  const renderStatusBadge = (status: UpgradeRequestStatus) => {
    switch (status) {
      case "PENDING":
        return (
          <Badge
            variant="outline"
            className="bg-amber-50 text-amber-700 border-amber-200 gap-1 text-[11px] font-semibold px-2 py-0.5"
          >
            <Clock className="h-3 w-3 text-amber-600" />
            Pending Review
          </Badge>
        );
      case "CONTACTED":
        return (
          <Badge
            variant="outline"
            className="bg-blue-50 text-blue-700 border-blue-200 gap-1 text-[11px] font-semibold px-2 py-0.5"
          >
            <PhoneCall className="h-3 w-3 text-blue-600" />
            Contacted
          </Badge>
        );
      case "APPROVED":
        return (
          <Badge
            variant="outline"
            className="bg-emerald-50 text-emerald-700 border-emerald-200 gap-1 text-[11px] font-semibold px-2 py-0.5"
          >
            <CheckCircle2 className="h-3 w-3 text-emerald-600" />
            Approved
          </Badge>
        );
      case "REJECTED":
        return (
          <Badge
            variant="outline"
            className="bg-rose-50 text-rose-700 border-rose-200 gap-1 text-[11px] font-semibold px-2 py-0.5"
          >
            <XCircle className="h-3 w-3 text-rose-600" />
            Declined
          </Badge>
        );
      case "CANCELLED":
        return (
          <Badge
            variant="outline"
            className="bg-slate-100 text-slate-600 border-slate-200 gap-1 text-[11px] font-medium px-2 py-0.5"
          >
            <Ban className="h-3 w-3 text-slate-400" />
            Cancelled
          </Badge>
        );
    }
  };

  const handleCancel = async (refId: string) => {
    if (confirm("Are you sure you want to cancel this pending upgrade request?")) {
      try {
        await cancelMutation.mutateAsync(refId);
      } catch (err: unknown) {
        const errorObj = err as { response?: { data?: { message?: string } } };
        alert(errorObj?.response?.data?.message || "Failed to cancel request.");
      }
    }
  };

  return (
    <Card className="border-slate-200 bg-white shadow-xs overflow-hidden space-y-4 p-5 sm:p-6">
      {/* Header and Filter Tabs */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-slate-900">
            Upgrade Request History
          </h3>
          <p className="text-xs text-slate-500">
            Track communication and status updates on your organization's subscription requests.
          </p>
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1 overflow-x-auto pb-1 sm:pb-0">
          {(
            [
              { label: "All", value: undefined },
              { label: "Pending", value: "PENDING" },
              { label: "Contacted", value: "CONTACTED" },
              { label: "Approved", value: "APPROVED" },
              { label: "Declined", value: "REJECTED" },
            ] as const
          ).map((tab) => {
            const isActive = selectedStatus === tab.value;
            return (
              <button
                key={tab.label}
                type="button"
                onClick={() => {
                  setSelectedStatus(tab.value);
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-md text-xs font-medium transition-colors cursor-pointer ${
                  isActive
                    ? "bg-[#0F766E] text-white"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="py-12 flex flex-col items-center justify-center text-xs text-slate-400 gap-2">
          <Loader2 className="h-5 w-5 animate-spin text-teal-600" />
          <span>Loading upgrade history...</span>
        </div>
      ) : requests.length === 0 ? (
        <div className="py-10 text-center flex flex-col items-center justify-center text-slate-400">
          <Inbox className="h-8 w-8 text-slate-300 mb-2" />
          <p className="text-sm font-medium text-slate-700">
            No upgrade requests found
          </p>
          <p className="text-xs text-slate-500 max-w-sm mt-1">
            {selectedStatus
              ? `There are no requests matching the "${selectedStatus}" status.`
              : "When your organization requests a plan upgrade, it will appear here with real-time status tracking."}
          </p>
        </div>
      ) : (
        <>
          {/* Desktop Table View */}
          <div className="hidden sm:block overflow-x-auto border border-slate-200 rounded-lg">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase font-semibold tracking-wider text-[11px]">
                <tr>
                  <th className="py-3 px-4">Request Ref</th>
                  <th className="py-3 px-4">Target Plan</th>
                  <th className="py-3 px-4">Staff Seats</th>
                  <th className="py-3 px-4">Submitted Date</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requests.map((req) => (
                  <tr key={req.refId} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-slate-800">
                      {req.refId}
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-900">
                      {req.requestedPlanName}
                    </td>
                    <td className="py-3 px-4 text-slate-600">
                      {req.currentActiveStaffCount} →{" "}
                      <strong className="text-slate-800">{req.requestedStaffCapacity}</strong> seats
                    </td>
                    <td className="py-3 px-4 text-slate-500">
                      {formatDate(req.createdAt)}
                    </td>
                    <td className="py-3 px-4">
                      {renderStatusBadge(req.status)}
                    </td>
                    <td className="py-3 px-4 text-right space-x-1.5">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => onSelectRequest(req)}
                        className="h-7 text-xs text-slate-700 hover:text-teal-800 hover:bg-teal-50 gap-1 cursor-pointer"
                      >
                        <Eye className="h-3.5 w-3.5" />
                        Details
                      </Button>

                      {req.status === "PENDING" && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleCancel(req.refId)}
                          disabled={cancelMutation.isPending}
                          className="h-7 text-xs text-rose-600 hover:text-rose-700 hover:bg-rose-50 gap-1 cursor-pointer"
                        >
                          <Ban className="h-3 w-3" />
                          Cancel
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Card View */}
          <div className="sm:hidden space-y-3">
            {requests.map((req) => (
              <div
                key={req.refId}
                className="p-3.5 rounded-lg border border-slate-200 bg-white space-y-2.5 shadow-2xs"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-semibold text-slate-800">
                    {req.refId}
                  </span>
                  {renderStatusBadge(req.status)}
                </div>

                <div className="flex justify-between items-baseline text-xs">
                  <span className="text-slate-500">Target Plan:</span>
                  <span className="font-bold text-slate-900">
                    {req.requestedPlanName} ({req.requestedStaffCapacity} seats)
                  </span>
                </div>

                <div className="flex justify-between text-xs text-slate-500">
                  <span>Submitted:</span>
                  <span>{formatDate(req.createdAt)}</span>
                </div>

                <div className="pt-2 border-t border-slate-100 flex items-center justify-end gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => onSelectRequest(req)}
                    className="h-7 text-xs gap-1"
                  >
                    <Eye className="h-3.5 w-3.5" />
                    View Details
                  </Button>

                  {req.status === "PENDING" && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleCancel(req.refId)}
                      disabled={cancelMutation.isPending}
                      className="h-7 text-xs text-rose-600 border-rose-200 hover:bg-rose-50 gap-1"
                    >
                      <Ban className="h-3 w-3" />
                      Cancel
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2 text-xs text-slate-500">
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <div className="flex items-center gap-1.5">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="h-7 text-xs px-2"
                >
                  <ChevronLeft className="h-3.5 w-3.5" />
                  Previous
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="h-7 text-xs px-2"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </Card>
  );
}
