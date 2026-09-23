import { useState, useMemo } from "react";
import {
  Building2,
  Check,
  ChevronRight,
  Filter,
  Globe,
  Mail,
  MapPin,
  Phone,
  Search,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyOrganizationProfileQuery } from "@/modules/org-admin/settings/hooks/useOrgProfile";
import { useUsersQuery } from "@/modules/super-admin/hooks/useUsers";
import type { UserResponse } from "@/modules/super-admin/types/userTypes";
import type { PatientResponse } from "@/modules/org-admin/patients/types/patientTypes";

interface Step2DoctorProps {
  selectedPatient: PatientResponse | null;
  selectedDoctor?: UserResponse | null;
  onSelectDoctor?: (doctor: UserResponse) => void;
  onBack: () => void;
  onContinue: () => void;
}

function getAvatarColor(name: string) {
  const colors = [
    { bg: "bg-teal-100", text: "text-teal-800" },
    { bg: "bg-emerald-100", text: "text-emerald-800" },
    { bg: "bg-blue-100", text: "text-blue-800" },
    { bg: "bg-indigo-100", text: "text-indigo-800" },
    { bg: "bg-purple-100", text: "text-purple-800" },
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % colors.length;
  return colors[index];
}

function getInitials(name: string) {
  const parts = name.trim().split(" ");
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

export function Step2Doctor({
  selectedPatient,
  selectedDoctor: initialSelectedDoctor,
  onSelectDoctor,
  onBack,
  onContinue,
}: Step2DoctorProps) {
  const { data: profile, isLoading: isProfileLoading } = useMyOrganizationProfileQuery();
  const { data: usersData, isLoading: isUsersLoading } = useUsersQuery({
    status: "ACTIVE",
    size: 50,
  });

  const staffList = useMemo(
    () => usersData?.content ?? [],
    [usersData?.content]
  );

  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<"ALL" | "ORG_ADMIN" | "LAB_STAFF">("ALL");

  // Local doctor state fallback
  const [localDoctor, setLocalDoctor] = useState<UserResponse | null>(
    initialSelectedDoctor || null
  );

  const activeDoctor = initialSelectedDoctor || localDoctor || staffList[0] || null;

  const handleSelect = (user: UserResponse) => {
    setLocalDoctor(user);
    if (onSelectDoctor) {
      onSelectDoctor(user);
    }
  };

  const filteredStaff: UserResponse[] = useMemo(() => {
    return staffList.filter((u: UserResponse) => {
      if (roleFilter !== "ALL" && u.role !== roleFilter) return false;
      if (!searchTerm.trim()) return true;
      const q = searchTerm.toLowerCase();
      return u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q);
    });
  }, [staffList, roleFilter, searchTerm]);

  return (
    <div className="space-y-6">
      {/* Selected Patient Banner Reminder */}
      {selectedPatient && (
        <div className="bg-[#F8FAFC] border border-slate-200/80 rounded-xl p-3 sm:px-4 sm:py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2 shadow-2xs">
          <div className="flex items-center gap-3">
            <div
              className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                getAvatarColor(selectedPatient.name).bg
              } ${getAvatarColor(selectedPatient.name).text}`}
            >
              {getInitials(selectedPatient.name)}
            </div>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
              <span className="font-bold text-slate-900">
                {selectedPatient.salutation ? `${selectedPatient.salutation}. ` : ""}
                {selectedPatient.name}
              </span>
              <span className="font-mono text-slate-600 bg-white px-1.5 py-0.5 rounded border border-slate-200 text-[11px] font-medium">
                {selectedPatient.patientCode}
              </span>
              <span className="text-slate-400">&bull;</span>
              <span className="text-slate-600">
                {selectedPatient.ageValue != null
                  ? `${selectedPatient.ageValue} yrs`
                  : selectedPatient.dateOfBirth || "—"}
                , <span className="capitalize">{selectedPatient.gender.toLowerCase()}</span>
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onBack}
            className="text-xs text-[#0F766E] font-semibold hover:underline self-end sm:self-center cursor-pointer"
          >
            ✏ Change Patient
          </button>
        </div>
      )}

      {/* Main 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* ======================================================== */}
        {/* LEFT COLUMN: Doctor / Staff Selection (8 cols)           */}
        {/* ======================================================== */}
        <div className="lg:col-span-8 space-y-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900 tracking-tight">
              Step 2: Reporting Doctor & Pathologist
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Select the authorized medical laboratory professional who will sign and certify this diagnostic report.
            </p>
          </div>

          {/* Search and Role Filter Bar */}
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <Input
                type="text"
                placeholder="Search staff by name or email..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 pr-8 h-10 text-xs bg-white border-slate-200 focus-visible:ring-[#0F766E]"
              />
              {searchTerm && (
                <button
                  type="button"
                  onClick={() => setSearchTerm("")}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-600 cursor-pointer"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
              <Filter className="h-3.5 w-3.5 text-slate-400 hidden sm:inline" />
              <select
                value={roleFilter}
                onChange={(e) =>
                  setRoleFilter(e.target.value as "ALL" | "ORG_ADMIN" | "LAB_STAFF")
                }
                className="h-10 rounded-md border border-slate-200 bg-white px-3 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-[#0F766E]"
              >
                <option value="ALL">All Roles</option>
                <option value="ORG_ADMIN">Lab Administrators</option>
                <option value="LAB_STAFF">Lab Staff / Pathologists</option>
              </select>
            </div>
          </div>

          {/* Clinical Staff Table */}
          <div className="rounded-xl border border-slate-200/90 bg-white shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-3.5">Staff Professional</th>
                    <th className="py-3 px-3">Role</th>
                    <th className="py-3 px-3">Status</th>
                    <th className="py-3 px-3">Email</th>
                    <th className="py-3 px-3.5 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {isUsersLoading ? (
                    <tr>
                      <td colSpan={5} className="p-4 space-y-2">
                        <Skeleton className="h-10 w-full" />
                        <Skeleton className="h-10 w-full" />
                        <Skeleton className="h-10 w-full" />
                      </td>
                    </tr>
                  ) : filteredStaff.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="py-10 text-center text-slate-500">
                        <div className="max-w-xs mx-auto space-y-1">
                          <p className="font-semibold text-slate-700">No laboratory staff found</p>
                          <p className="text-xs text-slate-400">
                            {searchTerm
                              ? `No users matched "${searchTerm}".`
                              : "No active users assigned to your organization."}
                          </p>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredStaff.map((user: UserResponse) => {
                      const isSelected = activeDoctor?.refId === user.refId;
                      const colors = getAvatarColor(user.name);
                      const isSignatureOwner = Boolean(
                        profile?.signatureConfigured &&
                          (profile.signatureOwnerRefId === user.refId ||
                            profile.signatureOwnerEmail === user.email)
                      );

                      return (
                        <tr
                          key={user.refId}
                          onClick={() => handleSelect(user)}
                          className={`transition-colors cursor-pointer ${
                            isSelected
                              ? "bg-teal-50/50"
                              : "hover:bg-slate-50/70"
                          }`}
                        >
                          <td className="py-3 px-3.5">
                            <div className="flex items-center gap-3">
                              <div
                                className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${colors.bg} ${colors.text}`}
                              >
                                {getInitials(user.name)}
                              </div>
                              <div className="min-w-0">
                                <p className="font-bold text-slate-900 truncate">
                                  {user.name}
                                </p>
                                <p className="text-[11px] text-slate-400 font-mono">
                                  ID: {user.refId.slice(0, 8)}...
                                </p>
                              </div>
                            </div>
                          </td>
                          <td className="py-3 px-3">
                            <Badge
                              variant="outline"
                              className={`text-[10px] font-semibold ${
                                user.role === "ORG_ADMIN"
                                  ? "bg-purple-50 text-purple-700 border-purple-200"
                                  : "bg-blue-50 text-blue-700 border-blue-200"
                              }`}
                            >
                              {user.role === "ORG_ADMIN" ? "Org Admin" : "Lab Staff"}
                            </Badge>
                          </td>
                          <td className="py-3 px-3">
                            <div className="flex flex-col gap-1">
                              <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700">
                                <Check className="h-3 w-3 stroke-[2.5]" />
                                Authorized
                              </span>
                              {isSignatureOwner && (
                                <span className="inline-flex items-center gap-1 text-[10px] font-medium text-teal-700 bg-teal-50 px-1.5 py-0.5 rounded border border-teal-200/60 w-fit">
                                  ✓ Signature Configured
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="py-3 px-3 text-slate-600 font-medium">
                            <div className="flex items-center gap-1.5">
                              <Mail className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                              <span className="truncate max-w-[180px]">{user.email}</span>
                            </div>
                          </td>
                          <td className="py-3 px-3.5 text-right" onClick={(e) => e.stopPropagation()}>
                            {isSelected ? (
                              <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold bg-[#0F766E] text-white shadow-2xs">
                                <Check className="h-3 w-3 stroke-[3]" />
                                Selected
                              </span>
                            ) : (
                              <button
                                type="button"
                                onClick={() => handleSelect(user)}
                                className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold bg-[#E6F4EA] text-[#0F766E] hover:bg-[#0F766E] hover:text-white transition-all cursor-pointer shadow-2xs border border-teal-200"
                              >
                                Select Signatory
                                <ChevronRight className="h-3 w-3 stroke-[2.5]" />
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            <div className="bg-slate-50/60 px-4 py-2.5 border-t border-slate-200 text-xs text-slate-500">
              Showing {filteredStaff.length} laboratory professional{filteredStaff.length === 1 ? "" : "s"}
            </div>
          </div>

          {/* Bottom Navigation */}
          <div className="flex items-center justify-between pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onBack}
              className="text-xs border-slate-300 text-slate-700"
            >
              ← Back to Patient
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={onContinue}
              className="text-xs bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold"
            >
              Continue to Select Tests →
            </Button>
          </div>
        </div>

        {/* ======================================================== */}
        {/* RIGHT COLUMN: Signatory & Organization Information (4)   */}
        {/* ======================================================== */}
        <div className="lg:col-span-4 space-y-4">
          {/* Selected Doctor Preview */}
          <Card className="border-slate-200/80 bg-white shadow-xs">
            <CardContent className="p-5 space-y-3.5">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Assigned Signatory
                </span>
                {activeDoctor && (
                  <Badge className="bg-[#E6F4EA] text-[#0F766E] text-[10px] font-semibold border-teal-200 hover:bg-[#E6F4EA]">
                    Active
                  </Badge>
                )}
              </div>

              {activeDoctor ? (
                <div className="flex items-start gap-3.5 pt-1">
                  <div
                    className={`h-12 w-12 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 shadow-xs ${
                      getAvatarColor(activeDoctor.name).bg
                    } ${getAvatarColor(activeDoctor.name).text}`}
                  >
                    {getInitials(activeDoctor.name)}
                  </div>
                  <div className="min-w-0 flex-1">
                    <h4 className="text-sm font-bold text-slate-900 truncate">
                      {activeDoctor.name}
                    </h4>
                    <p className="text-xs text-slate-500 truncate mt-0.5">
                      {activeDoctor.email}
                    </p>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      <Badge
                        variant="outline"
                        className="text-[10px] font-semibold bg-slate-50 text-slate-700 border-slate-200"
                      >
                        {activeDoctor.role === "ORG_ADMIN" ? "Laboratory Admin" : "Lab Staff"}
                      </Badge>
                      <Badge
                        variant="outline"
                        className="text-[10px] font-semibold bg-emerald-50 text-emerald-700 border-emerald-200"
                      >
                        ✓ Authorized
                      </Badge>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="py-4 text-center text-xs text-slate-400">
                  No reporting doctor selected yet.
                </div>
              )}
            </CardContent>
          </Card>

          {/* ORGANIZATION INFORMATION (Informational, from backend) */}
          <Card className="border-slate-200/80 bg-white shadow-xs">
            <CardContent className="p-5 space-y-3.5">
              <div className="flex items-center gap-2 text-slate-900">
                <Building2 className="h-4 w-4 text-[#0F766E]" />
                <span className="text-xs font-bold uppercase tracking-wider">
                  Organization Information
                </span>
              </div>

              {isProfileLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-4 w-1/2" />
                </div>
              ) : profile ? (
                <div className="space-y-2.5 text-xs text-slate-600">
                  <div>
                    <span className="text-[11px] font-semibold text-slate-400 block uppercase tracking-wide">
                      Organization Name
                    </span>
                    <span className="text-sm font-bold text-slate-900">
                      {profile.organizationName}
                    </span>
                  </div>

                  {(profile.addressLine1 || profile.city || profile.state) && (
                    <div>
                      <span className="text-[11px] font-semibold text-slate-400 block uppercase tracking-wide">
                        Address
                      </span>
                      <span className="text-xs text-slate-700 flex items-start gap-1.5 mt-0.5">
                        <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0 mt-0.5" />
                        {[profile.addressLine1, profile.addressLine2, profile.city, profile.state, profile.postalCode, profile.country]
                          .filter(Boolean)
                          .join(", ")}
                      </span>
                    </div>
                  )}

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 border-t border-slate-100">
                    {profile.phone && (
                      <div>
                        <span className="text-[10px] font-semibold text-slate-400 block uppercase tracking-wide">
                          Phone
                        </span>
                        <span className="text-xs text-slate-800 flex items-center gap-1 font-medium mt-0.5">
                          <Phone className="h-3 w-3 text-slate-400 shrink-0" />
                          {profile.phone}
                        </span>
                      </div>
                    )}

                    {profile.email && (
                      <div>
                        <span className="text-[10px] font-semibold text-slate-400 block uppercase tracking-wide">
                          Email
                        </span>
                        <span className="text-xs text-slate-800 flex items-center gap-1 font-medium mt-0.5 truncate">
                          <Mail className="h-3 w-3 text-slate-400 shrink-0" />
                          <span className="truncate">{profile.email}</span>
                        </span>
                      </div>
                    )}
                  </div>

                  {profile.website && (
                    <div className="pt-1">
                      <span className="text-[10px] font-semibold text-slate-400 block uppercase tracking-wide">
                        Website
                      </span>
                      <span className="text-xs text-[#0F766E] flex items-center gap-1 font-medium mt-0.5">
                        <Globe className="h-3 w-3 text-slate-400 shrink-0" />
                        {profile.website}
                      </span>
                    </div>
                  )}

                  <div className="rounded-lg bg-slate-50 border border-slate-200/80 p-3 space-y-1.5 mt-2">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-slate-500 font-medium">Digital Signature:</span>
                      {profile.signatureConfigured ? (
                        <span className="inline-flex items-center gap-1 font-semibold text-emerald-700">
                          <Check className="h-3 w-3 stroke-[2.5]" /> Configured
                        </span>
                      ) : (
                        <span className="text-amber-600 font-semibold text-[11px]">
                          Not configured yet
                        </span>
                      )}
                    </div>
                    {profile.signatureOwnerName && (
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-500 font-medium">Signatory Name:</span>
                        <span className="font-semibold text-slate-800">
                          {profile.signatureOwnerName}
                        </span>
                      </div>
                    )}
                  </div>

                  <p className="text-[11px] text-slate-400 italic pt-1 leading-normal">
                    Organization information is sourced directly from your verified profile and is read-only here.
                  </p>
                </div>
              ) : (
                <p className="text-xs text-slate-400">
                  Organization profile details could not be loaded.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
