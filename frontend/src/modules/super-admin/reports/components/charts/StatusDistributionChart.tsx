import { Badge } from "@/components/ui/badge";

interface StatusDistributionChartProps {
  draftCount: number;
  calculatedCount: number;
  finalizedCount: number;
}

export function StatusDistributionChart({
  draftCount,
  calculatedCount,
  finalizedCount,
}: StatusDistributionChartProps) {
  const total = draftCount + calculatedCount + finalizedCount;

  if (total === 0) {
    return (
      <div className="flex h-48 items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50 text-xs text-slate-500">
        No report status data available.
      </div>
    );
  }

  const finalizedPct = Math.round((finalizedCount / total) * 100);
  const calculatedPct = Math.round((calculatedCount / total) * 100);
  const draftPct = Math.max(0, 100 - finalizedPct - calculatedPct);

  // SVG Donut calculation
  const radius = 58;
  const circumference = 2 * Math.PI * radius;

  const finalizedStroke = (finalizedCount / total) * circumference;
  const calculatedStroke = (calculatedCount / total) * circumference;
  const draftStroke = (draftCount / total) * circumference;

  const finalizedOffset = 0;
  const calculatedOffset = -finalizedStroke;
  const draftOffset = -(finalizedStroke + calculatedStroke);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-around gap-6 py-2">
      {/* Donut graphic */}
      <div className="relative flex items-center justify-center shrink-0">
        <svg width="150" height="150" viewBox="0 0 150 150" className="-rotate-90">
          <circle
            cx="75"
            cy="75"
            r={radius}
            fill="transparent"
            stroke="#f1f5f9"
            strokeWidth="18"
          />

          {finalizedCount > 0 && (
            <circle
              cx="75"
              cy="75"
              r={radius}
              fill="transparent"
              stroke="#10b981"
              strokeWidth="18"
              strokeDasharray={`${finalizedStroke} ${circumference}`}
              strokeDashoffset={finalizedOffset}
              className="transition-all duration-500"
            />
          )}

          {calculatedCount > 0 && (
            <circle
              cx="75"
              cy="75"
              r={radius}
              fill="transparent"
              stroke="#0284c7"
              strokeWidth="18"
              strokeDasharray={`${calculatedStroke} ${circumference}`}
              strokeDashoffset={calculatedOffset}
              className="transition-all duration-500"
            />
          )}

          {draftCount > 0 && (
            <circle
              cx="75"
              cy="75"
              r={radius}
              fill="transparent"
              stroke="#f59e0b"
              strokeWidth="18"
              strokeDasharray={`${draftStroke} ${circumference}`}
              strokeDashoffset={draftOffset}
              className="transition-all duration-500"
            />
          )}
        </svg>

        <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
          <span className="text-xl font-extrabold text-slate-900 leading-none">
            {total.toLocaleString()}
          </span>
          <span className="text-[10px] uppercase font-semibold text-slate-400 mt-1">
            Total
          </span>
        </div>
      </div>

      {/* Breakdown Legend */}
      <div className="w-full max-w-xs space-y-3">
        {/* FINALIZED */}
        <div className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50/50 p-2 text-xs">
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-emerald-500 shrink-0" />
            <span className="font-medium text-slate-700">FINALIZED</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-900">
              {finalizedCount.toLocaleString()}
            </span>
            <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px] py-0 h-4">
              {finalizedPct}%
            </Badge>
          </div>
        </div>

        {/* CALCULATED */}
        <div className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50/50 p-2 text-xs">
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-sky-600 shrink-0" />
            <span className="font-medium text-slate-700">CALCULATED</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-900">
              {calculatedCount.toLocaleString()}
            </span>
            <Badge variant="outline" className="bg-sky-50 text-sky-700 border-sky-200 text-[10px] py-0 h-4">
              {calculatedPct}%
            </Badge>
          </div>
        </div>

        {/* DRAFT */}
        <div className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50/50 p-2 text-xs">
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-amber-500 shrink-0" />
            <span className="font-medium text-slate-700">DRAFT</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-900">
              {draftCount.toLocaleString()}
            </span>
            <Badge variant="outline" className="bg-amber-50 text-amber-700 border-amber-200 text-[10px] py-0 h-4">
              {draftPct}%
            </Badge>
          </div>
        </div>
      </div>
    </div>
  );
}
