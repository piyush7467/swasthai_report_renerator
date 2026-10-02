import type { OrgCategoryUsageStats } from "../../types/orgAnalyticsTypes";

interface OrgCategoryUsageChartProps {
  data: OrgCategoryUsageStats[];
  height?: number;
}

const CATEGORY_COLORS = [
  "#0284C7", // Sky blue
  "#059669", // Emerald
  "#6366F1", // Indigo
  "#D97706", // Amber
  "#EC4899", // Pink
  "#8B5CF6", // Purple
];

export function OrgCategoryUsageChart({
  data,
  height = 240,
}: OrgCategoryUsageChartProps) {
  if (!data || data.length === 0) {
    return (
      <div
        style={{ height }}
        className="flex flex-col items-center justify-center text-center p-6 select-none bg-slate-50/50 rounded-lg border border-dashed border-slate-200"
      >
        <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-400 mb-2">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <p className="text-xs font-semibold text-slate-700">No test category volume recorded</p>
        <p className="text-[11px] text-slate-400 mt-0.5">Assigned tests will populate report distributions</p>
      </div>
    );
  }

  return (
    <div
      style={{ minHeight: height }}
      className="flex flex-col justify-center space-y-3.5 px-2 py-1"
    >
      {data.map((cat, idx) => {
        const color = CATEGORY_COLORS[idx % CATEGORY_COLORS.length];
        return (
          <div key={cat.categoryName} className="space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span
                  className="w-2 h-2 rounded-full shrink-0"
                  style={{ backgroundColor: color }}
                />
                <span className="font-medium text-slate-700">{cat.categoryName}</span>
              </div>
              <div className="flex items-center gap-2 font-mono">
                <span className="text-slate-800 font-semibold">{cat.reportCount}</span>
                <span className="text-[11px] text-slate-400">({cat.percentage.toFixed(1)}%)</span>
              </div>
            </div>
            <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-500 ease-out"
                style={{
                  width: `${Math.min(Math.max(cat.percentage, 3), 100)}%`,
                  backgroundColor: color,
                }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}
