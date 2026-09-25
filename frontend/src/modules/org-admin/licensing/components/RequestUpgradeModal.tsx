import { useState, useEffect } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import {
  Sparkles,
  Users,
  FileText,
  ShieldAlert,
  Loader2,
  CheckCircle2,
  Phone,
  Mail,
  User,
  Info,
} from "lucide-react";
import { useAuth } from "@/core/auth/AuthContext";
import {
  useAvailablePlans,
  useCreateUpgradeRequest,
} from "../hooks/useOrgLicense";
import type { AvailablePlanResponse } from "../types/licenseTypes";

interface RequestUpgradeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function RequestUpgradeModal({
  isOpen,
  onClose,
  onSuccess,
}: RequestUpgradeModalProps) {
  const { user } = useAuth();
  const { data: plans, isLoading: isLoadingPlans } = useAvailablePlans();
  const createMutation = useCreateUpgradeRequest();

  const [selectedPlanRefId, setSelectedPlanRefId] = useState<string>("");
  const [contactName, setContactName] = useState<string>("");
  const [contactEmail, setContactEmail] = useState<string>("");
  const [contactPhone, setContactPhone] = useState<string>("");
  const [reason, setReason] = useState<string>("");
  const [additionalMessage, setAdditionalMessage] = useState<string>("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Initialize form when opening
  useEffect(() => {
    if (isOpen) {
      if (user) {
        setContactName(user.name || "");
        setContactEmail(user.email || "");
      }
      setContactPhone("");
      setReason("");
      setAdditionalMessage("");
      setErrorMessage(null);

      // Pre-select first upgrade plan if available
      if (plans && plans.length > 0) {
        const upgradePlan = plans.find((p) => p.upgrade);
        if (upgradePlan) {
          setSelectedPlanRefId(upgradePlan.refId);
        } else {
          const firstNonCurrent = plans.find((p) => !p.currentPlan);
          if (firstNonCurrent) {
            setSelectedPlanRefId(firstNonCurrent.refId);
          }
        }
      }
    }
  }, [isOpen, user, plans]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!selectedPlanRefId) {
      setErrorMessage("Please select a target plan to upgrade to.");
      return;
    }

    if (!contactName.trim()) {
      setErrorMessage("Contact name is required.");
      return;
    }

    if (!contactEmail.trim()) {
      setErrorMessage("Contact email is required.");
      return;
    }

    try {
      await createMutation.mutateAsync({
        requestedPlanRefId: selectedPlanRefId,
        contactName: contactName.trim(),
        contactEmail: contactEmail.trim(),
        contactPhone: contactPhone.trim() || undefined,
        reason: reason.trim() || undefined,
        additionalMessage: additionalMessage.trim() || undefined,
      });

      onSuccess?.();
      onClose();
    } catch (err: unknown) {
      const errorObj = err as {
        response?: {
          status?: number;
          data?: { message?: string };
        };
        message?: string;
      };

      if (errorObj?.response?.status === 409) {
        setErrorMessage(
          errorObj.response.data?.message ||
            "Your organization already has an active pending upgrade request for this plan."
        );
      } else if (errorObj?.response?.data?.message) {
        setErrorMessage(errorObj.response.data.message);
      } else {
        setErrorMessage("Failed to submit upgrade request. Please check your network and try again.");
      }
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[560px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2.5 text-[#0F766E]">
            <div className="p-2 rounded-lg bg-teal-50 border border-teal-100">
              <Sparkles className="h-5 w-5 text-[#0F766E]" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold text-slate-900">
                Request Subscription Plan Upgrade
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 pt-0.5">
                Submit an upgrade request to increase your staff capacity and features.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {errorMessage && (
          <div className="flex items-start gap-2.5 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs">
            <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          {/* Plan Selection Cards */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-700 block">
              Select Desired Plan:
            </label>

            {isLoadingPlans ? (
              <div className="py-6 flex items-center justify-center text-xs text-slate-400 gap-2">
                <Loader2 className="h-4 w-4 animate-spin text-teal-600" />
                <span>Loading available plans...</span>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {plans?.map((plan: AvailablePlanResponse) => {
                  const isCurrent = plan.currentPlan;
                  const isSelected = selectedPlanRefId === plan.refId;

                  return (
                    <button
                      key={plan.refId}
                      type="button"
                      disabled={isCurrent}
                      onClick={() => setSelectedPlanRefId(plan.refId)}
                      className={`text-left p-3 rounded-xl border transition-all ${
                        isCurrent
                          ? "bg-slate-50 border-slate-200 opacity-60 cursor-not-allowed"
                          : isSelected
                          ? "bg-teal-50/60 border-[#0F766E] ring-1 ring-[#0F766E] shadow-2xs"
                          : "bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50/50 cursor-pointer"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-sm text-slate-900">
                          {plan.name}
                        </span>
                        {isCurrent ? (
                          <Badge
                            variant="outline"
                            className="bg-slate-100 text-slate-600 text-[10px] px-1.5 py-0"
                          >
                            Current
                          </Badge>
                        ) : plan.upgrade ? (
                          <Badge
                            variant="outline"
                            className="bg-teal-50 text-[#0F766E] border-teal-200 text-[10px] px-1.5 py-0"
                          >
                            Upgrade
                          </Badge>
                        ) : null}
                      </div>

                      <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-600">
                        <Users className="h-3.5 w-3.5 text-slate-400" />
                        <span>Up to {plan.maxLabStaff} staff seats</span>
                      </div>

                      <div className="mt-0.5 flex items-center gap-1.5 text-[11px] text-slate-500">
                        <FileText className="h-3 w-3 text-slate-400" />
                        <span>
                          {plan.maxReportsPerMonth && plan.maxReportsPerMonth > 0
                            ? `${plan.maxReportsPerMonth.toLocaleString()} reports / mo`
                            : "Unlimited reports"}
                        </span>
                      </div>

                      {plan.annualPrice > 0 ? (
                        <div className="mt-2 text-xs font-semibold text-slate-800">
                          ₹{plan.annualPrice.toLocaleString("en-IN")}{" "}
                          <span className="text-[10px] font-normal text-slate-500">
                            / year
                          </span>
                        </div>
                      ) : (
                        <div className="mt-2 text-xs font-semibold text-teal-800">
                          Custom pricing
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Offline Process Notice */}
          <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start gap-2">
            <Info className="h-4 w-4 shrink-0 text-slate-500 mt-0.5" />
            <span>
              Upon submission, a SwasthAI platform representative will review your organization's request, confirm details, and coordinate license activation.
            </span>
          </div>

          {/* Contact Details */}
          <div className="space-y-3 pt-1 border-t border-slate-100">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-medium text-slate-700">
                  Contact Person *
                </label>
                <div className="relative">
                  <User className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
                  <Input
                    required
                    value={contactName}
                    onChange={(e) => setContactName(e.target.value)}
                    placeholder="Full Name"
                    className="pl-8 text-xs h-8"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-medium text-slate-700">
                  Email Address *
                </label>
                <div className="relative">
                  <Mail className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
                  <Input
                    type="email"
                    required
                    value={contactEmail}
                    onChange={(e) => setContactEmail(e.target.value)}
                    placeholder="admin@laboratory.com"
                    className="pl-8 text-xs h-8"
                  />
                </div>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-medium text-slate-700">
                Contact Phone / WhatsApp
              </label>
              <div className="relative">
                <Phone className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
                <Input
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                  className="pl-8 text-xs h-8"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-medium text-slate-700">
                Reason for Upgrade
              </label>
              <Input
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="e.g. Onboarding 5 additional lab technicians for our new branch"
                className="text-xs h-8"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-medium text-slate-700">
                Additional Notes / Questions
              </label>
              <Textarea
                rows={2}
                value={additionalMessage}
                onChange={(e) => setAdditionalMessage(e.target.value)}
                placeholder="Any specific invoice details or questions..."
                className="text-xs resize-none"
              />
            </div>
          </div>

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={createMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={createMutation.isPending || !selectedPlanRefId}
              className="bg-[#0F766E] hover:bg-[#0d655e] text-white shadow-2xs gap-1.5 cursor-pointer"
            >
              {createMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Submitting Request...
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-4 w-4" />
                  Submit Upgrade Request
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
