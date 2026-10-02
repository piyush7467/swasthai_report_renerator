import { useMemo } from "react";

interface OrgReportStatusDonutProps {
  total: number;
  finalized: number;
  draft: number;
  height?: number;
}

export function OrgReportStatusDonut({
  total,
  finalized,
  draft,
  height = 240,
}: OrgReportStatusDonutProps) {
  const otherCount = Math.max(0, total - finalized - draft);

  const slices = useMemo(() => {
    if (total === 0) {
      return [
        { label: "Finalized", count: 0, percentage: 0, color: "#10B981" },
        { label: "Draft", count: 0, percentage: 0, color: "#F59E0B" },
        { label: "In Review", count: 0, percentage: 0, color: "#6366F1" },
      ];
    }

    const finPct = Math.round((finalized / total) * 100);
    const draftPct = Math.round((draft / total) * 100);
    const otherPct = Math.max(0, 100 - finPct - draftPct);

    return [
      { label: "Finalized", count: finalized, percentage: finPct, color: "#10B981" },
      { label: "Draft", count: draft, percentage: draftPct, color: "#F59E0B" },
      { label: "In Review", count: otherCount, percentage: otherPct, color: "#6366F1" },
    ];
  }, [total, finalized, draft, otherCount]);

  // SVG Donut calculation
  const radius = 64;
  const strokeWidth = 20;
  const circumference = 2 * Math.PI * radius;

  let currentOffset = 0;
  const arcs = slices.map((s) => {
    const strokeDasharray = `${(s.percentage / 100) * circumference} ${circumference}`;
    const strokeDashoffset = -currentOffset;
    currentOffset += (s.percentage / 100) * circumference;
    return { ...s, strokeDasharray, strokeDashoffset };
  });

  return (
    <div
      style={{ height }}
      className="flex flex-col sm:flex-row items-center justify-around gap-4 px-2"
    >
      {/* Donut SVG */}
      <div className="relative flex items-center justify-center">
        <svg width="160" height="160" viewBox="0 0 160 160" className="-rotate-90">
          <circle
            cx="80"
            cy="80"
            r={radius}
            fill="transparent"
            stroke="#F1F5F9"
            strokeWidth={strokeWidth}
          />
          {total > 0 &&
            arcs.map((arc, i) => (
              <circle
                key={i}
                cx="80"
                cy="80"
                r={radius}
                fill="transparent"
                stroke={arc.color}
                strokeWidth={strokeWidth}
                strokeDasharray={arc.strokeDasharray}
                strokeDashoffset={arc.strokeDashoffset}
                strokeLinecap="round"
                className="transition-all duration-500"
              />
            ))}
        </svg>

        {/* Center count */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
          <span className="text-2xl font-bold tracking-tight text-slate-800">
            {total}
          </span>
          <span className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">
            Reports
          </span>
        </div>
      </div>

      {/* Legend & Breakdown */}
      <div className="flex flex-col gap-2.5 w-full sm:w-auto min-w-[150px]">
        {slices.map((item) => (
          <div
            key={item.label}
            className="flex items-center justify-between text-xs gap-3 p-1.5 rounded-md hover:bg-slate-50 transition-colors"
          >
            <div className="flex items-center gap-2">
              <span
                className="w-2.5 h-2.5 rounded-full shrink-0"
                style={{ backgroundColor: item.color }}
              />
              <span className="text-slate-600 font-medium">{item.label}</span>
            </div>
            <div className="flex items-center gap-2 font-mono">
              <span className="font-semibold text-slate-800">{item.count}</span>
              <span className="text-[11px] text-slate-400">({item.percentage}%)</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
