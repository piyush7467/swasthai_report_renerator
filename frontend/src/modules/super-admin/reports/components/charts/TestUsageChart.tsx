import type { TestUsageStats } from "../../types/analyticsTypes";
import { Badge } from "@/components/ui/badge";

interface TestUsageChartProps {
  data: TestUsageStats[];
}

export function TestUsageChart({ data }: TestUsageChartProps) {
  if (!data || data.length === 0) {
    return (
      <div className="flex h-48 items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50 text-xs text-slate-500">
        No test utilization data recorded yet.
      </div>
    );
  }

  const maxUsage = Math.max(...data.map((d) => d.usageCount), 1);

  return (
    <div className="space-y-3">
      {data.map((item, index) => {
        const percentage = Math.round((item.usageCount / maxUsage) * 100);

        return (
          <div key={item.testRefId} className="group space-y-1">
            <div className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2 min-w-0 pr-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-100 text-[10px] font-bold text-slate-600 shrink-0">
                  {index + 1}
                </span>
                <span className="truncate font-medium text-slate-900 group-hover:text-blue-600 transition-colors">
                  {item.testName}
                </span>
                <span className="text-[11px] text-slate-400 shrink-0">
                  ({item.testCode})
                </span>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Badge variant="outline" className="text-[10px] py-0 h-4 text-slate-500 border-slate-200">
                  {item.categoryName}
                </Badge>
                <span className="font-bold text-slate-900 min-w-10 text-right">
                  {item.usageCount.toLocaleString()}{" "}
                  <span className="text-[10px] font-normal text-slate-500">
                    tests
                  </span>
                </span>
              </div>
            </div>

            {/* Progress Track */}
            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-blue-600 transition-all duration-500 group-hover:bg-blue-700"
                style={{ width: `${Math.max(percentage, 2)}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}
