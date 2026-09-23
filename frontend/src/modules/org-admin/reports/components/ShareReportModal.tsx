import { useState } from "react";
import {
  Check,
  Copy,
  Globe,
  Loader2,
  Mail,
  MessageSquare,
  Share2,
  ShieldCheck,
  X,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateReportShareMutation } from "../hooks/useReports";
import type { ReportResponse } from "../types/reportTypes";

interface ShareReportModalProps {
  report: ReportResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ShareReportModal({
  report,
  open,
  onOpenChange,
}: ShareReportModalProps) {
  const [copied, setCopied] = useState(false);
  const [generatedShareUrl, setGeneratedShareUrl] = useState<string | null>(null);
  const [shareError, setShareError] = useState<string | null>(null);

  const shareMutation = useCreateReportShareMutation(report?.refId || "");

  const getFullShareUrl = (relativeUrl: string) => {
    return `${window.location.origin}${relativeUrl}`;
  };

  const handleGenerateLink = async (channel: "WHATSAPP" | "EMAIL" | "LINK" | "SYSTEM") => {
    if (!report) return null;
    setShareError(null);

    try {
      const response = await shareMutation.mutateAsync({
        channel,
        recipient: null,
        expiresInDays: 7,
      });

      const fullUrl = getFullShareUrl(response.shareUrl);
      setGeneratedShareUrl(fullUrl);
      return fullUrl;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to generate secure share link.";
      setShareError(msg);
      return null;
    }
  };

  const handleCopyLink = async () => {
    let url = generatedShareUrl;
    if (!url) {
      url = await handleGenerateLink("LINK");
    }
    if (url) {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const handleWhatsAppShare = async () => {
    let url = generatedShareUrl;
    if (!url) {
      url = await handleGenerateLink("WHATSAPP");
    }
    if (!url || !report) return;

    const orgName = report.organizationName || "Diagnostic Centre";
    const patientName = report.patientName ? `${report.patientSalutation ? report.patientSalutation + ". " : ""}${report.patientName}` : "Patient";
    const message = `Hello ${patientName},\n\nYour diagnostic report from ${orgName} is ready.\n\nReport Reference: #${report.refId}\n\nAccess your secure verified report here:\n${url}\n\nThis link is confidential and verified by SwasthAI.`;

    const phoneClean = report.patientPhone ? report.patientPhone.replace(/\D/g, "") : "";
    const waUrl = phoneClean
      ? `https://wa.me/${phoneClean}?text=${encodeURIComponent(message)}`
      : `https://wa.me/?text=${encodeURIComponent(message)}`;

    window.open(waUrl, "_blank", "noopener,noreferrer");
  };

  const handleEmailShare = async () => {
    let url = generatedShareUrl;
    if (!url) {
      url = await handleGenerateLink("EMAIL");
    }
    if (!url || !report) return;

    const patientName = report.patientName ? `${report.patientSalutation ? report.patientSalutation + ". " : ""}${report.patientName}` : "Patient";
    const subject = `Diagnostic Report - ${patientName} (#${report.refId})`;
    const body = `Dear ${patientName},\n\nYour diagnostic test report is ready for viewing.\n\nReport Reference: #${report.refId}\n\nYou can securely view and download your verified report using the link below:\n${url}\n\nRegards,\n${report.organizationName || "Clinical Diagnostic Team"}`;

    const to = "";
    window.location.href = `mailto:${to}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  };

  const handleNativeShare = async () => {
    let url = generatedShareUrl;
    if (!url) {
      url = await handleGenerateLink("SYSTEM");
    }
    if (!url || !report) return;

    if (navigator.share) {
      try {
        await navigator.share({
          title: `Diagnostic Report #${report.refId}`,
          text: `Your diagnostic report from ${report.organizationName || "Diagnostic Centre"} is ready.`,
          url,
        });
      } catch {
        // User cancelled or share failed
      }
    } else {
      handleCopyLink();
    }
  };

  if (!report) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-slate-200 p-0 overflow-hidden">
        <DialogHeader className="p-5 pb-3 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-teal-50 text-[#0F766E] border border-teal-100">
              <Share2 className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Share Diagnostic Report
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono mt-0.5">
                Report #{report.refId} · {report.patientName || report.patientRefId}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="p-5 space-y-4">
          {shareError && (
            <div className="flex items-center justify-between p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs">
              <span>{shareError}</span>
              <button
                type="button"
                onClick={() => setShareError(null)}
                className="text-rose-500 hover:text-rose-700"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          )}

          {/* Security Notice */}
          <div className="rounded-lg border border-teal-100 bg-teal-50/60 p-3 flex items-start gap-2.5 text-xs text-teal-900">
            <ShieldCheck className="h-4 w-4 text-[#0F766E] shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold block text-[#0F766E]">Secure Access Token</span>
              <p className="mt-0.5 text-slate-600 text-[11px] leading-relaxed">
                Generates a cryptographically random, revocable link with a 7-day expiration. Never exposes internal database IDs.
              </p>
            </div>
          </div>

          {/* Quick Sharing Channels */}
          <div className="grid grid-cols-2 gap-2.5">
            <button
              type="button"
              onClick={handleWhatsAppShare}
              disabled={shareMutation.isPending}
              className="flex items-center justify-center gap-2 p-3 rounded-lg border border-slate-200 bg-white hover:bg-emerald-50/60 hover:border-emerald-300 text-slate-800 hover:text-emerald-800 text-xs font-semibold transition-colors shadow-2xs group cursor-pointer"
            >
              <MessageSquare className="h-4 w-4 text-emerald-600 group-hover:scale-110 transition-transform" />
              WhatsApp
            </button>

            <button
              type="button"
              onClick={handleEmailShare}
              disabled={shareMutation.isPending}
              className="flex items-center justify-center gap-2 p-3 rounded-lg border border-slate-200 bg-white hover:bg-blue-50/60 hover:border-blue-300 text-slate-800 hover:text-blue-800 text-xs font-semibold transition-colors shadow-2xs group cursor-pointer"
            >
              <Mail className="h-4 w-4 text-blue-600 group-hover:scale-110 transition-transform" />
              Email
            </button>
          </div>

          {/* Native Web Share for Mobile */}
          {typeof navigator !== "undefined" && "share" in navigator && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleNativeShare}
              disabled={shareMutation.isPending}
              className="w-full text-xs h-9 border-slate-200 text-slate-700 hover:bg-slate-50 gap-2 cursor-pointer"
            >
              <Share2 className="h-3.5 w-3.5 text-slate-500" />
              Share via System / Apps
            </Button>
          )}

          {/* Copy Secure Link Box */}
          <div className="space-y-1.5 pt-1">
            <Label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
              <span>Secure Patient Link</span>
              {generatedShareUrl && (
                <a
                  href={generatedShareUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-[11px] text-[#0F766E] hover:underline flex items-center gap-1 font-medium"
                >
                  <Globe className="h-3 w-3" />
                  Preview Viewer
                </a>
              )}
            </Label>
            <div className="flex gap-2">
              <Input
                readOnly
                value={generatedShareUrl || "Click Copy to generate secure token..."}
                className="h-9 text-xs font-mono bg-slate-50 text-slate-600"
              />
              <Button
                type="button"
                onClick={handleCopyLink}
                disabled={shareMutation.isPending}
                className="h-9 px-3 text-xs bg-[#0F766E] hover:bg-[#115E59] text-white shrink-0 font-semibold gap-1.5 cursor-pointer"
              >
                {shareMutation.isPending ? (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                ) : copied ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-emerald-300" />
                    Copied
                  </>
                ) : (
                  <>
                    <Copy className="h-3.5 w-3.5" />
                    Copy
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
