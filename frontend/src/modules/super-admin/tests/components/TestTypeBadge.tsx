import { Badge } from "@/components/ui/badge";
import type { TestType } from "../types/testTypes";

interface TestTypeBadgeProps {
  type: TestType | string;
  className?: string;
}

export function TestTypeBadge({ type, className }: TestTypeBadgeProps) {
  switch (type) {
    case "INDIVIDUAL":
      return (
        <Badge
          variant="outline"
          className={`border-blue-200 bg-blue-50 text-blue-700 font-medium ${className || ""}`}
        >
          Individual
        </Badge>
      );
    case "PANEL":
      return (
        <Badge
          variant="outline"
          className={`border-indigo-200 bg-indigo-50 text-indigo-700 font-medium ${className || ""}`}
        >
          Panel
        </Badge>
      );
    case "PROFILE":
      return (
        <Badge
          variant="outline"
          className={`border-purple-200 bg-purple-50 text-purple-700 font-medium ${className || ""}`}
        >
          Profile
        </Badge>
      );
    default:
      return (
        <Badge
          variant="outline"
          className={`border-slate-200 bg-slate-50 text-slate-700 font-medium ${className || ""}`}
        >
          {type}
        </Badge>
      );
  }
}
