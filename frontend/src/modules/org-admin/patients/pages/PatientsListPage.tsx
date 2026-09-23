import { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Edit2,
  FilePlus,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  UserPlus,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { usePatientsQuery } from "../hooks/usePatients";
import { EditPatientModal } from "../components/EditPatientModal";
import { RegisterPatientModal } from "../components/RegisterPatientModal";
import type { PatientResponse } from "../types/patientTypes";

export default function PatientsListPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const isLabStaff = location.pathname.startsWith("/lab-staff");
  const basePath = isLabStaff ? "/lab-staff/patients" : "/org-admin/patients";
  const reportsBasePath = isLabStaff ? "/lab-staff/reports" : "/org-admin/reports";

  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [patientToEdit, setPatientToEdit] = useState<PatientResponse | null>(null);
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

  const {
    data: patientsData,
    isLoading,
    isError,
    error,
    refetch,
  } = usePatientsQuery({
    page,
    size: 15,
    search: searchTerm.trim() || undefined,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  const patientsList = patientsData?.content ?? [];

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
    setPage(0);
  };

  const formatAge = (p: PatientResponse) => {
    if (p.ageValue != null) {
      return `${p.ageValue} ${p.ageUnit?.toLowerCase() || "yrs"}`;
    }
    if (p.dateOfBirth) {
      return p.dateOfBirth;
    }
    return "—";
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-teal-50 text-[#0F766E] border border-teal-100">
              <Users className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
                Patients Directory
              </h1>
              <p className="mt-0.5 text-xs text-slate-500">
                Central patient records, complete diagnostic history, and repeat visit intake.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => setIsRegisterModalOpen(true)}
            size="sm"
            className="gap-1.5 text-xs border-teal-200 text-[#0F766E] hover:bg-teal-50 font-semibold shadow-2xs cursor-pointer"
          >
            <UserPlus className="h-4 w-4" />
            Register Patient
          </Button>

          <Button
            onClick={() => navigate(`${reportsBasePath}/new`)}
            size="sm"
            className="gap-1.5 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold shadow-xs cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            Create Report
          </Button>
        </div>
      </div>

      {/* Search Bar & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-3 rounded-lg border border-slate-200 shadow-2xs">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <Input
            value={searchTerm}
            onChange={handleSearchChange}
            placeholder="Search by patient name, PID code, phone, or email..."
            className="pl-9 h-9 text-xs bg-slate-50/50 border-slate-200"
          />
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          className="text-xs h-9 text-slate-700 hover:bg-slate-100 border-slate-200 self-end sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`h-3.5 w-3.5 mr-1 text-slate-500 ${isLoading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Main Content Card */}
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
            <p className="text-sm font-semibold text-slate-800">Failed to load patients</p>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {error?.message || "An unexpected error occurred while loading patient records."}
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
        ) : patientsList.length === 0 ? (
          <div className="p-12 text-center">
            <Users className="h-10 w-10 text-slate-300 mx-auto mb-2" />
            <h3 className="text-sm font-bold text-slate-800">No Patients Found</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {searchTerm
                ? `No patient records matched "${searchTerm}".`
                : "No patient records have been created in your organization yet."}
            </p>
            <Button
              size="sm"
              onClick={() => navigate(`${reportsBasePath}/new`)}
              className="mt-4 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white"
            >
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              Register First Patient
            </Button>
          </div>
        ) : (
          <>
            {/* Desktop Table View */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50/75 text-slate-600 font-semibold uppercase text-[10px] tracking-wider">
                    <th className="py-3 px-4">Patient Name & Code</th>
                    <th className="py-3 px-3">Age / Gender</th>
                    <th className="py-3 px-3">Contact</th>
                    <th className="py-3 px-3 text-center">Total Reports</th>
                    <th className="py-3 px-3">Last Report Date</th>
                    <th className="py-3 px-3">Status</th>
                    <th className="py-3 px-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {patientsList.map((p) => {
                    const reportCount = p.totalReports ?? 0;
                    return (
                      <tr
                        key={p.refId}
                        onClick={() => navigate(`${basePath}/${p.refId}`)}
                        className="hover:bg-slate-50/75 transition-colors cursor-pointer group"
                      >
                        <td className="py-3 px-4">
                          <div className="flex flex-col">
                            <span className="font-semibold text-slate-900 group-hover:text-[#0F766E] transition-colors">
                              {p.salutation ? `${p.salutation}. ` : ""}
                              {p.name}
                            </span>
                            <span className="text-[11px] font-mono text-slate-400">
                              {p.patientCode}
                            </span>
                          </div>
                        </td>

                        <td className="py-3 px-3">
                          <span className="capitalize text-slate-700 font-medium">
                            {formatAge(p)} · {p.gender.toLowerCase()}
                          </span>
                        </td>

                        <td className="py-3 px-3">
                          <div className="flex flex-col gap-0.5 text-[11px] text-slate-600">
                            {p.phone && (
                              <span className="flex items-center gap-1">
                                <Phone className="h-3 w-3 text-slate-400" />
                                {p.phone}
                              </span>
                            )}
                            {p.email && (
                              <span className="flex items-center gap-1 text-slate-500">
                                <Mail className="h-3 w-3 text-slate-400" />
                                {p.email}
                              </span>
                            )}
                            {!p.phone && !p.email && <span className="text-slate-400">—</span>}
                          </div>
                        </td>

                        <td className="py-3 px-3 text-center">
                          <span
                            className={`inline-flex items-center justify-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                              reportCount > 0
                                ? "bg-teal-50 text-[#0F766E] border border-teal-200/60"
                                : "bg-slate-100 text-slate-500"
                            }`}
                          >
                            {reportCount} {reportCount === 1 ? "report" : "reports"}
                          </span>
                        </td>

                        <td className="py-3 px-3 text-slate-500 text-[11px]">
                          {p.lastReportDate
                            ? new Date(p.lastReportDate).toLocaleDateString("en-IN", {
                                day: "2-digit",
                                month: "short",
                                year: "numeric",
                              })
                            : "—"}
                        </td>

                        <td className="py-3 px-3">
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Active
                          </span>
                        </td>

                        <td className="py-3 px-3 text-right">
                          <div
                            className="flex items-center justify-end gap-1"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Button
                              variant="ghost"
                              size="sm"
                              title="Create New Report"
                              onClick={() => navigate(`${reportsBasePath}/new?patientRefId=${p.refId}`)}
                              className="h-7 px-2 text-xs text-[#0F766E] hover:bg-teal-50 hover:text-[#115E59]"
                            >
                              <FilePlus className="h-3.5 w-3.5 mr-1" />
                              New Report
                            </Button>

                            <Button
                              variant="ghost"
                              size="sm"
                              title="Edit Patient"
                              onClick={() => setPatientToEdit(p)}
                              className="h-7 w-7 p-0 text-slate-400 hover:text-slate-700"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </Button>

                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => navigate(`${basePath}/${p.refId}`)}
                              className="h-7 px-2.5 text-xs text-slate-700 border-slate-200 hover:bg-slate-50"
                            >
                              View
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile Cards View */}
            <div className="md:hidden divide-y divide-slate-100">
              {patientsList.map((p) => {
                const reportCount = p.totalReports ?? 0;
                return (
                  <div
                    key={p.refId}
                    onClick={() => navigate(`${basePath}/${p.refId}`)}
                    className="p-4 space-y-3 hover:bg-slate-50/75 transition-colors cursor-pointer"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className="text-sm font-bold text-slate-900">
                          {p.salutation ? `${p.salutation}. ` : ""}
                          {p.name}
                        </h4>
                        <span className="font-mono text-xs text-slate-400">
                          {p.patientCode}
                        </span>
                      </div>
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                          reportCount > 0
                            ? "bg-teal-50 text-[#0F766E] border border-teal-200/60"
                            : "bg-slate-100 text-slate-500"
                        }`}
                      >
                        {reportCount} {reportCount === 1 ? "report" : "reports"}
                      </span>
                    </div>

                    <div className="text-xs text-slate-600 flex flex-wrap items-center gap-x-3 gap-y-1">
                      <span className="capitalize font-medium">
                        {formatAge(p)} · {p.gender.toLowerCase()}
                      </span>
                      {p.phone && <span>📞 {p.phone}</span>}
                    </div>

                    <div className="flex items-center justify-between pt-1 border-t border-slate-100">
                      <span className="text-[11px] text-slate-400">
                        {p.lastReportDate
                          ? `Last: ${new Date(p.lastReportDate).toLocaleDateString("en-IN", {
                              day: "2-digit",
                              month: "short",
                            })}`
                          : "No reports yet"}
                      </span>

                      <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setPatientToEdit(p)}
                          className="h-7 px-2 text-xs text-slate-500"
                        >
                          Edit
                        </Button>
                        <Button
                          size="sm"
                          onClick={() => navigate(`${basePath}/${p.refId}`)}
                          className="h-7 px-3 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white"
                        >
                          View Patient
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}

        {/* Pagination Footer */}
        {patientsData && patientsData.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs">
            <span className="text-slate-500">
              Page {patientsData.page + 1} of {patientsData.totalPages} ({patientsData.totalElements} patients)
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
                disabled={patientsData.last}
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

      {/* Edit Patient Dialog */}
      {patientToEdit && (
        <EditPatientModal
          patient={patientToEdit}
          open={Boolean(patientToEdit)}
          onOpenChange={(open) => {
            if (!open) setPatientToEdit(null);
          }}
          onSuccess={() => {
            setPatientToEdit(null);
            refetch();
          }}
        />
      )}

      {/* Register Patient Dialog */}
      <RegisterPatientModal
        open={isRegisterModalOpen}
        onOpenChange={setIsRegisterModalOpen}
        onSuccess={(created) => {
          refetch();
          navigate(`${basePath}/${created.refId}`);
        }}
      />
    </div>
  );
}
