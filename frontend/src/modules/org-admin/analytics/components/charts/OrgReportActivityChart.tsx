import { useState, useMemo } from "react";
import type { OrgReportTrendPoint } from "../../types/orgAnalyticsTypes";

interface OrgReportActivityChartProps {
  data: OrgReportTrendPoint[];
  height?: number;
}

export function OrgReportActivityChart({
  data,
  height = 240,
}: OrgReportActivityChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const hasData = useMemo(
    () => Boolean(data && data.length > 0 && data.some((d) => d.totalReports > 0)),
    [data]
  );

  if (!hasData) {
    return (
      <div
        style={{ height }}
        className="flex flex-col items-center justify-center text-center p-6 select-none bg-slate-50/50 rounded-lg border border-dashed border-slate-200"
      >
        <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-400 mb-2">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
        </div>
        <p className="text-xs font-semibold text-slate-700">No report activity recorded</p>
        <p className="text-[11px] text-slate-400 mt-0.5">Generate or finalize reports to see daily volume</p>
      </div>
    );
  }

  // Display max 14 data points cleanly on chart
  const displayData = data.length > 14 ? data.slice(-14) : data;

  const maxVal = Math.max(
    ...displayData.map((d) => d.totalReports),
    4
  );

  const padding = { top: 20, right: 16, bottom: 28, left: 32 };
  const width = 600;
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  const barGroupWidth = chartWidth / displayData.length;
  const barWidth = Math.max(Math.min(barGroupWidth * 0.32, 14), 4);
  const barGap = 2;

  const getY = (val: number) => {
    return padding.top + chartHeight - (val / maxVal) * chartHeight;
  };

  const hoveredItem = hoveredIndex !== null ? displayData[hoveredIndex] : null;

  return (
    <div className="relative w-full" style={{ height }}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-full overflow-visible"
        preserveAspectRatio="none"
      >
        <defs>
          <linearGradient id="totalBarGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#38BDF8" />
            <stop offset="100%" stopColor="#0284C7" />
          </linearGradient>
          <linearGradient id="finalizedBarGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#34D399" />
            <stop offset="100%" stopColor="#059669" />
          </linearGradient>
        </defs>

        {/* Horizontal gridlines */}
        {[0, 0.33, 0.66, 1].map((ratio) => {
          const y = padding.top + chartHeight * (1 - ratio);
          const value = Math.round(maxVal * ratio);
          return (
            <g key={ratio}>
              <line
                x1={padding.left}
                y1={y}
                x2={width - padding.right}
                y2={y}
                stroke="#E2E8F0"
                strokeDasharray="3 3"
                strokeWidth="1"
              />
              <text
                x={padding.left - 6}
                y={y + 3}
                textAnchor="end"
                className="text-[9px] fill-slate-400 font-mono"
              >
                {value}
              </text>
            </g>
          );
        })}

        {/* Bars */}
        {displayData.map((d, i) => {
          const groupCenterX = padding.left + (i + 0.5) * barGroupWidth;
          const totalHeight = (d.totalReports / maxVal) * chartHeight;
          const totalY = getY(d.totalReports);

          const finalizedHeight = (d.finalizedReports / maxVal) * chartHeight;
          const finalizedY = getY(d.finalizedReports);

          const isHovered = hoveredIndex === i;

          return (
            <g
              key={d.date}
              className="cursor-pointer"
              onMouseEnter={() => setHoveredIndex(i)}
              onMouseLeave={() => setHoveredIndex(null)}
            >
              {/* Background hover highlight */}
              {isHovered && (
                <rect
                  x={padding.left + i * barGroupWidth}
                  y={padding.top}
                  width={barGroupWidth}
                  height={chartHeight}
                  fill="#F1F5F9"
                  opacity={0.6}
                  rx="4"
                />
              )}

              {/* Total Reports Bar */}
              {d.totalReports > 0 && (
                <rect
                  x={groupCenterX - barWidth - barGap / 2}
                  y={totalY}
                  width={barWidth}
                  height={Math.max(totalHeight, 2)}
                  rx="2"
                  fill="url(#totalBarGrad)"
                  className="transition-all duration-150"
                  opacity={isHovered ? 1 : 0.85}
                />
              )}

              {/* Finalized Reports Bar */}
              {d.finalizedReports > 0 && (
                <rect
                  x={groupCenterX + barGap / 2}
                  y={finalizedY}
                  width={barWidth}
                  height={Math.max(finalizedHeight, 2)}
                  rx="2"
                  fill="url(#finalizedBarGrad)"
                  className="transition-all duration-150"
                  opacity={isHovered ? 1 : 0.9}
                />
              )}

              {/* X-axis date label */}
              {(i % 2 === 0 || displayData.length <= 7) && (
                <text
                  x={groupCenterX}
                  y={height - 6}
                  textAnchor="middle"
                  className="text-[9px] fill-slate-400 font-mono"
                >
                  {d.date.length >= 10 ? d.date.slice(5) : d.date}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Floating Tooltip */}
      {hoveredItem && hoveredIndex !== null && (
        <div
          className="absolute pointer-events-none bg-slate-900 text-white rounded-md shadow-lg text-[11px] p-2 -translate-x-1/2 z-20 transition-all border border-slate-700/60"
          style={{
            left: `${((padding.left + (hoveredIndex + 0.5) * barGroupWidth) / width) * 100}%`,
            top: "8px",
          }}
        >
          <div className="font-semibold text-slate-200 border-b border-slate-700/80 pb-1 mb-1 font-mono">
            {hoveredItem.date}
          </div>
          <div className="flex items-center justify-between gap-3 text-sky-400">
            <span>Total Reports:</span>
            <span className="font-bold">{hoveredItem.totalReports}</span>
          </div>
          <div className="flex items-center justify-between gap-3 text-emerald-400">
            <span>Finalized:</span>
            <span className="font-bold">{hoveredItem.finalizedReports}</span>
          </div>
          {hoveredItem.draftReports > 0 && (
            <div className="flex items-center justify-between gap-3 text-amber-300">
              <span>Drafts:</span>
              <span className="font-bold">{hoveredItem.draftReports}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
