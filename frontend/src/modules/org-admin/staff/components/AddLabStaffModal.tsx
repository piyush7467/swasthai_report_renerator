import React, { useState } from "react";
import {
  AlertCircle,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  Mail,
  User,
  UserPlus,
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
import { useCreateLabStaffMutation } from "../hooks/useLabStaff";
import type { LabStaffResponse, LabStaffSummaryResponse } from "../types/labStaffTypes";

interface AddLabStaffModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  summary: LabStaffSummaryResponse | undefined;
  onSuccess?: (newStaff: LabStaffResponse) => void;
}

export function AddLabStaffModal({
  open,
  onOpenChange,
  summary,
  onSuccess,
}: AddLabStaffModalProps) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const createMutation = useCreateLabStaffMutation();

  const resetForm = () => {
    setName("");
    setEmail("");
    setPassword("");
    setConfirmPassword("");
    setShowPassword(false);
    setShowConfirmPassword(false);
    setErrorMsg(null);
  };

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      resetForm();
    }
    onOpenChange(newOpen);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    // Client-side validations
    if (!name.trim()) {
      setErrorMsg("Full name is required.");
      return;
    }
    if (!email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setErrorMsg("A valid email address is required.");
      return;
    }
    if (password.length < 6) {
      setErrorMsg("Password must be at least 6 characters.");
      return;
    }
    if (password !== confirmPassword) {
      setErrorMsg("Passwords do not match.");
      return;
    }

    try {
      const created = await createMutation.mutateAsync({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        password,
        confirmPassword,
      });
      handleOpenChange(false);
      onSuccess?.(created);
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
      const status = error?.response?.status;
      let serverMessage = error?.response?.data?.message;
      if (!serverMessage) {
        if (status === 403) serverMessage = "You do not have permission to add lab staff.";
        else if (status === 409) serverMessage = "Staff limit reached or an account with this email already exists.";
        else if (status === 429) serverMessage = "Too many requests. Please try again later.";
        else if (status && status >= 500) serverMessage = "Something went wrong on the server. Please try again.";
        else serverMessage = error?.message || "Failed to create lab staff account. Please try again.";
      }
      setErrorMsg(serverMessage);
    }
  };

  const isLimitReached = summary?.limitReached ?? false;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <div className="flex items-center gap-2 text-[#0F766E]">
            <div className="p-2 rounded-lg bg-teal-50 border border-teal-100">
              <UserPlus className="h-5 w-5" />
            </div>
            <DialogTitle className="text-xl font-bold text-slate-900">
              Add Lab Staff Account
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500 pt-1">
            Create an operational login for your laboratory staff member. The staff member will be assigned to your organization.
          </DialogDescription>
        </DialogHeader>

        {isLimitReached ? (
          <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-start gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
            <div>
              <p className="font-semibold text-amber-900">No staff seats available.</p>
              <p className="mt-0.5 text-amber-700">
                Deactivate an existing staff member or upgrade your plan.
              </p>
            </div>
          </div>
        ) : (
          <div className="p-2.5 rounded-lg bg-teal-50 border border-teal-200 text-teal-800 text-xs flex items-center justify-between">
            <span className="font-semibold text-[#0F766E]">
              {summary ? `${summary.remainingSlots} staff seat${summary.remainingSlots === 1 ? "" : "s"} available` : "Checking license seats..."}
            </span>
            <span className="text-[11px] text-teal-700 font-medium">
              Plan: {summary?.planName || "Current"}
            </span>
          </div>
        )}

        {errorMsg && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold text-rose-900">Error</p>
              <p className="mt-0.5 text-rose-700">{errorMsg}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 pt-1">
          {/* Full Name */}
          <div className="space-y-1.5">
            <Label htmlFor="staff-name" className="text-xs font-semibold text-slate-700">
              Full Name <span className="text-rose-500">*</span>
            </Label>
            <div className="relative">
              <User className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <Input
                id="staff-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Dr. Rajesh Kumar or Priya Sharma"
                className="pl-9 text-sm"
                disabled={createMutation.isPending || isLimitReached}
                required
              />
            </div>
          </div>

          {/* Email Address */}
          <div className="space-y-1.5">
            <Label htmlFor="staff-email" className="text-xs font-semibold text-slate-700">
              Email Address (Login Username) <span className="text-rose-500">*</span>
            </Label>
            <div className="relative">
              <Mail className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <Input
                id="staff-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="staff@laboratory.com"
                className="pl-9 text-sm"
                disabled={createMutation.isPending || isLimitReached}
                required
              />
            </div>
          </div>

          {/* Password */}
          <div className="space-y-1.5">
            <Label htmlFor="staff-password" className="text-xs font-semibold text-slate-700">
              Initial Password <span className="text-rose-500">*</span>
            </Label>
            <div className="relative">
              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <Input
                id="staff-password"
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Minimum 6 characters"
                className="pl-9 pr-10 text-sm"
                disabled={createMutation.isPending || isLimitReached}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          {/* Confirm Password */}
          <div className="space-y-1.5">
            <Label htmlFor="staff-confirm-password" className="text-xs font-semibold text-slate-700">
              Confirm Password <span className="text-rose-500">*</span>
            </Label>
            <div className="relative">
              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <Input
                id="staff-confirm-password"
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter password"
                className="pl-9 pr-10 text-sm"
                disabled={createMutation.isPending || isLimitReached}
                required
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none"
              >
                {showConfirmPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          <DialogFooter className="pt-3 gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={createMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={createMutation.isPending || isLimitReached}
              className="bg-[#0F766E] hover:bg-[#115E59] text-white font-medium"
            >
              {createMutation.isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Creating Account...
                </>
              ) : (
                "Create Staff Member"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
