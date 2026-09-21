import {
  AlertTriangle,
  CheckCircle2,
  FileCheck2,
  Loader2,
  Lock,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import type { ReportResponse } from "../types/reportTypes";

interface FinalizeReportDialogProps {
  report: ReportResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void>;
  isLoading: boolean;
}

export function FinalizeReportDialog({
  report,
  open,
  onOpenChange,
  onConfirm,
  isLoading,
}: FinalizeReportDialogProps) {
  // Check for any empty manual parameters
  const emptyManualParams: { testName: string; paramName: string }[] = [];
  report.tests.forEach((test) => {
    test.parameters.forEach((param) => {
      if (param.inputType === "MANUAL" && (!param.value || param.value.trim() === "")) {
        emptyManualParams.push({ testName: test.testName, paramName: param.parameterName });
      }
    });
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600">
              <FileCheck2 className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Finalize Diagnostic Report
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Sign off and commit immutable historical medical snapshots.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-3 py-1 text-xs">
          {emptyManualParams.length > 0 && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle className="text-xs font-semibold">
                Incomplete Parameters ({emptyManualParams.length})
              </AlertTitle>
              <AlertDescription className="text-[11px] mt-1 space-y-1">
                <p>The following manual parameters currently have no recorded values:</p>
                <ul className="list-disc list-inside max-h-24 overflow-y-auto">
                  {emptyManualParams.slice(0, 5).map((p, idx) => (
                    <li key={idx} className="truncate">
                      <span className="font-medium">{p.testName}</span>: {p.paramName}
                    </li>
                  ))}
                  {emptyManualParams.length > 5 && (
                    <li>...and {emptyManualParams.length - 5} more</li>
                  )}
                </ul>
                <p className="font-semibold text-rose-800 pt-1">
                  Required parameters cannot be blank. Finalization may be rejected by the server.
                </p>
              </AlertDescription>
            </Alert>
          )}

          <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3 space-y-2 text-slate-600">
            <div className="flex items-center gap-2 text-slate-900 font-semibold text-xs">
              <Lock className="h-3.5 w-3.5 text-slate-500" />
              Permanent Snapshot Operations:
            </div>
            <ul className="list-disc list-inside space-y-1 text-[11px]">
              <li>Patient name, current age, and medical code become permanently frozen.</li>
              <li>Organization branding, footer disclaimer, and signature owner are snapshotted.</li>
              <li>All calculation formulas (e.g. MCV, MCH, MCHC) and flags are recalculated and locked.</li>
              <li>Authorized PDF generation will be unlocked immediately upon finalization.</li>
            </ul>
          </div>

          <p className="text-[11px] text-amber-700 font-medium">
            Warning: This action cannot be reversed. Once finalized, no tests or parameters can be added, deleted, or modified.
          </p>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
            className="text-xs"
          >
            Go Back & Review
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={onConfirm}
            disabled={isLoading}
            className="text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-semibold"
          >
            {isLoading ? (
              <>
                <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                Finalizing Report...
              </>
            ) : (
              <>
                <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />
                Confirm & Finalize
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
