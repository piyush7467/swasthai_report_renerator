import { Badge } from "@/components/ui/badge";
import type { TestParameterDataType } from "../types/parameterTypes";

interface ParameterDataTypeBadgeProps {
  dataType: TestParameterDataType;
  className?: string;
}

export function ParameterDataTypeBadge({
  dataType,
  className,
}: ParameterDataTypeBadgeProps) {
  switch (dataType) {
    case "DECIMAL":
    case "INTEGER":
      return (
        <Badge
          variant="outline"
          className={`border-sky-200 bg-sky-50 text-sky-700 font-mono text-xs ${className || ""}`}
        >
          {dataType}
        </Badge>
      );
    case "TEXT":
      return (
        <Badge
          variant="outline"
          className={`border-amber-200 bg-amber-50 text-amber-700 font-mono text-xs ${className || ""}`}
        >
          TEXT
        </Badge>
      );
    case "BOOLEAN":
      return (
        <Badge
          variant="outline"
          className={`border-violet-200 bg-violet-50 text-violet-700 font-mono text-xs ${className || ""}`}
        >
          BOOL
        </Badge>
      );
    case "DATE":
    case "DATETIME":
      return (
        <Badge
          variant="outline"
          className={`border-emerald-200 bg-emerald-50 text-emerald-700 font-mono text-xs ${className || ""}`}
        >
          {dataType}
        </Badge>
      );
    case "ENUM":
      return (
        <Badge
          variant="outline"
          className={`border-rose-200 bg-rose-50 text-rose-700 font-mono text-xs ${className || ""}`}
        >
          ENUM
        </Badge>
      );
    default:
      return (
        <Badge
          variant="outline"
          className={`border-slate-200 bg-slate-50 text-slate-700 font-mono text-xs ${className || ""}`}
        >
          {dataType}
        </Badge>
      );
  }
}
