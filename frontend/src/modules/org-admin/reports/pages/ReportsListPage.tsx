import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Download,
  Eye,
  FileText,
  FlaskConical,
  Loader2,
  Plus,
  RefreshCw,
  Trash2,
  User,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/core/auth/AuthContext";

import { useReportsQuery, useDeleteReportMutation } from "../hooks/useReports";
import { ReportStatusBadge } from "../components/ReportStatusBadge";
import { DeleteReportDialog } from "../components/DeleteReportDialog";
import { reportApi } from "../api/reportApi";
import type { ReportResponse, ReportStatus } from "../types/reportTypes";

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string; error?: string } | undefined;
    const serverMsg = data?.message || data?.error;
    if (serverMsg) {
      if (typeof serverMsg === "string" && serverMsg.includes("deletion eligibility period")) {
        return "Report has not reached the deletion eligibility period. Under laboratory data retention policies, reports cannot be deleted until after the statutory retention period (5 days from creation).";
      }
      return serverMsg;
    }
    return err.message || fallback;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}

export default function ReportsListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const basePath = location.pathname.startsWith("/lab-staff")
    ? "/lab-staff/reports"
    : "/org-admin/reports";

  const [selectedStatus, setSelectedStatus] = useState<ReportStatus | "ALL">("ALL");
  const [page, setPage] = useState<number>(0);
  const [reportToDelete, setReportToDelete] = useState<ReportResponse | null>(null);
  const [downloadingRefId, setDownloadingRefId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const {
    data: reportsData,
    isLoading,
    isError,
    error,
    refetch,
  } = useReportsQuery({
    status: selectedStatus === "ALL" ? undefined : selectedStatus,
    page,
    size: 15,
    sort: "createdAt",
    direction: "desc",
  });

  const reportsList = reportsData?.content ?? [];
  const deleteMutation = useDeleteReportMutation();

  const handleStatusChange = (st: ReportStatus | "ALL") => {
    setSelectedStatus(st);
    setPage(0);
    setActionError(null);
  };

  const handleRefresh = () => {
    setActionError(null);
    refetch();
  };

  const handleDeleteConfirm = async () => {
    if (!reportToDelete) return;
    setActionError(null);

    try {
      await deleteMutation.mutateAsync(reportToDelete.refId);
      setReportToDelete(null);
    } catch (err: unknown) {
      const msg = extractErrorMessage(
        err,
        "Report deletion failed. Deletion may be restricted by retention policies."
      );
      setActionError(msg);
      setReportToDelete(null);
    }
  };

  const handleDownloadPdf = async (reportRefId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setDownloadingRefId(reportRefId);
    setActionError(null);

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
    } catch (err: unknown) {
      const msg = extractErrorMessage(
        err,
        "Failed to download PDF. Verify report status is finalized."
      );
      setActionError(msg);
    } finally {
      setDownloadingRefId(null);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-teal-50 text-[#0F766E] border border-teal-100">
              <FileText className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
                Diagnostic Reports
              </h1>
              <p className="mt-0.5 text-xs text-slate-500">
                Create, edit, calculate, and finalize clinical patient laboratory reports.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={() => navigate(`${basePath}/new`)}
            size="sm"
            className="gap-1.5 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold shadow-xs"
          >
            <Plus className="h-4 w-4" />
            Create Report
          </Button>
        </div>
      </div>

      {/* Dismissible Action Notice */}
      {actionError && (
        <div className="flex items-start justify-between gap-3 p-3.5 rounded-lg border border-rose-200 bg-rose-50/90 text-rose-900 text-xs shadow-xs animate-in fade-in duration-200">
          <div className="flex items-start gap-2.5">
            <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold block text-rose-950">Action Notice</span>
              <p className="mt-0.5 text-rose-800 leading-relaxed">{actionError}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setActionError(null)}
            className="text-rose-500 hover:text-rose-800 p-1 rounded-md hover:bg-rose-100/60 transition-colors"
            title="Dismiss error"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Filter Tabs Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-3 rounded-lg border border-slate-200 shadow-2xs">
        {/* Status Filter Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
          {(["ALL", "DRAFT", "CALCULATED", "FINALIZED"] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => handleStatusChange(st)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                selectedStatus === st
                  ? "bg-teal-50 text-[#0F766E] font-semibold border border-teal-200/80 shadow-2xs"
                  : "text-slate-600 hover:bg-slate-100/70 hover:text-slate-900 border border-transparent"
              }`}
            >
              {st === "ALL" ? "All Reports" : st}
            </button>
          ))}
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={handleRefresh}
          className="text-xs h-8 text-slate-700 hover:bg-slate-100 border-slate-200 self-end sm:self-auto"
        >
          <RefreshCw className={`h-3.5 w-3.5 mr-1 text-slate-500 ${isLoading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Reports Table Card */}
      <Card className="border-slate-200 bg-white overflow-hidden shadow-xs">
        {isLoading ? (
          <div className="p-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : isError ? (
          <div className="p-8 text-center">
            <AlertCircle className="h-8 w-8 text-rose-600 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-800">
              Failed to load reports
            </p>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {error?.message || "An unexpected network error occurred."}
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => refetch()}
              className="mt-4 text-xs"
            >
              Try Again
            </Button>
          </div>
        ) : reportsList.length === 0 ? (
          <div className="p-12 text-center">
            <FileText className="h-10 w-10 text-slate-300 mx-auto mb-2" />
            <h3 className="text-sm font-bold text-slate-800">No Reports Found</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {selectedStatus !== "ALL"
                ? `There are no reports with status "${selectedStatus}" in your organization.`
                : "Your organization has not created any diagnostic reports yet."}
            </p>
            <Button
              size="sm"
              onClick={() => navigate(`${basePath}/new`)}
              className="mt-4 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white"
            >
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              Create First Report
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/75 text-slate-600 font-semibold uppercase text-[10px] tracking-wider">
                  <th className="py-3 px-4">Report Reference</th>
                  <th className="py-3 px-4">Patient</th>
                  <th className="py-3 px-3">Status</th>
                  <th className="py-3 px-3">Tests</th>
                  <th className="py-3 px-3">Created</th>
                  <th className="py-3 px-3">Last Updated</th>
                  <th className="py-3 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {reportsList.map((rep) => (
                  <tr
                    key={rep.refId}
                    onClick={() => navigate(`${basePath}/${rep.refId}`)}
                    className="hover:bg-slate-50/75 transition-colors cursor-pointer"
                  >
                    <td className="py-3 px-4">
                      <span className="font-mono font-bold text-slate-900 hover:text-[#0F766E] transition-colors">
                        #{rep.refId}
                      </span>
                      {rep.includeOrganizationHeader && (
                        <span className="text-[10px] text-teal-600 block mt-0.5 font-medium">
                          Header Included
                        </span>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex flex-col">
                        <span className="font-semibold text-slate-900 text-xs">
                          {rep.patientName || "—"}
                        </span>
                        <div className="flex items-center gap-1 text-[11px] text-slate-400 font-mono mt-0.5">
                          <User className="h-3 w-3 text-slate-400" />
                          <span>{rep.patientCode ? `PID: ${rep.patientCode}` : rep.patientRefId}</span>
                        </div>
                      </div>
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

                    <td className="py-3 px-3 text-slate-500 text-[11px]">
                      {new Date(rep.createdAt).toLocaleDateString("en-IN", {
                        day: "2-digit",
                        month: "short",
                        year: "numeric",
                      })}
                    </td>

                    <td className="py-3 px-3 text-slate-500 text-[11px]">
                      {new Date(rep.updatedAt).toLocaleTimeString("en-IN", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </td>

                    <td className="py-3 px-3 text-right">
                      <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                        {rep.status === "FINALIZED" && (
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
                        )}

                        <Button
                          asChild
                          variant="ghost"
                          size="sm"
                          className="h-7 px-2 text-xs text-slate-700 hover:text-[#0F766E] hover:bg-teal-50/50"
                        >
                          <Link to={`${basePath}/${rep.refId}`}>
                            <Eye className="h-3.5 w-3.5 mr-1" />
                            Open
                          </Link>
                        </Button>

                        {user?.role === "ORG_ADMIN" && (
                          <Button
                            variant="ghost"
                            size="sm"
                            title="Delete Report"
                            onClick={() => setReportToDelete(rep)}
                            className="h-7 w-7 p-0 text-slate-400 hover:text-rose-600 hover:bg-rose-50/60"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {reportsData && reportsData.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs">
            <span className="text-slate-500">
              Page {reportsData.number + 1} of {reportsData.totalPages} ({reportsData.totalElements} reports)
            </span>

            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => {
                  setPage((p) => Math.max(0, p - 1));
                  setActionError(null);
                }}
                className="h-7 px-2 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={reportsData.last}
                onClick={() => {
                  setPage((p) => p + 1);
                  setActionError(null);
                }}
                className="h-7 px-2 text-xs"
              >
                Next
                <ChevronRight className="h-3.5 w-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {reportToDelete && (
        <DeleteReportDialog
          report={reportToDelete}
          open={Boolean(reportToDelete)}
          onOpenChange={(open) => {
            if (!open) setReportToDelete(null);
          }}
          onConfirm={handleDeleteConfirm}
          isLoading={deleteMutation.isPending}
        />
      )}
    </div>
  );
}
