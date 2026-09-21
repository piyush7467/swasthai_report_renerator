import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { AlertTriangle, Loader2, PauseCircle } from "lucide-react";
import type { LicenseResponse } from "../types/licensingTypes";
import { getLicenseExpiryInfo } from "../types/licensingTypes";

interface ConfirmDeactivateLicenseDialogProps {
  license: LicenseResponse | null;
  organizationName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void> | void;
  isLoading?: boolean;
}

export function ConfirmDeactivateLicenseDialog({
  license,
  organizationName,
  open,
  onOpenChange,
  onConfirm,
  isLoading = false,
}: ConfirmDeactivateLicenseDialogProps) {
  if (!license) return null;

  const expiryInfo = getLicenseExpiryInfo(license.expiresAt, license.startedAt);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600 sm:mx-0">
            <PauseCircle className="h-6 w-6" />
          </div>
          <DialogTitle className="text-base font-semibold text-slate-900 mt-2">
            Deactivate & Freeze License
          </DialogTitle>
          <DialogDescription className="text-xs text-slate-600 leading-relaxed space-y-2">
            <span>
              Are you sure you want to deactivate the subscription license for{" "}
              <strong className="text-slate-900">
                {organizationName || license.organizationRefId}
              </strong>
              ?
            </span>
          </DialogDescription>
        </DialogHeader>

        <div className="rounded-lg border border-amber-200 bg-amber-50/70 p-3.5 space-y-2 text-xs text-amber-900">
          <div className="flex items-center gap-1.5 font-semibold text-amber-950">
            <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
            <span>Important License Freeze Rules:</span>
          </div>
          <ul className="list-disc list-inside space-y-1 text-[11px] text-amber-800 leading-relaxed pl-1">
            <li>
              <strong>Immediate Suspension:</strong> Laboratory staff in this organization will be immediately blocked from creating diagnostic reports.
            </li>
            <li>
              <strong>Remaining Days Frozen:</strong> Currently{" "}
              <strong>{expiryInfo?.daysLeft ?? 0} days remain</strong> on this plan. These days will be safely frozen and will NOT tick away while paused.
            </li>
            <li>
              <strong>Fair Resumption:</strong> When you reactivate this license, the full remaining {expiryInfo?.daysLeft ?? 0} days will resume starting from that reactivation date.
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
            variant="destructive"
            onClick={async () => {
              await onConfirm();
              onOpenChange(false);
            }}
            disabled={isLoading}
            className="bg-amber-600 hover:bg-amber-700 text-white gap-1.5"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Deactivating...
              </>
            ) : (
              <>
                <PauseCircle className="h-4 w-4" />
                Yes, Deactivate License
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ConfirmDeactivateLicenseDialog;
