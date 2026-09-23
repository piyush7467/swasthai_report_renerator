import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  Calculator,
  Check,
  CornerDownLeft,
  FlaskConical,
  Loader2,
  Lock,
  Plus,
  RefreshCw,
  Trash2,
  User,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ParameterResultFlagBadge } from "../ParameterResultFlagBadge";
import { ReportStatusBadge } from "../ReportStatusBadge";
import { AddTestModal } from "../AddTestModal";
import { formatDisplayUnit } from "../../utils/unitFormatter";
import { testApi } from "@/modules/super-admin/tests/api/testApi";
import type {
  ReportParameterItemResponse,
  ReportResponse,
  ReportTestItemResponse,
} from "../../types/reportTypes";
import type { PatientResponse } from "@/modules/org-admin/patients/types/patientTypes";

interface Step4EnterResultsProps {
  report: ReportResponse;
  patient?: PatientResponse | null;
  onUpdateParameters: (
    reportTestRefId: string,
    parameters: { parameterRefId: string; value: string }[],
    lockVersion: number
  ) => Promise<void>;
  onRecalculate: () => Promise<void>;
  onRemoveTest: (reportTestRefId: string) => Promise<void>;
  onProceedToReview: () => void;
  onBackToTests: () => void;
  lockConflictError: string | null;
  onRefreshReport: () => void;
}

// Format reference ranges cleanly matching laboratory convention: e.g. "12 – 17", "> 10", "< 5", "—"
function formatReferenceRange(param: ReportParameterItemResponse): string {
  if (param.referenceMin != null && param.referenceMax != null) {
    return `${param.referenceMin} – ${param.referenceMax}`;
  }
  if (param.referenceMin != null) {
    return `> ${param.referenceMin}`;
  }
  if (param.referenceMax != null) {
    return `< ${param.referenceMax}`;
  }
  return "—";
}

