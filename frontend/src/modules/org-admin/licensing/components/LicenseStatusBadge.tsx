import { Badge } from "@/components/ui/badge";
import { CheckCircle2, AlertTriangle, XCircle, PauseCircle, Clock } from "lucide-react";
import type { LicenseStatus } from "../types/licenseTypes";

interface LicenseStatusBadgeProps {
  status: LicenseStatus | "EXPIRING_SOON" | "OVER_LIMIT";
  className?: string;
}

export function LicenseStatusBadge({ status, className }: LicenseStatusBadgeProps) {
  switch (status) {
    case "ACTIVE":
      return (
        <Badge
          variant="outline"
          className={`bg-emerald-50 text-emerald-700 border-emerald-200 font-medium gap-1 text-xs px-2.5 py-0.5 ${className ?? ""}`}
        >
          <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
          Active
        </Badge>
      );
    case "EXPIRING_SOON":
      return (
        <Badge
          variant="outline"
          className={`bg-amber-50 text-amber-700 border-amber-200 font-semibold gap-1 text-xs px-2.5 py-0.5 ${className ?? ""}`}
        >
          <Clock className="h-3.5 w-3.5 text-amber-600" />
          Expiring Soon
        </Badge>
      );
    case "EXPIRED":
      return (
        <Badge
          variant="outline"
          className={`bg-rose-50 text-rose-700 border-rose-200 font-semibold gap-1 text-xs px-2.5 py-0.5 ${className ?? ""}`}
        >
          <XCircle className="h-3.5 w-3.5 text-rose-600" />
          Expired
        </Badge>
      );
    case "DEACTIVATED":
      return (
        <Badge
          variant="outline"
          className={`bg-slate-100 text-slate-700 border-slate-200 font-medium gap-1 text-xs px-2.5 py-0.5 ${className ?? ""}`}
        >
          <PauseCircle className="h-3.5 w-3.5 text-slate-500" />
          Deactivated
        </Badge>
      );
    case "OVER_LIMIT":
      return (
        <Badge
          variant="outline"
          className={`bg-rose-50 text-rose-700 border-rose-200 font-semibold gap-1 text-xs px-2.5 py-0.5 ${className ?? ""}`}
        >
          <AlertTriangle className="h-3.5 w-3.5 text-rose-600" />
          Over Capacity Limit
        </Badge>
      );
    default:
      return (
        <Badge variant="outline" className={className}>
          {status}
        </Badge>
      );
  }
}
