import { CheckCircle2, AlertTriangle, MinusCircle } from "lucide-react";
import type { UserStatus } from "../types/userTypes";
import { cn } from "@/lib/utils";

interface UserStatusBadgeProps {
  status: UserStatus;
  className?: string;
}

export function UserStatusBadge({
  status,
  className,
}: UserStatusBadgeProps) {
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

    case "INACTIVE":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700",
            className,
          )}
        >
          <MinusCircle className="h-3.5 w-3.5 text-slate-500" />
          Inactive
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
