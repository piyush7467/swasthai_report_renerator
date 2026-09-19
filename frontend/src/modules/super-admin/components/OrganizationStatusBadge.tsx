import { CheckCircle2, AlertTriangle, Ban } from "lucide-react";
import type { OrganizationStatus } from "../types/organizationTypes";
import { cn } from "@/lib/utils";

interface OrganizationStatusBadgeProps {
  status: OrganizationStatus;
  className?: string;
}

export function OrganizationStatusBadge({
  status,
  className,
}: OrganizationStatusBadgeProps) {
  switch (status) {
    case "ACTIVE":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700",
            className,
          )}
        >
          <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
          Active
        </span>
      );

    case "SUSPENDED":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-700",
            className,
          )}
        >
          <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
          Suspended
        </span>
      );

    case "DISABLED":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-700",
            className,
          )}
        >
          <Ban className="h-3.5 w-3.5 text-rose-500" />
          Disabled
        </span>
      );

    default:
      return (
        <span
          className={cn(
            "inline-flex items-center rounded-full border border-slate-200 bg-slate-50 px-2.5 py-0.5 text-xs font-medium text-slate-700",
            className,
          )}
        >
          {status}
        </span>
      );
  }
}
