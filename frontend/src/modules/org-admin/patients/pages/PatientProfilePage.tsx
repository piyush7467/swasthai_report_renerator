import { useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Download,
  Edit2,
  Eye,
  FilePlus,
  FileText,
  FlaskConical,
  Loader2,
  Mail,
  MapPin,
  Phone,
  Plus,
  RefreshCw,
  Scale,
  Share2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { usePatientQuery, usePatientReportsQuery } from "../hooks/usePatients";
import { ReportStatusBadge } from "../../reports/components/ReportStatusBadge";
import { ShareReportModal } from "../../reports/components/ShareReportModal";
import { EditPatientModal } from "../components/EditPatientModal";
import { reportApi } from "../../reports/api/reportApi";
import type { ReportResponse } from "../../reports/types/reportTypes";

export default function PatientProfilePage() {
  const { patientRefId } = useParams<{ patientRefId: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  const isLabStaff = location.pathname.startsWith("/lab-staff");
  const basePath = isLabStaff ? "/lab-staff/patients" : "/org-admin/patients";
  const reportsBasePath = isLabStaff ? "/lab-staff/reports" : "/org-admin/reports";

  const [reportsPage, setReportsPage] = useState(0);
  const [reportToShare, setReportToShare] = useState<ReportResponse | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [downloadingRefId, setDownloadingRefId] = useState<string | null>(null);

  const {
    data: patient,
    isLoading: isPatientLoading,
    isError: isPatientError,
    error: patientError,
    refetch: refetchPatient,
  } = usePatientQuery(patientRefId);

  const {
    data: reportsData,
    isLoading: isReportsLoading,
    isError: isReportsError,
    error: reportsError,
    refetch: refetchReports,
  } = usePatientReportsQuery(patientRefId, {
    page: reportsPage,
    size: 10,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  const reportsList = reportsData?.content ?? [];
  const totalReportsCount = reportsData?.totalElements ?? patient?.totalReports ?? 0;

  const handleDownloadPdf = async (reportRefId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setDownloadingRefId(reportRefId);

    try {
      const blob = await reportApi.downloadReportPdf(reportRefId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `report-${reportRefId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      alert("Failed to download PDF. Please verify report status is finalized.");
    } finally {
      setDownloadingRefId(null);
    }
  };

  const formatAge = () => {
    if (!patient) return "—";
    if (patient.ageValue != null) {
      return `${patient.ageValue} ${patient.ageUnit?.toLowerCase() || "years"}`;
    }
    if (patient.dateOfBirth) {
      return patient.dateOfBirth;
    }
    return "—";
  };

  if (isPatientLoading) {
    return (
      <div className="space-y-6 pb-12">
        <div className="h-8 w-32 bg-slate-200 rounded animate-pulse" />
        <Skeleton className="h-44 w-full rounded-xl" />
        <Skeleton className="h-72 w-full rounded-xl" />
      </div>
    );
  }

  if (isPatientError || !patient) {
    return (
      <div className="p-8 text-center space-y-4">
        <AlertCircle className="h-10 w-10 text-rose-600 mx-auto" />
        <h3 className="text-base font-bold text-slate-900">Patient Not Found</h3>
        <p className="text-xs text-slate-500 max-w-sm mx-auto">
          {patientError?.message || "The requested patient record could not be found."}
        </p>
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate(basePath)}
          className="text-xs"
        >
          <ArrowLeft className="h-3.5 w-3.5 mr-1.5" />
          Back to Patients Directory
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Navigation Breadcrumb */}
      <div>
        <button
          type="button"
          onClick={() => navigate(basePath)}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-[#0F766E] transition-colors cursor-pointer"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to Patients Directory
        </button>
      </div>

      {/* ======================================================== */}
      {/* 1. PATIENT 360 IDENTITY HERO CARD                        */}
      {/* ======================================================== */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        {/* Top Header Strip */}
        <div className="p-6 border-b border-slate-100 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="h-14 w-14 rounded-2xl bg-teal-50 border border-teal-100 text-[#0F766E] flex items-center justify-center font-bold text-lg shrink-0">
              {patient.name.charAt(0).toUpperCase()}
            </div>

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                  {patient.salutation ? `${patient.salutation}. ` : ""}
                  {patient.name}
                </h1>
                <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                  {patient.patientCode}
                </span>
                <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  Active Patient
                </span>
              </div>

              <p className="text-xs text-slate-500 font-medium capitalize">
                {formatAge()} · {patient.gender.toLowerCase()}
              </p>
            </div>
          </div>

          {/* Primary Action Group */}
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsEditModalOpen(true)}
              className="text-xs h-9 border-slate-200 text-slate-700 hover:bg-slate-50 gap-1.5 cursor-pointer"
            >
              <Edit2 className="h-3.5 w-3.5 text-slate-400" />
              Edit Patient
            </Button>

            <Button
              onClick={() =>
                navigate(`${reportsBasePath}/new?patientRefId=${patient.refId}`)
              }
              size="sm"
              className="text-xs h-9 bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold gap-1.5 shadow-xs cursor-pointer"
            >
              <FilePlus className="h-4 w-4" />
              Create New Report
            </Button>
          </div>
        </div>

        {/* Detailed Metadata Grid */}
        <div className="p-6 bg-slate-50/40 grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-5 gap-4 text-xs">
          <div>
            <span className="text-slate-400 text-[11px] block uppercase font-medium flex items-center gap-1">
              <Phone className="h-3 w-3 text-slate-400" /> Phone
            </span>
            <span className="font-semibold text-slate-800 mt-1 block">
              {patient.phone || "—"}
            </span>
          </div>

          <div>
            <span className="text-slate-400 text-[11px] block uppercase font-medium flex items-center gap-1">
              <Mail className="h-3 w-3 text-slate-400" /> Email
            </span>
            <span className="font-semibold text-slate-800 mt-1 block truncate">
              {patient.email || "—"}
            </span>
          </div>

          <div>
            <span className="text-slate-400 text-[11px] block uppercase font-medium flex items-center gap-1">
              <Calendar className="h-3 w-3 text-slate-400" /> Registered
            </span>
            <span className="font-semibold text-slate-800 mt-1 block">
              {new Date(patient.createdAt).toLocaleDateString("en-IN", {
                month: "short",
                year: "numeric",
              })}
            </span>
          </div>

          <div>
            <span className="text-slate-400 text-[11px] block uppercase font-medium flex items-center gap-1">
              <Scale className="h-3 w-3 text-slate-400" /> Weight
            </span>
            <span className="font-semibold text-slate-800 mt-1 block">
              {patient.weightKg != null ? `${patient.weightKg} kg` : "—"}
            </span>
          </div>

          <div className="col-span-2 sm:col-span-4 lg:col-span-1">
            <span className="text-slate-400 text-[11px] block uppercase font-medium flex items-center gap-1">
              <FileText className="h-3 w-3 text-slate-400" /> Total Reports
            </span>
            <span className="font-bold text-[#0F766E] mt-1 block text-sm">
              {totalReportsCount} {totalReportsCount === 1 ? "report" : "reports"}
            </span>
          </div>

          {patient.address && (
            <div className="col-span-2 sm:col-span-4 lg:col-span-5 pt-1 border-t border-slate-100 flex items-center gap-1.5 text-slate-500">
              <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0" />
              <span>{patient.address}</span>
            </div>
          )}
        </div>
      </div>

      {/* ======================================================== */}
      {/* 2. REPORT HISTORY SECTION                                */}
      {/* ======================================================== */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-bold text-slate-900">
              Report History
            </h2>
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
              {totalReportsCount}
            </span>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => refetchReports()}
            className="text-xs h-8 text-slate-600 border-slate-200"
          >
            <RefreshCw className={`h-3 w-3 mr-1 text-slate-400 ${isReportsLoading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>

        <Card className="border-slate-200 bg-white overflow-hidden shadow-xs">
          {isReportsLoading ? (
            <div className="p-6 space-y-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : isReportsError ? (
            <div className="p-8 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-rose-600 mx-auto" />
              <p className="text-sm font-semibold text-slate-800">
                Unable to load report history
              </p>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                {reportsError?.message || "An unexpected error occurred while fetching reports."}
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={() => refetchReports()}
                className="text-xs mt-2"
              >
                Try Again
              </Button>
            </div>
          ) : reportsList.length === 0 ? (
            <div className="p-12 text-center space-y-3">
              <FileText className="h-10 w-10 text-slate-300 mx-auto" />
              <h3 className="text-sm font-bold text-slate-800">No Reports Yet</h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Create the first diagnostic report for this patient to establish their medical history.
              </p>
              <Button
                size="sm"
                onClick={() =>
                  navigate(`${reportsBasePath}/new?patientRefId=${patient.refId}`)
                }
                className="mt-2 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold cursor-pointer"
              >
                <Plus className="h-3.5 w-3.5 mr-1.5" />
                Create New Report
              </Button>
            </div>
          ) : (
            <>
              {/* Desktop Reports Table */}
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50/75 text-slate-600 font-semibold uppercase text-[10px] tracking-wider">
                      <th className="py-3 px-4">Report Reference</th>
                      <th className="py-3 px-3">Date</th>
                      <th className="py-3 px-3">Status</th>
                      <th className="py-3 px-3">Tests</th>
                      <th className="py-3 px-3">Reporting Professional</th>
                      <th className="py-3 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {reportsList.map((rep) => {
                      const isFinalized = rep.status === "FINALIZED";
                      return (
                        <tr
                          key={rep.refId}
                          onClick={() => navigate(`${reportsBasePath}/${rep.refId}`)}
                          className="hover:bg-slate-50/75 transition-colors cursor-pointer group"
                        >
                          <td className="py-3 px-4">
                            <span className="font-mono font-bold text-slate-900 group-hover:text-[#0F766E] transition-colors">
                              #{rep.refId}
                            </span>
                            {rep.includeOrganizationHeader && (
                              <span className="text-[10px] text-teal-600 block mt-0.5 font-medium">
                                Header Included
                              </span>
                            )}
                          </td>

                          <td className="py-3 px-3 text-slate-600 font-medium">
                            {new Date(rep.createdAt).toLocaleDateString("en-IN", {
                              day: "2-digit",
                              month: "short",
                              year: "numeric",
                            })}
                          </td>

                          <td className="py-3 px-3">
                            <ReportStatusBadge status={rep.status} />
                          </td>

                          <td className="py-3 px-3">
                            <div className="flex items-center gap-1.5 text-slate-700 font-medium">
                              <FlaskConical className="h-3.5 w-3.5 text-teal-600" />
                              <span>{rep.tests?.length ?? 0}</span>
                            </div>
                          </td>

                          <td className="py-3 px-3 text-slate-600">
                            {rep.finalizedByName || rep.createdByName || "Staff"}
                          </td>

                          <td className="py-3 px-3 text-right">
                            <div
                              className="flex items-center justify-end gap-1.5"
                              onClick={(e) => e.stopPropagation()}
                            >
                              {isFinalized && (
                                <>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    title="Download PDF"
                                    onClick={(e) => handleDownloadPdf(rep.refId, e)}
                                    disabled={downloadingRefId === rep.refId}
                                    className="h-7 w-7 p-0 text-[#0F766E] hover:bg-teal-50 border-teal-200"
                                  >
                                    {downloadingRefId === rep.refId ? (
                                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                    ) : (
                                      <Download className="h-3.5 w-3.5" />
                                    )}
                                  </Button>

                                  <Button
                                    variant="outline"
                                    size="sm"
                                    title="Share Report"
                                    onClick={() => setReportToShare(rep)}
                                    className="h-7 px-2 text-xs text-[#0F766E] hover:bg-teal-50 border-teal-200 gap-1"
                                  >
                                    <Share2 className="h-3 w-3" />
                                    Share
                                  </Button>
                                </>
                              )}

                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => navigate(`${reportsBasePath}/${rep.refId}`)}
                                className="h-7 px-2 text-xs text-slate-700 hover:text-[#0F766E] hover:bg-teal-50/50"
                              >
                                <Eye className="h-3.5 w-3.5 mr-1" />
                                {isFinalized ? "Open" : "Continue"}
                              </Button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Mobile Reports Cards */}
              <div className="md:hidden divide-y divide-slate-100">
                {reportsList.map((rep) => {
                  const isFinalized = rep.status === "FINALIZED";
                  return (
                    <div
                      key={rep.refId}
                      onClick={() => navigate(`${reportsBasePath}/${rep.refId}`)}
                      className="p-4 space-y-3 hover:bg-slate-50/75 transition-colors cursor-pointer"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <span className="font-mono font-bold text-slate-900 text-sm">
                            #{rep.refId}
                          </span>
                          <span className="text-[11px] text-slate-400 block mt-0.5">
                            {new Date(rep.createdAt).toLocaleDateString("en-IN", {
                              day: "2-digit",
                              month: "short",
                              year: "numeric",
                            })}
                          </span>
                        </div>
                        <ReportStatusBadge status={rep.status} />
                      </div>

                      <div className="flex items-center justify-between text-xs text-slate-600">
                        <span className="flex items-center gap-1">
                          <FlaskConical className="h-3.5 w-3.5 text-teal-600" />
                          {rep.tests?.length ?? 0} tests
                        </span>
                        <span>{rep.finalizedByName || rep.createdByName || "Staff"}</span>
                      </div>

                      <div
                        className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100"
                        onClick={(e) => e.stopPropagation()}
                      >
                        {isFinalized && (
                          <>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={(e) => handleDownloadPdf(rep.refId, e)}
                              disabled={downloadingRefId === rep.refId}
                              className="h-8 px-2.5 text-xs text-[#0F766E] border-teal-200"
                            >
                              <Download className="h-3.5 w-3.5 mr-1" />
                              PDF
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => setReportToShare(rep)}
                              className="h-8 px-2.5 text-xs text-[#0F766E] border-teal-200"
                            >
                              <Share2 className="h-3.5 w-3.5 mr-1" />
                              Share
                            </Button>
                          </>
                        )}
                        <Button
                          size="sm"
                          onClick={() => navigate(`${reportsBasePath}/${rep.refId}`)}
                          className="h-8 px-3 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white"
                        >
                          {isFinalized ? "Open" : "Continue"}
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          )}

          {/* Reports Pagination */}
          {reportsData && reportsData.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs">
              <span className="text-slate-500">
                Page {reportsData.number + 1} of {reportsData.totalPages} ({reportsData.totalElements} reports)
              </span>

              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={reportsPage === 0}
                  onClick={() => setReportsPage((p) => Math.max(0, p - 1))}
                  className="h-7 px-2 text-xs"
                >
                  <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={reportsData.last}
                  onClick={() => setReportsPage((p) => p + 1)}
                  className="h-7 px-2 text-xs"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </Card>
      </div>

      {/* Share Report Modal */}
      {reportToShare && (
        <ShareReportModal
          report={reportToShare}
          open={Boolean(reportToShare)}
          onOpenChange={(open) => {
            if (!open) setReportToShare(null);
          }}
        />
      )}

      {/* Edit Patient Modal */}
      {isEditModalOpen && (
        <EditPatientModal
          patient={patient}
          open={isEditModalOpen}
          onOpenChange={setIsEditModalOpen}
          onSuccess={() => {
            refetchPatient();
          }}
        />
      )}
    </div>
  );
}
