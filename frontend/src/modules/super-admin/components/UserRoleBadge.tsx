import { ShieldCheck, Building2, FlaskConical } from "lucide-react";
import type { UserRole } from "../types/userTypes";
import { cn } from "@/lib/utils";

interface UserRoleBadgeProps {
  role: UserRole;
  className?: string;
}

export function UserRoleBadge({ role, className }: UserRoleBadgeProps) {
  switch (role) {
    case "SUPER_ADMIN":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-purple-200 bg-purple-50 px-2.5 py-0.5 text-xs font-medium text-purple-700",
            className,
          )}
        >
          <ShieldCheck className="h-3.5 w-3.5 text-purple-600" />
          Super Admin
        </span>
      );

    case "ORG_ADMIN":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-sky-200 bg-sky-50 px-2.5 py-0.5 text-xs font-medium text-sky-700",
            className,
          )}
        >
          <Building2 className="h-3.5 w-3.5 text-sky-600" />
          Org Admin
        </span>
      );

    case "LAB_STAFF":
      return (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-full border border-teal-200 bg-teal-50 px-2.5 py-0.5 text-xs font-medium text-teal-700",
            className,
          )}
        >
          <FlaskConical className="h-3.5 w-3.5 text-teal-600" />
          Lab Staff
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
          {role}
        </span>
      );
  }
}
