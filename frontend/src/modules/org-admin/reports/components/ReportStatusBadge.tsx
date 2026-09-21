import { Badge } from "@/components/ui/badge";
import type { ReportStatus } from "../types/reportTypes";

interface ReportStatusBadgeProps {
  status: ReportStatus;
  className?: string;
}

export function ReportStatusBadge({ status, className = "" }: ReportStatusBadgeProps) {
  switch (status) {
    case "DRAFT":
      return (
        <Badge
          variant="outline"
          className={`border-amber-300 bg-amber-50 text-amber-800 font-medium text-[11px] ${className}`}
        >
          Draft
        </Badge>
      );
    case "CALCULATED":
      return (
        <Badge
          variant="outline"
          className={`border-indigo-300 bg-indigo-50 text-indigo-800 font-medium text-[11px] ${className}`}
        >
          Calculated
        </Badge>
      );
    case "FINALIZED":
      return (
        <Badge
          variant="outline"
          className={`border-emerald-300 bg-emerald-50 text-emerald-800 font-medium text-[11px] ${className}`}
        >
          Finalized
        </Badge>
      );
    default:
      return (
        <Badge variant="outline" className={`text-slate-600 text-[11px] ${className}`}>
          {status}
        </Badge>
      );
  }
}
