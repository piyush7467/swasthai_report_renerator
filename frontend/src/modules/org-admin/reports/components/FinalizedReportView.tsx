import { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  Download,
  Loader2,
  Printer,
  QrCode,
  ShieldCheck,
  MapPin,
  Phone,
  Mail,
  Globe,
  FlaskConical,
  FileText,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ParameterResultFlagBadge } from "./ParameterResultFlagBadge";
import { reportApi } from "../api/reportApi";
import { orgProfileApi } from "../../settings/api/orgProfileApi";
import { formatDisplayUnit } from "../utils/unitFormatter";
import { useMyOrganizationProfileQuery } from "../../settings/hooks/useOrgProfile";
import type {
  ReportResponse,
  ReportTestItemResponse,
  ReportParameterItemResponse,
} from "../types/reportTypes";
import type { PatientResponse } from "../../patients/types/patientTypes";

interface FinalizedReportViewProps {
  report: ReportResponse;
  patient?: PatientResponse | null;
}

export function FinalizedReportView({ report, patient }: FinalizedReportViewProps) {
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [includeHeaderPreview, setIncludeHeaderPreview] = useState(
    report.includeOrganizationHeader ?? true,
  );

  const { data: orgProfile } = useMyOrganizationProfileQuery();
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const [signatureUrl, setSignatureUrl] = useState<string | null>(null);
  const [qrUrl, setQrUrl] = useState<string | null>(null);

  // Load Organization Logo
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

  // Load Organization Signature
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

  // Load Report Verification QR code
  useEffect(() => {
    let isMounted = true;
    let url: string | null = null;

    if (report.status === "FINALIZED") {
      reportApi
        .getReportQrBlob(report.refId)
        .then((blob) => {
          if (isMounted) {
            url = URL.createObjectURL(blob);
            setQrUrl(url);
          }
        })
        .catch(() => {});
    }

    return () => {
      isMounted = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [report.refId, report.status]);

  const handleDownloadPdf = async () => {
    setIsDownloading(true);
    setDownloadError(null);

    try {
      const blob = await reportApi.downloadReportPdf(report.refId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;

      // Filename formatted dynamically as date_patientName.pdf
      const dateStr = (report.finalizedAt || report.createdAt).slice(0, 10);
      const rawName = patient?.name || report.patientName || "Report";
      const sanitizedName = rawName.trim().replace(/[^a-zA-Z0-9_-]/g, "_");
      link.download = `${dateStr}_${sanitizedName}.pdf`;

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

  // Group tests by department/section
  const groupedSections = useMemo(() => {
    const map = new Map<string, ReportTestItemResponse[]>();
    report.tests.forEach((test) => {
      let section = "HAEMATOLOGY";
      const nameUpper = test.testName.toUpperCase();
      if (
        nameUpper.includes("KIDNEY") ||
        nameUpper.includes("LIVER") ||
        nameUpper.includes("LIPID") ||
        nameUpper.includes("SUGAR") ||
        nameUpper.includes("GLUCOSE") ||
        nameUpper.includes("URIC") ||
        nameUpper.includes("BIOCHEMISTRY")
      ) {
        section = "BIOCHEMISTRY";
      } else if (nameUpper.includes("CULTURE") || nameUpper.includes("MICROBIOLOGY")) {
        section = "MICROBIOLOGY";
      }
      if (!map.has(section)) {
        map.set(section, []);
      }
      map.get(section)!.push(test);
    });
    return Array.from(map.entries());
  }, [report.tests]);

  const patientCode = patient?.patientCode || report.patientCode || report.patientRefId;
  const patientDisplayName = patient
    ? `${patient.salutation ? `${patient.salutation} ` : ""}${patient.name}`
    : report.patientName
    ? `${report.patientSalutation ? `${report.patientSalutation} ` : ""}${report.patientName}`
    : "—";

  const ageText =
    patient?.ageValue != null
      ? `${patient.ageValue} ${patient.ageUnit?.toLowerCase() ?? "Y"}`
      : patient?.dateOfBirth
      ? `DOB: ${patient.dateOfBirth}`
      : report.patientAgeAtReportingValue != null
      ? `${report.patientAgeAtReportingValue} ${report.patientAgeAtReportingUnit?.toLowerCase() ?? "Y"}`
      : "--";

  const genderText = patient?.gender || report.patientGender || "--";
  const formattedAgeGender = `${ageText} / ${genderText}`;

  const formattedDate = new Date(report.createdAt).toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  const formattedDateTime = new Date(report.finalizedAt || report.createdAt).toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

  const orgName = report.organizationName || orgProfile?.organizationName || "Clinical Laboratory";
  const orgAddress = [
    report.organizationAddressLine1 || orgProfile?.addressLine1,
    report.organizationCity || orgProfile?.city,
    report.organizationState || orgProfile?.state,
    report.organizationPostalCode || orgProfile?.postalCode,
  ]
    .filter(Boolean)
    .join(", ");
  const orgPhone = report.organizationPhone || orgProfile?.phone;
  const orgEmail = report.organizationEmail || orgProfile?.email;
  const orgWebsite = report.organizationWebsite || orgProfile?.website;
  const orgTagline = report.organizationReportFooterText || orgProfile?.reportFooterText;
  const orgDisclaimer = report.organizationReportDisclaimer || orgProfile?.reportDisclaimer;

  return (
    <div className="max-w-4xl mx-auto space-y-4 sm:space-y-6 pb-16 px-2 sm:px-4">
      {/* Top Controls Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 sm:gap-4 bg-white p-3 sm:p-4 rounded-xl border border-slate-200 shadow-xs">
        <Button asChild variant="ghost" size="sm" className="gap-1.5 text-xs text-slate-600 hover:text-slate-900 w-fit">
          <Link to="/org-admin/reports">
            <ArrowLeft className="h-3.5 w-3.5" />
            Back to Reports
          </Link>
        </Button>

        <div className="flex flex-wrap items-center gap-2.5 sm:gap-3">
          <button
            type="button"
            onClick={() => setIncludeHeaderPreview(!includeHeaderPreview)}
            className={`text-xs px-2.5 py-1.5 rounded-lg border font-medium transition-colors ${
              includeHeaderPreview
                ? "bg-teal-50 border-teal-200 text-[#0F766E]"
                : "bg-slate-50 border-slate-200 text-slate-600"
            }`}
          >
            Header: {includeHeaderPreview ? "ON (Letterhead)" : "OFF (Pre-printed)"}
          </button>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => window.print()}
            className="gap-1.5 text-xs border-slate-300 text-slate-700"
          >
            <Printer className="h-3.5 w-3.5 text-slate-500" />
            Print Report
          </Button>

          <div className="hidden sm:flex items-center gap-1.5 text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-lg font-medium">
            <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
            Finalized & Authenticated
          </div>

          <Button
            size="sm"
            onClick={handleDownloadPdf}
            disabled={isDownloading}
            className="gap-2 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold shadow-xs flex-1 sm:flex-none justify-center"
          >
            {isDownloading ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Generating Official PDF...
              </>
            ) : (
              <>
                <Download className="h-3.5 w-3.5" />
                Download Official PDF
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

      {/* Main Medical Report Sheet */}
      <div className="bg-white rounded-xl border border-slate-300 shadow-md p-4 sm:p-8 md:p-10 space-y-5 sm:space-y-6 print:shadow-none print:border-none print:p-0">
        {/* 1. Hospital Organization Header */}
        {includeHeaderPreview ? (
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
            {/* Left: Logo & Hospital Name */}
            <div className="flex items-center gap-3">
              {logoUrl ? (
                <img
                  src={logoUrl}
                  alt={orgName}
                  className="h-12 w-auto max-w-[100px] object-contain rounded shrink-0"
                />
              ) : null}
              <div>
                <h1 className="text-lg sm:text-xl font-extrabold text-[#0F2C59] tracking-tight uppercase">
                  {orgName}
                </h1>
                {orgTagline && (
                  <p className="text-[10px] sm:text-[11px] font-bold text-teal-700 tracking-wider uppercase mt-0.5">
                    {orgTagline}
                  </p>
                )}
              </div>
            </div>

            {/* Middle: Contact Info with Icons (Only if available) */}
            {(orgAddress || orgPhone || orgEmail || orgWebsite) && (
              <div className="text-[11px] text-slate-700 space-y-1">
                {orgAddress && (
                  <div className="flex items-center gap-1.5">
                    <MapPin className="h-3.5 w-3.5 text-[#185A9D] shrink-0" />
                    <span>{orgAddress}</span>
                  </div>
                )}
                {orgPhone && (
                  <div className="flex items-center gap-1.5">
                    <Phone className="h-3.5 w-3.5 text-[#185A9D] shrink-0" />
                    <span>{orgPhone}</span>
                  </div>
                )}
                {orgEmail && (
                  <div className="flex items-center gap-1.5">
                    <Mail className="h-3.5 w-3.5 text-[#185A9D] shrink-0" />
                    <span>{orgEmail}</span>
                  </div>
                )}
                {orgWebsite && (
                  <div className="flex items-center gap-1.5">
                    <Globe className="h-3.5 w-3.5 text-[#185A9D] shrink-0" />
                    <span>{orgWebsite}</span>
                  </div>
                )}
              </div>
            )}

            {/* Right: Tagline / Disclaimer (if available) */}
            {orgDisclaimer && (
              <div className="hidden md:flex items-center gap-3 pl-4 border-l-2 border-[#185A9D] py-1 max-w-xs">
                <div className="text-xs font-semibold text-[#185A9D] leading-snug">
                  <p>{orgDisclaimer}</p>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="border border-dashed border-slate-300 rounded-lg p-4 text-center bg-slate-50/50 text-slate-400 text-xs">
            [ Pre-printed Letterhead Space Reserved (Header OFF) ]
          </div>
        )}

        {/* 2. Patient Demographics Card */}
        <div className="rounded-xl border border-slate-300 p-3 sm:p-5 bg-white text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6 divide-y sm:divide-y-0 lg:divide-x divide-slate-200">
            {/* Column 1: Patient Details */}
            <div className="space-y-1.5">
              <div className="flex">
                <span className="w-28 text-slate-600 font-medium shrink-0">Patient Name</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="font-bold text-slate-950">{patientDisplayName}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-slate-600 font-medium shrink-0">Age / Sex</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{formattedAgeGender}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-slate-600 font-medium shrink-0">Patient ID</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="font-mono font-medium text-slate-900">{patientCode}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-slate-600 font-medium shrink-0">Referred By</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{orgName}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-slate-600 font-medium shrink-0">Address</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{patient?.address || "—"}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-slate-600 font-medium shrink-0">Mobile No</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{patient?.phone || report.patientPhone || "—"}</span>
              </div>
            </div>

            {/* Column 2: Date & Registration Timestamps */}
            <div className="pt-3 sm:pt-0 sm:pl-0 lg:pl-6 space-y-1.5">
              <div className="flex">
                <span className="w-32 text-slate-600 font-medium shrink-0">Registered On</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{formattedDate}</span>
              </div>
              <div className="flex">
                <span className="w-32 text-slate-600 font-medium shrink-0">Reported On</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{formattedDateTime}</span>
              </div>
              <div className="flex">
                <span className="w-32 text-slate-600 font-medium shrink-0">Printed On</span>
                <span className="text-slate-400 mr-2">:</span>
                <span className="text-slate-900">{formattedDateTime}</span>
              </div>
            </div>

            {/* Column 3: Real Verification QR */}
            <div className="pt-3 lg:pt-0 lg:pl-6 flex flex-col items-center justify-center space-y-2">
              {qrUrl ? (
                <div className="flex flex-col items-center">
                  <div className="p-1.5 rounded border border-slate-200 bg-white shadow-2xs">
                    <img src={qrUrl} alt="Verification QR" className="h-16 w-16 object-contain" />
                  </div>
                  <span className="text-[10px] text-slate-500 font-medium mt-1">Scan for Verification</span>
                  <span className="font-mono text-[9px] text-slate-400 mt-0.5">#{report.refId}</span>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center p-3 text-center">
                  <ShieldCheck className="h-8 w-8 text-emerald-600 mb-1" />
                  <span className="text-[10px] text-slate-600 font-medium">Electronically Verified</span>
                  <span className="font-mono text-[9px] text-slate-400 mt-0.5">#{report.refId}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 3. Centered Pill Ribbon: LABORATORY REPORT */}
        <div className="flex justify-center my-2">
          <div className="inline-block border border-teal-600/60 bg-teal-50/60 rounded-full px-8 py-1 shadow-2xs">
            <span className="text-xs sm:text-sm font-extrabold text-[#0F766E] tracking-wider uppercase">
              LABORATORY REPORT
            </span>
          </div>
        </div>

        {/* 4. Investigation Sections & Tables */}
        <div className="space-y-6">
          {groupedSections.map(([sectionName, tests]: [string, ReportTestItemResponse[]]) => {
            return (
              <div key={sectionName} className="rounded-xl border border-slate-200 overflow-hidden shadow-xs">
                {/* Department Section Header */}
                <div className="bg-slate-100/90 border-b border-slate-200 px-4 py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <FlaskConical className="h-4 w-4 text-[#0F766E]" />
                    <span className="font-extrabold text-xs sm:text-sm tracking-wide uppercase text-slate-900">
                      {sectionName}
                    </span>
                  </div>
                  <span className="text-[11px] font-mono text-[#0F766E] bg-white px-2 py-0.5 rounded border border-teal-200/60">
                    Clinical Investigation
                  </span>
                </div>

                {/* Results Table (Responsive scrollable container) */}
                <div className="overflow-x-auto min-w-full">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-slate-50 text-slate-700 border-b border-slate-200 font-bold uppercase text-[11px]">
                        <th className="py-2.5 px-4 w-[34%]">Test Name</th>
                        <th className="py-2.5 px-3 w-[18%] text-right">Result</th>
                        <th className="py-2.5 px-3 w-[14%] text-center whitespace-nowrap">Unit</th>
                        <th className="py-2.5 px-3 w-[20%] text-center">Reference Range</th>
                        <th className="py-2.5 px-3 w-[14%] text-center">Flag</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {tests.map((test: ReportTestItemResponse) => (
                        <div key={test.refId} className="contents">
                          {/* Panel Subheader */}
                          <tr className="bg-slate-50/80 border-y border-slate-200">
                            <td colSpan={5} className="py-1.5 px-4 font-bold text-slate-800 text-[11px] uppercase tracking-wide">
                              {test.testName}
                            </td>
                          </tr>

                          {test.parameters.map((param: ReportParameterItemResponse) => {
                            const isAbnormal = param.flag && param.flag !== "NORMAL";
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
                                <td className="py-2.5 px-4 pl-7 text-slate-900 font-medium">
                                  {param.parameterName}
                                </td>
                                <td className={`py-2.5 px-3 text-right font-mono ${isAbnormal ? "font-extrabold text-slate-950 text-sm" : "text-slate-800"}`}>
                                  {param.value || "—"}
                                </td>
                                <td className="py-2.5 px-3 text-center font-mono text-slate-600 text-[11px] whitespace-nowrap">
                                  {formatDisplayUnit(param.unit)}
                                </td>
                                <td className="py-2.5 px-3 text-center font-mono text-slate-600 text-[11px]">
                                  {refRange}
                                </td>
                                <td className="py-2.5 px-3 text-center">
                                  <ParameterResultFlagBadge flag={param.flag} />
                                </td>
                              </tr>
                            );
                          })}
                        </div>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            );
          })}
        </div>

        {/* 5. Authoritative Doctor Signature & Verification Footer */}
        <div className="pt-6 border-t border-slate-200">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
            {/* Left: QR Code & Verification Notice */}
            <div className="flex items-center gap-3">
              {qrUrl ? (
                <div className="p-1 rounded border border-slate-200 bg-white shrink-0 shadow-2xs">
                  <img src={qrUrl} alt="Verification QR" className="h-12 w-12 object-contain" />
                </div>
              ) : (
                <div className="p-1 rounded border border-slate-200 bg-white shrink-0">
                  <QrCode className="h-10 w-10 text-slate-800" />
                </div>
              )}
              <div className="h-10 w-0.5 bg-[#185A9D]" />
              <div className="text-[11px] text-slate-600 space-y-0.5">
                <p className="font-bold text-slate-900">Report ID: {report.refId}</p>
                <p>Scan this QR code to verify the authenticity of this report.</p>
                <p className="text-[10px] text-slate-500">This report is electronically generated and verified.</p>
              </div>
            </div>

            {/* Right: Pathologist Signature Block */}
            <div className="text-center sm:text-right shrink-0">
              {signatureUrl && (
                <div className="flex justify-center sm:justify-end mb-1">
                  <img
                    src={signatureUrl}
                    alt="Digital Signature"
                    className="h-10 w-auto max-w-[140px] object-contain"
                  />
                </div>
              )}
              <p className="font-bold text-xs text-slate-900">
                {report.organizationSignatureOwnerName ||
                  orgProfile?.signatureOwnerName ||
                  report.finalizedByName ||
                  "Authorized Signatory"}
              </p>
              <p className="text-[11px] text-slate-500 font-medium">Authorized Signatory</p>
              <p className="text-[10px] text-slate-400">{orgName}</p>
            </div>
          </div>

          <div className="flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-100 pt-3 mt-4">
            <span className="flex items-center gap-1 font-mono">
              <FileText className="h-3 w-3" />
              Electronic Pathology Record
            </span>
            <span>Page 1 of 1</span>
          </div>
        </div>
      </div>
    </div>
  );
}
