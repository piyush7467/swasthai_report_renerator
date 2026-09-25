import { useState, useEffect } from "react";
import axios from "axios";
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
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Sparkles,
  Building2,
  Mail,
  User,
  Phone,
  CheckCircle2,
  AlertCircle,
  Clock,
  PhoneCall,
  ShieldCheck,
  ArrowRight,
  Loader2,
} from "lucide-react";
import { useUpdateUpgradeRequestStatusMutation } from "../hooks/useLicensing";
import type { PlanUpgradeRequestResponse } from "../types/licensingTypes";

interface ReviewUpgradeRequestModalProps {
  request: PlanUpgradeRequestResponse | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function ReviewUpgradeRequestModal({
  request,
  isOpen,
  onClose,
  onSuccess,
}: ReviewUpgradeRequestModalProps) {
  const [adminNotes, setAdminNotes] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [actionType, setActionType] = useState<"CONTACTED" | "APPROVED" | "REJECTED" | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const updateMutation = useUpdateUpgradeRequestStatusMutation();

  useEffect(() => {
    if (request) {
      setAdminNotes(request.adminNotes || "");
      setRejectionReason(request.rejectionReason || "");
      setActionType(null);
      setErrorMessage(null);
    }
  }, [request, isOpen]);

  if (!request) return null;

  const isPending = request.status === "PENDING";
  const isContacted = request.status === "CONTACTED";
  const isActionable = isPending || isContacted;

  const handleUpdateStatus = async (status: "CONTACTED" | "APPROVED" | "REJECTED") => {
    setErrorMessage(null);
    if (status === "REJECTED" && !rejectionReason.trim()) {
      setErrorMessage("Please provide a reason for rejecting this upgrade request.");
      return;
    }

    try {
      await updateMutation.mutateAsync({
        refId: request.refId,
        request: {
          status,
          adminNotes: adminNotes.trim() || undefined,
          rejectionReason: status === "REJECTED" ? rejectionReason.trim() : undefined,
        },
      });

      onSuccess?.();
      onClose();
    } catch (err: unknown) {
      let msg = "Failed to update request status";
      if (axios.isAxiosError(err)) {
        msg = err.response?.data?.message || err.message || msg;
      } else if (err instanceof Error) {
        msg = err.message;
      }
      setErrorMessage(msg);
    }
  };

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
    <Dialog open={isOpen} onOpenChange={(open) => !open && !updateMutation.isPending && onClose()}>
      <DialogContent className="sm:max-w-[620px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-teal-50 border border-teal-100">
              <Sparkles className="h-5 w-5 text-[#0F766E]" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Review Plan Upgrade Request
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono">
                Ref: {request.refId}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {errorMessage && (
          <Alert variant="destructive" className="my-2">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription className="text-xs">{errorMessage}</AlertDescription>
          </Alert>
        )}

        <div className="space-y-4 py-2 text-xs">
          {/* Organization & Status Bar */}
          <div className="flex items-center justify-between p-3 rounded-lg border bg-slate-50/80">
            <div className="flex items-center gap-2">
              <Building2 className="h-4 w-4 text-slate-500" />
              <div>
                <span className="font-semibold text-slate-800 text-sm block">
                  {request.organizationName}
                </span>
                <span className="text-[11px] text-slate-500 font-mono">
                  {request.organizationRefId}
                </span>
              </div>
            </div>
            <div className="text-right">
              <Badge
                variant="outline"
                className={`font-semibold ${
                  request.status === "PENDING"
                    ? "bg-amber-50 text-amber-700 border-amber-200"
                    : request.status === "CONTACTED"
                    ? "bg-blue-50 text-blue-700 border-blue-200"
                    : request.status === "APPROVED"
                    ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                    : "bg-rose-50 text-rose-700 border-rose-200"
                }`}
              >
                {request.status}
              </Badge>
              <span className="block text-[11px] text-slate-400 mt-0.5">
                {formatDate(request.createdAt)}
              </span>
            </div>
          </div>

          {/* Plan Transition Box */}
          <div className="p-3.5 rounded-lg border border-slate-200 bg-white shadow-xs">
            <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">
              Proposed Subscription Plan Change
            </div>
            <div className="grid grid-cols-2 gap-3 items-center">
              <div className="p-2.5 rounded-md bg-slate-50 border border-slate-100">
                <span className="text-[10px] uppercase text-slate-400 font-semibold block">
                  Current Tier
                </span>
                <span className="font-semibold text-slate-800 text-sm block">
                  {request.currentPlanName}
                </span>
                <span className="text-[11px] text-slate-500">
                  {request.currentActiveStaffCount} active staff
                </span>
              </div>

              <div className="p-2.5 rounded-md bg-teal-50 border border-teal-100">
                <div className="flex items-center gap-1 text-[#0F766E] font-semibold text-[10px] uppercase">
                  <span>Requested Tier</span>
                  <ArrowRight className="h-3 w-3" />
                </div>
                <span className="font-bold text-teal-950 text-sm block">
                  {request.requestedPlanName}
                </span>
                <span className="text-[11px] text-teal-800 font-medium">
                  {request.requestedStaffCapacity} max staff seats
                </span>
              </div>
            </div>
          </div>

          {/* Contact Details */}
          <div className="p-3 rounded-lg border border-slate-200 bg-slate-50/50 space-y-1.5">
            <span className="text-xs font-semibold text-slate-700 block mb-1">
              Requester Contact Information:
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-slate-600">
              <div className="flex items-center gap-2">
                <User className="h-3.5 w-3.5 text-slate-400" />
                <span>{request.contactName}</span>
              </div>
              <div className="flex items-center gap-2">
                <Mail className="h-3.5 w-3.5 text-slate-400" />
                <a
                  href={`mailto:${request.contactEmail}`}
                  className="text-teal-700 hover:underline"
                >
                  {request.contactEmail}
                </a>
              </div>
              {request.contactPhone && (
                <div className="flex items-center gap-2">
                  <Phone className="h-3.5 w-3.5 text-slate-400" />
                  <a
                    href={`tel:${request.contactPhone}`}
                    className="text-teal-700 hover:underline"
                  >
                    {request.contactPhone}
                  </a>
                </div>
              )}
            </div>
          </div>

          {/* Reason / Justification */}
          {request.reason && (
            <div className="space-y-1">
              <span className="text-xs font-semibold text-slate-700">Upgrade Justification:</span>
              <p className="text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                {request.reason}
              </p>
            </div>
          )}

          {/* Admin Review Inputs (if actionable) */}
          {isActionable ? (
            <div className="space-y-3 pt-1 border-t border-slate-100">
              {/* Guidance Banner */}
              {isPending && (
                <div className="p-3 rounded-lg bg-amber-50/80 border border-amber-200 text-amber-900 text-xs flex items-start gap-2">
                  <Clock className="h-4 w-4 mt-0.5 shrink-0 text-amber-700" />
                  <div>
                    <span className="font-semibold block text-amber-950">
                      Step 1: Contact Organization Administrator
                    </span>
                    <span className="text-[11px] text-amber-800 mt-0.5 block">
                      Please reach out to the requester to confirm requirements and payment arrangements offline, then click <strong>"Mark Contacted"</strong>. After contact is established, the plan upgrade can be approved.
                    </span>
                  </div>
                </div>
              )}

              {isContacted && (
                <div className="p-3 rounded-lg bg-blue-50/80 border border-blue-200 text-blue-900 text-xs flex items-start gap-2">
                  <PhoneCall className="h-4 w-4 mt-0.5 shrink-0 text-blue-700" />
                  <div>
                    <span className="font-semibold block text-blue-950">
                      Step 2: Ready for Final Approval
                    </span>
                    <span className="text-[11px] text-blue-800 mt-0.5 block">
                      Contact has been confirmed. You can now approve this request to immediately upgrade the organization's plan, or reject if terms were not met.
                    </span>
                  </div>
                </div>
              )}

              <div className="space-y-1">
                <Label htmlFor="adminNotes" className="text-xs font-semibold text-slate-700">
                  Admin Internal Notes (Optional)
                </Label>
                <Textarea
                  id="adminNotes"
                  placeholder="e.g. Spoke with Lab Director on call. Payment confirmed via wire transfer..."
                  value={adminNotes}
                  onChange={(e) => setAdminNotes(e.target.value)}
                  rows={2}
                  className="text-xs"
                />
              </div>

              {actionType === "REJECTED" && (
                <div className="space-y-1 p-3 rounded-md bg-rose-50 border border-rose-200">
                  <Label htmlFor="rejectionReason" className="text-xs font-semibold text-rose-800">
                    Rejection Reason (Required - visible to organization admin)
                  </Label>
                  <Textarea
                    id="rejectionReason"
                    placeholder="e.g. Plan downgrade conflict, pending verification..."
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    rows={2}
                    className="text-xs bg-white"
                  />
                </div>
              )}

              {actionType === "APPROVED" && (
                <div className="p-3 rounded-md bg-emerald-50 border border-emerald-200 flex items-start gap-2 text-emerald-800">
                  <ShieldCheck className="h-4 w-4 shrink-0 text-emerald-600 mt-0.5" />
                  <span>
                    <strong>Confirmation:</strong> Approving will immediately upgrade{" "}
                    <strong>{request.organizationName}</strong> to{" "}
                    <strong>{request.requestedPlanName}</strong> with a limit of{" "}
                    <strong>{request.requestedStaffCapacity} staff seats</strong>.
                  </span>
                </div>
              )}
            </div>
          ) : (
            /* Historical Review Results */
            <div className="p-3 rounded-lg border bg-slate-50 space-y-1.5">
              <div className="flex items-center justify-between text-slate-500">
                <span>Reviewed by: {request.reviewedByName || request.reviewedByEmail || "Admin"}</span>
                <span>Reviewed on: {formatDate(request.reviewedAt)}</span>
              </div>
              {request.adminNotes && (
                <div className="pt-1 text-slate-700">
                  <span className="font-semibold block">Reviewer Notes:</span>
                  <p>{request.adminNotes}</p>
                </div>
              )}
              {request.rejectionReason && (
                <div className="pt-1 text-rose-700">
                  <span className="font-semibold block">Rejection Reason:</span>
                  <p>{request.rejectionReason}</p>
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={updateMutation.isPending}
          >
            {isActionable ? "Cancel" : "Close"}
          </Button>

          {isActionable && (
            <div className="flex items-center gap-2">
              {/* Reject button */}
              {actionType !== "REJECTED" ? (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setActionType("REJECTED")}
                  disabled={updateMutation.isPending}
                  className="text-rose-700 border-rose-200 hover:bg-rose-50"
                >
                  Reject
                </Button>
              ) : (
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => handleUpdateStatus("REJECTED")}
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending && (
                    <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
                  )}
                  Confirm Rejection
                </Button>
              )}

              {/* Step 1: Mark Contacted for PENDING requests */}
              {isPending && actionType !== "REJECTED" && (
                <Button
                  size="sm"
                  onClick={() => handleUpdateStatus("CONTACTED")}
                  disabled={updateMutation.isPending}
                  className="bg-blue-600 hover:bg-blue-700 text-white gap-1"
                >
                  {updateMutation.isPending ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <PhoneCall className="h-3.5 w-3.5" />
                  )}
                  Mark Contacted
                </Button>
              )}

              {/* Step 2: Approve Upgrade for CONTACTED requests */}
              {isContacted && actionType !== "REJECTED" && (
                actionType !== "APPROVED" ? (
                  <Button
                    size="sm"
                    onClick={() => setActionType("APPROVED")}
                    disabled={updateMutation.isPending}
                    className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    Approve Upgrade
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    onClick={() => handleUpdateStatus("APPROVED")}
                    disabled={updateMutation.isPending}
                    className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold gap-1"
                  >
                    {updateMutation.isPending && (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    )}
                    Confirm & Apply Upgrade
                  </Button>
                )
              )}
            </div>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
