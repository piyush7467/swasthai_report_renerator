import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { CheckCircle2, XCircle, ShieldAlert } from "lucide-react";
import type { SecurityAuditLog } from "../types/analyticsTypes";

interface AuditDetailsDialogProps {
  log: SecurityAuditLog | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function AuditDetailsDialog({
  log,
  open,
  onOpenChange,
}: AuditDetailsDialogProps) {
  if (!log) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-indigo-600" />
            <DialogTitle className="text-base font-semibold text-slate-900">
              Security Audit Record Details
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            Immutable platform audit trail recorded at {new Date(log.createdAt).toLocaleString()}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 text-xs pt-2">
          {/* Action & Status */}
          <div className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50 p-3">
            <div>
              <p className="text-[10px] font-medium uppercase text-slate-400">Action</p>
              <p className="mt-0.5 font-mono font-bold text-slate-800">{log.action}</p>
            </div>
            <div>
              {log.success ? (
                <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200 flex items-center gap-1">
                  <CheckCircle2 className="h-3 w-3" />
                  Granted / Success
                </Badge>
              ) : (
                <Badge variant="destructive" className="flex items-center gap-1">
                  <XCircle className="h-3 w-3" />
                  Denied / Failed
                </Badge>
              )}
            </div>
          </div>

          {/* Details Grid */}
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-lg border border-slate-100 p-2.5">
              <span className="text-[10px] font-medium text-slate-400 uppercase">Audit Ref ID</span>
              <p className="font-mono font-semibold text-slate-800 mt-0.5">{log.refId}</p>
            </div>

            <div className="rounded-lg border border-slate-100 p-2.5">
              <span className="text-[10px] font-medium text-slate-400 uppercase">Actor Email</span>
              <p className="font-semibold text-slate-800 mt-0.5 truncate">{log.actorEmail}</p>
            </div>

            <div className="rounded-lg border border-slate-100 p-2.5">
              <span className="text-[10px] font-medium text-slate-400 uppercase">Target Organization</span>
              <p className="font-semibold text-slate-800 mt-0.5 truncate">
                {log.targetOrganizationName || log.targetOrganizationRefId || "Global"}
              </p>
              {log.targetOrganizationRefId && (
                <p className="text-[10px] font-mono text-slate-500">{log.targetOrganizationRefId}</p>
              )}
            </div>

            <div className="rounded-lg border border-slate-100 p-2.5">
              <span className="text-[10px] font-medium text-slate-400 uppercase">Target Report Ref ID</span>
              <p className="font-mono font-semibold text-slate-800 mt-0.5">
                {log.targetReportRefId || "N/A"}
              </p>
            </div>

            <div className="rounded-lg border border-slate-100 p-2.5">
              <span className="text-[10px] font-medium text-slate-400 uppercase">Client IP Address</span>
              <p className="font-mono font-semibold text-slate-800 mt-0.5">
                {log.ipAddress || "Unknown"}
              </p>
            </div>

            <div className="rounded-lg border border-slate-100 p-2.5">
              <span className="text-[10px] font-medium text-slate-400 uppercase">Timestamp (ISO)</span>
              <p className="font-mono text-[11px] text-slate-700 mt-0.5">
                {new Date(log.createdAt).toLocaleString()}
              </p>
            </div>
          </div>

          {/* Justification */}
          <div className="rounded-lg border border-slate-200 bg-amber-50/50 p-3 space-y-1">
            <span className="text-[10px] font-bold text-amber-800 uppercase tracking-wide">
              Mandatory Justification Provided
            </span>
            <p className="text-xs text-slate-800 leading-relaxed italic">
              &ldquo;{log.justification}&rdquo;
            </p>
          </div>

          {/* Failure Reason if any */}
          {log.failureReason && (
            <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 space-y-1">
              <span className="text-[10px] font-bold text-rose-800 uppercase tracking-wide">
                Failure / Denial Reason
              </span>
              <p className="text-xs text-rose-700">{log.failureReason}</p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
