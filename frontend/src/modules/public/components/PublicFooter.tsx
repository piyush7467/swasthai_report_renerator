import { Link } from "react-router-dom";
import { ShieldCheck, Mail, Lock } from "lucide-react";
import logoImg from "@/assets/logo.png";

export function PublicFooter() {
  return (
    <footer className="border-t border-slate-200 bg-slate-50 text-slate-600">
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-4">
          {/* Brand info */}
          <div className="md:col-span-2 space-y-3">
            <div className="flex items-center gap-2.5">
              <img src={logoImg} alt="SwasthAI" className="size-6 object-contain" />
              <span className="text-base font-bold text-slate-900">SwasthAI Lab</span>
            </div>
            <p className="text-xs text-slate-500 max-w-sm leading-relaxed">
              Intelligent diagnostic laboratory workflow management, multi-test biochemical cataloging, and automated certified pathology report generation.
            </p>
            <div className="flex flex-wrap items-center gap-2 pt-1">
              <span className="inline-flex items-center gap-1 rounded-md bg-white border border-slate-200 px-2 py-1 text-[11px] font-medium text-slate-700 shadow-2xs">
                <ShieldCheck className="size-3 text-teal-600" />
                DPDP Act Compliant
              </span>
              <span className="inline-flex items-center gap-1 rounded-md bg-white border border-slate-200 px-2 py-1 text-[11px] font-medium text-slate-700 shadow-2xs">
                <Lock className="size-3 text-teal-600" />
                256-Bit SSL Encrypted
              </span>
            </div>
          </div>

          {/* Quick Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900">Compliance & Legal</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/privacy" className="hover:text-teal-600 transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <Link to="/terms" className="hover:text-teal-600 transition-colors">
                  Terms & Conditions
                </Link>
              </li>
              <li>
                <Link to="/data-deletion" className="hover:text-teal-600 transition-colors">
                  Data Deletion Request
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact / Help */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900">Support & Help</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/help" className="hover:text-teal-600 transition-colors">
                  Help Desk & FAQs
                </Link>
              </li>
              <li className="flex items-center gap-1.5 text-slate-500">
                <Mail className="size-3.5 text-slate-400" />
                <a href="mailto:support@swasthai.com" className="hover:text-teal-600">
                  support@swasthai.com
                </a>
              </li>
              <li className="text-[11px] text-slate-400">
                Operating Hours: Mon - Sat, 9:00 AM - 6:00 PM IST
              </li>
            </ul>
          </div>
        </div>

        {/* Medical Disclaimer Banner */}
        <div className="mt-8 rounded-xl bg-amber-50/80 border border-amber-200/80 p-3 text-[11px] text-amber-900 leading-relaxed">
          <strong>Medical Notice:</strong> SwasthAI Lab is a clinical reporting software suite intended for certified healthcare institutions, pathologists, and laboratory technicians. It does not replace professional medical advice, clinical diagnosis, or prescriptive guidance.
        </div>

        {/* Bottom copyright */}
        <div className="mt-6 flex flex-col sm:flex-row items-center justify-between border-t border-slate-200/60 pt-6 text-[11px] text-slate-500 gap-2">
          <p>© {new Date().getFullYear()} SwasthAI Healthcare Technologies. All rights reserved.</p>
          <p>Version 1.0.0 (Production Release)</p>
        </div>
      </div>
    </footer>
  );
}

