import {
  AlertTriangle,
  Calculator,
  FilePlus,
  FlaskConical,
  Info,
  Loader2,
} from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { EmptyState } from "@/components/ui/empty-state";

import { useTestParametersQuery } from "@/modules/super-admin/tests/hooks/useParameters";
import { TestTypeBadge } from "@/modules/super-admin/tests/components/TestTypeBadge";
import type { OrganizationTestResponse } from "@/modules/super-admin/tests/types/assignmentTypes";

interface TestParametersDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  test: OrganizationTestResponse | null;
  onCreateReport?: (testRefId: string) => void;
}

export function TestParametersDrawer({
  open,
  onOpenChange,
  test,
  onCreateReport,
}: TestParametersDrawerProps) {
  const testRefId = test?.testRefId;

  const {
    data: parametersData,
    isLoading,
    isError,
    error,
    refetch,
  } = useTestParametersQuery(testRefId, { size: 100 });

  const parameters = parametersData?.content ?? [];

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="w-full sm:max-w-xl md:max-w-2xl p-0 flex flex-col h-full bg-slate-50 overflow-hidden"
      >
        {/* Header */}
        <SheetHeader className="p-6 bg-white border-b border-slate-200 shrink-0">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1.5 pr-6">
              <div className="flex flex-wrap items-center gap-2">
                <span className="inline-flex items-center px-2 py-0.5 rounded font-mono text-xs font-semibold bg-teal-50 text-teal-700 border border-teal-200">
                  {test?.testCode || "TEST"}
                </span>
                {test && <TestTypeBadge type={test.testType} />}
                <Badge
                  variant="outline"
                  className={
                    test?.status === "ACTIVE"
                      ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                      : "border-slate-200 bg-slate-100 text-slate-600"
                  }
                >
                  {test?.status || "ACTIVE"}
                </Badge>
              </div>
              <SheetTitle className="text-xl font-bold text-slate-900 tracking-tight leading-snug">
                {test?.testName || "Diagnostic Test Details"}
              </SheetTitle>
              <SheetDescription className="text-xs text-slate-500">
                Biological reference intervals, reporting units, and clinical parameters.
              </SheetDescription>
            </div>
          </div>

          {/* Test Validity Info */}
          {(test?.effectiveFrom || test?.effectiveUntil) && (
            <div className="mt-3 text-xs bg-slate-50 border border-slate-200 rounded-lg p-2.5 flex items-center justify-between text-slate-600">
              <span className="font-medium text-slate-700">Assignment Period:</span>
              <span>
                {test.effectiveFrom ? new Date(test.effectiveFrom).toLocaleDateString() : "Immediate"}
                {" — "}
                {test.effectiveUntil ? new Date(test.effectiveUntil).toLocaleDateString() : "Ongoing"}
              </span>
            </div>
          )}
        </SheetHeader>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-2">
              <FlaskConical className="size-4 text-teal-600" />
              Configured Parameters ({parameters.length})
            </h3>
            {isLoading && (
              <span className="text-xs text-slate-400 flex items-center gap-1.5">
                <Loader2 className="size-3 animate-spin" />
                Loading parameters...
              </span>
            )}
          </div>

          {/* Error State */}
          {isError && (
            <Alert variant="destructive">
              <AlertTriangle className="size-4" />
              <AlertDescription className="text-xs flex items-center justify-between">
                <span>
                  {error instanceof Error ? error.message : "Failed to load parameters"}
                </span>
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-xs bg-white text-slate-900 ml-2"
                  onClick={() => void refetch()}
                >
                  Retry
                </Button>
              </AlertDescription>
            </Alert>
          )}

          {/* Loading Skeletons */}
          {isLoading && (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="p-4 bg-white rounded-xl border border-slate-200 space-y-2">
                  <div className="flex justify-between">
                    <Skeleton className="h-4 w-40" />
                    <Skeleton className="h-4 w-16" />
                  </div>
                  <Skeleton className="h-3 w-28" />
                  <Skeleton className="h-8 w-full" />
                </div>
              ))}
            </div>
          )}

          {/* Empty Parameters */}
          {!isLoading && !isError && parameters.length === 0 && (
            <div className="py-8 bg-white rounded-xl border border-slate-200">
              <EmptyState
                title="No Parameters Configured"
                description="This diagnostic test does not have clinical parameters registered yet. Contact Super Admin to configure parameter specifications."
              />
            </div>
          )}

          {/* Parameters List */}
          {!isLoading && !isError && parameters.length > 0 && (
            <div className="space-y-3">
              {parameters.map((param, index) => {
                const hasRefRange =
                  param.referenceMin !== null &&
                  param.referenceMin !== undefined &&
                  param.referenceMax !== null &&
                  param.referenceMax !== undefined;

                const hasCritical =
                  (param.criticalLow !== null && param.criticalLow !== undefined) ||
                  (param.criticalHigh !== null && param.criticalHigh !== undefined);

                return (
                  <div
                    key={param.refId || index}
                    className="p-4 bg-white rounded-xl border border-slate-200/90 shadow-sm hover:border-teal-200 transition-colors space-y-3"
                  >
                    {/* Header info */}
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-bold text-slate-900">
                            {param.name}
                          </span>
                          {param.required && (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-rose-50 text-rose-600 border border-rose-200">
                              Required
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-0.5">
                          <span className="font-mono text-xs text-slate-500">
                            {param.code}
                          </span>
                          {param.unit && (
                            <>
                              <span className="text-slate-300">•</span>
                              <span className="text-xs font-medium text-teal-700 bg-teal-50 px-1.5 py-0.2 rounded">
                                Unit: {param.unit}
                              </span>
                            </>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <Badge variant="secondary" className="text-[10px] font-medium bg-slate-100 text-slate-600">
                          {param.dataType}
                        </Badge>
                        {param.inputType === "CALCULATED" ? (
                          <Badge variant="outline" className="text-[10px] font-medium border-amber-200 bg-amber-50 text-amber-700 flex items-center gap-1">
                            <Calculator className="size-3" />
                            {param.calculationType !== "NONE" ? param.calculationType : "Calc"}
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="text-[10px] font-medium border-slate-200 text-slate-500">
                            Manual
                          </Badge>
                        )}
                      </div>
                    </div>

                    {/* Biological Reference Interval */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                      <div>
                        <span className="text-slate-500 block text-[11px]">Biological Reference Interval</span>
                        <span className="font-semibold text-slate-800">
                          {hasRefRange ? (
                            <>
                              {param.referenceMin} – {param.referenceMax} {param.unit ?? ""}
                            </>
                          ) : (
                            <span className="text-slate-400 font-normal italic">
                              Qualitative / Not specified
                            </span>
                          )}
                        </span>
                      </div>

                      {hasCritical && (
                        <div>
                          <span className="text-rose-500 block text-[11px] font-medium flex items-center gap-1">
                            <AlertTriangle className="size-3" /> Critical Alarm Range
                          </span>
                          <span className="font-semibold text-rose-700">
                            {param.criticalLow !== null && param.criticalLow !== undefined && (
                              <span>&lt; {param.criticalLow} </span>
                            )}
                            {param.criticalLow !== null &&
                              param.criticalLow !== undefined &&
                              param.criticalHigh !== null &&
                              param.criticalHigh !== undefined && (
                                <span className="text-slate-300">| </span>
                              )}
                            {param.criticalHigh !== null && param.criticalHigh !== undefined && (
                              <span>&gt; {param.criticalHigh} </span>
                            )}
                            {param.unit ?? ""}
                          </span>
                        </div>
                      )}
                    </div>

                    {/* Interpretation / Description */}
                    {(param.interpretationGuidance || param.reportDescription) && (
                      <div className="text-xs text-slate-500 bg-slate-50/50 p-2 rounded border border-slate-100/80 flex items-start gap-1.5">
                        <Info className="size-3.5 text-slate-400 shrink-0 mt-0.5" />
                        <span className="leading-relaxed">
                          {param.interpretationGuidance || param.reportDescription}
                        </span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-white border-t border-slate-200 shrink-0 flex items-center justify-between gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="text-slate-700"
          >
            Close
          </Button>

          {onCreateReport && test && test.status === "ACTIVE" && (
            <Button
              type="button"
              className="bg-teal-600 hover:bg-teal-700 text-white flex items-center gap-2 shadow-sm"
              onClick={() => {
                onOpenChange(false);
                onCreateReport(test.testRefId);
              }}
            >
              <FilePlus className="size-4" />
              Generate Report With This Test
            </Button>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
