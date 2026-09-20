import { Badge } from "@/components/ui/badge";
import type { TestStatus } from "../types/testTypes";

interface TestStatusBadgeProps {
  status: TestStatus;
  className?: string;
}

export function TestStatusBadge({ status, className }: TestStatusBadgeProps) {
  if (status === "ACTIVE") {
    return (
      <Badge
        variant="outline"
        className={`border-emerald-200 bg-emerald-50 text-emerald-700 font-medium ${className || ""}`}
      >
        Active
      </Badge>
    );
  }

  return (
    <Badge
      variant="outline"
      className={`border-slate-200 bg-slate-100 text-slate-600 font-medium ${className || ""}`}
    >
      Inactive
    </Badge>
  );
}
