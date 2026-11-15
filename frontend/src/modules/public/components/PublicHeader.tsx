import { Link, useLocation } from "react-router-dom";
import { Shield, FileText, HelpCircle, Trash2, ArrowRight } from "lucide-react";
import logoImg from "@/assets/logo.png";

export function PublicHeader() {
  const location = useLocation();

  const navLinks = [
    { label: "Privacy Policy", to: "/privacy", icon: Shield },
    { label: "Terms & Conditions", to: "/terms", icon: FileText },
    { label: "Help & FAQs", to: "/help", icon: HelpCircle },
    { label: "Data Deletion", to: "/data-deletion", icon: Trash2 },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200/80 bg-white/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3.5 sm:px-6">
        {/* Brand */}
        <Link to="/" className="flex items-center gap-3 transition-opacity hover:opacity-90">
          <div className="flex size-10 items-center justify-center rounded-xl bg-teal-50 border border-teal-100 shadow-xs">
            <img src={logoImg} alt="SwasthAI Logo" className="size-7 object-contain" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold tracking-tight text-slate-900">SwasthAI</span>
              <span className="rounded-md bg-teal-50 px-1.5 py-0.5 text-[11px] font-semibold text-teal-700 border border-teal-100">
                Lab
              </span>
            </div>
            <p className="text-[11px] text-slate-500 font-medium">Diagnostic Laboratory Suite</p>
          </div>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-1">
          {navLinks.map((item) => {
            const isActive = location.pathname === item.to;
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                  isActive
                    ? "bg-teal-50 text-teal-700 font-semibold"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                }`}
              >
                <Icon className="size-3.5" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* Action Button */}
        <div className="flex items-center gap-3">
          <Link
            to="/login"
            className="inline-flex items-center gap-1.5 rounded-xl bg-teal-600 px-4 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-teal-700 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
          >
            <span>Lab Sign In</span>
            <ArrowRight className="size-3.5" />
          </Link>
        </div>
      </div>
    </header>
  );
}

