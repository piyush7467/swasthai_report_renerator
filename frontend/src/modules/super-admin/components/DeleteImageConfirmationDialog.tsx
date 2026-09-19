import { AlertTriangle, Loader2 } from "lucide-react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface DeleteImageConfirmationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  type: "logo" | "signature";
  organizationName: string;
  onConfirm: () => Promise<void> | void;
  isDeleting: boolean;
}

export function DeleteImageConfirmationDialog({
  open,
  onOpenChange,
  type,
  organizationName,
  onConfirm,
  isDeleting,
}: DeleteImageConfirmationDialogProps) {
  const itemLabel = type === "logo" ? "organization logo" : "authorized signature";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-rose-100 text-rose-600">
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-semibold text-slate-900">
                Delete {type === "logo" ? "Organization Logo" : "Authorized Signature"}
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                {organizationName}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="py-2 text-sm text-slate-600 space-y-2">
          <p>
            Are you sure you want to permanently delete the {itemLabel}?
          </p>
          <p className="text-xs text-slate-500 bg-slate-50 p-2.5 rounded border border-slate-200">
            Once removed, newly generated reports and digital previews for this organization will no longer include this specimen until a replacement is uploaded.
          </p>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isDeleting}
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Deleting...
              </>
            ) : (
              `Delete ${type === "logo" ? "Logo" : "Signature"}`
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
