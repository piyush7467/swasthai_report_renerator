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
          className={`border-emerald-200 bg-emerald-50/80 text-emerald-700 text-[11px] font-semibold px-2 py-0.5 rounded-md uppercase tracking-wider inline-flex items-center justify-center ${className}`}
        >
          NORMAL
        </Badge>
      );
    case "LOW":
      return (
        <Badge
          variant="outline"
          className={`border-amber-200 bg-amber-50/90 text-amber-700 text-[11px] font-semibold px-2 py-0.5 rounded-md uppercase tracking-wider inline-flex items-center justify-center ${className}`}
        >
          LOW
        </Badge>
      );
    case "HIGH":
      return (
        <Badge
          variant="outline"
          className={`border-rose-200 bg-rose-50/90 text-rose-700 text-[11px] font-semibold px-2 py-0.5 rounded-md uppercase tracking-wider inline-flex items-center justify-center ${className}`}
        >
          HIGH
        </Badge>
      );
    case "CRITICAL_LOW":
      return (
        <Badge
          variant="outline"
          className={`border-red-300 bg-red-100/80 text-red-800 text-[11px] font-bold px-2 py-0.5 rounded-md uppercase tracking-wider inline-flex items-center justify-center ${className}`}
        >
          CRITICAL LOW
        </Badge>
      );
    case "CRITICAL_HIGH":
      return (
        <Badge
          variant="outline"
          className={`border-red-300 bg-red-100/80 text-red-800 text-[11px] font-bold px-2 py-0.5 rounded-md uppercase tracking-wider inline-flex items-center justify-center ${className}`}
        >
          CRITICAL HIGH
        </Badge>
      );
    default:
      return null;
  }
}
