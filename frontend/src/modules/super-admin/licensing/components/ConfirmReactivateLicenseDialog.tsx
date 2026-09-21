import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { CheckCircle2, Loader2, PlayCircle } from "lucide-react";
import type { LicenseResponse } from "../types/licensingTypes";
import { getLicenseExpiryInfo } from "../types/licensingTypes";

interface ConfirmReactivateLicenseDialogProps {
  license: LicenseResponse | null;
  organizationName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void> | void;
  isLoading?: boolean;
}

export function ConfirmReactivateLicenseDialog({
  license,
  organizationName,
  open,
  onOpenChange,
  onConfirm,
  isLoading = false,
}: ConfirmReactivateLicenseDialogProps) {
  if (!license) return null;

  const expiryInfo = getLicenseExpiryInfo(license.expiresAt, license.startedAt, license.status);
  const remainingDays = Math.max(0, expiryInfo?.daysLeft ?? 0);

  // Projected new expiration date starting from today
  const projectedExpiryDate = (() => {
    const projected = new Date();
    projected.setDate(projected.getDate() + remainingDays);
    return projected.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  })();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 sm:mx-0">
            <PlayCircle className="h-6 w-6" />
          </div>
          <DialogTitle className="text-base font-semibold text-slate-900 mt-2">
            Reactivate Organization License
          </DialogTitle>
          <DialogDescription className="text-xs text-slate-600 leading-relaxed">
            Resume the frozen subscription for{" "}
            <strong className="text-slate-900">
              {organizationName || license.organizationRefId}
            </strong>
            .
          </DialogDescription>
        </DialogHeader>

        <div className="rounded-lg border border-emerald-200 bg-emerald-50/60 p-3.5 space-y-2 text-xs text-emerald-950">
          <div className="flex items-center gap-1.5 font-semibold text-emerald-900">
            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>Validity Resumption Details:</span>
          </div>
          <ul className="list-disc list-inside space-y-1 text-[11px] text-emerald-800 leading-relaxed pl-1">
            <li>
              <strong>Unlocks Reporting:</strong> Laboratory personnel can immediately generate and sign diagnostic patient reports.
            </li>
            <li>
              <strong>{remainingDays} Days Restored:</strong> The license resumes from today with all {remainingDays} remaining frozen days intact.
            </li>
            <li>
              <strong>New Expiry Date:</strong> Valid from today until{" "}
              <strong className="font-mono text-emerald-950">{projectedExpiryDate}</strong>.
            </li>
          </ul>
        </div>

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
            onClick={async () => {
              await onConfirm();
              onOpenChange(false);
            }}
            disabled={isLoading}
            className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Reactivating...
              </>
            ) : (
              <>
                <PlayCircle className="h-4 w-4" />
                Yes, Reactivate License
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ConfirmReactivateLicenseDialog;
