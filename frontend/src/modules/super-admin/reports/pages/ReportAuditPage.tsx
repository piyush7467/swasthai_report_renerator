import { useState } from "react";
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eye,
  Filter,
  ShieldAlert,
  XCircle,
  Building2,
} from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminSecurityAudit } from "../hooks/useAdminAnalytics";
import { useOrganizationsQuery } from "../../hooks/useOrganizations";
import { AuditDetailsDialog } from "../components/AuditDetailsDialog";
import { BreakGlassDialog } from "../components/BreakGlassDialog";
import type { SecurityAuditLog } from "../types/analyticsTypes";

export default function ReportAuditPage() {
  const [selectedAction, setSelectedAction] = useState<string>("");
  const [selectedOrgRefId, setSelectedOrgRefId] = useState<string>("");
  const [successFilter, setSuccessFilter] = useState<string>("");
  const [page, setPage] = useState<number>(0);

  const [selectedLog, setSelectedLog] = useState<SecurityAuditLog | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false);
  const [isBreakGlassOpen, setIsBreakGlassOpen] = useState<boolean>(false);

  // Organizations list for dropdown filter
  const { data: orgsData } = useOrganizationsQuery({
    page: 0,
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const organizations = orgsData?.content ?? [];

  const {
    data: auditData,
    isLoading: isAuditLoading,
  } = useAdminSecurityAudit({
    action: selectedAction || undefined,
    targetOrganizationRefId: selectedOrgRefId || undefined,
    success:
      successFilter === "true"
        ? true
        : successFilter === "false"
        ? false
        : undefined,
    page,
    size: 15,
    sort: "createdAt",
    direction: "desc",
  });

  const handleOpenDetails = (log: SecurityAuditLog) => {
    setSelectedLog(log);
    setIsDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="h-6 w-6 text-indigo-600" />
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Platform Security & Audit Trail
            </h1>
          </div>
          <p className="mt-1 text-sm text-slate-600">
            Immutable, read-only audit log tracking break-glass report access and security events.
          </p>
        </div>

        {/* Global Filter Toolbar & Actions */}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <Button
            onClick={() => setIsBreakGlassOpen(true)}
            size="sm"
            className="gap-1.5 text-xs bg-rose-600 hover:bg-rose-700 text-white font-semibold shadow-xs"
          >
            <ShieldAlert className="h-3.5 w-3.5" />
            Emergency Break-Glass
          </Button>

          {/* Action Filter */}
          <div className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 shadow-2xs">
            <Filter className="h-3.5 w-3.5 text-slate-400" />
            <select
              value={selectedAction}
              onChange={(e) => {
                setSelectedAction(e.target.value);
                setPage(0);
              }}
              className="bg-transparent font-medium text-slate-700 outline-hidden"
            >
              <option value="">All Actions</option>
              <option value="BREAK_GLASS_REPORT_ACCESS">
                BREAK_GLASS_REPORT_ACCESS
              </option>
            </select>
          </div>

          {/* Organization Filter */}
          <div className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 shadow-2xs">
            <Building2 className="h-3.5 w-3.5 text-slate-400" />
            <select
              value={selectedOrgRefId}
              onChange={(e) => {
                setSelectedOrgRefId(e.target.value);
                setPage(0);
              }}
              className="bg-transparent font-medium text-slate-700 outline-hidden"
            >
              <option value="">All Organizations</option>
              {organizations.map((org) => (
                <option key={org.refId} value={org.refId}>
                  {org.name}
                </option>
              ))}
            </select>
          </div>

          {/* Success Status Filter */}
          <div className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 shadow-2xs">
            <select
              value={successFilter}
              onChange={(e) => {
                setSuccessFilter(e.target.value);
                setPage(0);
              }}
              className="bg-transparent font-medium text-slate-700 outline-hidden"
            >
              <option value="">All Statuses</option>
              <option value="true">Granted (Success)</option>
              <option value="false">Denied (Failure)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Table Card */}
      <Card className="border-slate-200 bg-white">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold text-slate-900">
            Security Audit Logs
          </CardTitle>
          <CardDescription className="text-xs text-slate-500">
            Sorted chronologically by event timestamp (newest first).
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-y border-slate-200 bg-slate-50/75 text-slate-600 font-semibold">
                  <th className="py-2.5 px-4">Timestamp</th>
                  <th className="py-2.5 px-3">Action</th>
                  <th className="py-2.5 px-3">Actor</th>
                  <th className="py-2.5 px-3">Target Organization</th>
                  <th className="py-2.5 px-3">Report Ref ID</th>
                  <th className="py-2.5 px-3">Result</th>
                  <th className="py-2.5 px-4 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {isAuditLoading ? (
                  Array.from({ length: 6 }).map((_, i) => (
                    <tr key={i}>
                      <td colSpan={7} className="py-3 px-4">
                        <Skeleton className="h-5 w-full" />
                      </td>
                    </tr>
                  ))
                ) : !auditData?.content || auditData.content.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-slate-500">
                      No security audit events recorded matching the filter criteria.
                    </td>
                  </tr>
                ) : (
                  auditData.content.map((log) => (
                    <tr key={log.refId} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-3 px-4 font-mono text-slate-600 whitespace-nowrap">
                        {new Date(log.createdAt).toLocaleString()}
                      </td>

                      <td className="py-3 px-3">
                        <Badge
                          variant="outline"
                          className="bg-indigo-50 font-mono text-indigo-700 border-indigo-200 text-[10px]"
                        >
                          {log.action}
                        </Badge>
                      </td>

                      <td className="py-3 px-3 font-medium text-slate-900 truncate max-w-44">
                        {log.actorEmail}
                      </td>

                      <td className="py-3 px-3">
                        <div className="font-semibold text-slate-800 truncate max-w-44">
                          {log.targetOrganizationName || log.targetOrganizationRefId || "Global"}
                        </div>
                        {log.targetOrganizationRefId && (
                          <div className="font-mono text-[10px] text-slate-400">
                            {log.targetOrganizationRefId}
                          </div>
                        )}
                      </td>

                      <td className="py-3 px-3 font-mono text-slate-700">
                        {log.targetReportRefId || "—"}
                      </td>

                      <td className="py-3 px-3">
                        {log.success ? (
                          <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px] flex items-center gap-1 w-fit">
                            <CheckCircle2 className="h-3 w-3" />
                            Success
                          </Badge>
                        ) : (
                          <Badge variant="destructive" className="text-[10px] flex items-center gap-1 w-fit">
                            <XCircle className="h-3 w-3" />
                            Denied
                          </Badge>
                        )}
                      </td>

                      <td className="py-3 px-4 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenDetails(log)}
                          className="h-7 px-2 text-xs text-indigo-700 hover:text-indigo-900 hover:bg-indigo-50"
                        >
                          <Eye className="h-3.5 w-3.5 mr-1" />
                          View
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Bar */}
          {auditData && auditData.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs">
              <span className="text-slate-500">
                Page {auditData.page + 1} of {auditData.totalPages} ({auditData.totalElements} records)
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
                  disabled={auditData.last}
                  onClick={() => setPage((p) => p + 1)}
                  className="h-7 px-2 text-xs"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Audit Details Modal */}
      <AuditDetailsDialog
        log={selectedLog}
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />

      {/* Break-Glass Emergency Dialog */}
      <BreakGlassDialog
        open={isBreakGlassOpen}
        onOpenChange={setIsBreakGlassOpen}
      />
    </div>
  );
}
