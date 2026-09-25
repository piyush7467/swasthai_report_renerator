import { useState } from "react";
import { AlertCircle, AlertTriangle, CheckCircle2, Loader2, PowerOff, ShieldAlert } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  useDeactivateLabStaffMutation,
  useUpdateLabStaffStatusMutation,
} from "../hooks/useLabStaff";
import type { LabStaffResponse, LabStaffSummaryResponse } from "../types/labStaffTypes";

interface DeactivateStaffDialogProps {
  staff: LabStaffResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  summary: LabStaffSummaryResponse | undefined;
  onSuccess?: (updated: LabStaffResponse) => void;
}

export function DeactivateStaffDialog({
  staff,
  open,
  onOpenChange,
  summary,
  onSuccess,
}: DeactivateStaffDialogProps) {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState(false);
  const deactivateMutation = useDeactivateLabStaffMutation();
  const statusMutation = useUpdateLabStaffStatusMutation();

  if (!staff) return null;

  const isCurrentlyActive = staff.status === "ACTIVE";
  const isReactivating = !isCurrentlyActive;
  const isLimitReached = (summary?.remainingSlots ?? 0) <= 0;
  const cannotReactivate = isReactivating && isLimitReached;
  const isPending = deactivateMutation.isPending || statusMutation.isPending;

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      setConfirmed(false);
      setErrorMsg(null);
    }
    onOpenChange(newOpen);
  };

  const handleConfirm = async () => {
    setErrorMsg(null);
    try {
      let updated: LabStaffResponse;
      if (isCurrentlyActive) {
        updated = await deactivateMutation.mutateAsync(staff.refId);
      } else {
        updated = await statusMutation.mutateAsync({
          refId: staff.refId,
          request: { status: "ACTIVE" },
        });
      }
      handleOpenChange(false);
      onSuccess?.(updated);
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
      const status = error?.response?.status;
      let serverMessage = error?.response?.data?.message;
      if (!serverMessage) {
        if (status === 403) serverMessage = "You do not have permission to modify staff status.";
        else if (status === 404) serverMessage = "Staff member not found or does not belong to your organization.";
        else if (status === 409) serverMessage = "Plan limit reached or account status conflict.";
        else if (status === 429) serverMessage = "Too many requests. Please try again later.";
        else if (status && status >= 500) serverMessage = "Something went wrong on the server. Please try again.";
        else serverMessage = error?.message || `Failed to ${isCurrentlyActive ? "deactivate" : "reactivate"} staff account.`;
      }
      setErrorMsg(serverMessage);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <div className="flex items-center gap-2 text-slate-900">
            <div
              className={`p-2 rounded-lg border ${
                isCurrentlyActive
                  ? "bg-rose-50 text-rose-700 border-rose-200"
                  : "bg-teal-50 text-[#0F766E] border-teal-200"
              }`}
            >
              {isCurrentlyActive ? (
                <PowerOff className="h-5 w-5" />
              ) : (
                <CheckCircle2 className="h-5 w-5" />
              )}
            </div>
            <DialogTitle className="text-xl font-bold text-slate-900">
              {isCurrentlyActive ? "Deactivate Staff Account" : "Reactivate Staff Account"}
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500 pt-1">
            {isCurrentlyActive
              ? `You are about to deactivate this lab staff account for ${staff.name} (${staff.email}).`
              : `Restore operational portal access for ${staff.name} (${staff.email}).`}
          </DialogDescription>
        </DialogHeader>

        {cannotReactivate && (
          <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-start gap-2">
            <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
            <div>
              <p className="font-semibold text-amber-900">No Active Staff Seats Available</p>
              <p className="mt-0.5 text-amber-700">
                No active staff seat is available. Upgrade your plan or deactivate another active staff member.
              </p>
            </div>
          </div>
        )}

        {errorMsg && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold text-rose-900">Operation Failed</p>
              <p className="mt-0.5 text-rose-700">{errorMsg}</p>
            </div>
          </div>
        )}

        <div className="py-2 text-xs text-slate-600 space-y-3">
          {isCurrentlyActive ? (
            <>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200 space-y-2.5">
                <p className="font-semibold text-slate-900 flex items-center gap-1.5">
                  <ShieldAlert className="h-4 w-4 text-rose-600" />
                  Consequences of deactivation:
                </p>
                <ul className="space-y-1.5 text-slate-600 pl-1">
                  <li className="flex items-start gap-2">
                    <span className="text-rose-500 font-bold">•</span>
                    <span>Staff will be immediately signed out.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-rose-500 font-bold">•</span>
                    <span>Existing access tokens will no longer be accepted.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-rose-500 font-bold">•</span>
                    <span>The staff member cannot log in again while inactive.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-rose-500 font-bold">•</span>
                    <span>They will lose access to reports, patients, and organization data.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-rose-500 font-bold">•</span>
                    <span>The account will remain retained for 10 days before automated permanent cleanup.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-emerald-600 font-bold">•</span>
                    <span>Historical reports will remain preserved.</span>
                  </li>
                </ul>
              </div>

              {/* Explicit confirmation checkbox */}
              <div className="flex items-start space-x-2 pt-1">
                <input
                  type="checkbox"
                  id="confirm-deactivate"
                  checked={confirmed}
                  onChange={(e) => setConfirmed(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-300 text-rose-600 focus:ring-rose-500 mt-0.5 cursor-pointer accent-rose-600"
                />
                <label
                  htmlFor="confirm-deactivate"
                  className="text-xs font-medium text-slate-700 leading-snug cursor-pointer select-none"
                >
                  I understand the consequences of deactivating this staff account.
                </label>
              </div>
            </>
          ) : (
            <div className="p-3.5 rounded-lg bg-teal-50/60 border border-teal-100 space-y-2">
              <p className="font-semibold text-slate-900">
                Reactivating {staff.name} ({staff.email}):
              </p>
              <ul className="list-disc list-inside space-y-1 text-slate-600 pl-1">
                <li>Restores their login access with existing credentials.</li>
                <li>Consumes 1 active staff license slot under your current plan.</li>
                <li>Restores access to prepare diagnostic reports for your organization.</li>
              </ul>
            </div>
          )}
        </div>

        <DialogFooter className="pt-2 gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            onClick={() => handleOpenChange(false)}
            disabled={isPending}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleConfirm}
            disabled={isPending || cannotReactivate || (isCurrentlyActive && !confirmed)}
            className={
              isCurrentlyActive
                ? "bg-rose-600 hover:bg-rose-700 text-white font-medium"
                : "bg-[#0F766E] hover:bg-[#115E59] text-white font-medium"
            }
          >
            {isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {isCurrentlyActive ? "Deactivating..." : "Reactivating..."}
              </>
            ) : isCurrentlyActive ? (
              "Deactivate Staff"
            ) : (
              "Confirm Reactivation"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
