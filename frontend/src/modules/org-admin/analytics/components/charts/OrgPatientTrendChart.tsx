import { useState, useMemo } from "react";
import type { OrgPatientTrendPoint } from "../../types/orgAnalyticsTypes";

interface OrgPatientTrendChartProps {
  data: OrgPatientTrendPoint[];
  height?: number;
}

export function OrgPatientTrendChart({
  data,
  height = 240,
}: OrgPatientTrendChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const hasData = useMemo(
    () => Boolean(data && data.length > 0 && data.some((d) => d.newPatients > 0)),
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
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        </div>
        <p className="text-xs font-semibold text-slate-700">No patient registrations recorded</p>
        <p className="text-[11px] text-slate-400 mt-0.5">New patient intakes will display patient acquisition curve</p>
      </div>
    );
  }

  // Display max 14 points
  const displayData = data.length > 14 ? data.slice(-14) : data;

  const maxVal = Math.max(
    ...displayData.map((d) => d.newPatients),
    4
  );

  const padding = { top: 20, right: 16, bottom: 28, left: 32 };
  const width = 600;
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  const getX = (index: number) => {
    if (displayData.length <= 1) return padding.left + chartWidth / 2;
    return padding.left + (index / (displayData.length - 1)) * chartWidth;
  };

  const getY = (val: number) => {
    return padding.top + chartHeight - (val / maxVal) * chartHeight;
  };

  // Generate SVG path points
  const points = displayData.map((d, i) => ({
    x: getX(i),
    y: getY(d.newPatients),
    d,
  }));

  const linePath = points.reduce((acc, p, i) => {
    if (i === 0) return `M ${p.x} ${p.y}`;
    const prev = points[i - 1];
    const cx1 = prev.x + (p.x - prev.x) / 2;
    const cy1 = prev.y;
    const cx2 = prev.x + (p.x - prev.x) / 2;
    const cy2 = p.y;
    return `${acc} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${p.x} ${p.y}`;
  }, "");

  const areaPath = `${linePath} L ${getX(points.length - 1)} ${padding.top + chartHeight} L ${getX(0)} ${padding.top + chartHeight} Z`;

  const hoveredItem = hoveredIndex !== null ? displayData[hoveredIndex] : null;

  return (
    <div className="relative w-full" style={{ height }}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-full overflow-visible"
        preserveAspectRatio="none"
      >
        <defs>
          <linearGradient id="patientAreaGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#6366F1" stopOpacity="0.3" />
            <stop offset="100%" stopColor="#6366F1" stopOpacity="0.0" />
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

        {/* Area fill */}
        <path d={areaPath} fill="url(#patientAreaGrad)" />

        {/* Line */}
        <path
          d={linePath}
          fill="none"
          stroke="#6366F1"
          strokeWidth="2.5"
          strokeLinecap="round"
        />

        {/* Points & Interactive circles */}
        {points.map((p, i) => {
          const isHovered = hoveredIndex === i;
          return (
            <g
              key={p.d.date}
              className="cursor-pointer"
              onMouseEnter={() => setHoveredIndex(i)}
              onMouseLeave={() => setHoveredIndex(null)}
            >
              {/* Invisible wider hit area */}
              <circle cx={p.x} cy={p.y} r="12" fill="transparent" />

              <circle
                cx={p.x}
                cy={p.y}
                r={isHovered ? "5" : "3.5"}
                fill="#FFFFFF"
                stroke="#6366F1"
                strokeWidth={isHovered ? "3" : "2"}
                className="transition-all duration-150"
              />

              {/* X label */}
              {(i % 2 === 0 || displayData.length <= 7) && (
                <text
                  x={p.x}
                  y={height - 6}
                  textAnchor="middle"
                  className="text-[9px] fill-slate-400 font-mono"
                >
                  {p.d.date.length >= 10 ? p.d.date.slice(5) : p.d.date}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Hover tooltip */}
      {hoveredItem && hoveredIndex !== null && (
        <div
          className="absolute pointer-events-none bg-slate-900 text-white rounded-md shadow-lg text-[11px] p-2 -translate-x-1/2 z-20 transition-all border border-slate-700/60"
          style={{
            left: `${(getX(hoveredIndex) / width) * 100}%`,
            top: "8px",
          }}
        >
          <div className="font-semibold text-slate-200 border-b border-slate-700/80 pb-1 mb-1 font-mono">
            {hoveredItem.date}
          </div>
          <div className="flex items-center justify-between gap-3 text-indigo-300">
            <span>New Patients:</span>
            <span className="font-bold">{hoveredItem.newPatients}</span>
          </div>
        </div>
      )}
    </div>
  );
}
