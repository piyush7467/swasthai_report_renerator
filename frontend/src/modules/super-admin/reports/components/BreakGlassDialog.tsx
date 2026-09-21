import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  CheckCircle2,
  Lock,
  RotateCw,
  ShieldAlert,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { adminAnalyticsApi } from "../api/adminAnalyticsApi";
import { ANALYTICS_QUERY_KEYS } from "../hooks/useAdminAnalytics";
import { useOrganizationsQuery } from "../../hooks/useOrganizations";

interface BreakGlassDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defaultReportRefId?: string;
  defaultOrgRefId?: string;
}

export function BreakGlassDialog({
  open,
  onOpenChange,
  defaultReportRefId = "",
  defaultOrgRefId = "",
}: BreakGlassDialogProps) {
  const queryClient = useQueryClient();

  const [orgRefId, setOrgRefId] = useState<string>(defaultOrgRefId);
  const [reportRefId, setReportRefId] = useState<string>(defaultReportRefId);
  const [justification, setJustification] = useState<string>("");
  const [retrievedReport, setRetrievedReport] = useState<any | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Organizations list for dropdown helper
  const { data: orgsData } = useOrganizationsQuery({
    page: 0,
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });
  const organizations = orgsData?.content ?? [];

  const breakGlassMutation = useMutation({
    mutationFn: async () => {
      if (!orgRefId.trim()) {
        throw new Error("Target organization reference ID is required.");
      }
      if (!reportRefId.trim()) {
        throw new Error("Target report reference ID is required.");
      }
      if (justification.trim().length < 10) {
        throw new Error("Justification must be at least 10 characters long.");
      }

      return await adminAnalyticsApi.breakGlassAccess(reportRefId.trim(), {
        organizationRefId: orgRefId.trim(),
        justification: justification.trim(),
      });
    },
    onSuccess: (data) => {
      setRetrievedReport(data);
      setErrorMessage(null);
      // Invalidate security audit and overview caches so the new audit entry immediately appears
      queryClient.invalidateQueries({ queryKey: ["admin", "audit"] });
      queryClient.invalidateQueries({ queryKey: ANALYTICS_QUERY_KEYS.overview });
    },
    onError: (err: any) => {
      setErrorMessage(
        err?.response?.data?.message || err?.message || "Break-glass access failed."
      );
    },
  });

  const handleReset = () => {
    setRetrievedReport(null);
    setErrorMessage(null);
    setJustification("");
  };

  const handleClose = () => {
    handleReset();
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2 text-rose-700">
            <ShieldAlert className="h-5 w-5 shrink-0" />
            <DialogTitle className="text-base font-bold">
              Emergency Break-Glass Report Access
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            Audited, emergency override to inspect patient reports for life-critical or legal compliance purposes.
          </DialogDescription>
        </DialogHeader>

        {/* Warning Banner */}
        <Alert variant="destructive" className="border-rose-300 bg-rose-50/80 text-rose-900">
          <AlertTriangle className="h-4 w-4 text-rose-700" />
          <AlertTitle className="text-xs font-bold text-rose-800">
            Mandatory Auditing Notice
          </AlertTitle>
          <AlertDescription className="text-[11px] text-rose-700 leading-relaxed mt-0.5">
            Break-glass access is strictly audited. Your email, current timestamp, justification, and client IP address will be permanently recorded to the immutable platform audit trail.
          </AlertDescription>
        </Alert>

        {errorMessage && (
          <Alert variant="destructive" className="py-2">
            <AlertDescription className="text-xs">{errorMessage}</AlertDescription>
          </Alert>
        )}

        {/* Form or Result View */}
        {!retrievedReport ? (
          <div className="space-y-4 pt-1 text-xs">
            {/* Target Organization */}
            <div className="space-y-1.5">
              <Label className="text-xs font-medium text-slate-700 flex items-center justify-between">
                <span>Target Organization Ref ID</span>
                <span className="text-[10px] text-slate-400 font-normal">e.g. ORG-7K4M92XQ</span>
              </Label>
              <div className="flex gap-2">
                <Input
                  type="text"
                  placeholder="Enter or select organization refId..."
                  value={orgRefId}
                  onChange={(e) => setOrgRefId(e.target.value)}
                  className="h-8 text-xs font-mono"
                />
                <select
                  onChange={(e) => {
                    if (e.target.value) setOrgRefId(e.target.value);
                  }}
                  className="h-8 rounded-md border border-slate-200 bg-slate-50 px-2 text-xs font-medium text-slate-700"
                  defaultValue=""
                >
                  <option value="" disabled>
                    Pick lab...
                  </option>
                  {organizations.map((org) => (
                    <option key={org.refId} value={org.refId}>
                      {org.name} ({org.refId})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Target Report Ref ID */}
            <div className="space-y-1.5">
              <Label className="text-xs font-medium text-slate-700 flex items-center justify-between">
                <span>Target Report Reference ID</span>
                <span className="text-[10px] text-slate-400 font-normal">e.g. RPT-AB12CD34</span>
              </Label>
              <Input
                type="text"
                placeholder="RPT-..."
                value={reportRefId}
                onChange={(e) => setReportRefId(e.target.value)}
                className="h-8 text-xs font-mono"
              />
            </div>

            {/* Mandatory Justification */}
            <div className="space-y-1.5">
              <Label className="text-xs font-medium text-slate-700 flex items-center justify-between">
                <span>Clinical / Emergency Justification (Mandatory)</span>
                <span className={`text-[10px] ${justification.trim().length >= 10 ? "text-emerald-600 font-medium" : "text-amber-600"}`}>
                  {justification.trim().length}/10 chars min
                </span>
              </Label>
              <textarea
                rows={3}
                placeholder="State the clinical emergency, legal subpoena, or critical diagnostic reason requiring break-glass review..."
                value={justification}
                onChange={(e) => setJustification(e.target.value)}
                className="w-full rounded-md border border-slate-200 bg-white p-2.5 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-hidden focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" size="sm" onClick={handleClose} className="text-xs">
                Cancel
              </Button>
              <Button
                type="button"
                size="sm"
                variant="destructive"
                disabled={breakGlassMutation.isPending || justification.trim().length < 10 || !orgRefId || !reportRefId}
                onClick={() => breakGlassMutation.mutate()}
                className="text-xs gap-1.5 bg-rose-700 hover:bg-rose-800"
              >
                {breakGlassMutation.isPending ? (
                  <>
                    <RotateCw className="h-3.5 w-3.5 animate-spin" />
                    Executing Break-Glass...
                  </>
                ) : (
                  <>
                    <Lock className="h-3.5 w-3.5" />
                    Confirm & Execute Break-Glass
                  </>
                )}
              </Button>
            </DialogFooter>
          </div>
        ) : (
          /* Retrieved Report Summary View */
          <div className="space-y-4 pt-1 text-xs">
            <div className="flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-emerald-900">
              <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
              <div className="text-xs">
                <span className="font-bold">Break-Glass Access Granted & Audited</span>
                <p className="text-[11px] text-emerald-700 mt-0.5">
                  An immutable security audit log record has been registered for this session.
                </p>
              </div>
            </div>

            {/* Report Metadata */}
            <div className="grid grid-cols-2 gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
              <div>
                <span className="text-[10px] uppercase font-medium text-slate-400">Report Ref ID</span>
                <p className="font-mono font-bold text-slate-800 mt-0.5">{retrievedReport.refId}</p>
              </div>
              <div>
                <span className="text-[10px] uppercase font-medium text-slate-400">Status</span>
                <div className="mt-0.5">
                  <Badge variant="outline" className="text-[10px] bg-white font-medium">
                    {retrievedReport.status}
                  </Badge>
                </div>
              </div>
              <div>
                <span className="text-[10px] uppercase font-medium text-slate-400">Organization</span>
                <p className="font-semibold text-slate-800 mt-0.5 truncate">
                  {retrievedReport.organizationName || retrievedReport.organizationRefId}
                </p>
              </div>
              <div>
                <span className="text-[10px] uppercase font-medium text-slate-400">Patient Ref ID</span>
                <p className="font-mono text-slate-800 mt-0.5">{retrievedReport.patientRefId}</p>
              </div>
            </div>

            {/* Tests Included */}
            <div className="rounded-lg border border-slate-200 p-3 space-y-2">
              <span className="text-[10px] uppercase font-bold text-slate-500">
                Diagnostic Tests in Report ({retrievedReport.tests?.length || 0})
              </span>
              <div className="space-y-1.5 max-h-40 overflow-y-auto pr-1">
                {retrievedReport.tests?.map((test: any, idx: number) => (
                  <div key={test.refId || idx} className="rounded border border-slate-100 bg-slate-50/50 p-2 text-[11px] flex items-center justify-between">
                    <div>
                      <span className="font-semibold text-slate-900">{test.testName}</span>
                      <span className="text-slate-400 font-mono ml-1.5">({test.testCode})</span>
                    </div>
                    <Badge variant="outline" className="text-[9px] py-0 h-4 bg-white">
                      {test.parameterResults?.length || 0} params
                    </Badge>
                  </div>
                ))}
              </div>
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" size="sm" onClick={handleReset} className="text-xs">
                Inspect Another Report
              </Button>
              <Button type="button" size="sm" onClick={handleClose} className="text-xs bg-slate-900 hover:bg-slate-800 text-white">
                Close
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