export function Step4EnterResults({
  report,
  patient,
  onUpdateParameters,
  onRecalculate,
  onRemoveTest,
  onProceedToReview,
  onBackToTests,
  lockConflictError,
  onRefreshReport,
}: Step4EnterResultsProps) {
  // Local parameter values state: { [reportTestRefId]: { [parameterRefId]: string } }
  const [localParamValues, setLocalParamValues] = useState<
    Record<string, Record<string, string>>
  >({});
  const [saveStatus, setSaveStatus] = useState<
    "IDLE" | "SAVING" | "SAVED" | "ERROR"
  >("IDLE");
  const [isRecalculating, setIsRecalculating] = useState(false);
  const [isAddTestOpen, setIsAddTestOpen] = useState(false);

  // Delete test confirmation state
  const [testToDelete, setTestToDelete] = useState<ReportTestItemResponse | null>(null);
  const [isDeletingTest, setIsDeletingTest] = useState(false);

  // Active focused parameter tracker for row highlight & guidance bar
  const [activeParam, setActiveParam] = useState<{
    reportTestRefId: string;
    testName: string;
    param: ReportParameterItemResponse;
  } | null>(null);

  const pendingSaveTimeoutRef = useRef<
    Record<string, ReturnType<typeof setTimeout>>
  >({});
  const desktopInputRefs = useRef<Record<string, HTMLInputElement | null>>({});
  const mobileInputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  // Helper to retrieve currently visible active input (desktop vs mobile)
  const getVisibleInput = useCallback((key: string): HTMLInputElement | null => {
    const desktopEl = desktopInputRefs.current[key];
    if (desktopEl && desktopEl.offsetParent !== null) {
      return desktopEl;
    }
    const mobileEl = mobileInputRefs.current[key];
    if (mobileEl && mobileEl.offsetParent !== null) {
      return mobileEl;
    }
    return desktopEl || mobileEl || null;
  }, []);

  // Fetch test catalog map to get authentic test category/department without hardcoding
  const { data: testsCatalogData } = useQuery({
    queryKey: ["tests", "catalog-map"],
    queryFn: () => testApi.getTests({ size: 100 }),
    staleTime: 5 * 60 * 1000,
  });

  const testCategoryMap = useMemo(() => {
    const map: Record<string, string> = {};
    testsCatalogData?.content?.forEach((t) => {
      if (t.refId) {
        map[t.refId] = t.categoryName || t.reportSection || "";
      }
    });
    return map;
  }, [testsCatalogData]);

  // Flattened list of all MANUAL (editable) parameters for sequential navigation
  const manualParamList = useMemo(() => {
    const list: {
      key: string;
      reportTestRefId: string;
      testName: string;
      param: ReportParameterItemResponse;
    }[] = [];

    report.tests.forEach((test) => {
      test.parameters.forEach((param) => {
        if (param.inputType === "MANUAL") {
          list.push({
            key: `${test.refId}_${param.parameterRefId}`,
            reportTestRefId: test.refId,
            testName: test.testName,
            param,
          });
        }
      });
    });
    return list;
  }, [report]);

  // Sync incoming report data with local state
  useEffect(() => {
    const initial: Record<string, Record<string, string>> = {};
    report.tests.forEach((test) => {
      initial[test.refId] = {};
      test.parameters.forEach((param) => {
        initial[test.refId][param.parameterRefId] = param.value ?? "";
      });
    });
    setLocalParamValues(initial);

    // Default first active param if none selected
    if (manualParamList.length > 0 && !activeParam) {
      setActiveParam({
        reportTestRefId: manualParamList[0].reportTestRefId,
        testName: manualParamList[0].testName,
        param: manualParamList[0].param,
      });
    }
  }, [report, manualParamList, activeParam]);

  // Direct save execution without debounce
  const executeSave = useCallback(
    async (reportTestRefId: string, paramOverrides?: Record<string, string>) => {
      const currentTest = report.tests.find((t) => t.refId === reportTestRefId);
      if (!currentTest) return;

      const manualParameters = currentTest.parameters
        .filter((p) => p.inputType === "MANUAL")
        .map((p) => ({
          parameterRefId: p.parameterRefId,
          value:
            paramOverrides?.[p.parameterRefId] !== undefined
              ? paramOverrides[p.parameterRefId]
              : localParamValues[reportTestRefId]?.[p.parameterRefId] ??
                p.value ??
                "",
        }));

      setSaveStatus("SAVING");
      try {
        await onUpdateParameters(
          reportTestRefId,
          manualParameters,
          report.lockVersion
        );
        setSaveStatus("SAVED");
      } catch {
        setSaveStatus("ERROR");
      }
    },
    [report, localParamValues, onUpdateParameters]
  );

  // Debounced auto-save handler
  const handleValueChange = useCallback(
    (reportTestRefId: string, parameterRefId: string, newValue: string) => {
      setLocalParamValues((prev) => ({
        ...prev,
        [reportTestRefId]: {
          ...prev[reportTestRefId],
          [parameterRefId]: newValue,
        },
      }));

      // Clear pending timer for this specific test
      if (pendingSaveTimeoutRef.current[reportTestRefId]) {
        clearTimeout(pendingSaveTimeoutRef.current[reportTestRefId]);
      }

      setSaveStatus("SAVING");

      // 600ms debounce timer for autosave
      pendingSaveTimeoutRef.current[reportTestRefId] = setTimeout(async () => {
        await executeSave(reportTestRefId, { [parameterRefId]: newValue });
      }, 600);
    },
    [executeSave]
  );

  // Focus next manual parameter on Enter key
  const handleKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>,
    currentKey: string,
    reportTestRefId: string,
    parameterRefId: string,
    currentValue: string
  ) => {
    if (e.key === "Enter") {
      e.preventDefault();

      // Flush debounce timer immediately to guarantee persistence
      if (pendingSaveTimeoutRef.current[reportTestRefId]) {
        clearTimeout(pendingSaveTimeoutRef.current[reportTestRefId]);
      }
      void executeSave(reportTestRefId, { [parameterRefId]: currentValue });

      const currentIndex = manualParamList.findIndex((p) => p.key === currentKey);
      if (currentIndex !== -1 && currentIndex < manualParamList.length - 1) {
        const nextItem = manualParamList[currentIndex + 1];
        setActiveParam({
          reportTestRefId: nextItem.reportTestRefId,
          testName: nextItem.testName,
          param: nextItem.param,
        });

        setTimeout(() => {
          const nextInput = getVisibleInput(nextItem.key);
          if (nextInput) {
            nextInput.focus();
            nextInput.select();
            nextInput.scrollIntoView({ behavior: "smooth", block: "center" });
          }
        }, 15);
      }
    }
  };

  // Row click selection
  const handleRowClick = (
    reportTestRefId: string,
    testName: string,
    param: ReportParameterItemResponse
  ) => {
    setActiveParam({ reportTestRefId, testName, param });
    const key = `${reportTestRefId}_${param.parameterRefId}`;
    setTimeout(() => {
      const targetInput = getVisibleInput(key);
      if (targetInput) {
        targetInput.focus();
        targetInput.select();
      }
    }, 15);
  };

  // Recalculate trigger
  const handleRecalculateClick = async () => {
    setIsRecalculating(true);
    try {
      await onRecalculate();
      setSaveStatus("SAVED");
    } finally {
      setIsRecalculating(false);
    }
  };

  // Confirm delete test
  const handleConfirmDeleteTest = async () => {
    if (!testToDelete) return;
    setIsDeletingTest(true);
    try {
      await onRemoveTest(testToDelete.refId);
      setTestToDelete(null);
    } finally {
      setIsDeletingTest(false);
    }
  };

  const existingTestRefIds = report.tests.map((t) => t.testRefId);

  return (
    <div className="space-y-6 pb-20">
      {/* ======================================================== */}
      {/* 1. REPORT & PATIENT HEADER BAR                           */}
      {/* ======================================================== */}
      <Card className="border-slate-200/90 bg-white shadow-xs">
        <CardContent className="p-4 sm:px-6 sm:py-4">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
            <div className="flex items-start sm:items-center gap-3.5">
              <div className="h-10 w-10 rounded-full bg-[#0F766E] text-white flex items-center justify-center shrink-0 shadow-xs font-bold text-sm">
                {patient ? (
                  patient.name.slice(0, 2).toUpperCase()
                ) : (
                  <User className="h-5 w-5" />
                )}
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-bold text-slate-900 text-base">
                    {patient
                      ? `${patient.salutation ? patient.salutation + ". " : ""}${patient.name}`
                      : "Diagnostic Patient"}
                  </span>
                  {patient && (
                    <Badge
                      variant="outline"
                      className="font-mono text-xs text-[#0F766E] border-teal-200 bg-[#E6F4EA] font-semibold"
                    >
                      {patient.patientCode}
                    </Badge>
                  )}
                  <ReportStatusBadge status={report.status} />
                  <span className="text-xs font-mono text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                    Ref: #{report.refId.slice(0, 8)}
                  </span>
                </div>

                <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-600">
                  {patient && (
                    <>
                      <span className="capitalize font-medium">
                        {patient.gender.toLowerCase()}
                      </span>
                      <span>&bull;</span>
                      <span>
                        {patient.ageValue != null
                          ? `${patient.ageValue} ${patient.ageUnit?.toLowerCase() ?? "years"}`
                          : patient.dateOfBirth
                          ? `DOB: ${patient.dateOfBirth}`
                          : "Age N/A"}
                      </span>
                      <span>&bull;</span>
                    </>
                  )}
                  <span className="text-slate-500 font-mono">
                    Report v{report.reportVersion}
                  </span>
                </div>
              </div>
            </div>

            {/* Autosave Status Indicator & Global Actions */}
            <div className="flex flex-wrap items-center gap-2.5 self-start lg:self-center">
              {/* Clean unobtrusive autosave state */}
              <div className="flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-md bg-slate-50 border border-slate-200/80">
                {saveStatus === "SAVING" ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin text-[#0F766E]" />
                    <span className="text-slate-600 font-medium">Saving...</span>
                  </>
                ) : saveStatus === "SAVED" ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-emerald-600 stroke-[3]" />
                    <span className="text-emerald-700 font-semibold">✓ All changes saved</span>
                  </>
                ) : saveStatus === "ERROR" ? (
                  <>
                    <AlertCircle className="h-3.5 w-3.5 text-rose-600" />
                    <span className="text-rose-600 font-semibold">⚠ Save failed</span>
                  </>
                ) : (
                  <span className="text-slate-500 font-medium">✓ Auto-save active</span>
                )}
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleRecalculateClick}
                disabled={isRecalculating}
                className="text-xs h-8 gap-1.5 border-slate-300 text-slate-700 hover:text-purple-700 cursor-pointer"
              >
                <Calculator
                  className={`h-3.5 w-3.5 text-purple-600 ${
                    isRecalculating ? "animate-spin" : ""
                  }`}
                />
                Recalculate
              </Button>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsAddTestOpen(true)}
                className="text-xs h-8 gap-1.5 border-slate-300 text-slate-700 hover:text-[#0F766E] cursor-pointer"
              >
                <Plus className="h-3.5 w-3.5 text-[#0F766E]" />
                Add Test
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Optimistic Lock Conflict Alert */}
      {lockConflictError && (
        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertTitle>Edit Conflict Detected</AlertTitle>
          <AlertDescription className="flex items-center justify-between gap-4 mt-1 text-xs">
            <span>{lockConflictError}</span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onRefreshReport}
              className="text-xs shrink-0 bg-white"
            >
              <RefreshCw className="h-3 w-3 mr-1" />
              Refresh Latest Data
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* ======================================================== */}
      {/* 2. TEST PANELS & CLINICAL RESULT TABLES                  */}
      {/* ======================================================== */}
      {report.tests.length === 0 ? (
        <Card className="border-slate-200 p-8 text-center bg-white shadow-xs">
          <FlaskConical className="h-10 w-10 text-slate-300 mx-auto mb-3" />
          <h3 className="text-sm font-bold text-slate-800">No Tests in Report</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Add diagnostic tests from your laboratory catalog to enter patient parameter values.
          </p>
          <Button
            type="button"
            size="sm"
            onClick={() => setIsAddTestOpen(true)}
            className="mt-4 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white"
          >
            <Plus className="h-3.5 w-3.5 mr-1" />
            Add Test Panel
          </Button>
        </Card>
      ) : (
        report.tests.map((test) => (
          <Card
            key={test.refId}
            className="border-slate-200/90 bg-white shadow-xs overflow-hidden"
          >
            {/* COMPACT & PROFESSIONAL TEST HEADER (Neutral/Teal palette) */}
            <div className="bg-slate-50/90 border-b border-slate-200/80 px-4 sm:px-6 py-3.5 flex items-center justify-between">
              <div className="flex items-center gap-3 min-w-0">
                <div className="h-8 w-8 rounded-lg bg-[#E6F4EA] text-[#0F766E] flex items-center justify-center shrink-0">
                  <FlaskConical className="h-4 w-4" />
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-slate-900 text-sm sm:text-base truncate">
                      {test.testName}
                    </h3>
                    <span className="font-mono text-xs font-semibold text-[#0F766E] bg-[#E6F4EA] px-2 py-0.5 rounded border border-teal-200/60 shrink-0">
                      {test.testCode}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 font-medium mt-0.5">
                    {testCategoryMap[test.testRefId] || `${test.parameters.length} parameter${test.parameters.length === 1 ? "" : "s"}`}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => setTestToDelete(test)}
                  className="h-8 px-2.5 text-xs text-slate-400 hover:text-rose-600 hover:bg-rose-50/70 transition-colors cursor-pointer"
                  title="Remove test panel from report"
                >
                  <Trash2 className="h-3.5 w-3.5 mr-1" />
                  Remove
                </Button>
              </div>
            </div>

            {/* DESKTOP TABLE VIEW (sm and up) */}
            <div className="hidden sm:block overflow-x-auto">
              <table className="w-full text-left border-collapse min-w-[700px]">
                <thead>
                  <tr className="bg-slate-50/70 border-b border-slate-200/80 text-slate-600 text-[12px] font-semibold tracking-wide uppercase">
                    <th className="py-3.5 px-6 w-[36%] min-w-[220px]">Parameter</th>
                    <th className="py-3.5 px-4 w-[22%] min-w-[160px]">Result</th>
                    <th className="py-3.5 px-4 w-[14%] min-w-[110px] whitespace-nowrap">Unit</th>
                    <th className="py-3.5 px-4 w-[16%] min-w-[110px]">Reference</th>
                    <th className="py-3.5 px-4 text-center w-[12%] min-w-[100px]">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {test.parameters.map((param) => {
                    const key = `${test.refId}_${param.parameterRefId}`;
                    const isCalculated = param.inputType === "CALCULATED";
                    const isActive =
                      activeParam?.reportTestRefId === test.refId &&
                      activeParam?.param.parameterRefId === param.parameterRefId;

                    const displayVal =
                      localParamValues[test.refId]?.[param.parameterRefId] ??
                      param.value ??
                      "";

                    return (
                      <tr
                        key={param.refId}
                        onClick={() =>
                          handleRowClick(test.refId, test.testName, param)
                        }
                        className={`transition-colors cursor-pointer ${
                          isActive
                            ? "bg-teal-50/50 ring-1 ring-inset ring-teal-300"
                            : isCalculated
                            ? "bg-slate-50/40"
                            : "hover:bg-slate-50/60"
                        }`}
                      >
                        {/* 1. Parameter Column (36%) */}
                        <td className="py-4 px-6">
                          <div className="font-semibold text-slate-900 text-[15px] leading-snug">
                            {param.parameterName}
                          </div>
                          <div className="font-mono text-xs text-slate-500 mt-0.5">
                            {param.parameterCode}
                          </div>
                        </td>

                        {/* 2. Result Column (22%) */}
                        <td className="py-2.5 px-4" onClick={(e) => e.stopPropagation()}>
                          {isCalculated ? (
                            <div className="flex items-center gap-2">
                              <div className="h-11 px-3.5 rounded-lg bg-slate-100/90 border border-slate-200 flex items-center justify-center font-mono font-bold text-[15px] text-slate-800 min-w-[85px] shadow-2xs select-none">
                                {displayVal || "—"}
                              </div>
                              <Badge
                                variant="outline"
                                className="bg-slate-100 text-slate-600 border-slate-300 text-[11px] font-medium flex items-center gap-1 shrink-0 select-none py-1"
                                title="Calculated automatically by formula"
                              >
                                <Lock className="h-3 w-3 text-slate-500" />
                                Calculated
                              </Badge>
                            </div>
                          ) : (
                            <div className="relative max-w-[170px]">
                              <Input
                                ref={(el) => {
                                  desktopInputRefs.current[key] = el;
                                }}
                                type={
                                  param.dataType === "INTEGER" ||
                                  param.dataType === "DECIMAL"
                                    ? "number"
                                    : "text"
                                }
                                step={param.dataType === "DECIMAL" ? "any" : "1"}
                                value={displayVal}
                                onChange={(e) =>
                                  handleValueChange(
                                    test.refId,
                                    param.parameterRefId,
                                    e.target.value
                                  )
                                }
                                onKeyDown={(e) =>
                                  handleKeyDown(
                                    e,
                                    key,
                                    test.refId,
                                    param.parameterRefId,
                                    e.currentTarget.value
                                  )
                                }
                                onFocus={(e) => {
                                  e.currentTarget.select();
                                  setActiveParam({
                                    reportTestRefId: test.refId,
                                    testName: test.testName,
                                    param,
                                  });
                                }}
                                placeholder="Enter value..."
                                aria-label={`Result for ${param.parameterName}`}
                                className="h-11 px-3.5 text-[15px] font-semibold font-mono text-slate-900 bg-white border-slate-200 focus:bg-white focus:border-[#0F766E] focus:ring-2 focus:ring-[#0F766E]/20 transition-all rounded-lg shadow-2xs"
                              />
                            </div>
                          )}
                        </td>

                        {/* 3. Unit Column (14% - NEVER WRAPS) */}
                        <td className="py-4 px-4 whitespace-nowrap min-w-[110px]">
                          <span className="text-sm font-medium text-slate-700 inline-block whitespace-nowrap">
                            {formatDisplayUnit(param.unit)}
                          </span>
                        </td>

                        {/* 4. Reference Range Column (16%) */}
                        <td className="py-4 px-4 whitespace-nowrap min-w-[110px]">
                          <span className="text-sm font-mono font-medium text-slate-600">
                            {formatReferenceRange(param)}
                          </span>
                        </td>

                        {/* 5. Status Column (12%) */}
                        <td className="py-4 px-4 text-center whitespace-nowrap min-w-[100px]">
                          <ParameterResultFlagBadge flag={param.flag} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* MOBILE STACKED CARDS VIEW (< sm) */}
            <div className="sm:hidden divide-y divide-slate-100 p-4 space-y-4">
              {test.parameters.map((param) => {
                const key = `${test.refId}_${param.parameterRefId}`;
                const isCalculated = param.inputType === "CALCULATED";
                const displayVal =
                  localParamValues[test.refId]?.[param.parameterRefId] ??
                  param.value ??
                  "";

                return (
                  <div
                    key={param.refId}
                    className="pt-4 first:pt-0 space-y-3"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className="font-bold text-slate-900 text-[15px]">
                          {param.parameterName}
                        </h4>
                        <span className="font-mono text-xs text-slate-400">
                          {param.parameterCode}
                        </span>
                      </div>
                      <ParameterResultFlagBadge flag={param.flag} />
                    </div>

                    {/* Result Input / Box */}
                    <div>
                      <label className="text-xs font-semibold text-slate-600 block mb-1">
                        Result
                      </label>
                      {isCalculated ? (
                        <div className="flex items-center gap-2">
                          <div className="h-11 px-3.5 rounded-lg bg-slate-100 border border-slate-200 flex-1 flex items-center font-mono font-bold text-base text-slate-800">
                            {displayVal || "—"}
                          </div>
                          <Badge
                            variant="outline"
                            className="bg-slate-100 text-slate-600 border-slate-300 text-xs font-medium flex items-center gap-1 shrink-0 h-11 px-3"
                          >
                            <Lock className="h-3.5 w-3.5 text-slate-500" />
                            Calculated
                          </Badge>
                        </div>
                      ) : (
                        <Input
                          ref={(el) => {
                            mobileInputRefs.current[key] = el;
                          }}
                          type={
                            param.dataType === "INTEGER" ||
                            param.dataType === "DECIMAL"
                              ? "number"
                              : "text"
                          }
                          step={param.dataType === "DECIMAL" ? "any" : "1"}
                          value={displayVal}
                          onChange={(e) =>
                            handleValueChange(
                              test.refId,
                              param.parameterRefId,
                              e.target.value
                            )
                          }
                          onKeyDown={(e) =>
                            handleKeyDown(
                              e,
                              key,
                              test.refId,
                              param.parameterRefId,
                              e.currentTarget.value
                            )
                          }
                          onFocus={(e) => {
                            e.currentTarget.select();
                            setActiveParam({
                              reportTestRefId: test.refId,
                              testName: test.testName,
                              param,
                            });
                          }}
                          placeholder="Enter value..."
                          aria-label={`Result for ${param.parameterName}`}
                          className="h-11 px-3 text-base font-semibold font-mono text-slate-900 bg-white border-slate-200 focus:border-[#0F766E] focus:ring-2 focus:ring-[#0F766E]/20"
                        />
                      )}
                    </div>

                    {/* Unit & Reference Metadata Grid */}
                    <div className="grid grid-cols-2 gap-2 text-xs bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                      <div>
                        <span className="text-slate-500">Unit:</span>{" "}
                        <strong className="text-slate-800 font-medium whitespace-nowrap">
                          {formatDisplayUnit(param.unit)}
                        </strong>
                      </div>
                      <div>
                        <span className="text-slate-500">Reference:</span>{" "}
                        <strong className="text-slate-800 font-mono">
                          {formatReferenceRange(param)}
                        </strong>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* FOCUSED PARAMETER DOCKED HELPER (Active parameter guidance) */}
            {activeParam && activeParam.reportTestRefId === test.refId && (
              <div className="bg-[#F0FDF4] border-t border-emerald-200/80 px-4 sm:px-6 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-slate-700">
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-[11px] font-bold text-[#0F766E] uppercase tracking-wider">
                      Active Parameter:
                    </span>
                    <span className="font-bold text-slate-900 text-sm">
                      {activeParam.param.parameterName}
                    </span>
                    <span className="font-mono text-xs text-slate-500 font-semibold">
                      ({activeParam.param.parameterCode})
                    </span>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-slate-600 flex-wrap">
                    <span>
                      Unit: <strong className="text-slate-800 font-medium">{activeParam.param.unit || "—"}</strong>
                    </span>
                    <span className="text-slate-300">&bull;</span>
                    <span>
                      Reference: <strong className="text-slate-800 font-mono font-medium">{formatReferenceRange(activeParam.param)}</strong>
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 text-xs text-[#0F766E] font-medium shrink-0 self-start sm:self-center">
                  <CornerDownLeft className="h-4 w-4" />
                  <span>
                    Press{" "}
                    <kbd className="px-1.5 py-0.5 bg-white border border-emerald-300 rounded font-mono font-bold text-[11px] shadow-2xs">
                      Enter ↵
                    </kbd>{" "}
                    to save & advance
                  </span>
                </div>
              </div>
            )}
          </Card>
        ))
      )}

      {/* ======================================================== */}
      {/* 3. WORKSTATION BOTTOM NAVIGATION CONTROLS                */}
      {/* ======================================================== */}
      <div className="flex items-center justify-between pt-4 border-t border-slate-200">
        <Button
          type="button"
          variant="outline"
          onClick={onBackToTests}
          className="text-xs h-9 px-4 border-slate-300 text-slate-700 cursor-pointer"
        >
          &larr; Back to Tests
        </Button>
        <Button
          type="button"
          onClick={onProceedToReview}
          className="bg-[#0F766E] hover:bg-[#115E59] text-white text-xs h-9 px-5 font-semibold flex items-center gap-1.5 shadow-xs cursor-pointer"
        >
          <span>Proceed to Review & Finalize</span>
          <ArrowRight className="h-4 w-4" />
        </Button>
      </div>

      {/* Add Test Modal */}
      <AddTestModal
        reportRefId={report.refId}
        lockVersion={report.lockVersion}
        existingTestRefIds={existingTestRefIds}
        open={isAddTestOpen}
        onOpenChange={setIsAddTestOpen}
      />

      {/* Remove Test Confirmation Dialog */}
      <Dialog
        open={Boolean(testToDelete)}
        onOpenChange={(open) => !open && setTestToDelete(null)}
      >
        <DialogContent className="max-w-md p-5">
          <DialogHeader>
            <DialogTitle className="text-base font-bold text-slate-900">
              Remove {testToDelete?.testName}?
            </DialogTitle>
            <DialogDescription className="text-xs text-slate-500 pt-1">
              Are you sure you want to remove this test panel and its parameter values from this report draft?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="flex flex-row justify-end gap-2 pt-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={isDeletingTest}
              onClick={() => setTestToDelete(null)}
              className="text-xs border-slate-300 text-slate-700 cursor-pointer"
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="destructive"
              size="sm"
              disabled={isDeletingTest}
              onClick={handleConfirmDeleteTest}
              className="text-xs font-bold cursor-pointer"
            >
              {isDeletingTest ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
                  Removing...
                </>
              ) : (
                "Remove Test"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
