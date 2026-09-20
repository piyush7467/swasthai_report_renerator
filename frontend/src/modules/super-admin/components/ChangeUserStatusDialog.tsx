import { useState } from "react";
import axios from "axios";
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  MinusCircle,
  ShieldAlert,
} from "lucide-react";

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
import type { UserResponse, UserStatus } from "../types/userTypes";
import { useUpdateUserStatusMutation } from "../hooks/useUsers";

interface ChangeUserStatusDialogProps {
  user: UserResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ChangeUserStatusDialog({
  user,
  open,
  onOpenChange,
}: ChangeUserStatusDialogProps) {
  const [selectedStatus, setSelectedStatus] = useState<UserStatus | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const statusMutation = useUpdateUserStatusMutation();

  const handleOpenChange = (nextOpen: boolean) => {
    if (!statusMutation.isPending) {
      if (!nextOpen) {
        setSelectedStatus(null);
        setErrorMessage(null);
      }
      onOpenChange(nextOpen);
    }
  };

  if (!user) return null;

  const currentStatus = user.status;
  const isProtectedSuperAdmin = user.role === "SUPER_ADMIN";

  const allStatuses: UserStatus[] = ["ACTIVE", "INACTIVE", "SUSPENDED"];
  const availableTransitions = allStatuses.filter((s) => s !== currentStatus);

  const handleConfirm = async () => {
    if (!selectedStatus || isProtectedSuperAdmin) return;

    setErrorMessage(null);
    try {
      await statusMutation.mutateAsync({
        refId: user.refId,
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
            apiError.message || "Failed to update user status.",
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
                Update User Status
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono">
                {user.name} ({user.refId})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {isProtectedSuperAdmin ? (
          <div className="space-y-4 py-2">
            <Alert className="border-purple-200 bg-purple-50 text-purple-900">
              <ShieldAlert className="h-4 w-4 text-purple-700" />
              <AlertTitle className="text-xs font-semibold">
                Protected Account
              </AlertTitle>
              <AlertDescription className="text-xs text-purple-800 leading-relaxed">
                The SUPER_ADMIN account cannot have its operational status altered.
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
              <Alert
                variant="destructive"
                className="border-red-200 bg-red-50 py-2.5 text-red-800"
              >
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
                Selecting a new status immediately impacts account authentication capability.
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
                          ? status === "ACTIVE"
                            ? "border-emerald-300 bg-emerald-50/50 ring-2 ring-emerald-200"
                            : status === "SUSPENDED"
                            ? "border-amber-300 bg-amber-50/50 ring-2 ring-amber-200"
                            : "border-slate-300 bg-slate-100 ring-2 ring-slate-200"
                          : "border-slate-200 bg-white hover:bg-slate-50"
                      }`}
                    >
                      {status === "ACTIVE" && (
                        <CheckCircle2 className="mt-0.5 h-4 w-4 text-emerald-600 shrink-0" />
                      )}
                      {status === "INACTIVE" && (
                        <MinusCircle className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
                      )}
                      {status === "SUSPENDED" && (
                        <AlertTriangle className="mt-0.5 h-4 w-4 text-amber-600 shrink-0" />
                      )}
                      <div>
                        <p className="text-sm font-semibold text-slate-900">
                          {status === "ACTIVE" && "Reactivate (ACTIVE)"}
                          {status === "INACTIVE" && "Deactivate (INACTIVE)"}
                          {status === "SUSPENDED" && "Suspend (SUSPENDED)"}
                        </p>
                        <p className="text-xs text-slate-500">
                          {status === "ACTIVE" &&
                            "User can sign in and perform diagnostic report workflows."}
                          {status === "INACTIVE" &&
                            "Account is disabled. User cannot log in until reactivated."}
                          {status === "SUSPENDED" &&
                            "User session is immediately blocked due to administrative hold."}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

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
                disabled={!selectedStatus || statusMutation.isPending}
                className={
                  selectedStatus === "ACTIVE"
                    ? "bg-emerald-600 text-white hover:bg-emerald-700"
                    : selectedStatus === "SUSPENDED"
                    ? "bg-amber-600 text-white hover:bg-amber-700"
                    : "bg-slate-700 text-white hover:bg-slate-800"
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
