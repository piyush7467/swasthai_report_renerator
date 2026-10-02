import { useState } from "react";
import type { ReportTrendPoint } from "../../types/analyticsTypes";

interface ReportTrendChartProps {
  data: ReportTrendPoint[];
  height?: number;
}

export function ReportTrendChart({
  data,
  height = 240,
}: ReportTrendChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const hasData = Boolean(
    data &&
      data.length > 0 &&
      data.some((d) => d.totalCount > 0 || d.finalizedCount > 0)
  );

  if (!hasData) {
    return (
      <div
        style={{ height }}
        className="flex flex-col items-center justify-center text-center p-6 select-none"
      >
        {/* Clean document with magnifying glass illustration */}
        <div className="relative mb-3 flex items-center justify-center">
          <svg
            width="64"
            height="64"
            viewBox="0 0 64 64"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            className="text-slate-300"
          >
            {/* Paper back */}
            <path
              d="M18 10H38L48 20V52C48 54.2091 46.2091 56 44 56H18C15.7909 56 14 54.2091 14 52V14C14 11.7909 15.7909 10 18 10Z"
              fill="#F8FAFC"
              stroke="#E2E8F0"
              strokeWidth="2"
            />
            {/* Folded corner */}
            <path
              d="M38 10V18C38 19.1046 38.8954 20 40 20H48"
              stroke="#CBD5E1"
              strokeWidth="2"
            />
            {/* Subtle lines */}
            <line x1="22" y1="28" x2="34" y2="28" stroke="#E2E8F0" strokeWidth="2" strokeLinecap="round" />
            <line x1="22" y1="36" x2="30" y2="36" stroke="#E2E8F0" strokeWidth="2" strokeLinecap="round" />
            {/* Magnifying Glass */}
            <circle
              cx="40"
              cy="40"
              r="10"
              fill="white"
              stroke="#94A3B8"
              strokeWidth="2.5"
            />
            <line
              x1="47.5"
              y1="47.5"
              x2="54"
              y2="54"
              stroke="#94A3B8"
              strokeWidth="3"
              strokeLinecap="round"
            />
            <path
              d="M37 37C38.5 35.5 40.5 35 42 35.5"
              stroke="#CBD5E1"
              strokeWidth="1.5"
              strokeLinecap="round"
            />
          </svg>
        </div>

        <p className="text-xs sm:text-sm font-semibold text-slate-700">
          No report Activity in this period
        </p>
        <p className="text-[11px] sm:text-xs text-slate-400 mt-1 max-w-xs">
          Try selecting a different time range or check back later.
        </p>
      </div>
    );
  }

  const maxVal = Math.max(
    ...data.map((d) => d.totalCount),
    5 // Minimum scale
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
            <stop offset="0%" stopColor="#0284c7" stopOpacity="0.2" />
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
              stroke="#f1f5f9"
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

              {/* Finalized dot */}
              <circle
                cx={cx}
                cy={getY(d.finalizedCount)}
                r={isHovered ? 4 : 2.5}
                fill="#ffffff"
                stroke="#10b981"
                strokeWidth={isHovered ? 2 : 1}
              />
            </g>
          );
        })}

        {/* X-axis date labels */}
        {xLabels.map((lbl, idx) => {
          const x = getX(lbl.index);
          const formattedDate = new Date(lbl.date).toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
          });

          return (
            <text
              key={idx}
              x={x}
              y={height - 8}
              textAnchor="middle"
              fontSize="11"
              fill="#94a3b8"
            >
              {formattedDate}
            </text>
          );
        })}
      </svg>

      {/* Floating Tooltip */}
      {hoveredPoint && hoveredIndex !== null && (
        <div
          className="absolute z-20 pointer-events-none -translate-x-1/2 rounded-lg bg-slate-900 px-3 py-2 text-xs text-white shadow-lg transition-all"
          style={{
            left: `${(getX(hoveredIndex) / width) * 100}%`,
            top: `${getY(hoveredPoint.totalCount) - 40}px`,
          }}
        >
          <p className="font-semibold text-slate-200">
            {new Date(hoveredPoint.date).toLocaleDateString("en-US", {
              month: "short",
              day: "numeric",
              year: "numeric",
            })}
          </p>
          <div className="mt-1 flex items-center gap-3">
            <span className="text-sky-400">Total: {hoveredPoint.totalCount}</span>
            <span className="text-emerald-400">
              Finalized: {hoveredPoint.finalizedCount}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
