import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
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
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

import { useReportsQuery, useDeleteReportMutation } from "../hooks/useReports";
import { ReportStatusBadge } from "../components/ReportStatusBadge";
import { CreateReportModal } from "../components/CreateReportModal";
import { DeleteReportDialog } from "../components/DeleteReportDialog";
import { reportApi } from "../api/reportApi";
import type { ReportResponse, ReportStatus } from "../types/reportTypes";

export default function ReportsListPage() {
  const navigate = useNavigate();
  const [selectedStatus, setSelectedStatus] = useState<ReportStatus | "ALL">("ALL");
  const [page, setPage] = useState<number>(0);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
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

  const handleDeleteConfirm = async () => {
    if (!reportToDelete) return;
    setActionError(null);

    try {
      await deleteMutation.mutateAsync(reportToDelete.refId);
      setReportToDelete(null);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Report deletion failed. Deletion may be restricted by retention policies.";
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
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to download PDF. Verify report status is finalized.";
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
          <div className="flex items-center gap-2">
            <FileText className="h-6 w-6 text-blue-600" />
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Diagnostic Reports
            </h1>
          </div>
          <p className="mt-1 text-sm text-slate-600">
            Create, edit, calculate, and finalize clinical patient laboratory reports.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            size="sm"
            className="gap-1.5 text-xs bg-blue-600 hover:bg-blue-700 text-white font-semibold shadow-xs"
          >
            <Plus className="h-4 w-4" />
            Create Report
          </Button>
        </div>
      </div>

      {actionError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle className="text-xs font-semibold">Action Error</AlertTitle>
          <AlertDescription className="text-xs">{actionError}</AlertDescription>
        </Alert>
      )}

      {/* Filter Tabs Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-3 rounded-lg border border-slate-200 shadow-2xs">
        {/* Status Filter Tabs */}
        <div className="flex items-center gap-1 overflow-x-auto pb-1 sm:pb-0">
          {(["ALL", "DRAFT", "CALCULATED", "FINALIZED"] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => {
                setSelectedStatus(st);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                selectedStatus === st
                  ? "bg-blue-50 text-blue-700 font-semibold border border-blue-200"
                  : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
              }`}
            >
              {st === "ALL" ? "All Reports" : st.charAt(0) + st.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          className="h-8 text-xs gap-1.5 text-slate-600"
        >
          <RefreshCw className="h-3 w-3" />
          Refresh
        </Button>
      </div>

      {/* Main Table Card */}
      <Card className="border-slate-200 bg-white shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-3">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : isError ? (
          <div className="p-6">
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error loading reports</AlertTitle>
              <AlertDescription className="text-xs">
                {error?.message || "Failed to retrieve organization reports."}
              </AlertDescription>
            </Alert>
          </div>
        ) : reportsList.length === 0 ? (
          <div className="text-center py-16 px-4">
            <FileText className="mx-auto h-10 w-10 text-slate-300 mb-2" />
            <h3 className="text-sm font-semibold text-slate-900">
              No reports found
            </h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {selectedStatus !== "ALL"
                ? `There are no reports with status "${selectedStatus}" in your organization.`
                : "Your organization has not created any diagnostic reports yet."}
            </p>
            <Button
              size="sm"
              onClick={() => setIsCreateModalOpen(true)}
              className="mt-4 text-xs bg-blue-600 hover:bg-blue-700 text-white"
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
                  <th className="py-3 px-4">Patient Ref</th>
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
                    onClick={() => navigate(`/org-admin/reports/${rep.refId}`)}
                    className="hover:bg-slate-50/75 transition-colors cursor-pointer"
                  >
                    <td className="py-3 px-4">
                      <span className="font-mono font-bold text-slate-900 hover:text-blue-600 transition-colors">
                        #{rep.refId}
                      </span>
                      {rep.includeOrganizationHeader && (
                        <span className="text-[10px] text-slate-400 block mt-0.5">
                          Header Included
                        </span>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center gap-1.5">
                        <User className="h-3.5 w-3.5 text-slate-400" />
                        <span className="font-mono text-xs text-slate-700 font-medium">
                          {rep.patientRefId}
                        </span>
                      </div>
                    </td>

                    <td className="py-3 px-3">
                      <ReportStatusBadge status={rep.status} />
                    </td>

                    <td className="py-3 px-3">
                      <div className="flex items-center gap-1 text-slate-600 font-medium">
                        <FlaskConical className="h-3.5 w-3.5 text-purple-600" />
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
                            className="h-7 w-7 p-0 text-blue-600 hover:bg-blue-50 border-blue-200"
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
                          className="h-7 px-2 text-xs text-slate-700 hover:text-blue-600"
                        >
                          <Link to={`/org-admin/reports/${rep.refId}`}>
                            <Eye className="h-3.5 w-3.5 mr-1" />
                            Open
                          </Link>
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          title="Delete Report"
                          onClick={() => setReportToDelete(rep)}
                          className="h-7 w-7 p-0 text-slate-400 hover:text-rose-600"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
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
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="h-7 px-2 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={reportsData.last}
                onClick={() => setPage((p) => p + 1)}
                className="h-7 px-2 text-xs"
              >
                Next
                <ChevronRight className="h-3.5 w-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Modals */}
      <CreateReportModal
        open={isCreateModalOpen}
        onOpenChange={setIsCreateModalOpen}
      />

      {reportToDelete && (
        <DeleteReportDialog
          reportRefId={reportToDelete.refId}
          open={Boolean(reportToDelete)}
          onOpenChange={(open) => !open && setReportToDelete(null)}
          onConfirm={handleDeleteConfirm}
          isLoading={deleteMutation.isPending}
        />
      )}
    </div>
  );
}
