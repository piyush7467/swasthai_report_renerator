import { useState } from "react";
import { CheckCircle2, Trash2 } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";
import { usePlatformContact } from "@/core/hooks/usePlatformContact";

export default function DataDeletionPage() {
  const contact = usePlatformContact();
  const [email, setEmail] = useState("");
  const [orgName, setOrgName] = useState("");
  const [details, setDetails] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    setSubmitted(true);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1">
        {/* Header */}
        <section className="border-b border-slate-200 bg-white py-14 px-6">
          <div className="mx-auto max-w-2xl">
            <p className="text-sm font-semibold uppercase tracking-wider text-red-600">
              Account Management
            </p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
              Request Account & Data Deletion
            </h1>
            <p className="mt-3 text-base text-slate-600">
              Submit a request to permanently delete your laboratory account, staff credentials, and stored records.
            </p>
          </div>
        </section>

        {/* Content */}
        <section className="py-12 px-6">
          <div className="mx-auto max-w-2xl space-y-8">

            {/* Explanation */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 space-y-4">
              <h2 className="text-xl font-bold text-slate-900">
                What happens when you request deletion?
              </h2>
              <ul className="list-disc pl-6 space-y-2.5 text-base text-slate-700 leading-relaxed">
                <li>
                  <strong className="text-slate-900">Staff Logins:</strong> All login credentials and station tokens are immediately deactivated.
                </li>
                <li>
                  <strong className="text-slate-900">Clinic Assets:</strong> Uploaded letterheads, logos, and digital signature stamps are permanently deleted from our servers.
                </li>
                <li>
                  <strong className="text-slate-900">Patient Data:</strong> Unfinalized draft reports and patient profiles are purged. Finalized reports are handled in accordance with local healthcare record-keeping requirements.
                </li>
                <li>
                  <strong className="text-slate-900">Timeline:</strong> Requests are verified and processed within 30 days.
                </li>
              </ul>
            </div>

            {/* Form */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs">
              {submitted ? (
                <div className="text-center py-6 space-y-4">
                  <div className="flex size-14 items-center justify-center rounded-2xl bg-teal-50 text-teal-700 mx-auto">
                    <CheckCircle2 className="size-8" />
                  </div>
                  <h3 className="text-2xl font-bold text-slate-900">Request Received</h3>
                  <p className="text-base text-slate-600 max-w-md mx-auto leading-relaxed">
                    We received your deletion request for <strong className="text-slate-900">{email}</strong>. Our team will verify authorization and send a confirmation to your email within 48 hours.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-5">
                  <h3 className="text-xl font-bold text-slate-900">Submit Your Request</h3>

                  <div className="space-y-2">
                    <label className="block text-sm font-semibold text-slate-800">
                      Registered Laboratory Email <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="admin@yourlab.com"
                      className="w-full rounded-xl border border-slate-300 px-4 py-3 text-base text-slate-900 placeholder:text-slate-400 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="block text-sm font-semibold text-slate-800">
                      Laboratory / Clinic Name
                    </label>
                    <input
                      type="text"
                      value={orgName}
                      onChange={(e) => setOrgName(e.target.value)}
                      placeholder="e.g. Metro Diagnostic Lab"
                      className="w-full rounded-xl border border-slate-300 px-4 py-3 text-base text-slate-900 placeholder:text-slate-400 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="block text-sm font-semibold text-slate-800">
                      Additional Notes (Optional)
                    </label>
                    <textarea
                      rows={3}
                      value={details}
                      onChange={(e) => setDetails(e.target.value)}
                      placeholder="e.g. Delete all staff profiles and draft patient reports..."
                      className="w-full rounded-xl border border-slate-300 px-4 py-3 text-base text-slate-900 placeholder:text-slate-400 focus:border-teal-600 focus:outline-hidden focus:ring-1 focus:ring-teal-600"
                    />
                  </div>

                  <button
                    type="submit"
                    className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 px-6 py-3.5 text-base font-semibold text-white shadow-xs hover:bg-red-700 transition cursor-pointer"
                  >
                    <Trash2 className="size-5" />
                    <span>Submit Deletion Request</span>
                  </button>
                </form>
              )}
            </div>

            {/* Email note */}
            <p className="text-center text-sm text-slate-500">
              You can also email your request directly to{" "}
              <a href={`mailto:${contact.privacyEmail}`} className="font-semibold text-teal-700 hover:underline">
                {contact.privacyEmail}
              </a>.
            </p>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}
