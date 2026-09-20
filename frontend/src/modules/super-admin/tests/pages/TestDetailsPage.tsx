import { useState } from "react";
import { useParams, useNavigate, Link, useSearchParams } from "react-router-dom";
import axios from "axios";
import {
  AlertCircle,
  ArrowLeft,
  ArrowUpDown,
  Building2,
  Check,
  CheckCircle2,
  Clock,
  Coins,
  Copy,
  Edit,
  ExternalLink,
  Eye,
  FileSpreadsheet,
  FlaskConical,
  Loader2,
  MoreVertical,
  Plus,
  RefreshCw,
  RotateCcw,
  Sliders,
  Trash2,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import type { ApiErrorResponse } from "@/core/auth/authTypes";

import {
  useTestQuery,
  useDeleteTestMutation,
  useUpdateTestMutation,
} from "../hooks/useTests";
import {
  useTestParametersQuery,
  useDeactivateParameterMutation,
  useUpdateParameterMutation,
} from "../hooks/useParameters";
import { useOrganizationsQuery } from "../../hooks/useOrganizations";
import {
  useAssignmentsQuery,
  useDeactivateAssignmentMutation,
} from "../hooks/useAssignments";
import { organizationTestApi } from "../api/organizationTestApi";

import { TestStatusBadge } from "../components/TestStatusBadge";
import { TestTypeBadge } from "../components/TestTypeBadge";
import { ParameterDataTypeBadge } from "../components/ParameterDataTypeBadge";
import { ConfirmDeactivateDialog } from "../components/ConfirmDeactivateDialog";
import { ParameterDetailsDialog } from "../components/ParameterDetailsDialog";
import { UpdateAssignmentDialog } from "../components/UpdateAssignmentDialog";

import type { TestParameterResponse } from "../types/parameterTypes";
import type { OrganizationTestResponse } from "../types/assignmentTypes";

export function TestDetailsPage() {
  const { refId } = useParams<{ refId: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const activeTab = searchParams.get("tab") || "overview";
  const handleTabChange = (tab: string) => {
    setSearchParams({ tab });
  };

  const [copied, setCopied] = useState(false);

  // Dialog targets
  const [deactivateTestOpen, setDeactivateTestOpen] = useState(false);
  const [deactivateParamTarget, setDeactivateParamTarget] =
    useState<TestParameterResponse | null>(null);
  const [selectedParamForDetails, setSelectedParamForDetails] =
    useState<TestParameterResponse | null>(null);
  const [assignmentToEdit, setAssignmentToEdit] =
    useState<OrganizationTestResponse | null>(null);
  const [deactivateAssignmentTarget, setDeactivateAssignmentTarget] =
    useState<OrganizationTestResponse | null>(null);

  // Parameter tab filters & pagination
  const [paramStatus, setParamStatus] = useState<string>("ALL");
  const [paramSort, setParamSort] = useState<
    "displayOrder" | "name" | "code" | "dataType"
  >("displayOrder");
  const [paramDirection, setParamDirection] = useState<"asc" | "desc">("asc");
  const [paramPage, setParamPage] = useState(0);

  // Assignments tab state
  const [selectedOrgRefId, setSelectedOrgRefId] = useState<string>("");
  const [isAssigningSingle, setIsAssigningSingle] = useState(false);
  const [assignSuccessMsg, setAssignSuccessMsg] = useState<string | null>(null);
  const [assignErrorMsg, setAssignErrorMsg] = useState<string | null>(null);

  // 1. Fetch Test Details
  const {
    data: test,
    isLoading: testLoading,
    isError: testIsError,
    error: testError,
    refetch: refetchTest,
  } = useTestQuery(refId);

  // 2. Fetch Parameters for this Test
  const {
    data: paramsData,
    isLoading: paramsLoading,
    refetch: refetchParams,
  } = useTestParametersQuery(refId, {
    status: paramStatus !== "ALL" ? paramStatus : undefined,
    page: paramPage,
    size: 20,
    sort: paramSort,
    direction: paramDirection,
  });

  // 3. Fetch Organizations for Assignments tab
  const { data: orgsData, isLoading: orgsLoading } = useOrganizationsQuery({
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const effectiveOrgRefId =
    selectedOrgRefId ||
    (orgsData?.content && orgsData.content.length > 0
      ? orgsData.content[0].refId
      : "");

  const selectedOrg = orgsData?.content?.find(
    (o) => o.refId === effectiveOrgRefId,
  );

  // 4. Fetch assignments for currently selected organization
  const {
    data: orgAssignmentsData,
    isLoading: orgAssignmentsLoading,
    refetch: refetchOrgAssignments,
  } = useAssignmentsQuery({
    organizationRefId: effectiveOrgRefId,
    size: 100,
  });

  const currentAssignmentForTest = orgAssignmentsData?.content?.find(
    (a) => a.testRefId === test?.refId,
  );

  // Mutations
  const deleteTestMutation = useDeleteTestMutation();
  const updateTestMutation = useUpdateTestMutation();
  const deactivateParamMutation = useDeactivateParameterMutation();
  const updateParamMutation = useUpdateParameterMutation();
  const deactivateAssignmentMutation = useDeactivateAssignmentMutation();

  const copyRefId = async () => {
    if (!test?.refId) return;
    await navigator.clipboard.writeText(test.refId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDeactivateTest = async () => {
    if (!test) return;
    await deleteTestMutation.mutateAsync(test.refId);
  };

  const handleReactivateTest = async () => {
    if (!test) return;
    await updateTestMutation.mutateAsync({
      refId: test.refId,
      request: { status: "ACTIVE" },
    });
  };

  const handleDeactivateParameter = async () => {
    if (!deactivateParamTarget) return;
    await deactivateParamMutation.mutateAsync(deactivateParamTarget.refId);
    setDeactivateParamTarget(null);
  };

  const handleReactivateParam = async (param: TestParameterResponse) => {
    await updateParamMutation.mutateAsync({
      parameterRefId: param.refId,
      request: { status: "ACTIVE" },
    });
  };

  const handleAssignToSelectedOrg = async () => {
    if (!effectiveOrgRefId || !test) return;
    setIsAssigningSingle(true);
    setAssignSuccessMsg(null);
    setAssignErrorMsg(null);
    try {
      await organizationTestApi.assignTest({
        organizationRefId: effectiveOrgRefId,
        testRefId: test.refId,
      });
      void refetchOrgAssignments();
      setAssignSuccessMsg(
        `Successfully assigned ${test.name} to ${selectedOrg?.name || "the organization"}.`,
      );
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        setAssignErrorMsg(err.response?.data?.message || err.message);
      } else {
        setAssignErrorMsg("Failed to assign test to organization.");
      }
    } finally {
      setIsAssigningSingle(false);
    }
  };

  const handleDeactivateAssignment = async () => {
    if (!deactivateAssignmentTarget) return;
    await deactivateAssignmentMutation.mutateAsync(
      deactivateAssignmentTarget.refId,
    );
    setDeactivateAssignmentTarget(null);
    void refetchOrgAssignments();
  };

  if (testLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (testIsError || !test) {
    return (
      <div className="space-y-4">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate("/super-admin/tests")}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Test Catalog
        </Button>
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Test Not Found</AlertTitle>
          <AlertDescription>
            {testError?.message || "Could not retrieve the requested diagnostic test details."}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const paramTotalPages = paramsData?.totalPages ?? 0;

  return (
    <div className="space-y-6 pb-12">
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <Link
          to="/super-admin/tests"
          className="inline-flex items-center text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
          Back to Test Catalog
        </Link>
      </div>

      {/* Clinical Test Header Banner */}
      <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-xs">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-mono text-xs font-semibold uppercase tracking-wider text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                {test.code}
              </span>
              <TestTypeBadge type={test.testType} />
              <TestStatusBadge status={test.status} />
              {test.prioritySupported && (
                <Badge
                  variant="outline"
                  className="border-amber-200 bg-amber-50 text-amber-800 text-[11px]"
                >
                  Priority / STAT
                </Badge>
              )}
            </div>

            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              {test.name}
            </h1>

            {test.shortName && (
              <p className="text-sm font-medium text-slate-500">
                Short Display Name:{" "}
                <span className="text-slate-800">{test.shortName}</span>
              </p>
            )}

            <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 pt-1">
              <span className="flex items-center gap-1.5">
                <FlaskConical className="h-3.5 w-3.5 text-blue-600" />
                Category:{" "}
                <strong className="text-slate-700">{test.categoryName}</strong>
              </span>

              <span className="flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-slate-400" />
                Turnaround:{" "}
                <strong className="text-slate-700">
                  {test.turnaroundTimeHours ? `${test.turnaroundTimeHours}h` : "Standard"}
                </strong>
              </span>

              <span className="flex items-center gap-1.5">
                <Coins className="h-3.5 w-3.5 text-emerald-600" />
                Base Price:{" "}
                <strong className="text-slate-700">
                  {test.basePrice != null ? `${test.currency || "INR"} ${test.basePrice}` : "Not set"}
                </strong>
              </span>

              <div className="flex items-center gap-1">
                <span className="text-slate-400">Ref:</span>
                <span className="font-mono text-[11px] text-slate-600">{test.refId}</span>
                <button
                  onClick={copyRefId}
                  className="text-slate-400 hover:text-slate-600 p-0.5"
                  title="Copy Reference ID"
                >
                  {copied ? (
                    <Check className="h-3 w-3 text-emerald-600" />
                  ) : (
                    <Copy className="h-3 w-3" />
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Header Action Buttons */}
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                void refetchTest();
                void refetchParams();
                void refetchOrgAssignments();
              }}
            >
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>

            <Button
              size="sm"
              onClick={() => navigate(`/super-admin/tests/${test.refId}/edit`)}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              <Edit className="mr-2 h-4 w-4" />
              Edit Test
            </Button>

            {test.status === "ACTIVE" ? (
              <Button
                size="sm"
                variant="outline"
                className="text-amber-600 border-amber-200 hover:bg-amber-50"
                onClick={() => setDeactivateTestOpen(true)}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Deactivate
              </Button>
            ) : (
              <Button
                size="sm"
                variant="outline"
                className="text-emerald-600 border-emerald-200 hover:bg-emerald-50"
                onClick={handleReactivateTest}
                disabled={updateTestMutation.isPending}
              >
                <RotateCcw className="mr-2 h-4 w-4" />
                {updateTestMutation.isPending ? "Reactivating..." : "Reactivate Test"}
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Tabs Layout: Overview, Parameters, Assignments */}
      <Tabs value={activeTab} onValueChange={handleTabChange} className="space-y-6">
        <TabsList className="bg-slate-100 p-1">
          <TabsTrigger value="overview" className="text-sm">
            Overview
          </TabsTrigger>
          <TabsTrigger value="parameters" className="text-sm flex items-center gap-1.5">
            Parameters
            {paramsData && (
              <span className="rounded-full bg-slate-200 px-2 py-0.5 text-xs font-semibold text-slate-700">
                {paramsData.totalElements}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="assignments" className="text-sm flex items-center gap-1.5">
            <Building2 className="h-3.5 w-3.5 mr-1 text-slate-500" />
            Organization Assignments
          </TabsTrigger>
        </TabsList>

        {/* TAB 1: OVERVIEW */}
        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            {/* Identification */}
            <Card>
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <FlaskConical className="h-4 w-4 text-blue-600" />
                  <CardTitle className="text-sm font-semibold">
                    Identification & Classification
                  </CardTitle>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Test Code</span>
                  <span className="font-mono font-semibold text-slate-900">{test.code}</span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Full Name</span>
                  <span className="font-medium text-slate-900 text-right">{test.name}</span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Short Name</span>
                  <span className="font-medium text-slate-900">{test.shortName || "—"}</span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Diagnostic Category</span>
                  <span className="font-medium text-slate-900">{test.categoryName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Test Type</span>
                  <TestTypeBadge type={test.testType} />
                </div>
              </CardContent>
            </Card>

            {/* Specimen & Collection */}
            <Card>
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <FileSpreadsheet className="h-4 w-4 text-emerald-600" />
                  <CardTitle className="text-sm font-semibold">
                    Specimen & Collection Details
                  </CardTitle>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Primary Sample Type</span>
                  <span className="font-semibold text-slate-900">{test.sampleType}</span>
                </div>
                {test.sampleType === "OTHER" && test.customSampleType && (
                  <div className="flex justify-between border-b border-slate-100 pb-2">
                    <span className="text-slate-500">Custom Specimen</span>
                    <span className="font-medium text-slate-900">{test.customSampleType}</span>
                  </div>
                )}
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Container / Vacutainer</span>
                  <span className="font-medium text-slate-900">{test.specimenContainer || "Standard"}</span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Required Volume</span>
                  <span className="font-medium text-slate-900">
                    {test.sampleVolume ? `${test.sampleVolume} ${test.sampleVolumeUnit || "mL"}` : "Unspecified"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Patient Fasting Required</span>
                  <span className={test.fastingRequired ? "font-semibold text-amber-700" : "text-slate-600"}>
                    {test.fastingRequired ? "Yes (Fasting Mandatory)" : "No Fasting Required"}
                  </span>
                </div>
              </CardContent>
            </Card>

            {/* Processing & Operations */}
            <Card>
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-indigo-600" />
                  <CardTitle className="text-sm font-semibold">
                    Processing & Operations
                  </CardTitle>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Turnaround Time (TAT)</span>
                  <span className="font-semibold text-slate-900">
                    {test.turnaroundTimeHours ? `${test.turnaroundTimeHours} hours` : "Not defined"}
                  </span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Priority / STAT Supported</span>
                  <span className={test.prioritySupported ? "text-emerald-700 font-medium" : "text-slate-500"}>
                    {test.prioritySupported ? "Yes" : "No"}
                  </span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Outsourced Laboratory Test</span>
                  <span className={test.outsourced ? "text-amber-700 font-medium" : "text-slate-500"}>
                    {test.outsourced ? "Yes" : "No (In-House)"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Report Section Heading</span>
                  <span className="font-medium text-slate-900">{test.reportSection || "Default Section"}</span>
                </div>
              </CardContent>
            </Card>

            {/* Commercial & Billing */}
            <Card>
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <Coins className="h-4 w-4 text-amber-600" />
                  <CardTitle className="text-sm font-semibold">
                    Commercial & Billing
                  </CardTitle>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Base Catalog Price</span>
                  <span className="font-bold text-slate-900">
                    {test.basePrice != null ? `${test.currency || "INR"} ${test.basePrice}` : "Not configured"}
                  </span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Billing / Insurance Code</span>
                  <span className="font-mono font-medium text-slate-800">{test.billingCode || "—"}</span>
                </div>
                <div className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Effective Date Period</span>
                  <span className="text-xs text-slate-600">
                    {test.effectiveFrom || test.effectiveUntil ? (
                      <span>{test.effectiveFrom || "Start"} → {test.effectiveUntil || "Indefinite"}</span>
                    ) : (
                      <span className="text-slate-400">Always active</span>
                    )}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Specification Version</span>
                  <span className="font-mono text-xs font-semibold text-slate-700">v{test.version}</span>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Clinical Instructions */}
          {(test.patientPreparation || test.laboratoryInstructions || test.interpretationGuidance) && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-semibold">
                  Clinical Instructions & Interpretation
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4 text-xs">
                {test.patientPreparation && (
                  <div>
                    <h4 className="font-semibold text-slate-700 mb-1">Patient Preparation:</h4>
                    <p className="rounded-md bg-slate-50 p-2.5 text-slate-600 border border-slate-100 whitespace-pre-wrap">
                      {test.patientPreparation}
                    </p>
                  </div>
                )}
                {test.laboratoryInstructions && (
                  <div>
                    <h4 className="font-semibold text-slate-700 mb-1">Laboratory Handling Instructions:</h4>
                    <p className="rounded-md bg-slate-50 p-2.5 text-slate-600 border border-slate-100 whitespace-pre-wrap">
                      {test.laboratoryInstructions}
                    </p>
                  </div>
                )}
                {test.interpretationGuidance && (
                  <div>
                    <h4 className="font-semibold text-slate-700 mb-1">Clinical Interpretation Guidance:</h4>
                    <p className="rounded-md bg-slate-50 p-2.5 text-slate-600 border border-slate-100 whitespace-pre-wrap">
                      {test.interpretationGuidance}
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </TabsContent>

        {/* TAB 2: PARAMETERS */}
        <TabsContent value="parameters" className="space-y-4">
          {/* Header & Filter Toolbar */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-base font-semibold text-slate-900">
                Diagnostic Parameters
              </h2>
              <p className="text-xs text-slate-500">
                Measurement components, biological reference limits, and calculation formulas.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Button
                size="sm"
                onClick={() =>
                  navigate(`/super-admin/tests/${test.refId}/parameters/new`)
                }
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Parameter
              </Button>
            </div>
          </div>

          {/* Filter Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 bg-white p-3 rounded-lg border border-slate-200">
            <div className="w-full sm:w-40">
              <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-1">
                Status Filter
              </label>
              <Select
                value={paramStatus}
                onValueChange={(val) => {
                  setParamStatus(val);
                  setParamPage(0);
                }}
              >
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue placeholder="All Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Status</SelectItem>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="w-full sm:w-48">
              <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-1">
                Sort By
              </label>
              <Select
                value={paramSort}
                onValueChange={(val) => {
                  setParamSort(val as "displayOrder" | "name" | "code" | "dataType");
                  setParamPage(0);
                }}
              >
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="displayOrder">Display Order</SelectItem>
                  <SelectItem value="name">Parameter Name</SelectItem>
                  <SelectItem value="code">Parameter Code</SelectItem>
                  <SelectItem value="dataType">Data Type</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="sm:self-end">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  setParamDirection((prev) => (prev === "asc" ? "desc" : "asc"))
                }
                className="h-8 text-xs px-2.5"
                title={`Sort Direction: ${paramDirection.toUpperCase()}`}
              >
                <ArrowUpDown className="h-3.5 w-3.5 mr-1" />
                {paramDirection.toUpperCase()}
              </Button>
            </div>
          </div>

          {/* Parameters Table */}
          <div className="rounded-lg border border-slate-200 bg-white shadow-xs overflow-hidden">
            {paramsLoading ? (
              <div className="p-6 space-y-3">
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : paramsData?.content && paramsData.content.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-700">
                  <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                    <tr>
                      <th scope="col" className="px-5 py-3">
                        Order
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Parameter
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Code
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Data Type
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Input / Formula
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Unit
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Reference Limits
                      </th>
                      <th scope="col" className="px-5 py-3">
                        Status
                      </th>
                      <th scope="col" className="px-5 py-3 text-right">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {paramsData.content.map((param) => (
                      <tr
                        key={param.refId}
                        className="hover:bg-slate-50/80 transition-colors"
                      >
                        <td className="px-5 py-3 font-mono text-xs text-slate-500">
                          #{param.displayOrder}
                        </td>
                        <td className="px-5 py-3">
                          <button
                            type="button"
                            onClick={() => setSelectedParamForDetails(param)}
                            className="font-semibold text-slate-900 hover:text-blue-600 transition-colors text-left block"
                          >
                            {param.name}
                          </button>
                          {param.description && (
                            <span className="text-xs text-slate-500 truncate block max-w-xs">
                              {param.description}
                            </span>
                          )}
                        </td>
                        <td className="px-5 py-3 font-mono text-xs font-medium text-slate-600">
                          {param.code}
                        </td>
                        <td className="px-5 py-3">
                          <ParameterDataTypeBadge dataType={param.dataType} />
                        </td>
                        <td className="px-5 py-3 text-xs">
                          {param.inputType === "CALCULATED" ? (
                            <Badge
                              variant="outline"
                              className="border-indigo-200 bg-indigo-50 text-indigo-700 font-mono text-xs"
                            >
                              CALC: {param.calculationType}
                            </Badge>
                          ) : (
                            <span className="text-slate-500">Manual</span>
                          )}
                        </td>
                        <td className="px-5 py-3 text-xs font-mono text-slate-600">
                          {param.unit || "—"}
                        </td>
                        <td className="px-5 py-3 text-xs text-slate-700">
                          {param.referenceMin != null || param.referenceMax != null ? (
                            <span>
                              {param.referenceMin ?? 0} – {param.referenceMax ?? "∞"}
                            </span>
                          ) : (
                            "—"
                          )}
                        </td>
                        <td className="px-5 py-3">
                          <TestStatusBadge status={param.status} />
                        </td>
                        <td className="px-5 py-3 text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 text-slate-500 hover:text-slate-900"
                              >
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-44">
                              <DropdownMenuItem
                                onClick={() => setSelectedParamForDetails(param)}
                              >
                                <Eye className="mr-2 h-4 w-4 text-slate-500" />
                                View Details
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(
                                    `/super-admin/tests/${test.refId}/parameters/${param.refId}/edit`,
                                  )
                                }
                              >
                                <Edit className="mr-2 h-4 w-4 text-slate-500" />
                                Edit Parameter
                              </DropdownMenuItem>
                              {param.status === "ACTIVE" ? (
                                <DropdownMenuItem
                                  className="text-amber-600 focus:text-amber-700"
                                  onClick={() => setDeactivateParamTarget(param)}
                                >
                                  <Trash2 className="mr-2 h-4 w-4" />
                                  Deactivate
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem
                                  className="text-emerald-600 focus:text-emerald-700"
                                  onClick={() => void handleReactivateParam(param)}
                                >
                                  <RotateCcw className="mr-2 h-4 w-4" />
                                  Reactivate
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400 mb-3">
                  <Sliders className="h-6 w-6" />
                </div>
                <h3 className="text-base font-semibold text-slate-900">
                  {paramStatus !== "ALL" ? "No parameters found" : "No parameters defined"}
                </h3>
                <p className="mt-1 text-sm text-slate-500 max-w-sm">
                  {paramStatus !== "ALL"
                    ? "No parameters match the selected status filter."
                    : "Add measurement parameters, biological reference ranges, and critical limits to this test."}
                </p>
                <div className="mt-4">
                  <Button
                    size="sm"
                    onClick={() =>
                      navigate(`/super-admin/tests/${test.refId}/parameters/new`)
                    }
                    className="bg-blue-600 hover:bg-blue-700 text-white"
                  >
                    <Plus className="mr-2 h-4 w-4" />
                    Add Parameter
                  </Button>
                </div>
              </div>
            )}

            {/* Parameters Pagination */}
            {paramsData && paramsData.totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-slate-200 px-6 py-3 text-xs text-slate-600">
                <span>
                  Page {paramPage + 1} of {paramTotalPages}
                </span>
                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setParamPage((p) => Math.max(0, p - 1))}
                    disabled={paramPage === 0}
                    className="h-7 px-2"
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setParamPage((p) => Math.min(paramTotalPages - 1, p + 1))
                    }
                    disabled={paramPage >= paramTotalPages - 1}
                    className="h-7 px-2"
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </div>
        </TabsContent>

        {/* TAB 3: ORGANIZATION ASSIGNMENTS */}
        <TabsContent value="assignments" className="space-y-6">
          {/* Header & Direct Actions */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h2 className="text-base font-semibold text-slate-900">
                Organization Licensing & Assignments
              </h2>
              <p className="text-xs text-slate-500">
                Inspect and manage tenant licensing for {test.name} across organizations.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                asChild
              >
                <Link to={`/super-admin/tests/assignments?organizationRefId=${effectiveOrgRefId}`}>
                  <ExternalLink className="mr-1.5 h-3.5 w-3.5" />
                  View All Assignments
                </Link>
              </Button>

              <Button
                size="sm"
                onClick={() =>
                  navigate(
                    `/super-admin/tests/assignments/new?organizationRefId=${effectiveOrgRefId}`,
                  )
                }
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                <Plus className="mr-1.5 h-4 w-4" />
                Bulk Assign to Organizations
              </Button>
            </div>
          </div>

          {/* Feedback Banners */}
          {assignSuccessMsg && (
            <Alert className="border-emerald-200 bg-emerald-50 text-emerald-900">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" />
              <AlertTitle>Assigned Successfully</AlertTitle>
              <AlertDescription className="text-xs">{assignSuccessMsg}</AlertDescription>
            </Alert>
          )}

          {assignErrorMsg && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Assignment Notice</AlertTitle>
              <AlertDescription className="text-xs">{assignErrorMsg}</AlertDescription>
            </Alert>
          )}

          {/* Organization Inspector Card */}
          <Card className="border-slate-200 shadow-xs">
            <CardHeader className="border-b border-slate-100 pb-4">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div className="flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-blue-600" />
                  <CardTitle className="text-sm font-semibold text-slate-900">
                    Tenant Organization Inspector
                  </CardTitle>
                </div>

                <div className="w-full sm:w-72">
                  <Select
                    value={effectiveOrgRefId}
                    onValueChange={(val) => {
                      setSelectedOrgRefId(val);
                      setAssignSuccessMsg(null);
                      setAssignErrorMsg(null);
                    }}
                    disabled={orgsLoading}
                  >
                    <SelectTrigger className="h-8 text-xs">
                      <SelectValue placeholder="Select organization to inspect..." />
                    </SelectTrigger>
                    <SelectContent>
                      {orgsData?.content.map((org) => (
                        <SelectItem key={org.refId} value={org.refId}>
                          {org.name} ({org.code})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-6">
              {orgAssignmentsLoading ? (
                <div className="space-y-3">
                  <Skeleton className="h-8 w-full" />
                  <Skeleton className="h-16 w-full" />
                </div>
              ) : currentAssignmentForTest ? (
                // ASSIGNED STATE
                <div className="rounded-lg border border-emerald-200 bg-emerald-50/40 p-5 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                    <div className="flex items-center gap-2.5">
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
                        <Check className="h-4 w-4" />
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-slate-900">
                          {test.name} is Licensed to {selectedOrg?.name}
                        </h4>
                        <p className="text-xs text-slate-500">
                          Staff in this organization can select and generate reports for this test.
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setAssignmentToEdit(currentAssignmentForTest)}
                        className="h-8 text-xs"
                      >
                        <Edit className="mr-1.5 h-3.5 w-3.5" />
                        Edit Assignment
                      </Button>

                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          setDeactivateAssignmentTarget(currentAssignmentForTest)
                        }
                        className="h-8 text-xs text-amber-600 border-amber-200 hover:bg-amber-50"
                      >
                        <Trash2 className="mr-1.5 h-3.5 w-3.5" />
                        Deactivate
                      </Button>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 text-xs">
                    <div className="bg-white rounded p-2.5 border border-emerald-100">
                      <span className="text-slate-400 block text-[11px]">Assignment Status</span>
                      <span className="mt-0.5 block">
                        <TestStatusBadge status={currentAssignmentForTest.status} />
                      </span>
                    </div>

                    <div className="bg-white rounded p-2.5 border border-emerald-100">
                      <span className="text-slate-400 block text-[11px]">Effective Period</span>
                      <span className="font-medium text-slate-800 mt-0.5 block">
                        {currentAssignmentForTest.effectiveFrom || currentAssignmentForTest.effectiveUntil ? (
                          <span>
                            {currentAssignmentForTest.effectiveFrom || "Start"} →{" "}
                            {currentAssignmentForTest.effectiveUntil || "Indefinite"}
                          </span>
                        ) : (
                          "Always Active"
                        )}
                      </span>
                    </div>

                    <div className="bg-white rounded p-2.5 border border-emerald-100">
                      <span className="text-slate-400 block text-[11px]">Date Assigned</span>
                      <span className="font-medium text-slate-800 mt-0.5 block">
                        {new Date(currentAssignmentForTest.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                </div>
              ) : (
                // NOT ASSIGNED STATE
                <div className="rounded-lg border border-slate-200 bg-slate-50/50 p-6 text-center space-y-3">
                  <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                    <Building2 className="h-5 w-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-slate-800">
                      Not currently assigned to {selectedOrg?.name || "this organization"}
                    </h4>
                    <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
                      Staff and laboratory technicians in {selectedOrg?.name} will not be able to generate diagnostic reports for this test until it is assigned.
                    </p>
                  </div>

                  <div className="pt-2">
                    <Button
                      size="sm"
                      onClick={handleAssignToSelectedOrg}
                      disabled={isAssigningSingle || !effectiveOrgRefId}
                      className="bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      {isAssigningSingle ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Assigning...
                        </>
                      ) : (
                        <>
                          <Plus className="mr-2 h-4 w-4" />
                          Assign to {selectedOrg?.name}
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Parameter Details Modal Dialog */}
      <ParameterDetailsDialog
        open={Boolean(selectedParamForDetails)}
        onOpenChange={(open) => {
          if (!open) setSelectedParamForDetails(null);
        }}
        parameter={selectedParamForDetails}
        testRefId={test.refId}
      />

      {/* Edit Assignment Modal */}
      <UpdateAssignmentDialog
        open={Boolean(assignmentToEdit)}
        onOpenChange={(open) => {
          if (!open) setAssignmentToEdit(null);
        }}
        assignment={assignmentToEdit}
      />

      {/* Deactivate Assignment Confirmation Dialog */}
      <ConfirmDeactivateDialog
        open={Boolean(deactivateAssignmentTarget)}
        onOpenChange={(open) => {
          if (!open) setDeactivateAssignmentTarget(null);
        }}
        title={`Deactivate assignment for "${test.name}"?`}
        description={`This will deactivate the assignment for ${selectedOrg?.name || "the organization"}. Staff will no longer be able to select this test in new reports.`}
        confirmLabel="Deactivate Assignment"
        onConfirm={handleDeactivateAssignment}
        isLoading={deactivateAssignmentMutation.isPending}
      />

      {/* Deactivate Test Confirmation Dialog */}
      <ConfirmDeactivateDialog
        open={deactivateTestOpen}
        onOpenChange={setDeactivateTestOpen}
        title={`Deactivate "${test.name}"?`}
        description="Setting this test to INACTIVE prevents organizations from using it in new reports. Existing reports and historical records remain intact."
        confirmLabel="Deactivate Test"
        onConfirm={handleDeactivateTest}
        isLoading={deleteTestMutation.isPending}
      />

      {/* Deactivate Parameter Confirmation Dialog */}
      <ConfirmDeactivateDialog
        open={Boolean(deactivateParamTarget)}
        onOpenChange={(open) => {
          if (!open) setDeactivateParamTarget(null);
        }}
        title={`Deactivate parameter "${deactivateParamTarget?.name}"?`}
        description="This will set the parameter status to INACTIVE. It will no longer be prompted during report entry."
        confirmLabel="Deactivate Parameter"
        onConfirm={handleDeactivateParameter}
        isLoading={deactivateParamMutation.isPending}
      />
    </div>
  );
}

export default TestDetailsPage;
