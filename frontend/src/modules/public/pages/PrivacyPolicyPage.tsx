import { Shield, Lock, CheckCircle2, Building2 } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";

export default function PrivacyPolicyPage() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1">
        {/* Hero Header */}
        <section className="border-b border-slate-200 bg-white py-12 px-4 sm:px-6">
          <div className="mx-auto max-w-4xl text-center space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-teal-50 border border-teal-200 px-3 py-1 text-xs font-semibold text-teal-700">
              <Shield className="size-3.5" />
              <span>Healthcare Data Protection Compliant</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
              Privacy Policy
            </h1>
            <p className="text-sm text-slate-500 max-w-2xl mx-auto">
              Effective Date: November 2026 • Compliant with the Digital Personal Data Protection (DPDP) Act & Global Health Data Standards
            </p>
          </div>
        </section>

        {/* Content Container */}
        <section className="py-10 px-4 sm:px-6">
          <div className="mx-auto max-w-4xl space-y-8">
            
            {/* Quick Summary Card */}
            <div className="rounded-2xl border border-teal-200 bg-teal-50/60 p-6 shadow-xs">
              <h2 className="text-sm font-bold uppercase tracking-wider text-teal-900 flex items-center gap-2 mb-2">
                <CheckCircle2 className="size-4 text-teal-700" />
                Commitment to Patient Health Confidentiality
              </h2>
              <p className="text-xs text-teal-950 leading-relaxed">
                SwasthAI Lab provides diagnostic data management and report generation tools strictly for licensed laboratories, hospitals, and clinics. <strong>We do not sell, monetize, or disclose patient medical records to third-party advertisers or commercial data brokers under any circumstances.</strong>
              </p>
            </div>

            {/* 1. Introduction */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  1
                </span>
                Introduction & Scope
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                SwasthAI Lab (&quot;Platform&quot;, &quot;we&quot;, &quot;our&quot;) is operated by SwasthAI Healthcare Technologies. This Privacy Policy details how we collect, process, store, and safeguard personal demographic and clinical diagnostic information obtained when healthcare providers and patients use our mobile application and web portals.
              </p>
            </div>

            {/* 2. Information Collected */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-4">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  2
                </span>
                Information We Collect
              </h2>
              <div className="grid gap-4 sm:grid-cols-2 pt-2">
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 space-y-1.5">
                  <h3 className="text-xs font-bold text-slate-900 uppercase">Patient Data</h3>
                  <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
                    <li>Full Name, Age, Gender</li>
                    <li>Contact Number & Unique Patient ID (UHID)</li>
                    <li>Referring Clinician / Doctor details</li>
                  </ul>
                </div>
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 space-y-1.5">
                  <h3 className="text-xs font-bold text-slate-900 uppercase">Clinical & Test Data</h3>
                  <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
                    <li>Test names (CBC, LFT, KFT, Lipid Profile, etc.)</li>
                    <li>Measured bio-chemical values & unit metrics</li>
                    <li>Clinical observations and pathological notes</li>
                  </ul>
                </div>
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 space-y-1.5">
                  <h3 className="text-xs font-bold text-slate-900 uppercase">Laboratory Staff Data</h3>
                  <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
                    <li>Staff email, full name, clinical designation</li>
                    <li>Digital signature image for report certification</li>
                    <li>Authentication logs & station activity records</li>
                  </ul>
                </div>
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 space-y-1.5">
                  <h3 className="text-xs font-bold text-slate-900 uppercase">Diagnostic Documents</h3>
                  <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
                    <li>Generated PDF reports</li>
                    <li>QR verification tokens for report authenticity</li>
                    <li>Laboratory letterhead and branding assets</li>
                  </ul>
                </div>
              </div>
            </div>

            {/* 3. Purpose of Processing */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  3
                </span>
                How We Use Health Data
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                Collected diagnostic and personal data is processed solely for lawful healthcare operations:
              </p>
              <ul className="text-xs text-slate-600 space-y-2 list-disc list-inside pt-1">
                <li>Generating certified, standardized pathology and diagnostic reports.</li>
                <li>Affixing approved digital signatures and institutional letterheads.</li>
                <li>Facilitating secure report sharing and QR-code report verification for patients and doctors.</li>
                <li>Maintaining institutional audit logs to track report creation, edits, and finalization.</li>
                <li>Ensuring role-based security access within diagnostic laboratory workstations.</li>
              </ul>
            </div>

            {/* 4. Security & Protection */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  4
                </span>
                Data Security & Storage Safeguards
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                We implement robust security controls across mobile and cloud infrastructure:
              </p>
              <div className="grid gap-3 sm:grid-cols-3 pt-2">
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 text-center space-y-1">
                  <Lock className="size-5 text-teal-600 mx-auto" />
                  <p className="text-xs font-bold text-slate-900">TLS 1.3 Encryption</p>
                  <p className="text-[11px] text-slate-500">All data in transit is encrypted using 256-bit SSL protocols.</p>
                </div>
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 text-center space-y-1">
                  <Building2 className="size-5 text-teal-600 mx-auto" />
                  <p className="text-xs font-bold text-slate-900">Multi-Tenant Isolation</p>
                  <p className="text-[11px] text-slate-500">Each diagnostic clinic's records are strictly isolated at database level.</p>
                </div>
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 text-center space-y-1">
                  <Shield className="size-5 text-teal-600 mx-auto" />
                  <p className="text-xs font-bold text-slate-900">JWT Token Auth</p>
                  <p className="text-[11px] text-slate-500">Stateless cryptographic tokens prevent session spoofing.</p>
                </div>
              </div>
            </div>

            {/* 5. User Rights & Data Deletion */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  5
                </span>
                Patient Rights & Account/Data Deletion
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                Under the DPDP Act and mobile app store policies (Google Play & Indus Appstore), laboratories and patients have the right to inspect, correct, and request the complete purging of their records:
              </p>
              <ul className="text-xs text-slate-600 space-y-2 list-disc list-inside pt-1">
                <li><strong>Patient Profile Removal:</strong> Laboratory administrators can delete individual patient records directly from the station.</li>
                <li><strong>Account & Organization Purge:</strong> You may submit an online deletion request at any time through our dedicated portal (<a href="/data-deletion" className="text-teal-600 underline font-medium">Data Deletion Request Page</a>) or by emailing <span className="font-mono text-teal-700">privacy@swasthai.com</span>.</li>
                <li><strong>Retention Standards:</strong> In compliance with medical regulatory guidelines, verified diagnostic reports may be retained for the minimum statutory period required by local health authorities before automated purging.</li>
              </ul>
            </div>

            {/* 6. Contact Us */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  6
                </span>
                Grievance Officer & Inquiries
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                For questions, clarifications, or data protection concerns, contact our designated privacy officer:
              </p>
              <div className="rounded-xl border border-slate-100 bg-slate-50 p-4 text-xs space-y-1 text-slate-700">
                <p><strong>SwasthAI Healthcare Technologies Private Limited</strong></p>
                <p>Attention: Data Protection & Compliance Officer</p>
                <p>Email: <a href="mailto:privacy@swasthai.com" className="text-teal-600 font-medium">privacy@swasthai.com</a> | <a href="mailto:support@swasthai.com" className="text-teal-600 font-medium">support@swasthai.com</a></p>
                <p>Response Time: All privacy requests are acknowledged within 48 business hours.</p>
              </div>
            </div>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}

