import { useState, useEffect } from "react";
import {
  Building2,
  Calculator,
  Eye,
  FileCheck2,
  Printer,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ParameterResultFlagBadge } from "../ParameterResultFlagBadge";
import { ReportStatusBadge } from "../ReportStatusBadge";
import { FinalizeReportDialog } from "../FinalizeReportDialog";
import { formatDisplayUnit } from "../../utils/unitFormatter";
import type { ReportResponse } from "../../types/reportTypes";
import type { PatientResponse } from "@/modules/org-admin/patients/types/patientTypes";
import { useMyOrganizationProfileQuery } from "@/modules/org-admin/settings/hooks/useOrgProfile";
import { orgProfileApi } from "@/modules/org-admin/settings/api/orgProfileApi";

interface Step5ReviewReportProps {
  report: ReportResponse;
  patient?: PatientResponse | null;
  onUpdateHeaderOption: (include: boolean) => Promise<void>;
  onRecalculate: () => Promise<void>;
  onFinalize: () => Promise<void>;
  onBackToEditor: () => void;
  isFinalizing: boolean;
}

export function Step5ReviewReport({
  report,
  patient,
  onUpdateHeaderOption,
  onRecalculate,
  onFinalize,
  onBackToEditor,
  isFinalizing,
}: Step5ReviewReportProps) {
  const [isFinalizeDialogOpen, setIsFinalizeDialogOpen] = useState(false);
  const [isRecalculating, setIsRecalculating] = useState(false);
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const [signatureUrl, setSignatureUrl] = useState<string | null>(null);

  const { data: orgProfile } = useMyOrganizationProfileQuery();

  useEffect(() => {
    let isMounted = true;
    let url: string | null = null;

    if (orgProfile?.logoConfigured) {
      orgProfileApi
        .getLogoBlob()
        .then((blob) => {
          if (isMounted) {
            url = URL.createObjectURL(blob);
            setLogoUrl(url);
          }
        })
        .catch(() => {});
    }

    return () => {
      isMounted = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [orgProfile?.logoConfigured]);

  useEffect(() => {
    let isMounted = true;
    let url: string | null = null;

    if (orgProfile?.signatureConfigured) {
      orgProfileApi
        .getSignatureBlob()
        .then((blob) => {
          if (isMounted) {
            url = URL.createObjectURL(blob);
            setSignatureUrl(url);
          }
        })
        .catch(() => {});
    }

    return () => {
      isMounted = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [orgProfile?.signatureConfigured]);

  const handleRecalculateClick = async () => {
    setIsRecalculating(true);
    try {
      await onRecalculate();
    } finally {
      setIsRecalculating(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6 pb-20">
      {/* Top Review Bar & Letterhead Toggle */}
      <Card className="border-slate-200/90 bg-white shadow-xs">
        <CardContent className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-teal-50 text-[#0F766E] flex items-center justify-center shrink-0">
              <Eye className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Step 5: Pre-Finalization Clinical Review
              </h2>
              <p className="text-xs text-slate-500">
                Review A4 medical report preview before final electronic sign-off.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            {/* Include Organization Header Toggle Switch */}
            <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-lg">
              <Building2 className="h-3.5 w-3.5 text-slate-500" />
              <label
                htmlFor="header-toggle"
                className="text-xs font-semibold text-slate-700 cursor-pointer select-none"
              >
                Include Letterhead
              </label>
              <input
                id="header-toggle"
                type="checkbox"
                checked={report.includeOrganizationHeader}
                onChange={(e) => onUpdateHeaderOption(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-[#0F766E] focus:ring-[#0F766E] ml-1 cursor-pointer"
              />
            </div>

            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handlePrint}
              className="text-xs h-8 gap-1.5 border-slate-300 text-slate-700"
            >
              <Printer className="h-3.5 w-3.5 text-slate-500" />
              Print Preview
            </Button>

            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleRecalculateClick}
              disabled={isRecalculating}
              className="text-xs h-8 gap-1.5 border-slate-300 text-slate-700 hover:text-purple-700"
            >
              <Calculator className={`h-3.5 w-3.5 text-purple-600 ${isRecalculating ? "animate-spin" : ""}`} />
              Recalculate
            </Button>

            <Button
              type="button"
              size="sm"
              onClick={() => setIsFinalizeDialogOpen(true)}
              className="text-xs h-8 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold flex items-center gap-1.5 shadow-xs"
            >
              <FileCheck2 className="h-3.5 w-3.5" />
              Finalize & Sign Report
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* A4-LIKE CLINICAL SHEET */}
      <div className="bg-white rounded-xl border border-slate-300 shadow-md p-4 sm:p-8 md:p-10 max-w-4xl mx-auto font-sans">
        {/* OPTIONAL ORGANIZATION LETTERHEAD */}
        {report.includeOrganizationHeader ? (
          <div className="border-b-2 border-slate-800 pb-4 mb-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3.5">
                {logoUrl && (
                  <img
                    src={logoUrl}
                    alt={orgProfile?.organizationName || report.organizationName}
                    className="h-12 w-auto max-w-[100px] object-contain rounded shrink-0"
                  />
                )}
                <div>
                  <h1 className="text-lg sm:text-xl font-extrabold tracking-tight text-slate-900 uppercase">
                    {orgProfile?.organizationName || report.organizationName}
                  </h1>
                  {orgProfile?.addressLine1 && (
                    <p className="text-xs text-slate-600 mt-0.5">
                      {[orgProfile.addressLine1, orgProfile.city, orgProfile.state, orgProfile.country]
                        .filter(Boolean)
                        .join(", ")}
                    </p>
                  )}
                  <div className="flex flex-wrap items-center gap-x-3 text-[11px] text-slate-500 mt-1">
                    {orgProfile?.phone && <span>Tel: {orgProfile.phone}</span>}
                    {orgProfile?.email && <span>Email: {orgProfile.email}</span>}
                    {orgProfile?.website && <span>Web: {orgProfile.website}</span>}
                  </div>
                </div>
              </div>

              {orgProfile?.reportFooterText && (
                <div className="text-left sm:text-right shrink-0 max-w-xs">
                  <p className="text-xs font-semibold text-teal-800 italic">
                    {orgProfile.reportFooterText}
                  </p>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="border border-dashed border-slate-300 rounded-lg p-3 mb-5 text-center bg-slate-50/50">
            <span className="text-xs text-slate-400 italic">
              [ Pre-printed Letterhead Space Reserved (Header OFF) ]
            </span>
          </div>
        )}

        {/* PATIENT & REPORT CLINICAL DEMOGRAPHICS BLOCK (2 Columns) */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 border border-slate-200 rounded-lg p-3 sm:p-4 bg-slate-50/50 mb-6 text-xs">
          {/* Left Column: Patient Details */}
          <div className="space-y-1.5 border-b sm:border-b-0 sm:border-r border-slate-200 pb-3 sm:pb-0 sm:pr-4">
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Patient Name:</span>
              <span className="font-bold text-slate-900 text-sm">
                {patient
                  ? `${patient.salutation ? `${patient.salutation} ` : ""}${patient.name}`
                  : report.patientName
                  ? `${report.patientSalutation ? `${report.patientSalutation} ` : ""}${report.patientName}`
                  : "—"}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Patient ID / MRN:</span>
              <span className="font-mono font-bold text-slate-800">
                {patient?.patientCode || report.patientCode || "--"}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Age / Gender:</span>
              <span className="text-slate-800 font-medium">
                {patient?.ageValue != null
                  ? `${patient.ageValue} ${patient.ageUnit?.toLowerCase() ?? "Y"}`
                  : patient?.dateOfBirth
                  ? `DOB: ${patient.dateOfBirth}`
                  : report.patientAgeAtReportingValue != null
                  ? `${report.patientAgeAtReportingValue} ${report.patientAgeAtReportingUnit?.toLowerCase() ?? "Y"}`
                  : "--"}{" "}
                / {patient?.gender || report.patientGender || "--"}
              </span>
            </div>
            {(patient?.phone || report.patientPhone) && (
              <div className="flex items-center justify-between">
                <span className="text-slate-500 font-medium">Contact:</span>
                <span className="text-slate-700">{patient?.phone || report.patientPhone}</span>
              </div>
            )}
          </div>

          {/* Right Column: Report ID, Timestamps & Authoritative Status */}
          <div className="space-y-1.5 sm:pl-2">
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Report Reference:</span>
              <span className="font-mono font-bold text-blue-900">
                #{report.refId}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Ordered / Created:</span>
              <span className="text-slate-700">
                {new Date(report.createdAt).toLocaleString(undefined, {
                  dateStyle: "medium",
                  timeStyle: "short",
                })}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Status:</span>
              <ReportStatusBadge status={report.status} />
            </div>
            <div className="pt-1 flex items-center justify-between">
              <span className="text-slate-500 font-medium">Verification:</span>
              <span className="text-[11px] text-slate-500 italic bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                Verification available after finalization
              </span>
            </div>
          </div>
        </div>

        {/* DEPARTMENT SECTIONS & PARAMETER TABLES */}
        <div className="space-y-6">
          {report.tests.map((test) => {
            return (
              <div key={test.refId} className="border border-slate-200 rounded-lg overflow-hidden">
                {/* Department Section Header */}
                <div className="bg-slate-100/90 border-b border-slate-200 px-4 py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Sparkles className="h-4 w-4 text-[#0F766E]" />
                    <span className="font-bold text-xs sm:text-sm text-slate-900 tracking-wide uppercase">
                      {test.testName}
                    </span>
                  </div>
                  <span className="font-mono text-xs font-semibold text-[#0F766E] bg-white px-2 py-0.5 rounded border border-teal-200/60">
                    {test.testCode}
                  </span>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left border-collapse">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold text-[11px] uppercase tracking-wider">
                        <th className="py-2.5 px-4 w-[36%]">Test Parameter</th>
                        <th className="py-2.5 px-3 text-right w-[20%]">Observed Value</th>
                        <th className="py-2.5 px-3 w-[14%] whitespace-nowrap">Unit</th>
                        <th className="py-2.5 px-3 w-[18%]">Biological Reference Range</th>
                        <th className="py-2.5 px-3 text-center w-[12%]">Flag</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {test.parameters.map((param) => {
                        const isCalculated = param.inputType === "CALCULATED";
                        const refRange =
                          param.referenceMin != null && param.referenceMax != null
                            ? `${param.referenceMin} – ${param.referenceMax}`
                            : param.referenceMin != null
                            ? `> ${param.referenceMin}`
                            : param.referenceMax != null
                            ? `< ${param.referenceMax}`
                            : "—";

                        return (
                          <tr key={param.refId} className="hover:bg-slate-50/50">
                            <td className="py-2.5 px-4 font-semibold text-slate-900">
                              {param.parameterName}
                              {isCalculated && (
                                <span className="ml-1.5 text-[9px] text-[#0F766E] bg-teal-50 border border-teal-200 px-1.5 py-0.5 rounded font-mono font-medium">
                                  calc
                                </span>
                              )}
                            </td>
                            <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-900 text-sm">
                              {param.value || "—"}
                            </td>
                            <td className="py-2.5 px-3 text-slate-600 font-medium whitespace-nowrap">
                              {formatDisplayUnit(param.unit)}
                            </td>
                            <td className="py-2.5 px-3 text-slate-600 font-mono text-xs">
                              {refRange}
                            </td>
                            <td className="py-2.5 px-3 text-center">
                              <ParameterResultFlagBadge flag={param.flag} />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            );
          })}
        </div>

        {/* CLINICAL FOOTER & SIGNATORY BLOCK */}
        <div className="mt-8 pt-6 border-t-2 border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-6 items-end">
          <div className="space-y-1 text-slate-500 text-[11px]">
            <p className="font-semibold text-slate-700">Medical Examination Notes:</p>
            <p>
              {orgProfile?.reportDisclaimer ||
                "Test results relate only to the items tested. Please correlate clinically with patient symptoms. In case of unexpected or critical values, re-testing is recommended."}
            </p>
            <p className="pt-2 font-mono text-[10px] text-slate-400">
              Report Reference: #{report.refId}
            </p>
          </div>

          <div className="text-right flex flex-col items-end justify-end space-y-1">
            {signatureUrl ? (
              <img
                src={signatureUrl}
                alt="Signature"
                className="h-10 w-auto max-w-[140px] object-contain mb-1"
              />
            ) : (
              <div className="h-10 flex items-center justify-center">
                <div className="border border-dashed border-emerald-400 bg-emerald-50/60 rounded px-3 py-1 text-[11px] text-emerald-800 font-serif italic">
                  ✓ Ready for Electronic Sign-off
                </div>
              </div>
            )}
            <div className="font-bold text-slate-900 text-sm">
              {orgProfile?.signatureOwnerName || report.organizationSignatureOwnerName || report.finalizedByName || "Authorized Signatory"}
            </div>
            <div className="text-xs text-slate-500">
              Authorized Signatory
            </div>
            <div className="text-[10px] text-slate-400">
              {orgProfile?.organizationName || report.organizationName}
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Controls */}
      <div className="flex items-center justify-between pt-4">
        <Button
          type="button"
          variant="outline"
          onClick={onBackToEditor}
          className="text-xs h-9 px-4 border-slate-300"
        >
          &larr; Back to Results Editor
        </Button>

        <Button
          type="button"
          onClick={() => setIsFinalizeDialogOpen(true)}
          className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs h-9 px-6 font-semibold flex items-center gap-1.5 shadow-sm"
        >
          <FileCheck2 className="h-4 w-4" />
          Finalize & Sign Report
        </Button>
      </div>

      {/* Finalize Confirmation Dialog */}
      <FinalizeReportDialog
        report={report}
        open={isFinalizeDialogOpen}
        onOpenChange={setIsFinalizeDialogOpen}
        onConfirm={async () => {
          await onFinalize();
          setIsFinalizeDialogOpen(false);
        }}
        isLoading={isFinalizing}
      />
    </div>
  );
}
