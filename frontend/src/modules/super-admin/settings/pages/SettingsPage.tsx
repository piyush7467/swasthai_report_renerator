import { useState, useEffect } from "react";
import {
  Shield,
  Server,
  Clock,
  HardDrive,
  User,
  CheckCircle2,
  Lock,
  Mail,
  Phone,
  Building,
  Loader2,
  Save,
} from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/core/auth/AuthContext";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  platformContactApi,
  DEFAULT_CONTACT_SETTINGS,
  type PlatformContactSetting,
} from "@/core/api/platformContactApi";

export default function SettingsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<"platform" | "security" | "retention" | "contact">("platform");

  // Contact settings state
  const { data: contactData, isLoading: isLoadingContact } = useQuery({
    queryKey: ["admin-contact-settings"],
    queryFn: platformContactApi.getAdminContactInfo,
  });

  const [form, setForm] = useState<PlatformContactSetting>(DEFAULT_CONTACT_SETTINGS);
  const [isSaving, setIsSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  useEffect(() => {
    if (contactData) {
      setForm(contactData);
    }
  }, [contactData]);

  const handleSaveContact = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setSaveMessage(null);
    setSaveError(null);

    try {
      const updated = await platformContactApi.updateAdminContactInfo(form);
      setForm(updated);
      await queryClient.invalidateQueries({ queryKey: ["admin-contact-settings"] });
      await queryClient.invalidateQueries({ queryKey: ["platform-public-contact"] });
      setSaveMessage("Contact information updated successfully across the platform!");
      setTimeout(() => setSaveMessage(null), 4000);
    } catch {
      setSaveError("Failed to update contact settings. Please try again.");
      setTimeout(() => setSaveError(null), 4000);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
          System Settings & Platform Diagnostics
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Global architecture parameters, contact & compliance information, and data retention policies.
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2 overflow-x-auto no-scrollbar">
        <button
          type="button"
          onClick={() => setActiveTab("platform")}
          className={`flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-xs font-semibold whitespace-nowrap transition-colors ${
            activeTab === "platform"
              ? "bg-[#0F766E] text-white shadow-xs"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <Server className="h-3.5 w-3.5" />
          Platform Overview
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("contact")}
          className={`flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-xs font-semibold whitespace-nowrap transition-colors ${
            activeTab === "contact"
              ? "bg-[#0F766E] text-white shadow-xs"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <Mail className="h-3.5 w-3.5" />
          Public Contact & Compliance
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("security")}
          className={`flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-xs font-semibold whitespace-nowrap transition-colors ${
            activeTab === "security"
              ? "bg-[#0F766E] text-white shadow-xs"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <Shield className="h-3.5 w-3.5" />
          Security & Cryptography
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("retention")}
          className={`flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-xs font-semibold whitespace-nowrap transition-colors ${
            activeTab === "retention"
              ? "bg-[#0F766E] text-white shadow-xs"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <HardDrive className="h-3.5 w-3.5" />
          Data Retention & Storage
        </button>
      </div>

      {/* Tab 1: Platform Overview */}
      {activeTab === "platform" && (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white shadow-xs">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <User className="h-4 w-4 text-teal-600" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Super Admin Profile
                  </CardTitle>
                </div>
                <Badge className="bg-[#0F766E] text-white shadow-xs">{user?.role}</Badge>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Current authoritative administrator session parameters.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Full Name</p>
                  <p className="font-semibold text-slate-900 mt-0.5">{user?.name}</p>
                </div>

                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Email Address</p>
                  <p className="font-semibold text-slate-900 mt-0.5 truncate">{user?.email}</p>
                </div>

                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Authorization Scope</p>
                  <p className="font-semibold text-slate-900 mt-0.5">
                    {user?.organizationRefId || "Global / Multi-Tenant"}
                  </p>
                </div>

                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Security Clearance</p>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
                    <span className="text-xs font-semibold text-emerald-700">Root Governance</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab: Public Contact & Compliance */}
      {activeTab === "contact" && (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white shadow-xs">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg font-bold text-slate-900">
                    Public Contact & Legal Information Management
                  </CardTitle>
                  <CardDescription className="text-xs text-slate-500 mt-1">
                    Manage the support emails, WhatsApp number, phone helpline, and address displayed on the public website, Privacy Policy, Terms, and Help Desk pages.
                  </CardDescription>
                </div>
                <Badge variant="outline" className="text-xs border-teal-200 text-teal-800 bg-teal-50">
                  Live Public Sync
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              {isLoadingContact ? (
                <div className="py-12 flex items-center justify-center gap-2 text-slate-500">
                  <Loader2 className="size-5 animate-spin text-teal-600" />
                  <span className="text-sm">Loading contact configuration...</span>
                </div>
              ) : (
                <form onSubmit={handleSaveContact} className="space-y-6">
                  {saveMessage && (
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 flex items-center gap-2">
                      <CheckCircle2 className="size-4 shrink-0 text-emerald-600" />
                      <span>{saveMessage}</span>
                    </div>
                  )}

                  {saveError && (
                    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
                      {saveError}
                    </div>
                  )}

                  <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                    {/* Support Desk Email */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Mail className="size-3.5 text-teal-600" />
                        Support Desk Email
                      </label>
                      <input
                        type="email"
                        required
                        value={form.supportEmail}
                        onChange={(e) => setForm({ ...form, supportEmail: e.target.value })}
                        placeholder="support@swasthai.com"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Displayed in footer, help desk, and error states.</p>
                    </div>

                    {/* Privacy Officer Email */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Shield className="size-3.5 text-teal-600" />
                        Privacy Inquiries Email
                      </label>
                      <input
                        type="email"
                        required
                        value={form.privacyEmail}
                        onChange={(e) => setForm({ ...form, privacyEmail: e.target.value })}
                        placeholder="privacy@swasthai.com"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Target for data deletion requests and DPDP grievances.</p>
                    </div>

                    {/* Legal Email */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Building className="size-3.5 text-teal-600" />
                        Legal Notices Email
                      </label>
                      <input
                        type="email"
                        required
                        value={form.legalEmail}
                        onChange={(e) => setForm({ ...form, legalEmail: e.target.value })}
                        placeholder="legal@swasthai.com"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Referenced in Terms & Conditions agreement.</p>
                    </div>

                    {/* Helpline Phone */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Phone className="size-3.5 text-teal-600" />
                        Helpline Phone Number
                      </label>
                      <input
                        type="text"
                        required
                        value={form.helplinePhone}
                        onChange={(e) => setForm({ ...form, helplinePhone: e.target.value })}
                        placeholder="+91 99999 99999"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Official customer telephone helpline.</p>
                    </div>

                    {/* WhatsApp Number */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Phone className="size-3.5 text-emerald-600" />
                        WhatsApp Support Number
                      </label>
                      <input
                        type="text"
                        required
                        value={form.whatsappNumber}
                        onChange={(e) => setForm({ ...form, whatsappNumber: e.target.value })}
                        placeholder="919999999999 (country code + number without plus)"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Format: Country code followed by number (e.g. 919999999999 for wa.me links).</p>
                    </div>

                    {/* Operating Hours */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Clock className="size-3.5 text-teal-600" />
                        Support Operating Hours
                      </label>
                      <input
                        type="text"
                        required
                        value={form.operatingHours}
                        onChange={(e) => setForm({ ...form, operatingHours: e.target.value })}
                        placeholder="Monday to Saturday, 9:00 AM – 6:00 PM IST"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Business working hours displayed on Help and Footer.</p>
                    </div>

                    {/* Office Address */}
                    <div className="space-y-1.5 md:col-span-2">
                      <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                        <Building className="size-3.5 text-teal-600" />
                        Registered Entity / Office Address
                      </label>
                      <textarea
                        rows={2}
                        required
                        value={form.officeAddress}
                        onChange={(e) => setForm({ ...form, officeAddress: e.target.value })}
                        placeholder="SwasthAI Healthcare Technologies Private Limited"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                      />
                      <p className="text-[11px] text-slate-500">Entity name and registered physical office.</p>
                    </div>
                  </div>

                  <div className="flex justify-end pt-2">
                    <Button
                      type="submit"
                      disabled={isSaving}
                      className="bg-teal-600 hover:bg-teal-700 text-white rounded-xl h-11 px-6 font-semibold"
                    >
                      {isSaving ? (
                        <>
                          <Loader2 className="size-4 animate-spin mr-2" />
                          Saving Changes...
                        </>
                      ) : (
                        <>
                          <Save className="size-4 mr-2" />
                          Save Contact Settings
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 2: Security & Cryptography */}
      {activeTab === "security" && (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <Lock className="h-4 w-4 text-indigo-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Break-Glass Emergency Access Protocol
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Audited emergency pathway allowing Super Admins to inspect tenant clinical reports.
              </CardDescription>
            </CardHeader>
            <CardContent className="text-xs space-y-3 text-slate-600">
              <p className="leading-relaxed">
                By default, Super Admins have zero clinical report viewing permissions to maintain strict HIPAA/tenant privacy.
                When a life-critical or legal incident occurs, the Break-Glass protocol permits emergency access under three mandatory constraints:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-slate-700">
                <li><strong>Target Organization Verification:</strong> The Super Admin must explicitly specify the tenant reference ID.</li>
                <li><strong>Mandatory Clinical Justification:</strong> A permanent description (min. 10 chars) must be recorded before access is granted.</li>
                <li><strong>Immutable Security Log:</strong> Every access attempt writes an append-only entry in <code className="text-indigo-700 font-mono">security_audit_logs</code> recording actor email, timestamp, and client IP address.</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 3: Data Retention & Storage */}
      {activeTab === "retention" && (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-amber-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Automated Clinical Report Retention Policy
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Lifecycle progression from clinical finalization to soft-delete and automated permanent purge.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-xs">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Soft-Delete Threshold</p>
                  <p className="font-bold text-slate-800 mt-1">5 Days</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Eligible for batch deletion</p>
                </div>
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Permanent Purge Cutoff</p>
                  <p className="font-bold text-rose-700 mt-1">10 Days</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Automated hard deletion</p>
                </div>
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Purge Scheduler Cron</p>
                  <p className="font-mono font-bold text-slate-800 mt-1">0 0 * * * *</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Hourly execution</p>
                </div>
              </div>

              <Separator />

              <div className="space-y-2">
                <h4 className="font-semibold text-slate-900 text-xs">Digital Asset Storage Specifications</h4>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 mt-2">
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Storage Architecture</span>
                    <p className="font-medium text-slate-800 mt-0.5">Partitioned Local Disk Storage Service</p>
                  </div>
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Max Image/Signature Upload</span>
                    <p className="font-medium text-slate-800 mt-0.5">5 MB (5,242,880 bytes)</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
