import { AlertTriangle, Scale } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";

export default function TermsConditionsPage() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1">
        {/* Hero Header */}
        <section className="border-b border-slate-200 bg-white py-12 px-4 sm:px-6">
          <div className="mx-auto max-w-4xl text-center space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-teal-50 border border-teal-200 px-3 py-1 text-xs font-semibold text-teal-700">
              <Scale className="size-3.5" />
              <span>Diagnostic Software Agreement</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
              Terms & Conditions
            </h1>
            <p className="text-sm text-slate-500 max-w-2xl mx-auto">
              Please review these terms before deploying or utilizing the SwasthAI Lab diagnostic workstation and reporting platform.
            </p>
          </div>
        </section>

        {/* Content Container */}
        <section className="py-10 px-4 sm:px-6">
          <div className="mx-auto max-w-4xl space-y-8">
            
            {/* Prominent Medical Disclaimer Callout */}
            <div className="rounded-2xl border-2 border-amber-300 bg-amber-50/90 p-6 sm:p-8 shadow-xs space-y-3">
              <div className="flex items-center gap-3 text-amber-900">
                <AlertTriangle className="size-6 text-amber-600 shrink-0" />
                <h2 className="text-base sm:text-lg font-bold uppercase tracking-wide">
                  Crucial Medical Disclaimer & Clinical Responsibility
                </h2>
              </div>
              <p className="text-xs sm:text-sm text-amber-950 leading-relaxed">
                SwasthAI Lab is an automated diagnostic laboratory software solution developed exclusively for accredited clinics, diagnostic centers, and certified healthcare practitioners.
              </p>
              <div className="rounded-xl bg-white/90 p-4 border border-amber-200 text-xs text-amber-900 space-y-2">
                <p>
                  <strong>• Non-Diagnostic Nature of Software:</strong> SwasthAI does NOT offer independent medical advice, clinical diagnoses, or clinical prescriptions.
                </p>
                <p>
                  <strong>• Mandatory Human Review:</strong> All automated calculations, reference range comparisons, and flagged abnormalities MUST be reviewed, confirmed, and digitally signed by a licensed Pathologist or certified laboratory director prior to report issuance.
                </p>
                <p>
                  <strong>• Critical Action:</strong> Patients receiving diagnostic reports must consult a licensed physician or medical specialist for clinical interpretation and treatment planning.
                </p>
              </div>
            </div>

            {/* 1. Acceptance */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  1
                </span>
                Acceptance of Agreement
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                By creating an account, installing the SwasthAI Lab Android application, or logging into the laboratory workstation portal, you acknowledge that you are an authorized representative of your diagnostic facility and agree to adhere strictly to these terms.
              </p>
            </div>

            {/* 2. Authorized Use */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  2
                </span>
                Authorized Personnel & Station Security
              </h2>
              <ul className="text-xs sm:text-sm text-slate-600 space-y-2 list-disc list-inside">
                <li>Access is restricted to certified laboratory technicians, pathologists, and administrative personnel.</li>
                <li>Each user is strictly responsible for safeguarding their login credentials and session tokens.</li>
                <li>Credential sharing across unauthorized individuals or third-party entities is strictly prohibited.</li>
                <li>Laboratories are required to immediately deactivate station access for departed staff.</li>
              </ul>
            </div>

            {/* 3. Accuracy of Test Data */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  3
                </span>
                Laboratory Responsibility for Test Metrics
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                The operating laboratory maintains sole responsibility for sample collection, equipment calibration, and numerical metric input. Automated flags provided by SwasthAI are assistive reference points based on standard clinical catalogs and must be calibrated to your institution's specific clinical guidelines.
              </p>
            </div>

            {/* 4. Intellectual Property & Digital Signatures */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  4
                </span>
                Data Ownership & Certified Signatures
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                Your clinic retains 100% proprietary ownership over patient records, clinic logos, and custom report formats. Uploading a signatory stamp authorizes SwasthAI solely to apply that signature to finalized diagnostic documents requested by your staff.
              </p>
            </div>

            {/* 5. Limitation of Liability */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  5
                </span>
                Limitation of Liability & Service Guarantees
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                SwasthAI provides software services on an &quot;AS IS&quot; and &quot;AS AVAILABLE&quot; basis. Under no circumstances shall SwasthAI be liable for diagnostic interpretation errors made by attending clinicians, delays resulting from local hardware/network interruptions, or indirect clinical damages.
              </p>
            </div>

            {/* 6. Governing Law */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-3">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
                <span className="flex size-7 items-center justify-center rounded-lg bg-teal-100/70 text-xs font-bold text-teal-800">
                  6
                </span>
                Governing Law & Legal Inquiries
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                These terms are governed by the laws of India. Any legal correspondence or notices may be directed to <span className="font-mono text-teal-700">legal@swasthai.com</span>.
              </p>
            </div>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}

