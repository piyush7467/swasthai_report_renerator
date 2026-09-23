import { useState } from "react";
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
import { useCreatePatientMutation } from "../hooks/usePatients";
import type {
  AgeUnit,
  CreatePatientRequest,
  Gender,
  PatientResponse,
  Salutation,
} from "../types/patientTypes";

interface RegisterPatientModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (created: PatientResponse) => void;
}

export function RegisterPatientModal({
  open,
  onOpenChange,
  onSuccess,
}: RegisterPatientModalProps) {
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

  const createMutation = useCreatePatientMutation();

  const handleSalutationChange = (newSalutation: Salutation) => {
    setSalutation(newSalutation);
    if (newSalutation === "MR" || newSalutation === "MASTER") {
      setGender("MALE");
    } else if (newSalutation === "MRS" || newSalutation === "MS") {
      setGender("FEMALE");
    }
  };

  const resetForm = () => {
    setSalutation("MR");
    setName("");
    setGender("MALE");
    setDobKnown(false);
    setDob("");
    setAgeValue("");
    setAgeUnit("YEARS");
    setPhone("");
    setEmail("");
    setAddress("");
    setWeightKg("");
    setFormError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!name.trim()) {
      setFormError("Patient full name is required.");
      return;
    }

    if (!dobKnown && (!ageValue || Number(ageValue) <= 0)) {
      setFormError("Valid age value is required (e.g. 35).");
      return;
    }

    if (dobKnown && !dob) {
      setFormError("Date of birth is required when DOB toggle is selected.");
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
      const created = await createMutation.mutateAsync(payload);
      resetForm();
      onOpenChange(false);
      if (onSuccess) {
        onSuccess(created);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to register patient.";
      setFormError(msg);
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) resetForm();
        onOpenChange(nextOpen);
      }}
    >
      <DialogContent className="sm:max-w-lg border-slate-200 p-0 overflow-hidden max-h-[90vh] flex flex-col">
        <DialogHeader className="p-5 pb-3 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-teal-50 text-[#0F766E] border border-teal-100">
              <User className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Register New Patient
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Create a permanent clinical record for diagnostic visits.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 space-y-4">
          {formError && (
            <div className="flex items-center justify-between p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
                <span>{formError}</span>
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

          {/* Salutation & Full Name */}
          <div className="grid grid-cols-12 gap-3">
            <div className="col-span-4 sm:col-span-3">
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Salutation
              </Label>
              <select
                value={salutation}
                onChange={(e) => handleSalutationChange(e.target.value as Salutation)}
                className="w-full h-9 rounded-md border border-slate-300 bg-white px-2.5 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#0F766E]"
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

            <div className="col-span-8 sm:col-span-9">
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Full Name <span className="text-rose-500">*</span>
              </Label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Ramesh Kumar"
                required
                className="h-9 text-xs"
              />
            </div>
          </div>

          {/* Gender & Age / DOB */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Gender <span className="text-rose-500">*</span>
              </Label>
              <div className="grid grid-cols-3 gap-1.5">
                {(["MALE", "FEMALE", "OTHER"] as Gender[]).map((g) => (
                  <button
                    key={g}
                    type="button"
                    onClick={() => setGender(g)}
                    className={[
                      "h-9 rounded-md text-xs font-medium border transition-colors cursor-pointer capitalize",
                      gender === g
                        ? "bg-[#0F766E] text-white border-[#0F766E]"
                        : "bg-white text-slate-700 border-slate-200 hover:bg-slate-50",
                    ].join(" ")}
                  >
                    {g.toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <Label className="text-xs font-semibold text-slate-700">
                  {dobKnown ? "Date of Birth" : "Age"} <span className="text-rose-500">*</span>
                </Label>
                <button
                  type="button"
                  onClick={() => setDobKnown(!dobKnown)}
                  className="text-[11px] text-[#0F766E] hover:underline font-medium cursor-pointer"
                >
                  {dobKnown ? "Switch to Age" : "Switch to DOB"}
                </button>
              </div>

              {dobKnown ? (
                <Input
                  type="date"
                  value={dob}
                  onChange={(e) => setDob(e.target.value)}
                  className="h-9 text-xs"
                />
              ) : (
                <div className="flex gap-2">
                  <Input
                    type="number"
                    min="0"
                    max="150"
                    value={ageValue}
                    onChange={(e) => setAgeValue(e.target.value)}
                    placeholder="35"
                    className="h-9 text-xs flex-1"
                  />
                  <select
                    value={ageUnit}
                    onChange={(e) => setAgeUnit(e.target.value as AgeUnit)}
                    className="h-9 rounded-md border border-slate-300 bg-white px-2 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#0F766E]"
                  >
                    <option value="YEARS">Years</option>
                    <option value="MONTHS">Months</option>
                    <option value="DAYS">Days</option>
                  </select>
                </div>
              )}
            </div>
          </div>

          {/* Contact Details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Phone Number
              </Label>
              <div className="relative">
                <Phone className="h-3.5 w-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                <Input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="e.g. 9876543210"
                  className="h-9 text-xs pl-8 font-mono"
                />
              </div>
            </div>

            <div>
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Email Address
              </Label>
              <div className="relative">
                <Mail className="h-3.5 w-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="patient@example.com"
                  className="h-9 text-xs pl-8"
                />
              </div>
            </div>
          </div>

          {/* Address & Weight */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="sm:col-span-2">
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Residential Address
              </Label>
              <div className="relative">
                <MapPin className="h-3.5 w-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                <Input
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="e.g. 12/B Park Street, Kolkata"
                  className="h-9 text-xs pl-8"
                />
              </div>
            </div>

            <div>
              <Label className="text-xs font-semibold text-slate-700 mb-1.5 block">
                Weight (kg)
              </Label>
              <Input
                type="number"
                step="0.1"
                min="0"
                max="400"
                value={weightKg}
                onChange={(e) => setWeightKg(e.target.value)}
                placeholder="68.5"
                className="h-9 text-xs"
              />
            </div>
          </div>

          <DialogFooter className="p-0 pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => onOpenChange(false)}
              className="text-xs h-9 px-4"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={createMutation.isPending}
              className="text-xs h-9 px-5 bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold cursor-pointer"
            >
              {createMutation.isPending ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />
                  Registering...
                </>
              ) : (
                "Register Patient"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
