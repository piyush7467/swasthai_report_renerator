import { useState } from "react";
import { ChevronDown, ChevronUp, Mail, MessageSquare, ArrowRight } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";
import { usePlatformContact } from "@/core/hooks/usePlatformContact";

interface FaqItem {
  q: string;
  a: string;
}

const faqs: FaqItem[] = [
  {
    q: "How do I create a new diagnostic report?",
    a: "Log in to your laboratory station and head to the Worklist or Dashboard. Click 'New Report', select or add a patient, choose the test panel (e.g. CBC, Liver Function, Lipid Profile), enter the measured values, and click Generate Report."
  },
  {
    q: "How do I add our lab's logo and digital signature?",
    a: "If you have an Admin account, go to Settings -> Lab Profile & Branding. From there you can upload your clinic's logo, enter contact details and registration numbers, and upload the pathologist's digital signature for automatic stamping on all reports."
  },
  {
    q: "How does the QR verification feature work?",
    a: "Every finalized PDF report includes a secure QR code. When scanned by patients or doctors using any standard phone camera, it opens a secure page confirming the report is genuine and displaying the certified results."
  },
  {
    q: "What if a test measurement falls outside normal reference ranges?",
    a: "SwasthAI compares the entered value against standard clinical brackets. Values that are too high or too low are automatically highlighted with clear HIGH or LOW badges, allowing the technician and pathologist to spot abnormalities quickly before certifying."
  },
  {
    q: "How can we delete patient data or close our account?",
    a: "Individual patient records can be removed directly inside the app. If you wish to purge an entire organization account and all associated records, submit a request on our Data Deletion page or email privacy@swasthai.com."
  }
];

export default function HelpSupportPage() {
  const contact = usePlatformContact();
  const [openIdx, setOpenIdx] = useState<number | null>(0);

  const toggleFaq = (idx: number) => {
    setOpenIdx(openIdx === idx ? null : idx);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1">
        {/* Header */}
        <section className="border-b border-slate-200 bg-white py-14 px-6">
          <div className="mx-auto max-w-3xl">
            <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">
              Support Center
            </p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
              Help & Frequently Asked Questions
            </h1>
            <p className="mt-3 text-base text-slate-600">
              Quick answers about creating reports, setting up lab letterheads, and managing records.
            </p>
          </div>
        </section>

        {/* Content */}
        <section className="py-12 px-6">
          <div className="mx-auto max-w-3xl space-y-10">

            {/* Direct Contact Options */}
            <div className="grid gap-6 sm:grid-cols-2">
              <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-xs space-y-3">
                <div className="flex size-11 items-center justify-center rounded-xl bg-teal-50 text-teal-700">
                  <Mail className="size-6" />
                </div>
                <h3 className="text-lg font-bold text-slate-900">Email Support</h3>
                <p className="text-sm text-slate-600 leading-relaxed">
                  Have questions about station setup or technical issues? Write to our team.
                </p>
                <a
                  href={`mailto:${contact.supportEmail}`}
                  className="inline-flex items-center gap-1.5 text-sm font-semibold text-teal-700 hover:text-teal-800 underline"
                >
                  <span>{contact.supportEmail}</span>
                  <ArrowRight className="size-4" />
                </a>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-xs space-y-3">
                <div className="flex size-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700">
                  <MessageSquare className="size-6" />
                </div>
                <h3 className="text-lg font-bold text-slate-900">WhatsApp Help</h3>
                <p className="text-sm text-slate-600 leading-relaxed">
                  Need quick guidance on report generation or lab settings? Reach out on chat.
                </p>
                <a
                  href={`https://wa.me/${contact.whatsappNumber.replace(/\D/g, "")}?text=Hello%20SwasthAI%20Support`}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 text-sm font-semibold text-emerald-700 hover:text-emerald-800 underline"
                >
                  <span>Start WhatsApp Chat ({contact.helplinePhone})</span>
                  <ArrowRight className="size-4" />
                </a>
              </div>
            </div>

            {/* FAQs */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs space-y-6">
              <h2 className="text-2xl font-bold text-slate-900">
                Frequently Asked Questions
              </h2>
              <div className="divide-y divide-slate-200">
                {faqs.map((faq, idx) => {
                  const isOpen = openIdx === idx;
                  return (
                    <div key={idx} className="py-5 first:pt-2 last:pb-2">
                      <button
                        onClick={() => toggleFaq(idx)}
                        className="flex w-full items-center justify-between text-left gap-4 cursor-pointer"
                      >
                        <span className="text-base font-semibold text-slate-900">
                          {faq.q}
                        </span>
                        {isOpen ? (
                          <ChevronUp className="size-5 text-teal-700 shrink-0" />
                        ) : (
                          <ChevronDown className="size-5 text-slate-400 shrink-0" />
                        )}
                      </button>
                      {isOpen && (
                        <p className="mt-3 text-base text-slate-600 leading-relaxed">
                          {faq.a}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Data Deletion Link */}
            <div className="rounded-2xl border border-slate-200 bg-white p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h3 className="text-base font-bold text-slate-900">Need to delete your account or records?</h3>
                <p className="text-sm text-slate-600 mt-1">Submit a request through our dedicated deletion form.</p>
              </div>
              <a
                href="/data-deletion"
                className="shrink-0 rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 transition"
              >
                Go to Deletion Page
              </a>
            </div>

          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}
