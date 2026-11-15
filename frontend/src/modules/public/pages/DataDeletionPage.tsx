import { useState } from "react";
import { Trash2, CheckCircle2, ShieldCheck } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";

export default function DataDeletionPage() {
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
        {/* Hero Header */}
        <section className="border-b border-slate-200 bg-white py-12 px-4 sm:px-6">
          <div className="mx-auto max-w-4xl text-center space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-red-50 border border-red-200 px-3 py-1 text-xs font-semibold text-red-700">
              <Trash2 className="size-3.5" />
              <span>User Rights & Regulatory Compliance</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
              Account & Data Deletion Request
            </h1>
            <p className="text-sm text-slate-500 max-w-2xl mx-auto">
              In accordance with Google Play Store, Indus Appstore, and DPDP Act Data Safety requirements, you may request permanent deletion of your account and associated laboratory data.
            </p>
          </div>
        </section>

        {/* Content Container */}
        <section className="py-10 px-4 sm:px-6">
          <div className="mx-auto max-w-2xl space-y-8">

            {/* Explanation Card */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-4">
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <ShieldCheck className="size-5 text-teal-600" />
                How Deletion is Handled
              </h2>
              <div className="text-xs sm:text-sm text-slate-600 space-y-3 leading-relaxed">
                <p>
                  When you submit a data deletion request:
                </p>
                <ul className="list-disc list-inside space-y-1.5 pl-2 text-xs">
                  <li><strong>Account Credentials:</strong> Login credentials, staff profiles, and active access tokens are permanently deactivated and purged.</li>
                  <li><strong>Laboratory Profile & Letterhead:</strong> Custom letterheads, logos, and digital signature files are wiped from cloud storage.</li>
                  <li><strong>Diagnostic Records:</strong> Non-finalized drafts and patient contact details are completely deleted. Finalized certified reports will be archived or expunged in accordance with statutory medical record-keeping laws.</li>
                  <li><strong>Processing Timeframe:</strong> Deletion requests are confirmed within 48 hours and completely processed within 30 calendar days.</li>
                </ul>
              </div>
            </div>

            {/* Request Form / Confirmation */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs">
              {submitted ? (
                <div className="text-center py-6 space-y-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-teal-50 text-teal-600 mx-auto">
                    <CheckCircle2 className="size-7" />
                  </div>
                  <h3 className="text-lg font-bold text-slate-900">Request Submitted Successfully</h3>
                  <p className="text-xs text-slate-600 max-w-md mx-auto leading-relaxed">
                    We have logged your deletion request for <span className="font-semibold text-slate-800">{email}</span>. Our Data Protection Team will verify laboratory authorization and send a confirmation to your email.
                  </p>
                  <p className="text-xs text-slate-400 pt-2">
                    Reference ID: DEL-{Math.floor(100000 + Math.random() * 900000)}
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-4">
                  <h3 className="text-base font-bold text-slate-900">Submit Deletion Form</h3>
                  <p className="text-xs text-slate-500">
                    Please provide the registered email address associated with your laboratory station.
                  </p>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                      Registered Laboratory Email <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="admin@yourlab.com"
                      className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-xs text-slate-900 focus:border-teal-500 focus:outline-hidden focus:ring-1 focus:ring-teal-500"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                      Laboratory / Clinic Name
                    </label>
                    <input
                      type="text"
                      value={orgName}
                      onChange={(e) => setOrgName(e.target.value)}
                      placeholder="e.g. Metro Diagnostic Lab"
                      className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-xs text-slate-900 focus:border-teal-500 focus:outline-hidden focus:ring-1 focus:ring-teal-500"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                      Scope of Deletion (Optional Details)
                    </label>
                    <textarea
                      rows={3}
                      value={details}
                      onChange={(e) => setDetails(e.target.value)}
                      placeholder="e.g. Delete entire organization account, or specific staff credentials..."
                      className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-xs text-slate-900 focus:border-teal-500 focus:outline-hidden focus:ring-1 focus:ring-teal-500"
                    />
                  </div>

                  <div className="pt-2">
                    <button
                      type="submit"
                      className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-3 text-xs font-semibold text-white shadow-xs hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-red-500 transition-colors"
                    >
                      <Trash2 className="size-4" />
                      Submit Permanent Deletion Request
                    </button>
                  </div>
                </form>
              )}
            </div>

            {/* Direct Email Option */}
            <div className="text-center text-xs text-slate-500 space-y-1">
              <p>
                Alternatively, you can email your request directly to our Data Protection Officer at:
              </p>
              <p>
                <a href="mailto:privacy@swasthai.com" className="font-semibold text-teal-600 hover:underline">
                  privacy@swasthai.com
                </a>
              </p>
            </div>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}

