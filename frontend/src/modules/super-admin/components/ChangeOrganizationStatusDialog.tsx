import { useState } from "react";
import axios from "axios";
import { AlertCircle, AlertTriangle, CheckCircle2, Ban, Loader2, ShieldAlert } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import type {
  OrganizationResponse,
  OrganizationStatus,
} from "../types/organizationTypes";
import { useUpdateOrganizationStatusMutation } from "../hooks/useOrganizations";

interface ChangeOrganizationStatusDialogProps {
  organization: OrganizationResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ChangeOrganizationStatusDialog({
  organization,
  open,
  onOpenChange,
}: ChangeOrganizationStatusDialogProps) {
  const [selectedStatus, setSelectedStatus] = useState<OrganizationStatus | null>(
    null,
  );
  const [confirmDisabled, setConfirmDisabled] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const statusMutation = useUpdateOrganizationStatusMutation();

  const handleOpenChange = (nextOpen: boolean) => {
    if (!statusMutation.isPending) {
      if (!nextOpen) {
        setSelectedStatus(null);
        setConfirmDisabled(false);
        setErrorMessage(null);
      }
      onOpenChange(nextOpen);
    }
  };

  if (!organization) return null;

  const currentStatus = organization.status;
  const isTerminal = currentStatus === "DISABLED";

  const getAvailableTransitions = (): OrganizationStatus[] => {
    if (currentStatus === "ACTIVE") {
      return ["SUSPENDED", "DISABLED"];
    }
    if (currentStatus === "SUSPENDED") {
      return ["ACTIVE", "DISABLED"];
    }
    return [];
  };

  const availableTransitions = getAvailableTransitions();

  const handleConfirm = async () => {
    if (!selectedStatus || isTerminal) return;
    if (selectedStatus === "DISABLED" && !confirmDisabled) return;

    setErrorMessage(null);
    try {
      await statusMutation.mutateAsync({
        refId: organization.refId,
        data: {
          status: selectedStatus,
        },
      });
      onOpenChange(false);
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const apiError = error.response?.data;
        if (apiError) {
          setErrorMessage(
            apiError.message || "Failed to update organization status.",
          );
          return;
        }
      }
      setErrorMessage(
        "Unable to connect to the server. Please check your connection and try again.",
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-100 text-amber-700">
              <ShieldAlert className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-slate-900">
                Update Organization Status
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono">
                {organization.name} ({organization.code})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {isTerminal ? (
          <div className="space-y-4 py-2">
            <Alert variant="destructive" className="border-rose-200 bg-rose-50 py-3 text-rose-800">
              <Ban className="h-4 w-4 text-rose-600" />
              <AlertTitle className="text-xs font-semibold">Terminal State</AlertTitle>
              <AlertDescription className="text-xs text-rose-700">
                This organization is permanently DISABLED. The backend does not permit
                status transitions from a disabled state to prevent unauthorized identity reuse.
              </AlertDescription>
            </Alert>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Close
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="space-y-4 py-2">
            {errorMessage && (
              <Alert variant="destructive" className="border-red-200 bg-red-50 py-2.5 text-red-800">
                <AlertCircle className="h-4 w-4 text-red-600" />
                <AlertTitle className="text-xs font-semibold">Error</AlertTitle>
                <AlertDescription className="text-xs text-red-700">
                  {errorMessage}
                </AlertDescription>
              </Alert>
            )}

            <div className="rounded-lg border border-slate-100 bg-slate-50 p-3 text-xs text-slate-600 space-y-1">
              <p>
                <span className="font-medium text-slate-700">Current Status:</span>{" "}
                <span className="font-semibold text-slate-900">{currentStatus}</span>
              </p>
              <p className="text-[11px] text-slate-500">
                Choose a new operational state according to laboratory compliance guidelines.
              </p>
            </div>

            {/* Transition Options */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold text-slate-700">
                Select Target Status:
              </Label>
              <div className="grid grid-cols-1 gap-2">
                {availableTransitions.map((status) => {
                  const isSelected = selectedStatus === status;
                  return (
                    <button
                      key={status}
                      type="button"
                      onClick={() => setSelectedStatus(status)}
                      disabled={statusMutation.isPending}
                      className={`flex items-start gap-3 rounded-lg border p-3 text-left transition ${
                        isSelected
                          ? status === "DISABLED"
                            ? "border-rose-300 bg-rose-50/50 ring-2 ring-rose-200"
                            : status === "SUSPENDED"
                            ? "border-amber-300 bg-amber-50/50 ring-2 ring-amber-200"
                            : "border-emerald-300 bg-emerald-50/50 ring-2 ring-emerald-200"
                          : "border-slate-200 bg-white hover:bg-slate-50"
                      }`}
                    >
                      {status === "ACTIVE" && (
                        <CheckCircle2 className="mt-0.5 h-4 w-4 text-emerald-600 shrink-0" />
                      )}
                      {status === "SUSPENDED" && (
                        <AlertTriangle className="mt-0.5 h-4 w-4 text-amber-600 shrink-0" />
                      )}
                      {status === "DISABLED" && (
                        <Ban className="mt-0.5 h-4 w-4 text-rose-600 shrink-0" />
                      )}
                      <div>
                        <p className="text-sm font-semibold text-slate-900">
                          {status === "ACTIVE" && "Reactivate (ACTIVE)"}
                          {status === "SUSPENDED" && "Suspend (SUSPENDED)"}
                          {status === "DISABLED" && "Disable Permanently (DISABLED)"}
                        </p>
                        <p className="text-xs text-slate-500">
                          {status === "ACTIVE" &&
                            "Restore full laboratory access and reporting workflows."}
                          {status === "SUSPENDED" &&
                            "Temporarily pause laboratory logins and report finalization."}
                          {status === "DISABLED" &&
                            "Permanently revoke laboratory operations. This action cannot be undone."}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Permanent Destruction Warning if DISABLED is selected */}
            {selectedStatus === "DISABLED" && (
              <div className="rounded-lg border border-rose-200 bg-rose-50/70 p-3.5 space-y-2">
                <div className="flex items-center gap-2 text-rose-800">
                  <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0" />
                  <p className="text-xs font-semibold">Irreversible Administrative Action</p>
                </div>
                <p className="text-xs text-rose-700 leading-relaxed">
                  Disabling an organization marks it permanently terminal. All associated
                  user accounts will immediately be barred from signing in.
                </p>
                <label className="flex items-center gap-2 pt-1 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={confirmDisabled}
                    onChange={(e) => setConfirmDisabled(e.target.checked)}
                    className="h-4 w-4 rounded border-rose-300 text-rose-600 focus:ring-rose-500"
                  />
                  <span className="text-xs font-medium text-rose-900 select-none">
                    I understand this action is permanent and cannot be undone
                  </span>
                </label>
              </div>
            )}

            <DialogFooter className="pt-3">
              <Button
                type="button"
                variant="outline"
                disabled={statusMutation.isPending}
                onClick={() => onOpenChange(false)}
              >
                Cancel
              </Button>
              <Button
                type="button"
                disabled={
                  !selectedStatus ||
                  statusMutation.isPending ||
                  (selectedStatus === "DISABLED" && !confirmDisabled)
                }
                className={
                  selectedStatus === "DISABLED"
                    ? "bg-rose-600 text-white hover:bg-rose-700"
                    : selectedStatus === "SUSPENDED"
                    ? "bg-amber-600 text-white hover:bg-amber-700"
                    : "bg-emerald-600 text-white hover:bg-emerald-700"
                }
                onClick={() => void handleConfirm()}
              >
                {statusMutation.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : (
                  `Confirm ${selectedStatus ?? "Status"}`
                )}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
