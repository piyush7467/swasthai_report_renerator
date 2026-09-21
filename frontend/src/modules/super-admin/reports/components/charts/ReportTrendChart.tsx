import { useState } from "react";
import type { ReportTrendPoint } from "../../types/analyticsTypes";

interface ReportTrendChartProps {
  data: ReportTrendPoint[];
  height?: number;
}

export function ReportTrendChart({
  data,
  height = 260,
}: ReportTrendChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  if (!data || data.length === 0) {
    return (
      <div
        style={{ height }}
        className="flex items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50 text-xs text-slate-500"
      >
        No trend data available for the selected period.
      </div>
    );
  }

  const maxVal = Math.max(
    ...data.map((d) => d.totalCount),
    5 // Minimum scale so empty charts don't divide by zero
  );

  const padding = { top: 20, right: 20, bottom: 35, left: 40 };
  const width = 800; // Normalized SVG coordinate space
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  const getX = (index: number) => {
    if (data.length <= 1) return padding.left + chartWidth / 2;
    return padding.left + (index / (data.length - 1)) * chartWidth;
  };

  const getY = (val: number) => {
    return padding.top + chartHeight - (val / maxVal) * chartHeight;
  };

  // Build total path and area
  const totalPoints = data.map((d, i) => `${getX(i)},${getY(d.totalCount)}`).join(" ");
  const finalizedPoints = data.map((d, i) => `${getX(i)},${getY(d.finalizedCount)}`).join(" ");

  const areaPath = `${getX(0)},${padding.top + chartHeight} ${totalPoints} ${getX(
    data.length - 1
  )},${padding.top + chartHeight}`;

  // Y-axis grid ticks (4 steps)
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
    val: Math.round(ratio * maxVal),
    y: padding.top + chartHeight - ratio * chartHeight,
  }));

  // X-axis date labels: sample evenly up to 7 labels
  const step = Math.max(1, Math.floor(data.length / 6));
  const xLabels = data
    .map((d, i) => ({ date: d.date, index: i }))
    .filter((_, i) => i % step === 0 || i === data.length - 1);

  const hoveredPoint = hoveredIndex !== null ? data[hoveredIndex] : null;

  return (
    <div className="relative w-full">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full overflow-visible"
        style={{ height }}
        onMouseLeave={() => setHoveredIndex(null)}
      >
        <defs>
          <linearGradient id="totalGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#0284c7" stopOpacity="0.25" />
            <stop offset="100%" stopColor="#0284c7" stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {/* Horizontal grid lines */}
        {yTicks.map((tick, idx) => (
          <g key={idx}>
            <line
              x1={padding.left}
              y1={tick.y}
              x2={width - padding.right}
              y2={tick.y}
              stroke="#e2e8f0"
              strokeDasharray="4 4"
            />
            <text
              x={padding.left - 8}
              y={tick.y + 4}
              textAnchor="end"
              fontSize="11"
              fill="#94a3b8"
            >
              {tick.val}
            </text>
          </g>
        ))}

        {/* Area fill */}
        <polygon points={areaPath} fill="url(#totalGradient)" />

        {/* Total line (Sky/Blue) */}
        <polyline
          fill="none"
          stroke="#0284c7"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          points={totalPoints}
        />

        {/* Finalized line (Emerald) */}
        <polyline
          fill="none"
          stroke="#10b981"
          strokeWidth="2"
          strokeDasharray="3 3"
          strokeLinecap="round"
          strokeLinejoin="round"
          points={finalizedPoints}
        />

        {/* Interactive point triggers */}
        {data.map((d, i) => {
          const cx = getX(i);
          const cy = getY(d.totalCount);
          const isHovered = hoveredIndex === i;

          return (
            <g
              key={i}
              className="cursor-pointer"
              onMouseEnter={() => setHoveredIndex(i)}
            >
              {/* Invisible wide hit area */}
              <rect
                x={cx - chartWidth / (data.length * 2)}
                y={padding.top}
                width={chartWidth / data.length}
                height={chartHeight}
                fill="transparent"
              />

              {/* Dot indicator */}
              <circle
                cx={cx}
                cy={cy}
                r={isHovered ? 5 : 3}
                fill="#ffffff"
                stroke="#0284c7"
                strokeWidth={isHovered ? 2.5 : 1.5}
                className="transition-all duration-150"
              />

              {isHovered && (
                <line
                  x1={cx}
                  y1={padding.top}
                  x2={cx}
                  y2={padding.top + chartHeight}
                  stroke="#cbd5e1"
                  strokeWidth="1.5"
                  strokeDasharray="2 2"
                />
              )}
            </g>
          );
        })}

        {/* X-axis labels */}
        {xLabels.map((lbl, idx) => {
          const cx = getX(lbl.index);
          const formatted = lbl.date.slice(5); // MM-DD
          return (
            <text
              key={idx}
              x={cx}
              y={height - 8}
              textAnchor="middle"
              fontSize="11"
              fill="#64748b"
            >
              {formatted}
            </text>
          );
        })}
      </svg>

      {/* Floating Tooltip */}
      {hoveredPoint && hoveredIndex !== null && (
        <div
          className="pointer-events-none absolute top-2 rounded-md border border-slate-200 bg-white/95 px-3 py-2 shadow-md backdrop-blur-xs transition-all text-xs"
          style={{
            left: `clamp(10px, ${(getX(hoveredIndex) / width) * 100}%, calc(100% - 170px))`,
          }}
        >
          <div className="font-semibold text-slate-800 border-b border-slate-100 pb-1 mb-1">
            {hoveredPoint.date}
          </div>
          <div className="space-y-0.5 text-slate-600">
            <div className="flex items-center justify-between gap-4">
              <span className="flex items-center gap-1.5 text-sky-700 font-medium">
                <span className="h-2 w-2 rounded-full bg-sky-600 inline-block" />
                Total:
              </span>
              <span className="font-bold text-slate-900">
                {hoveredPoint.totalCount}
              </span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <span className="flex items-center gap-1.5 text-emerald-700">
                <span className="h-2 w-2 rounded-full bg-emerald-500 inline-block" />
                Finalized:
              </span>
              <span className="font-semibold text-slate-900">
                {hoveredPoint.finalizedCount}
              </span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <span className="flex items-center gap-1.5 text-amber-700">
                <span className="h-2 w-2 rounded-full bg-amber-500 inline-block" />
                Draft:
              </span>
              <span className="font-semibold text-slate-900">
                {hoveredPoint.draftCount}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Legend */}
      <div className="mt-2 flex items-center justify-center gap-5 text-xs text-slate-600">
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-sky-600" />
          <span>Total Generated</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
          <span>Finalized</span>
        </div>
      </div>
    </div>
  );
}
