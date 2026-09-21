import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Building2,
  CheckCircle2,
  FilePlus2,
  Loader2,
  Search,
  UserCheck,
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
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";

import { usePatientsQuery } from "../../patients/hooks/usePatients";
import { useCreateReportMutation } from "../hooks/useReports";
import type { PatientResponse } from "../../patients/types/patientTypes";

interface CreateReportModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateReportModal({
  open,
  onOpenChange,
}: CreateReportModalProps) {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [includeHeader, setIncludeHeader] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm.trim());
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  // Reset state on modal open/close
  useEffect(() => {
    if (!open) {
      setSearchTerm("");
      setDebouncedSearch("");
      setSelectedPatient(null);
      setIncludeHeader(true);
      setErrorMessage(null);
    }
  }, [open]);

  const {
    data: patientsData,
    isLoading: isPatientsLoading,
  } = usePatientsQuery({
    search: debouncedSearch || undefined,
    size: 10,
    sortBy: "name",
    sortDirection: "asc",
  });

  const patientsList = patientsData?.content ?? [];
  const createReportMutation = useCreateReportMutation();

  const handleCreate = async () => {
    if (!selectedPatient) {
      setErrorMessage("Please select a patient before creating the report.");
      return;
    }

    setErrorMessage(null);
    try {
      const newReport = await createReportMutation.mutateAsync({
        patientRefId: selectedPatient.refId,
        includeOrganizationHeader: includeHeader,
      });
      onOpenChange(false);
      navigate(`/org-admin/reports/${newReport.refId}`);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to create report draft. Ensure organization license is active.";
      setErrorMessage(msg);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-blue-50 text-blue-600">
              <FilePlus2 className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold text-slate-900">
                Create Diagnostic Report
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Select a registered patient to initialize a new report draft workspace.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {errorMessage && (
            <Alert variant="destructive">
              <AlertDescription className="text-xs">{errorMessage}</AlertDescription>
            </Alert>
          )}

          {/* Selected Patient Banner or Search */}
          {!selectedPatient ? (
            <div className="space-y-2">
              <Label className="text-xs font-semibold text-slate-700">
                Select Patient <span className="text-rose-500">*</span>
              </Label>
              <div className="relative">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <Input
                  type="search"
                  placeholder="Search by name, code, phone, or email..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-9 text-xs h-9"
                  autoFocus
                />
              </div>

              {/* Patient Search Results */}
              <div className="rounded-lg border border-slate-200 max-h-52 overflow-y-auto divide-y divide-slate-100 bg-white">
                {isPatientsLoading ? (
                  <div className="p-3 space-y-2">
                    <Skeleton className="h-9 w-full" />
                    <Skeleton className="h-9 w-full" />
                  </div>
                ) : patientsList.length === 0 ? (
                  <div className="p-4 text-center text-xs text-slate-500">
                    {debouncedSearch
                      ? `No patients found matching "${debouncedSearch}".`
                      : "Type to search registered patients in your organization."}
                  </div>
                ) : (
                  patientsList.map((patient) => (
                    <button
                      key={patient.refId}
                      type="button"
                      onClick={() => setSelectedPatient(patient)}
                      className="w-full text-left p-2.5 hover:bg-blue-50/60 transition-colors flex items-center justify-between group"
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-xs text-slate-900 group-hover:text-blue-700">
                            {patient.salutation ? `${patient.salutation}. ` : ""}
                            {patient.name}
                          </span>
                          <span className="font-mono text-[10px] text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
                            {patient.patientCode}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          {patient.gender} &bull;{" "}
                          {patient.ageValue != null ? `${patient.ageValue} ${patient.ageUnit?.toLowerCase() ?? "yrs"}` : "Age N/A"}{" "}
                          {patient.phone ? `&bull; ${patient.phone}` : ""}
                        </p>
                      </div>
                      <Badge variant="outline" className="text-[10px] text-slate-500 group-hover:border-blue-300 group-hover:text-blue-700">
                        Select
                      </Badge>
                    </button>
                  ))
                )}
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-blue-200 bg-blue-50/40 p-3 flex items-start justify-between gap-3">
              <div className="flex items-start gap-2.5">
                <div className="p-1.5 rounded-full bg-blue-100 text-blue-700 mt-0.5">
                  <UserCheck className="h-4 w-4" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-slate-900">
                      {selectedPatient.salutation ? `${selectedPatient.salutation}. ` : ""}
                      {selectedPatient.name}
                    </span>
                    <span className="font-mono text-xs text-blue-700 bg-blue-100 px-1.5 py-0.5 rounded font-medium">
                      {selectedPatient.patientCode}
                    </span>
                  </div>
                  <p className="text-xs text-slate-600 mt-0.5">
                    {selectedPatient.gender} &bull;{" "}
                    {selectedPatient.ageValue != null
                      ? `${selectedPatient.ageValue} ${selectedPatient.ageUnit?.toLowerCase() ?? "years"}`
                      : "Age unspecified"}{" "}
                    {selectedPatient.phone ? `&bull; Tel: ${selectedPatient.phone}` : ""}
                  </p>
                  {selectedPatient.email && (
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      {selectedPatient.email}
                    </p>
                  )}
                </div>
              </div>

              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => setSelectedPatient(null)}
                className="text-xs text-blue-700 hover:text-blue-900 hover:bg-blue-100/50 h-7"
              >
                Change
              </Button>
            </div>
          )}

          {/* Organization Header Toggle */}
          <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3 flex items-center justify-between gap-3">
            <div className="space-y-0.5">
              <Label htmlFor="include-header" className="text-xs font-semibold text-slate-900 flex items-center gap-1.5">
                <Building2 className="h-3.5 w-3.5 text-slate-500" />
                Include Organization Header
              </Label>
              <p className="text-[11px] text-slate-500">
                Render lab branding, address, and contact details on the report.
              </p>
            </div>
            <input
              id="include-header"
              type="checkbox"
              checked={includeHeader}
              onChange={(e) => setIncludeHeader(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
            />
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onOpenChange(false)}
            disabled={createReportMutation.isPending}
            className="text-xs"
          >
            Cancel
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={handleCreate}
            disabled={!selectedPatient || createReportMutation.isPending}
            className="text-xs bg-blue-600 hover:bg-blue-700 text-white font-medium"
          >
            {createReportMutation.isPending ? (
              <>
                <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                Creating Draft...
              </>
            ) : (
              <>
                <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />
                Create Report Draft
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
