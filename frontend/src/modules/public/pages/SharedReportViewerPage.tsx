import { useState } from "react";
import { useParams } from "react-router-dom";
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  Download,
  FlaskConical,
  Loader2,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useSharedReportQuery } from "@/modules/org-admin/reports/hooks/useReports";
import { reportApi } from "@/modules/org-admin/reports/api/reportApi";
import { formatDisplayUnit } from "@/modules/org-admin/reports/utils/unitFormatter";
import { ParameterResultFlagBadge } from "@/modules/org-admin/reports/components/ParameterResultFlagBadge";

export default function SharedReportViewerPage() {
  const { shareToken } = useParams<{ shareToken: string }>();
  const [downloading, setDownloading] = useState(false);

  const { data, isLoading, isError, error } = useSharedReportQuery(shareToken);

  const handleDownloadPdf = async () => {
    if (!shareToken) return;
    setDownloading(true);
    try {
      const blob = await reportApi.downloadSharedReportPdf(shareToken);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `diagnostic-report-${data?.report.refId || "verified"}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      alert("Failed to download PDF. Please try again later.");
    } finally {
      setDownloading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="text-center space-y-3">
          <Loader2 className="h-8 w-8 text-[#0F766E] animate-spin mx-auto" />
          <p className="text-sm font-semibold text-slate-700">
            Retrieving verified diagnostic report...
          </p>
          <p className="text-xs text-slate-400">
            Validating secure access token
          </p>
        </div>
      </div>
    );
  }

  if (isError || !data?.report) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <Card className="max-w-md w-full border-slate-200 shadow-sm">
          <CardContent className="p-8 text-center space-y-4">
            <div className="h-12 w-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto border border-rose-100">
              <AlertCircle className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                Report Access Unavailable
              </h2>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                {error?.message ||
                  "This shared report link is invalid, has expired, or has been revoked by the issuing diagnostic center."}
              </p>
            </div>
            <div className="p-3 bg-slate-50 rounded-lg text-[11px] text-slate-600 border border-slate-100">
              If you are a patient, please contact your diagnostic laboratory to request a fresh secure report link.
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const report = data.report;
  const isHeaderIncluded = report.includeOrganizationHeader ?? true;

  return (
    <div className="min-h-screen bg-slate-100/70 py-6 px-3 sm:px-6 lg:px-8">
      {/* Top Banner / Secure Verification Status */}
      <div className="max-w-4xl mx-auto mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-white p-3.5 rounded-xl border border-teal-200/90 shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-teal-50 text-[#0F766E]">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-900">
                Verified Medical Record
              </span>
              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                <CheckCircle2 className="h-2.5 w-2.5" /> Finalized
              </span>
            </div>
            <p className="text-[11px] text-slate-500">
              Digital copy issued by {report.organizationName}
            </p>
          </div>
        </div>

        <Button
          onClick={handleDownloadPdf}
          disabled={downloading}
          size="sm"
          className="bg-[#0F766E] hover:bg-[#115E59] text-white text-xs h-8 px-3.5 font-semibold gap-1.5 shrink-0 cursor-pointer shadow-xs"
        >
          {downloading ? (
            <>
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              Downloading...
            </>
          ) : (
            <>
              <Download className="h-3.5 w-3.5" />
              Download Official PDF
            </>
          )}
        </Button>
      </div>

      {/* Main Clinical Report Paper */}
      <div className="max-w-4xl mx-auto bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Lab Header */}
        {isHeaderIncluded && (
          <div className="p-6 border-b border-slate-200 bg-slate-50/50">
            <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <Building2 className="h-6 w-6 text-[#0F766E]" />
                  <h1 className="text-xl font-bold tracking-tight text-slate-900">
                    {report.organizationName}
                  </h1>
                </div>
                {(report.organizationAddressLine1 || report.organizationCity) && (
                  <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5">
                    <MapPin className="h-3 w-3 text-slate-400 shrink-0" />
                    {[
                      report.organizationAddressLine1,
                      report.organizationAddressLine2,
                      report.organizationCity,
                      report.organizationState,
                      report.organizationPostalCode,
                    ]
                      .filter(Boolean)
                      .join(", ")}
                  </p>
                )}
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1 text-xs text-slate-500">
                  {report.organizationPhone && (
                    <span className="flex items-center gap-1">
                      <Phone className="h-3 w-3 text-slate-400" />
                      {report.organizationPhone}
                    </span>
                  )}
                  {report.organizationEmail && (
                    <span className="flex items-center gap-1">
                      <Mail className="h-3 w-3 text-slate-400" />
                      {report.organizationEmail}
                    </span>
                  )}
                </div>
              </div>

              <div className="text-left sm:text-right shrink-0">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                  Diagnostic Report
                </span>
                <span className="font-mono font-bold text-slate-900 text-sm">
                  #{report.refId}
                </span>
                {report.finalizedAt && (
                  <span className="text-[11px] text-slate-500 block mt-0.5">
                    Date:{" "}
                    {new Date(report.finalizedAt).toLocaleDateString("en-IN", {
                      day: "2-digit",
                      month: "short",
                      year: "numeric",
                    })}
                  </span>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Patient Identity Strip */}
        <div className="p-5 border-b border-slate-200 bg-slate-50/30">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
            <div>
              <span className="text-slate-400 text-[11px] block uppercase font-medium">
                Patient Name
              </span>
              <span className="font-bold text-slate-900 mt-0.5 block">
                {report.patientSalutation ? `${report.patientSalutation}. ` : ""}
                {report.patientName || "—"}
              </span>
            </div>

            <div>
              <span className="text-slate-400 text-[11px] block uppercase font-medium">
                Patient Code / MRN
              </span>
              <span className="font-mono font-semibold text-slate-800 mt-0.5 block">
                {report.patientCode || report.patientRefId}
              </span>
            </div>

            <div>
              <span className="text-slate-400 text-[11px] block uppercase font-medium">
                Age / Gender
              </span>
              <span className="font-medium text-slate-800 mt-0.5 block capitalize">
                {report.patientAgeAtReportingValue != null
                  ? `${report.patientAgeAtReportingValue} ${report.patientAgeAtReportingUnit?.toLowerCase() || "years"}`
                  : "—"}{" "}
                · {report.patientGender?.toLowerCase() || "—"}
              </span>
            </div>

            <div>
              <span className="text-slate-400 text-[11px] block uppercase font-medium">
                Report Finalized
              </span>
              <span className="font-medium text-slate-800 mt-0.5 block">
                {report.finalizedAt
                  ? new Date(report.finalizedAt).toLocaleTimeString("en-IN", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : "—"}
              </span>
            </div>
          </div>
        </div>

        {/* Diagnostic Results Section */}
        <div className="p-6 space-y-6">
          {report.tests.map((test) => (
            <div
              key={test.refId}
              className="border border-slate-200 rounded-lg overflow-hidden bg-white"
            >
              {/* Test Header */}
              <div className="bg-slate-50/80 px-4 py-2.5 border-b border-slate-200 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <FlaskConical className="h-4 w-4 text-[#0F766E]" />
                  <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wide">
                    {test.testName}
                  </h3>
                </div>
                <span className="text-[10px] font-mono text-slate-400">
                  {test.testCode}
                </span>
              </div>

              {/* Parameters Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50/40 text-slate-500 font-semibold uppercase text-[10px] tracking-wider">
                      <th className="py-2.5 px-4">Investigation / Parameter</th>
                      <th className="py-2.5 px-4 font-bold text-slate-800">
                        Observed Value
                      </th>
                      <th className="py-2.5 px-3">Unit</th>
                      <th className="py-2.5 px-4">Biological Reference Range</th>
                      <th className="py-2.5 px-3 text-right">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {test.parameters.map((param) => {
                      const displayUnit = formatDisplayUnit(param.unit);
                      const hasRef =
                        param.referenceMin != null || param.referenceMax != null;
                      const refStr = hasRef
                        ? `${param.referenceMin ?? 0} - ${param.referenceMax ?? "∞"}`
                        : "—";

                      return (
                        <tr
                          key={param.refId}
                          className="hover:bg-slate-50/60 transition-colors"
                        >
                          <td className="py-2.5 px-4 font-medium text-slate-800">
                            {param.parameterName}
                          </td>
                          <td className="py-2.5 px-4 font-bold text-slate-900 font-mono text-[13px]">
                            {param.value || "—"}
                          </td>
                          <td className="py-2.5 px-3 text-slate-500 font-medium">
                            {displayUnit}
                          </td>
                          <td className="py-2.5 px-4 text-slate-600 font-mono text-[11px]">
                            {refStr}
                          </td>
                          <td className="py-2.5 px-3 text-right">
                            <ParameterResultFlagBadge flag={param.flag} />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>

        {/* Footer / Reporting Signoff & Verification Strip */}
        <div className="p-6 border-t border-slate-200 bg-slate-50/50">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
            {/* Disclaimer & Footer Notes */}
            <div className="text-[11px] text-slate-500 max-w-md space-y-1">
              <p className="font-semibold text-slate-700">
                {report.organizationReportFooterText ||
                  "Verified Clinical Diagnostic Result"}
              </p>
              <p className="leading-relaxed">
                {report.organizationReportDisclaimer ||
                  "This diagnostic report is authentic and verified. Clinical correlation is recommended."}
              </p>
            </div>

            {/* Pathologist / Signoff */}
            <div className="text-left sm:text-right border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-200">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Reporting Professional
              </span>
              <span className="font-bold text-slate-900 text-xs block mt-0.5">
                {report.organizationSignatureOwnerName ||
                  report.finalizedByName ||
                  "Consulting Pathologist"}
              </span>
              <span className="text-[10px] text-slate-500 block">
                Digital Verification Recorded
              </span>
            </div>
          </div>
        </div>

        {/* Access Notice */}
        <div className="bg-slate-100/80 px-6 py-2.5 border-t border-slate-200 flex items-center justify-between text-[10px] text-slate-500">
          <span>Protected by SwasthAI Medical Verification System</span>
          <span>Access Token Expiring in 7 Days</span>
        </div>
      </div>
    </div>
  );
}
