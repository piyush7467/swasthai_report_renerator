import { Link } from "react-router-dom";
import { usePlatformContact } from "@/core/hooks/usePlatformContact";
import logoImg from "@/assets/logo.png";

export function PublicFooter() {
  const contact = usePlatformContact();

  return (
    <footer className="border-t border-slate-200 bg-white text-slate-600">
      <div className="mx-auto max-w-5xl px-6 py-12">
        <div className="grid grid-cols-1 gap-10 md:grid-cols-3">
          {/* Brand info */}
          <div className="space-y-3">
            <div className="flex items-center gap-2.5">
              <img src={logoImg} alt="SwasthAI" className="size-7 object-contain" />
              <span className="text-lg font-bold text-slate-900">SwasthAI Lab</span>
            </div>
            <p className="text-sm text-slate-600 leading-relaxed">
              Software for diagnostic labs to record test values, manage patient records, and generate verified PDF reports.
            </p>
          </div>

          {/* Quick Links */}
          <div className="space-y-3">
            <h4 className="text-sm font-bold uppercase tracking-wider text-slate-900">Legal & Privacy</h4>
            <ul className="space-y-2.5 text-sm">
              <li>
                <Link to="/privacy" className="text-slate-600 hover:text-teal-700 transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <Link to="/terms" className="text-slate-600 hover:text-teal-700 transition-colors">
                  Terms of Service
                </Link>
              </li>
              <li>
                <Link to="/data-deletion" className="text-slate-600 hover:text-teal-700 transition-colors">
                  Request Data Deletion
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact / Help */}
          <div className="space-y-3">
            <h4 className="text-sm font-bold uppercase tracking-wider text-slate-900">Support</h4>
            <ul className="space-y-2.5 text-sm text-slate-600">
              <li>
                <Link to="/help" className="hover:text-teal-700 transition-colors">
                  Help Desk & FAQs
                </Link>
              </li>
              <li>
                Email:{" "}
                <a href={`mailto:${contact.supportEmail}`} className="font-medium text-teal-700 hover:underline">
                  {contact.supportEmail}
                </a>
              </li>
              <li className="text-xs text-slate-500">
                Support Hours: {contact.operatingHours}
              </li>
            </ul>
          </div>
        </div>

        {/* Medical Notice */}
        <div className="mt-10 rounded-xl bg-amber-50 border border-amber-200 p-4 text-sm text-amber-950 leading-relaxed">
          <strong className="font-semibold">Medical Notice:</strong> SwasthAI Lab is report-generation software for healthcare facilities and pathologists. It does not provide medical diagnoses or treatment advice. All test results must be reviewed and certified by a qualified medical professional.
        </div>

        {/* Bottom copyright */}
        <div className="mt-8 flex flex-col sm:flex-row items-center justify-between border-t border-slate-100 pt-6 text-sm text-slate-500 gap-2">
          <p>© {new Date().getFullYear()} SwasthAI Lab. All rights reserved.</p>
          <p>Version 1.0.0</p>
        </div>
      </div>
    </footer>
  );
}
