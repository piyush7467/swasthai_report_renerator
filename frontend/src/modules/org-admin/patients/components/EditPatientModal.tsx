import { useState, useEffect } from "react";
import {
  AlertCircle,
  Loader2,
  Mail,
  MapPin,
  Phone,
  User,
  X,
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
import { useUpdatePatientMutation } from "../hooks/usePatients";
import type {
  AgeUnit,
  Gender,
  PatientResponse,
  Salutation,
  UpdatePatientRequest,
} from "../types/patientTypes";

interface EditPatientModalProps {
  patient: PatientResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

export function EditPatientModal({
  patient,
  open,
  onOpenChange,
  onSuccess,
}: EditPatientModalProps) {
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

  const updateMutation = useUpdatePatientMutation(patient?.refId || "");

  useEffect(() => {
    if (patient && open) {
      setSalutation((patient.salutation as Salutation) || "MR");
      setName(patient.name || "");
      setGender(patient.gender || "MALE");
      setDobKnown(patient.dateOfBirthKnown || false);
      setDob(patient.dateOfBirth || "");
      setAgeValue(patient.ageValue != null ? String(patient.ageValue) : "");
      setAgeUnit(patient.ageUnit || "YEARS");
      setPhone(patient.phone || "");
      setEmail(patient.email || "");
      setAddress(patient.address || "");
      setWeightKg(patient.weightKg != null ? String(patient.weightKg) : "");
      setFormError(null);
    }
  }, [patient, open]);

  const handleSalutationChange = (newSalutation: Salutation) => {
    setSalutation(newSalutation);
    if (newSalutation === "MR" || newSalutation === "MASTER") {
      setGender("MALE");
    } else if (newSalutation === "MRS" || newSalutation === "MS") {
      setGender("FEMALE");
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!patient) return;
    setFormError(null);

    if (!name.trim()) {
      setFormError("Patient full name is required.");
      return;
    }

    if (!dobKnown && (!ageValue || Number(ageValue) <= 0)) {
      setFormError("Valid age value is required.");
      return;
    }

    if (dobKnown && !dob) {
      setFormError("Date of birth is required.");
      return;
    }

    const payload: UpdatePatientRequest = {
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
      await updateMutation.mutateAsync(payload);
      onOpenChange(false);
      onSuccess?.();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update patient.";
      setFormError(msg);
    }
  };

  if (!patient) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg border-slate-200 p-0 overflow-hidden">
        <form onSubmit={handleSubmit}>
          <DialogHeader className="p-5 pb-3 border-b border-slate-100 bg-slate-50/50">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-lg bg-teal-50 text-[#0F766E] border border-teal-100">
                <User className="h-5 w-5" />
              </div>
              <div>
                <DialogTitle className="text-base font-bold text-slate-900">
                  Edit Patient Details
                </DialogTitle>
                <DialogDescription className="text-xs text-slate-500 font-mono mt-0.5">
                  {patient.patientCode} · #{patient.refId}
                </DialogDescription>
              </div>
            </div>
          </DialogHeader>

          <div className="p-5 space-y-4 max-h-[70vh] overflow-y-auto">
            {formError && (
              <div className="flex items-start gap-2 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs">
                <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                <div className="flex-1">
                  <span className="font-semibold block">Update Error</span>
                  {formError}
                </div>
                <button
                  type="button"
                  onClick={() => setFormError(null)}
                  className="text-rose-500 hover:text-rose-700"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            )}

            {/* Demographics */}
            <div className="grid grid-cols-12 gap-3">
              <div className="col-span-4">
                <Label className="text-xs font-semibold text-slate-700">
                  Salutation
                </Label>
                <select
                  value={salutation}
                  onChange={(e) =>
                    handleSalutationChange(e.target.value as Salutation)
                  }
                  className="mt-1 w-full h-9 rounded-md border border-slate-200 bg-white px-2.5 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-[#0F766E]"
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

              <div className="col-span-8">
                <Label className="text-xs font-semibold text-slate-700">
                  Full Name <span className="text-rose-500">*</span>
                </Label>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Ramesh Kumar"
                  className="mt-1 h-9 text-xs"
                />
              </div>
            </div>

            {/* Gender & Age */}
            <div className="grid grid-cols-12 gap-3">
              <div className="col-span-5">
                <Label className="text-xs font-semibold text-slate-700">
                  Gender
                </Label>
                <select
                  value={gender}
                  onChange={(e) => setGender(e.target.value as Gender)}
                  className="mt-1 w-full h-9 rounded-md border border-slate-200 bg-white px-2.5 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-[#0F766E]"
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div className="col-span-7">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold text-slate-700">
                    {dobKnown ? "Date of Birth" : "Age"}
                  </Label>
                  <button
                    type="button"
                    onClick={() => setDobKnown(!dobKnown)}
                    className="text-[11px] text-[#0F766E] hover:underline font-medium"
                  >
                    {dobKnown ? "Enter age instead" : "Know exact DOB?"}
                  </button>
                </div>

                {dobKnown ? (
                  <Input
                    type="date"
                    value={dob}
                    onChange={(e) => setDob(e.target.value)}
                    className="mt-1 h-9 text-xs"
                  />
                ) : (
                  <div className="grid grid-cols-2 gap-2 mt-1">
                    <Input
                      type="number"
                      min="0"
                      max="150"
                      value={ageValue}
                      onChange={(e) => setAgeValue(e.target.value)}
                      placeholder="e.g. 54"
                      className="h-9 text-xs"
                    />
                    <select
                      value={ageUnit}
                      onChange={(e) => setAgeUnit(e.target.value as AgeUnit)}
                      className="h-9 rounded-md border border-slate-200 bg-white px-2 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-[#0F766E]"
                    >
                      <option value="YEARS">Years</option>
                      <option value="MONTHS">Months</option>
                      <option value="WEEKS">Weeks</option>
                      <option value="DAYS">Days</option>
                    </select>
                  </div>
                )}
              </div>
            </div>

            {/* Contact */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Phone className="h-3 w-3 text-slate-400" />
                  Phone Number
                </Label>
                <Input
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="e.g. 9876543210"
                  className="mt-1 h-9 text-xs"
                />
              </div>

              <div>
                <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Mail className="h-3 w-3 text-slate-400" />
                  Email Address
                </Label>
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="patient@example.com"
                  className="mt-1 h-9 text-xs"
                />
              </div>
            </div>

            {/* Address & Weight */}
            <div className="grid grid-cols-12 gap-3">
              <div className="col-span-8">
                <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <MapPin className="h-3 w-3 text-slate-400" />
                  Address
                </Label>
                <Input
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Street / City / Locality"
                  className="mt-1 h-9 text-xs"
                />
              </div>

              <div className="col-span-4">
                <Label className="text-xs font-semibold text-slate-700">
                  Weight (kg)
                </Label>
                <Input
                  type="number"
                  step="0.1"
                  value={weightKg}
                  onChange={(e) => setWeightKg(e.target.value)}
                  placeholder="e.g. 68"
                  className="mt-1 h-9 text-xs"
                />
              </div>
            </div>
          </div>

          <DialogFooter className="p-4 border-t border-slate-100 bg-slate-50/50 gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => onOpenChange(false)}
              disabled={updateMutation.isPending}
              className="text-xs h-9 border-slate-200"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={updateMutation.isPending}
              className="text-xs h-9 bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold shadow-xs"
            >
              {updateMutation.isPending ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                  Saving...
                </>
              ) : (
                "Save Changes"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
