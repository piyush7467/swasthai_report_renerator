import { Badge } from "@/components/ui/badge";
import type { ResultFlag } from "../types/reportTypes";

interface ParameterResultFlagBadgeProps {
  flag?: ResultFlag | null;
  className?: string;
}

export function ParameterResultFlagBadge({
  flag,
  className = "",
}: ParameterResultFlagBadgeProps) {
  if (!flag) return null;

  switch (flag) {
    case "NORMAL":
      return (
        <Badge
          variant="outline"
          className={`border-emerald-200 bg-emerald-50 text-emerald-700 text-[10px] font-semibold px-1.5 py-0 ${className}`}
        >
          Normal
        </Badge>
      );
    case "LOW":
      return (
        <Badge
          variant="outline"
          className={`border-amber-300 bg-amber-50 text-amber-800 text-[10px] font-semibold px-1.5 py-0 ${className}`}
        >
          Low
        </Badge>
      );
    case "HIGH":
      return (
        <Badge
          variant="outline"
          className={`border-orange-300 bg-orange-50 text-orange-800 text-[10px] font-semibold px-1.5 py-0 ${className}`}
        >
          High
        </Badge>
      );
    case "CRITICAL_LOW":
      return (
        <Badge
          variant="destructive"
          className={`bg-rose-600 text-white text-[10px] font-bold px-1.5 py-0 uppercase tracking-wide animate-pulse ${className}`}
        >
          Crit. Low
        </Badge>
      );
    case "CRITICAL_HIGH":
      return (
        <Badge
          variant="destructive"
          className={`bg-rose-600 text-white text-[10px] font-bold px-1.5 py-0 uppercase tracking-wide animate-pulse ${className}`}
        >
          Crit. High
        </Badge>
      );
    default:
      return null;
  }
}
