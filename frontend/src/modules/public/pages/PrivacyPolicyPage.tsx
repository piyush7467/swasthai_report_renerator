import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";
import { usePlatformContact } from "@/core/hooks/usePlatformContact";

export default function PrivacyPolicyPage() {
  const contact = usePlatformContact();

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1">
        {/* Header */}
        <section className="border-b border-slate-200 bg-white py-14 px-6">
          <div className="mx-auto max-w-3xl">
            <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">
              Legal & Compliance
            </p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
              Privacy Policy
            </h1>
            <p className="mt-3 text-base text-slate-600">
              Last updated: November 15, 2026
            </p>
          </div>
        </section>

        {/* Content */}
        <section className="py-12 px-6">
          <div className="mx-auto max-w-3xl space-y-10 text-base leading-relaxed text-slate-700">

            {/* Plain Promise */}
            <div className="rounded-2xl border border-teal-200 bg-teal-50/70 p-6 sm:p-8">
              <h2 className="text-lg font-bold text-teal-950">
                Our Core Promise
              </h2>
              <p className="mt-2 text-base text-teal-900 leading-relaxed">
                SwasthAI Lab is built for diagnostic clinics, pathology labs, and hospitals to generate verified reports. <strong>We do not sell patient data, we do not run third-party advertisements, and we do not share medical records with data brokers.</strong>
              </p>
            </div>

            {/* Section 1 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                1. What Information We Collect
              </h2>
              <p>
                When a laboratory station creates a diagnostic record, we process only the information needed to generate the report:
              </p>
              <ul className="list-disc pl-6 space-y-2 text-slate-700">
                <li>
                  <strong className="text-slate-900">Patient Details:</strong> Name, age, gender, contact number, and internal patient ID.
                </li>
                <li>
                  <strong className="text-slate-900">Test Measurements:</strong> Laboratory test panels (such as Complete Blood Count, Liver Function, Lipid Profile, Kidney Function), numerical values, and reference flags.
                </li>
                <li>
                  <strong className="text-slate-900">Lab Personnel Details:</strong> Name and email of the lab administrator, technician, or pathologist who logged in and reviewed the report.
                </li>
                <li>
                  <strong className="text-slate-900">Lab Branding & Signatures:</strong> Laboratory letterhead image, address, and the digital signature uploaded by authorized staff for report stamping.
                </li>
              </ul>
            </div>

            {/* Section 2 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                2. How We Use This Information
              </h2>
              <p>We use this data solely to run the service:</p>
              <ul className="list-disc pl-6 space-y-2 text-slate-700">
                <li>Creating and rendering certified PDF lab test reports.</li>
                <li>Allowing clinics to pull up previous patient test history when requested.</li>
                <li>Enabling patients or doctors to verify report authenticity via QR code.</li>
                <li>Maintaining station audit logs to record which staff member entered or certified results.</li>
              </ul>
            </div>

            {/* Section 3 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                3. How Your Data Is Protected
              </h2>
              <p>
                We use secure, modern safeguards to keep diagnostic records private:
              </p>
              <ul className="list-disc pl-6 space-y-2 text-slate-700">
                <li>
                  <strong className="text-slate-900">Encryption:</strong> All communication between your web browser or Android app and our servers is encrypted using TLS (HTTPS).
                </li>
                <li>
                  <strong className="text-slate-900">Clinic Isolation:</strong> Each diagnostic lab&apos;s data is strictly partitioned. Staff at one clinic cannot view records from another.
                </li>
                <li>
                  <strong className="text-slate-900">Access Control:</strong> Secure token authentication ensures only authenticated laboratory personnel can view or edit patient files.
                </li>
              </ul>
            </div>

            {/* Section 4 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                4. Your Rights and Data Deletion
              </h2>
              <p>
                You retain full ownership of your laboratory records. You can:
              </p>
              <ul className="list-disc pl-6 space-y-2 text-slate-700">
                <li>
                  Edit or delete patient drafts and records directly inside the application.
                </li>
                <li>
                  Request a complete purge of your account, staff profiles, and stored assets at any time through our{" "}
                  <a href="/data-deletion" className="font-semibold text-teal-700 underline">
                    Data Deletion Request Page
                  </a>.
                </li>
                <li>
                  Email our data protection team directly at{" "}
                  <a href={`mailto:${contact.privacyEmail}`} className="font-semibold text-teal-700 underline">
                    {contact.privacyEmail}
                  </a>.
                </li>
              </ul>
              <p className="text-sm text-slate-500">
                Note: Finalized medical records may be subject to minimum retention periods mandated by local public health authorities.
              </p>
            </div>

            {/* Section 5 */}
            <div className="space-y-4">
              <h2 className="text-2xl font-bold text-slate-900">
                5. Contact Us
              </h2>
              <p>
                If you have questions about our privacy practices, please contact us:
              </p>
              <div className="rounded-xl border border-slate-200 bg-white p-6 text-base space-y-2">
                <p className="font-bold text-slate-900">{contact.officeAddress}</p>
                <p>
                  Privacy Desk:{" "}
                  <a href={`mailto:${contact.privacyEmail}`} className="text-teal-700 font-semibold hover:underline">
                    {contact.privacyEmail}
                  </a>
                </p>
                <p>
                  General Support:{" "}
                  <a href={`mailto:${contact.supportEmail}`} className="text-teal-700 font-semibold hover:underline">
                    {contact.supportEmail}
                  </a>
                </p>
                <p className="text-sm text-slate-500 pt-1">
                  Operating Hours: {contact.operatingHours}
                </p>
              </div>
            </div>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}
