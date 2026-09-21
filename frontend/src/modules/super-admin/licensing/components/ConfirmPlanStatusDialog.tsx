import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import type { PlanResponse } from "../types/licensingTypes";

interface ConfirmPlanStatusDialogProps {
  plan: PlanResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void> | void;
  isLoading?: boolean;
}

export function ConfirmPlanStatusDialog({
  plan,
  open,
  onOpenChange,
  onConfirm,
  isLoading = false,
}: ConfirmPlanStatusDialogProps) {
  if (!plan) return null;
  const isDeactivating = plan.active;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div
            className={`mx-auto flex h-12 w-12 items-center justify-center rounded-full sm:mx-0 ${
              isDeactivating
                ? "bg-amber-100 text-amber-600"
                : "bg-emerald-100 text-emerald-600"
            }`}
          >
            {isDeactivating ? (
              <AlertTriangle className="h-6 w-6" />
            ) : (
              <CheckCircle2 className="h-6 w-6" />
            )}
          </div>
          <DialogTitle className="text-base font-semibold text-slate-900 mt-2">
            {isDeactivating
              ? "Deactivate Subscription Plan"
              : "Activate Subscription Plan"}
          </DialogTitle>
          <DialogDescription className="text-xs text-slate-600 leading-relaxed">
            {isDeactivating ? (
              <>
                Are you sure you want to deactivate{" "}
                <span className="font-semibold text-slate-900">
                  {plan.name}
                </span>{" "}
                (<span className="font-mono">{plan.code}</span>)?
                <br />
                <br />
                While deactivated, healthcare organizations cannot be newly
                licensed or renewed with this plan. Existing active licenses
                assigned to this plan will remain valid until their expiration.
              </>
            ) : (
              <>
                Are you sure you want to activate{" "}
                <span className="font-semibold text-slate-900">
                  {plan.name}
                </span>{" "}
                (<span className="font-mono">{plan.code}</span>)?
                <br />
                <br />
                Once activated, this plan will immediately become available for
                assignment and renewals across all healthcare tenant organizations.
              </>
            )}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="mt-4 flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant={isDeactivating ? "destructive" : "default"}
            onClick={async () => {
              await onConfirm();
              onOpenChange(false);
            }}
            disabled={isLoading}
            className={
              !isDeactivating
                ? "bg-emerald-600 hover:bg-emerald-700 text-white"
                : ""
            }
          >
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isDeactivating ? "Yes, Deactivate Plan" : "Yes, Activate Plan"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ConfirmPlanStatusDialog;
