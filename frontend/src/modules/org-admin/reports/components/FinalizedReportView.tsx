import { useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  Building2,
  Download,
  Loader2,
  QrCode,
  ShieldCheck,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ParameterResultFlagBadge } from "./ParameterResultFlagBadge";
import { reportApi } from "../api/reportApi";
import type { ReportResponse } from "../types/reportTypes";
import type { PatientResponse } from "../../patients/types/patientTypes";

interface FinalizedReportViewProps {
  report: ReportResponse;
  patient?: PatientResponse | null;
}

export function FinalizedReportView({ report, patient }: FinalizedReportViewProps) {
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const handleDownloadPdf = async () => {
    setIsDownloading(true);
    setDownloadError(null);

    try {
      const blob = await reportApi.downloadReportPdf(report.refId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `report-${report.refId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to generate and download report PDF. Please try again.";
      setDownloadError(msg);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-16">
      {/* Top Controls Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <Button asChild variant="ghost" size="sm" className="gap-1.5 text-xs text-slate-600 hover:text-slate-900 w-fit">
          <Link to="/org-admin/reports">
            <ArrowLeft className="h-3.5 w-3.5" />
            Back to Reports List
          </Link>
        </Button>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-lg font-medium">
            <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
            Finalized & Cryptographically Signed
          </div>

          <Button
            size="sm"
            onClick={handleDownloadPdf}
            disabled={isDownloading}
            className="gap-2 text-xs bg-blue-600 hover:bg-blue-700 text-white font-semibold shadow-xs"
          >
            {isDownloading ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Generating PDF...
              </>
            ) : (
              <>
                <Download className="h-3.5 w-3.5" />
                Download PDF
              </>
            )}
          </Button>
        </div>
      </div>

      {downloadError && (
        <Alert variant="destructive">
          <AlertDescription className="text-xs">{downloadError}</AlertDescription>
        </Alert>
      )}

      {/* Main Medical Report Preview Paper */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden p-6 sm:p-10 space-y-8 print:shadow-none print:border-none">
        {/* Organization Header */}
        {report.includeOrganizationHeader && (
          <div className="border-b border-slate-200 pb-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-xl bg-blue-50 text-blue-700 font-bold">
                  <Building2 className="h-7 w-7" />
                </div>
                <div>
                  <h2 className="text-xl font-extrabold text-slate-900 tracking-tight">
                    {report.organizationName}
                  </h2>
                  <p className="text-xs text-slate-500 font-medium mt-0.5">
                    Authorized Clinical Diagnostic Laboratory
                  </p>
                </div>
              </div>

              <div className="text-right text-xs text-slate-500 space-y-0.5">
                <p className="font-semibold text-slate-700">Official Diagnostic Report</p>
                <p className="font-mono text-slate-600 font-bold">#{report.refId}</p>
                <p className="text-[11px]">
                  Issued: {new Date(report.createdAt).toLocaleDateString("en-IN", {
                    day: "2-digit",
                    month: "short",
                    year: "numeric",
                  })}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Patient Demographic Snapshot */}
        <div className="rounded-xl border border-slate-100 bg-slate-50/75 p-5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
            <div>
              <span className="text-[11px] font-medium text-slate-400 uppercase tracking-wider block">
                Patient Name
              </span>
              <span className="font-bold text-slate-900 text-sm mt-0.5 block">
                {patient ? `${patient.salutation ? `${patient.salutation}. ` : ""}${patient.name}` : "Patient"}
              </span>
            </div>

            <div>
              <span className="text-[11px] font-medium text-slate-400 uppercase tracking-wider block">
                Patient Code
              </span>
              <span className="font-mono font-semibold text-slate-800 text-xs mt-0.5 block">
                {patient?.patientCode || report.patientRefId}
              </span>
            </div>

            <div>
              <span className="text-[11px] font-medium text-slate-400 uppercase tracking-wider block">
                Age / Gender
              </span>
              <span className="font-semibold text-slate-800 text-xs mt-0.5 block">
                {patient?.ageValue != null ? `${patient.ageValue} ${patient.ageUnit?.toLowerCase() ?? "yrs"}` : "—"} / {patient?.gender || "—"}
              </span>
            </div>

            <div>
              <span className="text-[11px] font-medium text-slate-400 uppercase tracking-wider block">
                Contact
              </span>
              <span className="font-medium text-slate-700 text-xs mt-0.5 block">
                {patient?.phone || "—"}
              </span>
            </div>
          </div>
        </div>

        {/* Diagnostic Test Sections */}
        <div className="space-y-8">
          {report.tests.map((test) => (
            <div key={test.refId} className="space-y-3">
              <div className="flex items-center justify-between border-b border-slate-200 pb-2">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-slate-900 text-sm">
                    {test.testName}
                  </span>
                  <span className="font-mono text-[11px] text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                    {test.testCode}
                  </span>
                </div>
                <span className="text-[11px] text-slate-400 font-medium">
                  {test.parameters.length} parameter{test.parameters.length === 1 ? "" : "s"}
                </span>
              </div>

              {/* Parameter Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px] tracking-wider bg-slate-50/50">
                      <th className="py-2 px-3 w-10">#</th>
                      <th className="py-2 px-3">Investigation</th>
                      <th className="py-2 px-3">Result</th>
                      <th className="py-2 px-3">Flag</th>
                      <th className="py-2 px-3">Unit</th>
                      <th className="py-2 px-3">Reference Range</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {test.parameters.map((param) => (
                      <tr key={param.refId} className="hover:bg-slate-50/50">
                        <td className="py-2.5 px-3 font-mono text-[11px] text-slate-400">
                          {param.displayOrder}
                        </td>
                        <td className="py-2.5 px-3">
                          <span className="font-semibold text-slate-900 block">
                            {param.parameterName}
                          </span>
                          <span className="font-mono text-[10px] text-slate-400">
                            {param.parameterCode}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 font-semibold text-slate-900 font-mono text-xs">
                          {param.value || "—"}
                        </td>
                        <td className="py-2.5 px-3">
                          <ParameterResultFlagBadge flag={param.flag} />
                        </td>
                        <td className="py-2.5 px-3 font-mono text-slate-600 text-[11px]">
                          {param.unit || "—"}
                        </td>
                        <td className="py-2.5 px-3 font-mono text-slate-600 text-[11px]">
                          {param.referenceMin != null || param.referenceMax != null ? (
                            <span>
                              {param.referenceMin ?? 0} – {param.referenceMax ?? "∞"}
                            </span>
                          ) : (
                            "—"
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>

        {/* Audit & Sign-off Footer */}
        <div className="border-t border-slate-200 pt-6 mt-8">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs text-slate-500">
            <div className="flex items-center gap-2">
              <QrCode className="h-6 w-6 text-slate-400 shrink-0" />
              <div>
                <p className="font-semibold text-slate-700">Digital Authentication & Verification</p>
                <p className="text-[11px]">This report was generated and cryptographically signed on the SwasthAI platform.</p>
              </div>
            </div>

            <div className="text-right">
              <p className="text-[11px] font-medium text-slate-500">Finalized By:</p>
              <p className="font-semibold text-slate-900">{report.finalizedByEmail || "Authorized Staff"}</p>
              <p className="text-[10px] text-slate-400 mt-0.5">
                {report.finalizedAt
                  ? new Date(report.finalizedAt).toLocaleString("en-IN")
                  : "Finalized"}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
