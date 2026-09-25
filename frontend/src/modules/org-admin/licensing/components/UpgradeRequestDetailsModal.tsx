import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Sparkles,
  Mail,
  User,
  Phone,
  AlertCircle,
  CheckCircle2,
} from "lucide-react";
import type { PlanUpgradeRequestResponse } from "../types/licenseTypes";

interface UpgradeRequestDetailsModalProps {
  request: PlanUpgradeRequestResponse | null;
  isOpen: boolean;
  onClose: () => void;
}

export function UpgradeRequestDetailsModal({
  request,
  isOpen,
  onClose,
}: UpgradeRequestDetailsModalProps) {
  if (!request) return null;

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

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <div className="flex items-center gap-2 text-teal-800">
            <div className="p-2 rounded-lg bg-teal-50 border border-teal-100">
              <Sparkles className="h-5 w-5 text-[#0F766E]" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Upgrade Request Details
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono">
                Ref: {request.refId}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2 text-xs">
          {/* Status Box */}
          <div className="p-3.5 rounded-lg border bg-slate-50 border-slate-200 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-semibold text-slate-700">Request Status</span>
              <Badge variant="outline" className="font-semibold">
                {request.status}
              </Badge>
            </div>

            {request.status === "APPROVED" && (
              <div className="flex items-start gap-2 text-emerald-800 bg-emerald-50 p-2.5 rounded-md border border-emerald-200">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600 mt-0.5" />
                <span>
                  <strong>Request Approved:</strong> Your organization's upgrade has been approved. The updated plan and seat limits will appear on your dashboard.
                </span>
              </div>
            )}

            {request.status === "REJECTED" && (
              <div className="flex items-start gap-2 text-rose-800 bg-rose-50 p-2.5 rounded-md border border-rose-200">
                <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                <div>
                  <p className="font-semibold">Request Declined</p>
                  <p className="mt-0.5">{request.rejectionReason || "No specific reason provided."}</p>
                </div>
              </div>
            )}

            {request.adminNotes && (
              <div className="text-slate-600 bg-white p-2.5 rounded-md border border-slate-200">
                <span className="font-semibold text-slate-800 block mb-0.5">
                  Admin Reviewer Notes:
                </span>
                <p>{request.adminNotes}</p>
              </div>
            )}
          </div>

          {/* Plan Comparison */}
          <div className="p-3 rounded-lg border border-slate-200 divide-y divide-slate-100">
            <div className="flex justify-between py-1.5">
              <span className="text-slate-500">Current Plan:</span>
              <span className="font-medium text-slate-800">{request.currentPlanName}</span>
            </div>
            <div className="flex justify-between py-1.5">
              <span className="text-slate-500">Requested Plan:</span>
              <span className="font-bold text-teal-900">{request.requestedPlanName}</span>
            </div>
            <div className="flex justify-between py-1.5">
              <span className="text-slate-500">Staff Capacity Change:</span>
              <span className="font-semibold text-slate-800">
                {request.currentActiveStaffCount} active → {request.requestedStaffCapacity} seats
              </span>
            </div>
          </div>

          {/* Contact Details */}
          <div className="p-3 rounded-lg border border-slate-200 space-y-1.5 bg-slate-50/50">
            <span className="text-xs font-semibold text-slate-700 block mb-1">
              Contact Provided:
            </span>
            <div className="flex items-center gap-2 text-slate-600">
              <User className="h-3.5 w-3.5 text-slate-400" />
              <span>{request.contactName}</span>
            </div>
            <div className="flex items-center gap-2 text-slate-600">
              <Mail className="h-3.5 w-3.5 text-slate-400" />
              <span>{request.contactEmail}</span>
            </div>
            {request.contactPhone && (
              <div className="flex items-center gap-2 text-slate-600">
                <Phone className="h-3.5 w-3.5 text-slate-400" />
                <span>{request.contactPhone}</span>
              </div>
            )}
          </div>

          {request.reason && (
            <div className="space-y-1">
              <span className="text-xs font-semibold text-slate-700">Upgrade Justification:</span>
              <p className="text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                {request.reason}
              </p>
            </div>
          )}

          <div className="flex justify-between text-[11px] text-slate-400 pt-1">
            <span>Submitted: {formatDate(request.createdAt)}</span>
            {request.reviewedAt && (
              <span>Reviewed: {formatDate(request.reviewedAt)}</span>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
