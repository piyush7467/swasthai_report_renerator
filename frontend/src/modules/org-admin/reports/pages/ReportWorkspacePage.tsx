import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate, useLocation, Link } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  FilePlus,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";

import {
  useReportQuery,
  useCreateReportMutation,
  useAddReportTestsBulkMutation,
  useRemoveReportTestMutation,
  useUpdateParametersMutation,
  useUpdateHeaderOptionMutation,
  useRecalculateReportMutation,
  useFinalizeReportMutation,
} from "../hooks/useReports";
import { usePatientQuery } from "../../patients/hooks/usePatients";
import type { PatientResponse } from "../../patients/types/patientTypes";
import type { ReportStepIndex } from "../components/ReportWorkflowStepper";

import { ReportWorkflowStepper } from "../components/ReportWorkflowStepper";
import { Step1Patient } from "../components/steps/Step1Patient";
import { Step2Doctor } from "../components/steps/Step2Doctor";
import { Step3SelectTests } from "../components/steps/Step3SelectTests";
import { Step4EnterResults } from "../components/steps/Step4EnterResults";
import { Step5ReviewReport } from "../components/steps/Step5ReviewReport";
import { FinalizedReportView } from "../components/FinalizedReportView";

export default function ReportWorkspacePage() {
  const { reportRefId } = useParams<{ reportRefId: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  const isNewReportFlow = !reportRefId || reportRefId === "new";
  const basePath = location.pathname.startsWith("/lab-staff")
    ? "/lab-staff/reports"
    : "/org-admin/reports";

  // Stepper State
  const [currentStep, setCurrentStep] = useState<ReportStepIndex>(1);
  const [selectedPatientForNew, setSelectedPatientForNew] =
    useState<PatientResponse | null>(null);
  const [selectedTestRefIdsForNew, setSelectedTestRefIdsForNew] = useState<
    string[]
  >([]);
  const [isCreatingDraft, setIsCreatingDraft] = useState(false);
  const [lockConflictError, setLockConflictError] = useState<string | null>(
    null
  );

  // Queries
  const {
    data: report,
    isLoading: isReportLoading,
    isError: isReportError,
    error: reportError,
    refetch: refetchReport,
  } = useReportQuery(isNewReportFlow ? undefined : reportRefId);

  const patientRefIdToFetch = isNewReportFlow
    ? selectedPatientForNew?.refId
    : report?.patientRefId;

  const { data: patientData } = usePatientQuery(patientRefIdToFetch);
  const activePatient = isNewReportFlow ? selectedPatientForNew : patientData;

  // Mutations
  const createReportMutation = useCreateReportMutation();
  const addTestsBulkMutation = useAddReportTestsBulkMutation();
  const removeTestMutation = useRemoveReportTestMutation();
  const updateParamsMutation = useUpdateParametersMutation();
  const updateHeaderMutation = useUpdateHeaderOptionMutation();
  const recalculateMutation = useRecalculateReportMutation();
  const finalizeMutation = useFinalizeReportMutation();

  // Set initial step for existing draft reports
  useEffect(() => {
    if (report) {
      if (report.status === "FINALIZED") return;

      // If existing draft has no tests yet, start at Step 3, otherwise Step 4
      if (report.tests.length === 0) {
        setCurrentStep((prev) => (prev < 3 ? 3 : prev));
      } else {
        setCurrentStep((prev) => (prev === 1 ? 4 : prev));
      }
    }
  }, [report]);

  // Handle parameters update from Step 4
  const handleUpdateParameters = useCallback(
    async (
      reportTestRefId: string,
      parameters: { parameterRefId: string; value: string }[],
      lockVersion: number
    ) => {
      if (!report) return;
      try {
        await updateParamsMutation.mutateAsync({
          reportRefId: report.refId,
          reportTestRefId,
          request: {
            lockVersion,
            parameters,
          },
        });
        setLockConflictError(null);
      } catch (err: unknown) {
        const errorText = err instanceof Error ? err.message : String(err);
        if (
          errorText.toLowerCase().includes("lock") ||
          errorText.toLowerCase().includes("optimistic")
        ) {
          setLockConflictError(
            "This report was modified concurrently. Please refresh the latest data."
          );
        }
        throw err;
      }
    },
    [report, updateParamsMutation]
  );

  // Handle header option toggle
  const handleUpdateHeaderOption = async (include: boolean) => {
    if (!report) return;
    await updateHeaderMutation.mutateAsync({
      reportRefId: report.refId,
      request: { includeOrganizationHeader: include },
    });
  };

  // Handle recalculate
  const handleRecalculate = async () => {
    if (!report) return;
    await recalculateMutation.mutateAsync(report.refId);
  };

  // Handle removing a test
  const handleRemoveTest = async (reportTestRefId: string) => {
    if (!report) return;
    await removeTestMutation.mutateAsync({
      reportRefId: report.refId,
      reportTestRefId,
    });
  };

  // Handle confirming tests in Step 3
  const handleConfirmTests = async (testRefIds: string[]) => {
    setSelectedTestRefIdsForNew(testRefIds);
    if (isNewReportFlow) {
      if (!selectedPatientForNew) return;
      setIsCreatingDraft(true);
      try {
        // 1. Create Report Draft
        const createdReport = await createReportMutation.mutateAsync({
          patientRefId: selectedPatientForNew.refId,
          includeOrganizationHeader: true,
        });

        // 2. Add Selected Tests in Bulk
        if (testRefIds.length > 0) {
          await addTestsBulkMutation.mutateAsync({
            reportRefId: createdReport.refId,
            request: {
              testRefIds,
              lockVersion: createdReport.lockVersion,
            },
          });
        }

        // 3. Seamlessly transition to Step 4 on the created report URL
        navigate(`${basePath}/${createdReport.refId}`, { replace: true });
        setCurrentStep(4);
      } finally {
        setIsCreatingDraft(false);
      }
    } else if (report) {
      // Existing draft: add any newly selected testRefIds
      const existingIds = new Set(report.tests.map((t) => t.testRefId));
      const newToAdd = testRefIds.filter((id) => !existingIds.has(id));

      if (newToAdd.length > 0) {
        await addTestsBulkMutation.mutateAsync({
          reportRefId: report.refId,
          request: {
            testRefIds: newToAdd,
            lockVersion: report.lockVersion,
          },
        });
      }
      setCurrentStep(4);
    }
  };

  // Finalize
  const handleFinalize = async () => {
    if (!report) return;
    await finalizeMutation.mutateAsync(report.refId);
  };

  // Loading state
  if (!isNewReportFlow && isReportLoading) {
    return (
      <div className="space-y-6 max-w-5xl mx-auto pb-12">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-80 w-full" />
      </div>
    );
  }

  // Error state
  if (!isNewReportFlow && (isReportError || !report)) {
    return (
      <div className="space-y-4 max-w-md mx-auto py-12 text-center">
        <Alert variant="destructive" className="text-left">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Report Not Found</AlertTitle>
          <AlertDescription className="text-xs">
            {reportError?.message ||
              "Could not retrieve the requested diagnostic report draft."}
          </AlertDescription>
        </Alert>
        <Button asChild variant="outline" size="sm">
          <Link to={basePath}>Back to Reports</Link>
        </Button>
      </div>
    );
  }

  // If report is already FINALIZED, render the locked clinical sheet directly
  if (report && report.status === "FINALIZED") {
    return <FinalizedReportView report={report} patient={activePatient} />;
  }

  // Determine max step user can navigate to
  const maxAccessibleStep: ReportStepIndex = isNewReportFlow
    ? selectedPatientForNew
      ? 3
      : 1
    : 5;

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-20">
      {/* Top Header & Back Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="h-8 px-2 text-xs text-slate-600 hover:text-slate-900"
          >
            <Link to={basePath}>
              <ArrowLeft className="h-3.5 w-3.5 mr-1" />
              Reports List
            </Link>
          </Button>

          <div className="h-4 w-px bg-slate-200" />

          <div className="flex items-center gap-2">
            <h1 className="text-base sm:text-lg font-bold text-slate-900 font-mono">
              {isNewReportFlow ? (
                <span className="flex items-center gap-1.5 font-sans">
                  <FilePlus className="h-4 w-4 text-[#0F766E]" />
                  New Diagnostic Report
                </span>
              ) : (
                `Report #${report?.refId}`
              )}
            </h1>
          </div>
        </div>
      </div>

      {/* 5-Step Workflow Stepper Bar */}
      <ReportWorkflowStepper
        currentStep={currentStep}
        maxAccessibleStep={maxAccessibleStep}
        onSelectStep={(step) => setCurrentStep(step)}
      />

      {/* STEP CONTENT RENDERING */}
      {currentStep === 1 && (
        <Step1Patient
          selectedPatient={activePatient ?? null}
          onSelectPatient={(p) => {
            setSelectedPatientForNew(p);
          }}
          onClearPatient={() => {
            setSelectedPatientForNew(null);
          }}
          onContinue={() => setCurrentStep(2)}
          readOnly={!isNewReportFlow}
        />
      )}

      {currentStep === 2 && (
        <Step2Doctor
          selectedPatient={activePatient ?? null}
          onBack={() => setCurrentStep(1)}
          onContinue={() => setCurrentStep(3)}
        />
      )}

      {currentStep === 3 && (
        <Step3SelectTests
          initialSelectedTestRefIds={selectedTestRefIdsForNew}
          alreadyAddedTests={report?.tests ?? []}
          onRemoveExistingTest={handleRemoveTest}
          onConfirm={handleConfirmTests}
          onBack={() => setCurrentStep(2)}
          isLoading={isCreatingDraft || addTestsBulkMutation.isPending}
        />
      )}

      {currentStep === 4 && report && (
        <Step4EnterResults
          report={report}
          patient={activePatient}
          onUpdateParameters={handleUpdateParameters}
          onRecalculate={handleRecalculate}
          onRemoveTest={handleRemoveTest}
          onProceedToReview={() => setCurrentStep(5)}
          onBackToTests={() => setCurrentStep(3)}
          lockConflictError={lockConflictError}
          onRefreshReport={() => refetchReport()}
        />
      )}

      {currentStep === 5 && report && (
        <Step5ReviewReport
          report={report}
          patient={activePatient}
          onUpdateHeaderOption={handleUpdateHeaderOption}
          onRecalculate={handleRecalculate}
          onFinalize={handleFinalize}
          onBackToEditor={() => setCurrentStep(4)}
          isFinalizing={finalizeMutation.isPending}
        />
      )}
    </div>
  );
}
