import { useState, useEffect, useRef } from "react";
import {
  CheckCircle2,
  Edit2,
  Loader2,
  Mail,
  MapPin,
  Phone,
  Search,
  User,
  UserPlus,
  X,
  ArrowRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  usePatientsQuery,
  useCreatePatientMutation,
} from "@/modules/org-admin/patients/hooks/usePatients";
import type {
  AgeUnit,
  CreatePatientRequest,
  Gender,
  PatientResponse,
  Salutation,
} from "@/modules/org-admin/patients/types/patientTypes";

interface Step1PatientProps {
  selectedPatient: PatientResponse | null;
  onSelectPatient: (patient: PatientResponse) => void;
  onClearPatient: () => void;
  onContinue: () => void;
  readOnly?: boolean;
}

// Generate consistent soft pastel avatar colors based on initials
function getAvatarColor(name: string) {
  const colors = [
    { bg: "bg-teal-100", text: "text-teal-800" },
    { bg: "bg-emerald-100", text: "text-emerald-800" },
    { bg: "bg-blue-100", text: "text-blue-800" },
    { bg: "bg-indigo-100", text: "text-indigo-800" },
    { bg: "bg-cyan-100", text: "text-cyan-800" },
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

function formatPatientAge(patient: PatientResponse) {
  if (patient.ageValue != null) {
    const unit = patient.ageUnit ? patient.ageUnit.toLowerCase() : "years";
    return `${patient.ageValue} ${unit}`;
  }
  if (patient.dateOfBirth) {
    return patient.dateOfBirth;
  }
  return "—";
}

export function Step1Patient({
  selectedPatient,
  onSelectPatient,
  onClearPatient,
  onContinue,
  readOnly = false,
}: Step1PatientProps) {
  // Mode: "overview" (default), "register" (active form), or "search" (existing patient search)
  const [mode, setMode] = useState<"overview" | "register" | "search">("overview");

  // Registration Form State
  const [salutation, setSalutation] = useState<Salutation>("MR");
  const [name, setName] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [dobKnown, setDobKnown] = useState(false);
  const [dob, setDob] = useState("");
  const [ageValue, setAgeValue] = useState<string>("");
  const [ageUnit, setAgeUnit] = useState<AgeUnit>("YEARS");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [weightKg, setWeightKg] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const nameInputRef = useRef<HTMLInputElement | null>(null);

  // Search State
  const [searchTerm, setSearchTerm] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  // Debounce search query (300ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm.trim());
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const { data: patientsData, isLoading: isSearchLoading } = usePatientsQuery({
    search: debouncedSearch || undefined,
    size: 20,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  const patientsList = patientsData?.content ?? [];
  const createPatientMutation = useCreatePatientMutation();

  // Focus name field when register mode is opened
  useEffect(() => {
    if (mode === "register") {
      nameInputRef.current?.focus();
    }
  }, [mode]);

  // Auto-sync gender based on salutation selection
  const handleSalutationChange = (newSalutation: Salutation) => {
    setSalutation(newSalutation);
    if (newSalutation === "MR" || newSalutation === "MASTER") {
      setGender("MALE");
    } else if (newSalutation === "MRS" || newSalutation === "MS") {
      setGender("FEMALE");
    }
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!name.trim()) {
      setFormError("Patient full name is required.");
      return;
    }

    if (!dobKnown && (!ageValue || Number(ageValue) <= 0)) {
      setFormError("Valid age value is required (e.g. 32).");
      return;
    }

    if (dobKnown && !dob) {
      setFormError("Date of birth is required when DOB toggle is active.");
      return;
    }

    const payload: CreatePatientRequest = {
      salutation,
      name: name.trim(),
      gender,
      dateOfBirthKnown: dobKnown,
      dateOfBirth: dobKnown && dob ? dob : null,
      ageValue: !dobKnown && ageValue ? Number(ageValue) : null,
      ageUnit: !dobKnown ? ageUnit : null,
      phone: phone.trim() || null,
      email: email.trim() || null,
      address: address.trim() || null,
      weightKg: weightKg ? Number(weightKg) : null,
    };

    try {
      const created = await createPatientMutation.mutateAsync(payload);
      onSelectPatient(created);
      setMode("overview");
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Failed to register patient.";
      setFormError(msg);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* ======================================================== */}
      {/* MAIN COLUMN (8 cols on lg)                               */}
      {/* ======================================================== */}
      <div className="lg:col-span-8 space-y-6">
        {/* ======================================================== */}
        {/* STATE A: PATIENT CONFIRMED (After Registration/Selection)*/}
        {/* ======================================================== */}
        {selectedPatient ? (
          <div className="bg-white rounded-xl border border-teal-200/90 shadow-sm overflow-hidden">
            {/* Header confirmation ribbon */}
            <div className="bg-emerald-50/90 border-b border-emerald-100 px-5 py-3.5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                <span className="text-sm font-bold text-emerald-800 tracking-wide uppercase">
                  ✓ Patient registered
                </span>
              </div>
              {!readOnly && (
                <button
                  type="button"
                  onClick={() => {
                    onClearPatient();
                    setMode("overview");
                  }}
                  className="text-xs font-semibold text-slate-500 hover:text-slate-800 underline flex items-center gap-1 cursor-pointer transition-colors"
                >
                  <Edit2 className="h-3 w-3" />
                  Change Patient
                </button>
              )}
            </div>

            {/* Confirmation Demographics Grid */}
            <div className="p-6 sm:p-7 space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                <div>
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                    PATIENT NAME
                  </div>
                  <div className="text-lg font-bold text-slate-900 mt-1 uppercase">
                    {selectedPatient.salutation
                      ? `${selectedPatient.salutation}. `
                      : ""}
                    {selectedPatient.name}
                  </div>
                </div>

                <div>
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                    PATIENT ID
                  </div>
                  <div className="font-mono text-lg font-bold text-[#0F766E] mt-1">
                    {selectedPatient.patientCode}
                  </div>
                </div>

                <div>
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                    AGE / GENDER
                  </div>
                  <div className="text-lg font-medium text-slate-800 mt-1 capitalize">
                    {formatPatientAge(selectedPatient)} ·{" "}
                    {selectedPatient.gender.toLowerCase()}
                  </div>
                </div>
              </div>

              {/* Contact Information & Metadata */}
              {(selectedPatient.phone ||
                selectedPatient.email ||
                selectedPatient.address) && (
                <div className="pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-slate-600">
                  {selectedPatient.phone && (
                    <div className="flex items-center gap-2">
                      <Phone className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="font-medium text-slate-800">
                        {selectedPatient.phone}
                      </span>
                    </div>
                  )}
                  {selectedPatient.email && (
                    <div className="flex items-center gap-2">
                      <Mail className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="font-medium text-slate-800 truncate">
                        {selectedPatient.email}
                      </span>
                    </div>
                  )}
                  {selectedPatient.address && (
                    <div className="flex items-center gap-2">
                      <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="font-medium text-slate-800 truncate">
                        {selectedPatient.address}
                      </span>
                    </div>
                  )}
                </div>
              )}

              {/* Primary Next Action */}
              <div className="pt-4 border-t border-slate-100 flex items-center justify-end">
                <Button
                  type="button"
                  onClick={onContinue}
                  className="bg-[#0F766E] hover:bg-[#115E59] text-white h-11 px-7 text-sm font-semibold flex items-center gap-2 shadow-xs cursor-pointer rounded-lg transition-all"
                >
                  <span>Continue to Reporting Professional</span>
                  <ArrowRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        ) : (
          /* ======================================================== */
          /* STATE B: REGISTER IS PRIMARY, SEARCH IS SECONDARY       */
          /* ======================================================== */
          <div className="space-y-6">
            {/* 1. PRIMARY HERO CARD: REGISTER NEW PATIENT */}
            <div className="bg-white rounded-xl border border-teal-200/90 shadow-sm p-6 sm:p-7 relative overflow-hidden transition-all">
              <div className="max-w-2xl space-y-3">
                <div className="text-xs font-bold text-[#0F766E] uppercase tracking-wider">
                  Step 1 · Patient
                </div>
                <h2 className="text-2xl font-bold text-slate-900 tracking-tight">
                  Register a new patient to begin the diagnostic report.
                </h2>
                <p className="text-sm text-slate-600 leading-relaxed">
                  Create a patient record with the required information before continuing to the report.
                </p>

                {mode !== "register" && (
                  <div className="pt-3">
                    <Button
                      type="button"
                      onClick={() => {
                        setMode("register");
                        setFormError(null);
                      }}
                      className="bg-[#0F766E] hover:bg-[#115E59] text-white h-12 px-7 text-sm font-semibold flex items-center gap-2.5 shadow-sm rounded-lg cursor-pointer transition-all"
                    >
                      <UserPlus className="h-4 w-4" />
                      <span>+ Register New Patient</span>
                    </Button>
                  </div>
                )}
              </div>

              {/* INLINE REGISTRATION FORM (EXPANDS WHEN CLICKED) */}
              {mode === "register" && (
                <form
                  onSubmit={handleRegisterSubmit}
                  className="mt-6 pt-6 border-t border-slate-100 space-y-6"
                >
                  {formError && (
                    <Alert variant="destructive">
                      <AlertDescription className="text-xs">
                        {formError}
                      </AlertDescription>
                    </Alert>
                  )}

                  {/* Section 1: Demographics */}
                  <div className="space-y-3">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      1. Patient Demographics
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-12 gap-3.5">
                      {/* Salutation */}
                      <div className="sm:col-span-3 space-y-1">
                        <Label htmlFor="salutation" className="text-xs font-medium text-slate-700">
                          Title *
                        </Label>
                        <select
                          id="salutation"
                          value={salutation}
                          onChange={(e) =>
                            handleSalutationChange(e.target.value as Salutation)
                          }
                          className="w-full h-10 px-2.5 rounded-lg border border-slate-200 bg-white text-sm font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#0F766E]/20 focus:border-[#0F766E]"
                        >
                          <option value="MR">Mr.</option>
                          <option value="MRS">Mrs.</option>
                          <option value="MS">Ms.</option>
                          <option value="MASTER">Master</option>
                          <option value="BABY">Baby</option>
                          <option value="DR">Dr.</option>
                          <option value="OTHER">Other</option>
                        </select>
                      </div>

                      {/* Full Name */}
                      <div className="sm:col-span-6 space-y-1">
                        <Label htmlFor="fullName" className="text-xs font-medium text-slate-700">
                          Patient Full Name *
                        </Label>
                        <Input
                          ref={nameInputRef}
                          id="fullName"
                          type="text"
                          required
                          placeholder="e.g. Ram Sharma"
                          value={name}
                          onChange={(e) => setName(e.target.value)}
                          className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                        />
                      </div>

                      {/* Gender */}
                      <div className="sm:col-span-3 space-y-1">
                        <Label htmlFor="gender" className="text-xs font-medium text-slate-700">
                          Gender *
                        </Label>
                        <select
                          id="gender"
                          value={gender}
                          onChange={(e) => setGender(e.target.value as Gender)}
                          className="w-full h-10 px-2.5 rounded-lg border border-slate-200 bg-white text-sm font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#0F766E]/20 focus:border-[#0F766E]"
                        >
                          <option value="MALE">Male</option>
                          <option value="FEMALE">Female</option>
                          <option value="OTHER">Other</option>
                        </select>
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Age or Date of Birth */}
                  <div className="space-y-3 pt-1">
                    <div className="flex items-center justify-between">
                      <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                        2. Age & Birth Details
                      </h3>
                      <button
                        type="button"
                        onClick={() => setDobKnown(!dobKnown)}
                        className="text-xs font-semibold text-[#0F766E] hover:underline cursor-pointer"
                      >
                        {dobKnown
                          ? "Switch to Age (Years)"
                          : "Enter Date of Birth instead"}
                      </button>
                    </div>

                    {!dobKnown ? (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                        <div className="space-y-1">
                          <Label htmlFor="ageVal" className="text-xs font-medium text-slate-700">
                            Age Value *
                          </Label>
                          <Input
                            id="ageVal"
                            type="number"
                            min="1"
                            max="125"
                            placeholder="e.g. 32"
                            value={ageValue}
                            onChange={(e) => setAgeValue(e.target.value)}
                            className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                          />
                        </div>

                        <div className="space-y-1">
                          <Label htmlFor="ageUnit" className="text-xs font-medium text-slate-700">
                            Age Unit *
                          </Label>
                          <select
                            id="ageUnit"
                            value={ageUnit}
                            onChange={(e) =>
                              setAgeUnit(e.target.value as AgeUnit)
                            }
                            className="w-full h-10 px-2.5 rounded-lg border border-slate-200 bg-white text-sm font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#0F766E]/20 focus:border-[#0F766E]"
                          >
                            <option value="YEARS">Years</option>
                            <option value="MONTHS">Months</option>
                            <option value="WEEKS">Weeks</option>
                            <option value="DAYS">Days</option>
                          </select>
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-1 max-w-sm">
                        <Label htmlFor="dobDate" className="text-xs font-medium text-slate-700">
                          Date of Birth *
                        </Label>
                        <Input
                          id="dobDate"
                          type="date"
                          max={new Date().toISOString().split("T")[0]}
                          value={dob}
                          onChange={(e) => setDob(e.target.value)}
                          className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                        />
                      </div>
                    )}
                  </div>

                  {/* Section 3: Contact Details */}
                  <div className="space-y-3 pt-1">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      3. Contact & Communication
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                      <div className="space-y-1">
                        <Label htmlFor="regPhone" className="text-xs font-medium text-slate-700">
                          Mobile Phone
                        </Label>
                        <Input
                          id="regPhone"
                          type="tel"
                          placeholder="e.g. 9876543210"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value)}
                          className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                        />
                      </div>

                      <div className="space-y-1">
                        <Label htmlFor="regEmail" className="text-xs font-medium text-slate-700">
                          Email Address (Optional)
                        </Label>
                        <Input
                          id="regEmail"
                          type="email"
                          placeholder="e.g. patient@example.com"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 4: Physical & Locality Details */}
                  <div className="space-y-3 pt-1">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      4. Address & Clinical Details (Optional)
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-12 gap-3.5">
                      <div className="sm:col-span-8 space-y-1">
                        <Label htmlFor="regAddress" className="text-xs font-medium text-slate-700">
                          Address / Locality
                        </Label>
                        <Input
                          id="regAddress"
                          type="text"
                          placeholder="e.g. Flat 402, Sector 12, Dwarka, New Delhi"
                          value={address}
                          onChange={(e) => setAddress(e.target.value)}
                          className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                        />
                      </div>

                      <div className="sm:col-span-4 space-y-1">
                        <Label htmlFor="regWeight" className="text-xs font-medium text-slate-700">
                          Weight (kg)
                        </Label>
                        <Input
                          id="regWeight"
                          type="number"
                          step="0.1"
                          min="0.5"
                          max="300"
                          placeholder="e.g. 68.5"
                          value={weightKg}
                          onChange={(e) => setWeightKg(e.target.value)}
                          className="h-10 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Form Submission Actions */}
                  <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setMode("overview")}
                      className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
                    >
                      Cancel
                    </Button>

                    <Button
                      type="submit"
                      disabled={createPatientMutation.isPending}
                      className="bg-[#0F766E] hover:bg-[#115E59] text-white h-11 px-7 text-sm font-semibold flex items-center gap-2 shadow-xs cursor-pointer rounded-lg"
                    >
                      {createPatientMutation.isPending ? (
                        <>
                          <Loader2 className="h-4 w-4 animate-spin" />
                          <span>Registering Patient...</span>
                        </>
                      ) : (
                        <>
                          <span>Register Patient & Confirm</span>
                          <ArrowRight className="h-4 w-4" />
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              )}
            </div>

            {/* 2. SECONDARY SECTION: ALREADY REGISTERED? SEARCH */}
            <div className="bg-slate-50/80 rounded-xl border border-slate-200/90 p-5 sm:p-6 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-bold text-slate-800">
                    Already registered?
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Search and select an existing patient from your organization.
                  </p>
                </div>

                {mode !== "search" ? (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setMode("search");
                    }}
                    className="h-9 px-4 text-xs font-semibold text-slate-700 border-slate-300 hover:bg-white hover:text-slate-900 self-start sm:self-center cursor-pointer"
                  >
                    <Search className="h-3.5 w-3.5 mr-1.5 text-slate-500" />
                    Search Existing Patient
                  </Button>
                ) : (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setMode("overview")}
                    className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
                  >
                    Close Search
                  </Button>
                )}
              </div>

              {/* SEARCH INPUT & RESULTS TABLE (WHEN SEARCH MODE IS ACTIVE) */}
              {mode === "search" && (
                <div className="pt-3 border-t border-slate-200/80 space-y-3.5">
                  <div className="relative">
                    <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
                    <Input
                      type="text"
                      placeholder="Search by patient name, patient code (e.g. PAT-000123), phone, or email..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10 pr-9 h-11 text-sm bg-white border-slate-200 focus-visible:ring-2 focus-visible:ring-[#0F766E]/20 focus-visible:border-[#0F766E]"
                      autoFocus
                    />
                    {searchTerm && (
                      <button
                        type="button"
                        onClick={() => setSearchTerm("")}
                        className="absolute right-3.5 top-3.5 text-slate-400 hover:text-slate-600 cursor-pointer"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    )}
                  </div>

                  {/* Results Container */}
                  <div className="bg-white rounded-lg border border-slate-200 overflow-hidden shadow-2xs">
                    {isSearchLoading ? (
                      <div className="p-4 space-y-3">
                        <div className="h-10 bg-slate-100 rounded animate-pulse" />
                        <div className="h-10 bg-slate-100 rounded animate-pulse" />
                      </div>
                    ) : patientsList.length === 0 ? (
                      <div className="py-8 text-center px-4">
                        <User className="h-8 w-8 text-slate-300 mx-auto mb-2" />
                        <p className="text-xs font-semibold text-slate-700">
                          {searchTerm
                            ? `No patients found matching "${searchTerm}".`
                            : "No patients registered yet."}
                        </p>
                        <p className="text-[11px] text-slate-400 mt-1">
                          You can register this patient as a new walk-in patient above.
                        </p>
                      </div>
                    ) : (
                      <div className="divide-y divide-slate-100">
                        {patientsList.map((patient) => {
                          const colors = getAvatarColor(patient.name);
                          return (
                            <div
                              key={patient.refId}
                              className="p-3 sm:px-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/70 transition-colors"
                            >
                              <div className="flex items-center gap-3">
                                <div
                                  className={`h-9 w-9 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${colors.bg} ${colors.text}`}
                                >
                                  {getInitials(patient.name)}
                                </div>
                                <div>
                                  <div className="flex items-center gap-2">
                                    <span className="text-sm font-bold text-slate-900">
                                      {patient.salutation
                                        ? `${patient.salutation}. `
                                        : ""}
                                      {patient.name}
                                    </span>
                                    <Badge
                                      variant="outline"
                                      className="font-mono text-[11px] text-slate-500 border-slate-200"
                                    >
                                      {patient.patientCode}
                                    </Badge>
                                  </div>
                                  <div className="text-xs text-slate-500 flex flex-wrap items-center gap-x-3 gap-y-0.5 mt-0.5">
                                    <span className="capitalize">
                                      {patient.gender.toLowerCase()} ·{" "}
                                      {formatPatientAge(patient)}
                                    </span>
                                    {patient.phone && (
                                      <span>📞 {patient.phone}</span>
                                    )}
                                  </div>
                                </div>
                              </div>

                              <Button
                                type="button"
                                size="sm"
                                onClick={() => {
                                  onSelectPatient(patient);
                                  setMode("overview");
                                }}
                                className="bg-[#0F766E] hover:bg-[#115E59] text-white text-xs h-8 px-4 font-semibold shrink-0 cursor-pointer"
                              >
                                Select Patient
                              </Button>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* ======================================================== */}
      {/* SIDEBAR COLUMN: Diagnostic Lab Intake Guide (4 cols)     */}
      {/* ======================================================== */}
      <div className="lg:col-span-4 space-y-4">
        <Card className="border-slate-200/80 bg-white shadow-2xs">
          <CardContent className="p-5 space-y-4">
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Diagnostic Intake Protocol
              </h3>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                Diagnostic laboratories primarily register new walk-in patients on the spot. All demographic and biological parameters are captured immediately.
              </p>
            </div>

            <div className="rounded-lg bg-slate-50 border border-slate-100 p-3.5 space-y-2">
              <p className="text-[11px] font-bold text-slate-700 uppercase tracking-wide">
                Intake Highlights
              </p>
              <ul className="space-y-1.5 text-xs text-slate-600">
                <li className="flex items-center gap-2">
                  <span className="text-[#0F766E] font-bold">✓</span>
                  Unique Patient Code / MRN auto-generation
                </li>
                <li className="flex items-center gap-2">
                  <span className="text-[#0F766E] font-bold">✓</span>
                  Instant biological gender sync
                </li>
                <li className="flex items-center gap-2">
                  <span className="text-[#0F766E] font-bold">✓</span>
                  Automated age-specific biological reference ranges
                </li>
              </ul>
            </div>

            {selectedPatient && (
              <div className="pt-2">
                <Button
                  type="button"
                  onClick={onContinue}
                  className="w-full h-11 text-xs font-bold bg-[#0F766E] hover:bg-[#115E59] text-white shadow-xs rounded-lg cursor-pointer"
                >
                  Continue to Reporting Doctor →
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
