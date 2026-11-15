import { AlertTriangle } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";
import { usePlatformContact } from "@/core/hooks/usePlatformContact";

export default function TermsConditionsPage() {
  const contact = usePlatformContact();

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1">
        {/* Header */}
        <section className="border-b border-slate-200 bg-white py-14 px-6">
          <div className="mx-auto max-w-3xl">
            <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">
              Legal & Terms
            </p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
              Terms & Conditions
            </h1>
            <p className="mt-3 text-base text-slate-600">
              Last updated: November 15, 2026
            </p>
          </div>
        </section>

        {/* Content */}
        <section className="py-12 px-6">
          <div className="mx-auto max-w-3xl space-y-10 text-base leading-relaxed text-slate-700">

            {/* Medical Disclaimer Banner */}
            <div className="rounded-2xl border-2 border-amber-300 bg-amber-50 p-6 sm:p-8 space-y-3">
              <div className="flex items-center gap-3 text-amber-950 font-bold text-lg">
                <AlertTriangle className="size-6 text-amber-600 shrink-0" />
                <span>Important Medical Disclaimer</span>
              </div>
              <p className="text-base text-amber-950 leading-relaxed">
                SwasthAI Lab is software for diagnostic laboratories to record test measurements and format certified reports. <strong>The software does not provide medical diagnoses, treatment decisions, or clinical recommendations.</strong>
              </p>
              <ul className="list-disc pl-6 space-y-1.5 text-sm text-amber-900">
                <li>
                  Every report must be reviewed and certified by a qualified pathologist or doctor.
                </li>
                <li>
                  Automated flags (high/low reference brackets) are general reference indicators and must be validated against your laboratory&apos;s equipment standards.
                </li>
                <li>
                  Patients receiving reports must consult a licensed medical professional for advice.
                </li>
              </ul>
            </div>

            {/* Section 1 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                1. Acceptance of Terms
              </h2>
              <p>
                By using SwasthAI Lab through the web application or Android app, your laboratory agrees to these terms. If you do not agree, please do not use the service.
              </p>
            </div>

            {/* Section 2 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                2. Intended Users & Account Security
              </h2>
              <ul className="list-disc pl-6 space-y-2 text-slate-700">
                <li>
                  SwasthAI Lab is intended solely for diagnostic clinics, hospitals, pathologists, and trained lab technicians.
                </li>
                <li>
                  You are responsible for keeping your account credentials confidential. You must not share logins with unauthorized individuals.
                </li>
                <li>
                  You must immediately revoke access for staff members who leave your organization.
                </li>
              </ul>
            </div>

            {/* Section 3 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                3. Accuracy of Laboratory Values
              </h2>
              <p>
                Your facility is solely responsible for sample collection, measurement accuracy, and numerical values entered into the station. SwasthAI provides tools to format, compute ranges, and render PDF reports, but does not verify physical lab specimens.
              </p>
            </div>

            {/* Section 4 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                4. Ownership of Records & Assets
              </h2>
              <p>
                Your clinic owns all patient records, clinic logos, and custom letterheads entered into SwasthAI. We do not claim ownership over any patient data or clinical observations created by your laboratory.
              </p>
            </div>

            {/* Section 5 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                5. Limitation of Liability
              </h2>
              <p>
                To the fullest extent permitted by law, SwasthAI is not liable for clinical interpretation errors, diagnostic delays, or technical disruptions resulting from third-party Internet outages or local hardware failures.
              </p>
            </div>

            {/* Section 6 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                6. Contact Information
              </h2>
              <p>
                For questions regarding these terms, contact us at{" "}
                <a href={`mailto:${contact.legalEmail}`} className="font-semibold text-teal-700 underline">
                  {contact.legalEmail}
                </a>.
              </p>
              <p className="text-sm text-slate-500">
                Registered entity: {contact.officeAddress}
              </p>
            </div>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}
