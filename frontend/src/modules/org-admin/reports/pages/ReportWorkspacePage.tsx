import { useState, useEffect, useRef, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Building2,
  CheckCircle2,
  FileCheck2,
  FlaskConical,
  Loader2,
  Plus,
  RefreshCw,
  Trash2,
  User,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

import {
  useReportQuery,
  useRemoveReportTestMutation,
  useUpdateParametersMutation,
  useUpdateHeaderOptionMutation,
  useFinalizeReportMutation,
} from "../hooks/useReports";
import { usePatientQuery } from "../../patients/hooks/usePatients";
import { ReportStatusBadge } from "../components/ReportStatusBadge";
import { ParameterResultFlagBadge } from "../components/ParameterResultFlagBadge";
import { AddTestModal } from "../components/AddTestModal";
import { FinalizeReportDialog } from "../components/FinalizeReportDialog";
import { FinalizedReportView } from "../components/FinalizedReportView";

export default function ReportWorkspacePage() {
  const { reportRefId } = useParams<{ reportRefId: string }>();

  const {
    data: report,
    isLoading: isReportLoading,
    isError: isReportError,
    error: reportError,
    refetch: refetchReport,
  } = useReportQuery(reportRefId);

  const { data: patient } = usePatientQuery(report?.patientRefId);

  // Modals state
  const [isAddTestOpen, setIsAddTestOpen] = useState(false);
  const [isFinalizeOpen, setIsFinalizeOpen] = useState(false);
  const [lockConflictError, setLockConflictError] = useState<string | null>(null);

  // Autosave status state
  const [saveStatus, setSaveStatus] = useState<"IDLE" | "SAVING" | "SAVED" | "ERROR">("IDLE");

  // Local parameter values state: { [reportTestRefId]: { [parameterRefId]: string } }
  const [localParamValues, setLocalParamValues] = useState<Record<string, Record<string, string>>>({});
  const pendingSaveTimeoutRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

  // Mutations
  const removeTestMutation = useRemoveReportTestMutation();
  const updateParamsMutation = useUpdateParametersMutation();
  const updateHeaderMutation = useUpdateHeaderOptionMutation();
  const finalizeMutation = useFinalizeReportMutation();

  // Initialize local parameter values when report data loads
  useEffect(() => {
    if (report?.tests) {
      const initial: Record<string, Record<string, string>> = {};
      report.tests.forEach((test) => {
        initial[test.refId] = {};
        test.parameters.forEach((param) => {
          initial[test.refId][param.parameterRefId] = param.value ?? "";
        });
      });
      setLocalParamValues(initial);
      setLockConflictError(null);
    }
  }, [report]);

  // Handle parameter value change with debounced autosave
  const handleParamChange = useCallback(
    (reportTestRefId: string, parameterRefId: string, newValue: string) => {
      setLocalParamValues((prev) => ({
        ...prev,
        [reportTestRefId]: {
          ...prev[reportTestRefId],
          [parameterRefId]: newValue,
        },
      }));

      // Clear existing timer for this test
      if (pendingSaveTimeoutRef.current[reportTestRefId]) {
        clearTimeout(pendingSaveTimeoutRef.current[reportTestRefId]);
      }

      setSaveStatus("SAVING");

      // Set 800ms debounce timer for autosave
      pendingSaveTimeoutRef.current[reportTestRefId] = setTimeout(async () => {
        if (!report) return;

        // Gather all manual parameters for this test
        const currentTest = report.tests.find((t) => t.refId === reportTestRefId);
        if (!currentTest) return;

        const manualParameters = currentTest.parameters
          .filter((p) => p.inputType === "MANUAL")
          .map((p) => ({
            parameterRefId: p.parameterRefId,
            value: p.parameterRefId === parameterRefId ? newValue : localParamValues[reportTestRefId]?.[p.parameterRefId] ?? p.value ?? "",
          }));

        try {
          await updateParamsMutation.mutateAsync({
            reportRefId: report.refId,
            reportTestRefId,
            request: {
              lockVersion: report.lockVersion,
              parameters: manualParameters,
            },
          });
          setSaveStatus("SAVED");
          setLockConflictError(null);
        } catch (err: unknown) {
          setSaveStatus("ERROR");
          const errorText = err instanceof Error ? err.message : String(err);
          if (errorText.toLowerCase().includes("lock") || errorText.toLowerCase().includes("optimistic")) {
            setLockConflictError(
              "This report was updated elsewhere. Please refresh the report before continuing."
            );
          }
        }
      }, 800);
    },
    [report, localParamValues, updateParamsMutation]
  );

  // Manual trigger to flush all pending values immediately and recalculate formulas
  const handleCalculateAll = async () => {
    if (!report) return;
    setSaveStatus("SAVING");
    setLockConflictError(null);

    try {
      for (const test of report.tests) {
        const manualParameters = test.parameters
          .filter((p) => p.inputType === "MANUAL")
          .map((p) => ({
            parameterRefId: p.parameterRefId,
            value: localParamValues[test.refId]?.[p.parameterRefId] ?? p.value ?? "",
          }));

        if (manualParameters.length > 0) {
          await updateParamsMutation.mutateAsync({
            reportRefId: report.refId,
            reportTestRefId: test.refId,
            request: {
              lockVersion: report.lockVersion,
              parameters: manualParameters,
            },
          });
        }
      }
      setSaveStatus("SAVED");
    } catch (err: unknown) {
      setSaveStatus("ERROR");
      const errorText = err instanceof Error ? err.message : String(err);
      if (errorText.toLowerCase().includes("lock") || errorText.toLowerCase().includes("optimistic")) {
        setLockConflictError(
          "This report was updated elsewhere. Please refresh the report before continuing."
        );
      }
    }
  };

  const handleHeaderOptionToggle = async (include: boolean) => {
    if (!report) return;
    try {
      await updateHeaderMutation.mutateAsync({
        reportRefId: report.refId,
        request: { includeOrganizationHeader: include },
      });
    } catch {
      // Handled by react-query
    }
  };

  const handleRemoveTest = async (reportTestRefId: string) => {
    if (!report) return;
    try {
      await removeTestMutation.mutateAsync({
        reportRefId: report.refId,
        reportTestRefId,
      });
    } catch {
      // Handled by react-query
    }
  };

  const handleFinalizeConfirm = async () => {
    if (!report) return;
    try {
      await finalizeMutation.mutateAsync(report.refId);
      setIsFinalizeOpen(false);
    } catch {
      // Error handled by dialog / react-query
    }
  };

  if (isReportLoading) {
    return (
      <div className="space-y-6 max-w-5xl mx-auto pb-12">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isReportError || !report) {
    return (
      <div className="space-y-4 max-w-md mx-auto py-12 text-center">
        <Alert variant="destructive" className="text-left">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Report Not Found</AlertTitle>
          <AlertDescription className="text-xs">
            {reportError?.message || "Could not retrieve the requested diagnostic report."}
          </AlertDescription>
        </Alert>
        <Button asChild variant="outline" size="sm">
          <Link to="/org-admin/reports">Back to Reports List</Link>
        </Button>
      </div>
    );
  }

  // If report is FINALIZED, render the read-only clinical preview with PDF download
  if (report.status === "FINALIZED") {
    return <FinalizedReportView report={report} patient={patient} />;
  }

  const existingTestRefIds = report.tests.map((t) => t.testRefId);

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-20">
      {/* Workspace Navigation Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Button asChild variant="ghost" size="sm" className="h-8 px-2 text-xs text-slate-600">
            <Link to="/org-admin/reports">
              <ArrowLeft className="h-3.5 w-3.5 mr-1" />
              Reports
            </Link>
          </Button>

          <div className="h-4 w-px bg-slate-200" />

          <div className="flex items-center gap-2">
            <h1 className="text-lg font-bold text-slate-900 font-mono">
              #{report.refId}
            </h1>
            <ReportStatusBadge status={report.status} />
            <span className="text-[11px] font-mono text-slate-400">
              (v{report.reportVersion})
            </span>
          </div>
        </div>

        {/* Action Controls & Autosave Indicator */}
        <div className="flex items-center gap-2.5">
          {/* Autosave Status Indicator */}
          <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium px-2 py-1">
            {saveStatus === "SAVING" ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin text-blue-600" />
                <span>Saving...</span>
              </>
            ) : saveStatus === "SAVED" ? (
              <>
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                <span>All changes saved</span>
              </>
            ) : saveStatus === "ERROR" ? (
              <>
                <AlertCircle className="h-3.5 w-3.5 text-rose-600" />
                <span className="text-rose-600 font-medium">Save failed</span>
              </>
            ) : (
              <span>Draft mode</span>
            )}
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleCalculateAll}
            disabled={updateParamsMutation.isPending}
            className="gap-1.5 text-xs text-slate-700 hover:bg-slate-50"
          >
            <RefreshCw className="h-3.5 w-3.5 text-blue-600" />
            Recalculate
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={() => setIsFinalizeOpen(true)}
            disabled={report.tests.length === 0 || finalizeMutation.isPending}
            className="gap-1.5 text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-semibold shadow-xs"
          >
            <FileCheck2 className="h-3.5 w-3.5" />
            Finalize Report
          </Button>
        </div>
      </div>

      {/* Optimistic Locking Conflict Warning Banner */}
      {lockConflictError && (
        <Alert variant="destructive" className="border-rose-300 bg-rose-50">
          <AlertTriangle className="h-4 w-4 text-rose-600" />
          <AlertTitle className="text-xs font-semibold text-rose-900">
            Concurrent Modification Conflict
          </AlertTitle>
          <AlertDescription className="text-xs text-rose-800 flex items-center justify-between gap-4 mt-1">
            <span>{lockConflictError}</span>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                void refetchReport();
                setLockConflictError(null);
              }}
              className="text-xs border-rose-300 text-rose-900 hover:bg-rose-100 h-7"
            >
              <RefreshCw className="h-3 w-3 mr-1" />
              Refresh Report
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Patient & Organization Header Strip */}
      <Card className="border-slate-200 bg-white shadow-xs">
        <CardContent className="p-4 sm:p-5">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            {/* Patient Demographics */}
            <div className="flex items-start gap-3">
              <div className="p-2.5 rounded-full bg-blue-50 text-blue-600 shrink-0 mt-0.5">
                <User className="h-5 w-5" />
              </div>
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-sm sm:text-base text-slate-900">
                    {patient ? `${patient.salutation ? `${patient.salutation}. ` : ""}${patient.name}` : "Patient"}
                  </span>
                  <span className="font-mono text-xs text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded font-medium">
                    {patient?.patientCode || report.patientRefId}
                  </span>
                </div>
                <p className="text-xs text-slate-500">
                  Gender: <span className="font-medium text-slate-700">{patient?.gender || "—"}</span> &bull; Age:{" "}
                  <span className="font-medium text-slate-700">
                    {patient?.ageValue != null ? `${patient.ageValue} ${patient.ageUnit?.toLowerCase() ?? "yrs"}` : "Unspecified"}
                  </span>{" "}
                  {patient?.phone ? `&bull; Tel: ${patient.phone}` : ""}
                </p>
              </div>
            </div>

            {/* Organization Header Toggle */}
            <div className="flex items-center gap-3 pt-3 md:pt-0 border-t md:border-t-0 border-slate-100">
              <div className="text-right">
                <label
                  htmlFor="workspace-header-toggle"
                  className="text-xs font-semibold text-slate-800 flex items-center gap-1.5 cursor-pointer justify-end"
                >
                  <Building2 className="h-3.5 w-3.5 text-slate-500" />
                  Print Organization Header
                </label>
                <p className="text-[11px] text-slate-400">
                  {report.includeOrganizationHeader ? "Branding included in PDF" : "Branding omitted"}
                </p>
              </div>
              <input
                id="workspace-header-toggle"
                type="checkbox"
                checked={report.includeOrganizationHeader}
                onChange={(e) => handleHeaderOptionToggle(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 cursor-pointer"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tests and Parameters Section */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-slate-900">
              Assigned Diagnostic Tests ({report.tests.length})
            </h2>
            <p className="text-xs text-slate-500">
              Enter clinical observations and values below. Calculated parameters update automatically.
            </p>
          </div>

          <Button
            size="sm"
            onClick={() => setIsAddTestOpen(true)}
            className="gap-1.5 text-xs bg-purple-600 hover:bg-purple-700 text-white font-semibold"
          >
            <Plus className="h-3.5 w-3.5" />
            Add Test
          </Button>
        </div>

        {report.tests.length === 0 ? (
          <Card className="border-dashed border-slate-300 bg-slate-50/50">
            <CardHeader className="text-center py-12">
              <FlaskConical className="mx-auto h-10 w-10 text-slate-300 mb-2" />
              <CardTitle className="text-sm font-semibold text-slate-900">
                No Tests Added Yet
              </CardTitle>
              <CardDescription className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                Add one or more diagnostic tests from your laboratory catalog to begin entering patient investigation results.
              </CardDescription>
              <Button
                size="sm"
                onClick={() => setIsAddTestOpen(true)}
                className="mt-4 text-xs bg-purple-600 hover:bg-purple-700 text-white mx-auto"
              >
                <Plus className="mr-1.5 h-3.5 w-3.5" />
                Add First Test
              </Button>
            </CardHeader>
          </Card>
        ) : (
          report.tests.map((test) => (
            <Card key={test.refId} className="border-slate-200 bg-white shadow-xs overflow-hidden">
              <CardHeader className="bg-slate-50/75 border-b border-slate-200 py-3 px-4 sm:px-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <span className="font-bold text-sm text-slate-900">
                      {test.testName}
                    </span>
                    <span className="font-mono text-xs text-slate-500 bg-slate-200/70 px-2 py-0.5 rounded font-medium">
                      {test.testCode}
                    </span>
                  </div>

                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => handleRemoveTest(test.refId)}
                    disabled={removeTestMutation.isPending}
                    className="text-xs text-slate-400 hover:text-rose-600 h-7 px-2"
                  >
                    <Trash2 className="h-3.5 w-3.5 mr-1" />
                    Remove Test
                  </Button>
                </div>
              </CardHeader>

              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px] tracking-wider bg-slate-50/30">
                        <th className="py-2.5 px-4 w-12">#</th>
                        <th className="py-2.5 px-4">Parameter Name</th>
                        <th className="py-2.5 px-3">Code</th>
                        <th className="py-2.5 px-3 w-44">Observation Result</th>
                        <th className="py-2.5 px-3">Flag</th>
                        <th className="py-2.5 px-3">Unit</th>
                        <th className="py-2.5 px-3">Reference Interval</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {test.parameters.map((param) => {
                        const isCalculated = param.inputType === "CALCULATED";
                        const currentValue = localParamValues[test.refId]?.[param.parameterRefId] ?? param.value ?? "";

                        return (
                          <tr key={param.refId} className="hover:bg-slate-50/50 transition-colors">
                            <td className="py-2.5 px-4 font-mono text-slate-400 text-[11px]">
                              {param.displayOrder}
                            </td>

                            <td className="py-2.5 px-4">
                              <span className="font-semibold text-slate-900 block">
                                {param.parameterName}
                              </span>
                              {isCalculated && (
                                <span className="font-mono text-[10px] text-indigo-600 bg-indigo-50 px-1.5 py-0.2 rounded font-medium inline-block mt-0.5">
                                  CALC: {param.calculationType}
                                </span>
                              )}
                            </td>

                            <td className="py-2.5 px-3 font-mono text-slate-500 text-[11px]">
                              {param.parameterCode}
                            </td>

                            {/* Result Entry / Display */}
                            <td className="py-2 px-3">
                              {isCalculated ? (
                                <div className="font-mono font-bold text-xs text-indigo-950 bg-indigo-50/50 border border-indigo-100 rounded px-2.5 py-1.5">
                                  {param.value || <span className="text-slate-400 font-normal italic">Pending...</span>}
                                </div>
                              ) : (
                                <Input
                                  type={param.dataType === "INTEGER" || param.dataType === "DECIMAL" ? "text" : "text"}
                                  value={currentValue}
                                  placeholder="Enter result..."
                                  onChange={(e) =>
                                    handleParamChange(test.refId, param.parameterRefId, e.target.value)
                                  }
                                  className="h-8 text-xs font-mono font-medium focus-visible:ring-blue-500"
                                />
                              )}
                            </td>

                            {/* Flag */}
                            <td className="py-2.5 px-3">
                              <ParameterResultFlagBadge flag={param.flag} />
                            </td>

                            {/* Unit */}
                            <td className="py-2.5 px-3 font-mono text-slate-600 text-[11px]">
                              {param.unit || "—"}
                            </td>

                            {/* Reference Limits */}
                            <td className="py-2.5 px-3 font-mono text-slate-600 text-[11px]">
                              {param.referenceMin != null || param.referenceMax != null ? (
                                <span>
                                  {param.referenceMin ?? 0} &ndash; {param.referenceMax ?? "∞"}
                                </span>
                              ) : (
                                "—"
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Modals */}
      <AddTestModal
        reportRefId={report.refId}
        lockVersion={report.lockVersion}
        existingTestRefIds={existingTestRefIds}
        open={isAddTestOpen}
        onOpenChange={setIsAddTestOpen}
      />

      <FinalizeReportDialog
        report={report}
        open={isFinalizeOpen}
        onOpenChange={setIsFinalizeOpen}
        onConfirm={handleFinalizeConfirm}
        isLoading={finalizeMutation.isPending}
      />
    </div>
  );
}
