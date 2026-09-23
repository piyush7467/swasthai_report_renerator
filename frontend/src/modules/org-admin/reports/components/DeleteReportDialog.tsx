import {
  AlertTriangle,
  Calendar,
  FileText,
  Loader2,
  Trash2,
  User,
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
import { ReportStatusBadge } from "./ReportStatusBadge";
import type { ReportResponse } from "../types/reportTypes";

interface DeleteReportDialogProps {
  report: ReportResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void>;
  isLoading: boolean;
}

export function DeleteReportDialog({
  report,
  open,
  onOpenChange,
  onConfirm,
  isLoading,
}: DeleteReportDialogProps) {
  if (!report) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-slate-200">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-rose-50 text-rose-600 border border-rose-100">
              <Trash2 className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Delete Diagnostic Report
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono mt-0.5">
                Ref ID: #{report.refId}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-3.5 py-1 text-xs">
          {/* Summary Box */}
          <div className="rounded-lg border border-slate-200 bg-slate-50/70 p-3 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 flex items-center gap-1.5">
                <User className="h-3.5 w-3.5 text-slate-400" />
                Patient
              </span>
              <span className="font-semibold text-slate-800">
                {report.patientName || report.patientRefId}
              </span>
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 flex items-center gap-1.5">
                <FileText className="h-3.5 w-3.5 text-slate-400" />
                Status
              </span>
              <ReportStatusBadge status={report.status} />
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-slate-400" />
                Created
              </span>
              <span className="text-slate-700 font-medium">
                {new Date(report.createdAt).toLocaleDateString("en-IN", {
                  day: "2-digit",
                  month: "short",
                  year: "numeric",
                })}
              </span>
            </div>
          </div>

          <p className="text-slate-600 text-xs leading-relaxed">
            Are you sure you want to soft-delete this report? Only Organization Administrators have permission to delete reports.
          </p>

          <div className="rounded-lg border border-amber-200 bg-amber-50/80 p-3 text-[11px] text-amber-900 space-y-1.5">
            <div className="flex items-center gap-1.5 font-semibold text-amber-950">
              <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
              Clinical Retention Compliance Policy
            </div>
            <p className="text-amber-800 leading-relaxed">
              In accordance with laboratory data retention regulations, reports can only be deleted once they have satisfied the mandatory retention period (5 days from creation). Recent reports will be protected from deletion.
            </p>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0 pt-2 border-t border-slate-100">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
            className="text-xs h-9 border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant="destructive"
            size="sm"
            onClick={onConfirm}
            disabled={isLoading}
            className="text-xs h-9 bg-rose-600 hover:bg-rose-700 text-white font-semibold shadow-xs"
          >
            {isLoading ? (
              <>
                <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                Deleting...
              </>
            ) : (
              <>
                <Trash2 className="mr-1.5 h-3.5 w-3.5" />
                Confirm Deletion
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
