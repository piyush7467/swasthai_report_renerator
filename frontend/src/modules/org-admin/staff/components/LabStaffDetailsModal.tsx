import {
  Calendar,
  CheckCircle2,
  FileCheck,
  FilePlus,
  LogIn,
  Mail,
  Shield,
  User,
  XCircle,
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
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useLabStaffDetailsQuery } from "../hooks/useLabStaff";

interface LabStaffDetailsModalProps {
  staffRefId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function LabStaffDetailsModal({
  staffRefId,
  open,
  onOpenChange,
}: LabStaffDetailsModalProps) {
  const { data: staff, isLoading, isError } = useLabStaffDetailsQuery(
    staffRefId ?? undefined
  );

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "Never";
    try {
      return new Date(isoString).toLocaleString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <div className="flex items-center gap-2 text-[#0F766E]">
            <div className="p-2 rounded-lg bg-teal-50 border border-teal-100">
              <User className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-xl font-bold text-slate-900">
                Staff Member Details
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Operational profile and diagnostic report activity history.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {isLoading ? (
          <div className="space-y-4 py-4">
            <Skeleton className="h-6 w-1/2" />
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        ) : isError || !staff ? (
          <div className="py-6 text-center text-sm text-slate-500">
            Failed to load staff details. Please try again.
          </div>
        ) : (
          <div className="space-y-5 py-2">
            {/* Header info */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-lg font-bold text-slate-900">{staff.name}</h3>
                <div className="flex items-center gap-1.5 mt-0.5 text-xs text-slate-500">
                  <Mail className="h-3.5 w-3.5" />
                  <span>{staff.email}</span>
                </div>
              </div>
              <div>
                {staff.status === "ACTIVE" ? (
                  <Badge
                    variant="outline"
                    className="bg-emerald-50 text-emerald-700 border-emerald-200 text-xs font-semibold px-2.5 py-0.5"
                  >
                    <CheckCircle2 className="h-3 w-3 mr-1" />
                    Active
                  </Badge>
                ) : (
                  <Badge
                    variant="outline"
                    className="bg-slate-100 text-slate-600 border-slate-200 text-xs font-semibold px-2.5 py-0.5"
                  >
                    <XCircle className="h-3 w-3 mr-1" />
                    Inactive
                  </Badge>
                )}
              </div>
            </div>

            {/* Diagnostic Activity Metrics */}
            <div>
              <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider block mb-2">
                Diagnostic Clinical Activity
              </span>
              <div className="grid grid-cols-2 gap-3">
                <div className="p-3.5 rounded-lg bg-teal-50/50 border border-teal-100/80">
                  <div className="flex items-center gap-2 text-teal-800">
                    <FilePlus className="h-4 w-4 text-[#0F766E]" />
                    <span className="text-xs font-medium">Reports Created</span>
                  </div>
                  <div className="mt-2 text-2xl font-bold text-teal-950">
                    {staff.reportsCreated}
                  </div>
                  <span className="text-[11px] text-teal-700">
                    Drafted & initiated
                  </span>
                </div>

                <div className="p-3.5 rounded-lg bg-teal-50/50 border border-teal-100/80">
                  <div className="flex items-center gap-2 text-teal-800">
                    <FileCheck className="h-4 w-4 text-[#0F766E]" />
                    <span className="text-xs font-medium">Reports Finalized</span>
                  </div>
                  <div className="mt-2 text-2xl font-bold text-teal-950">
                    {staff.reportsFinalized}
                  </div>
                  <span className="text-[11px] text-teal-700">
                    Signed & completed
                  </span>
                </div>
              </div>
            </div>

            {/* Account Metadata Details */}
            <div>
              <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider block mb-2">
                Account & Security Info
              </span>
              <div className="rounded-lg border border-slate-200 divide-y divide-slate-100 text-xs">
                <div className="flex justify-between items-center p-2.5">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <Shield className="h-3.5 w-3.5" />
                    Assigned Role
                  </span>
                  <span className="font-semibold text-slate-800">
                    Laboratory Staff (LAB_STAFF)
                  </span>
                </div>

                <div className="flex justify-between items-center p-2.5">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <LogIn className="h-3.5 w-3.5" />
                    Last Login
                  </span>
                  <span className="text-slate-700 font-medium">
                    {formatDate(staff.lastLoginAt)}
                  </span>
                </div>

                <div className="flex justify-between items-center p-2.5">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5" />
                    Created On
                  </span>
                  <span className="text-slate-700 font-medium">
                    {formatDate(staff.createdAt)}
                  </span>
                </div>

                <div className="flex justify-between items-center p-2.5">
                  <span className="text-slate-500">Account Reference ID</span>
                  <span className="font-mono text-[11px] text-slate-600">
                    {staff.refId}
                  </span>
                </div>

                {staff.status === "INACTIVE" && (
                  <>
                    <div className="flex justify-between items-center p-2.5 bg-slate-50/60">
                      <span className="text-slate-500">Deactivated At</span>
                      <span className="text-slate-700 font-medium">
                        {formatDate(staff.inactiveAt)}
                      </span>
                    </div>

                    {staff.deactivatedByName && (
                      <div className="flex justify-between items-center p-2.5 bg-slate-50/60">
                        <span className="text-slate-500">Deactivated By</span>
                        <span className="text-slate-700 font-medium">
                          {staff.deactivatedByName}
                        </span>
                      </div>
                    )}

                    <div className="flex justify-between items-center p-2.5 bg-slate-50/60">
                      <span className="text-slate-500">Permanent Cleanup</span>
                      <span className="text-slate-700 font-medium">
                        {staff.eligibleForCleanupAt
                          ? formatDate(staff.eligibleForCleanupAt)
                          : "Scheduled (10-day retention)"}
                      </span>
                    </div>

                    <div className="flex justify-between items-center p-2.5 bg-slate-50/60">
                      <span className="text-slate-500">Retention Status</span>
                      <span className="font-medium text-xs">
                        {staff.cleanupEligible ? (
                          <span className="text-rose-600 font-semibold">
                            Eligible for permanent deletion
                          </span>
                        ) : (
                          <span className="text-amber-700">
                            Protected within 10-day safety grace period
                          </span>
                        )}
                      </span>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        <DialogFooter className="pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
          >
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
