import { Badge } from "@/components/ui/badge";

interface PlanStatusBadgeProps {
  active: boolean;
  className?: string;
}

export function PlanStatusBadge({ active, className = "" }: PlanStatusBadgeProps) {
  if (active) {
    return (
      <Badge
        variant="outline"
        className={`border-emerald-200 bg-emerald-50 text-emerald-700 font-medium px-2 py-0.5 ${className}`}
      >
        <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-emerald-500 inline-block" />
        Active
      </Badge>
    );
  }

  return (
    <Badge
      variant="outline"
      className={`border-slate-200 bg-slate-100 text-slate-600 font-medium px-2 py-0.5 ${className}`}
    >
      <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-slate-400 inline-block" />
      Inactive
    </Badge>
  );
}
