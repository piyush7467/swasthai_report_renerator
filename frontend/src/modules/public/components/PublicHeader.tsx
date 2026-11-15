import { Link, useLocation } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import logoImg from "@/assets/logo.png";

export function PublicHeader() {
  const location = useLocation();

  const navLinks = [
    { label: "Privacy Policy", to: "/privacy" },
    { label: "Terms of Service", to: "/terms" },
    { label: "Help & FAQs", to: "/help" },
    { label: "Delete Data", to: "/data-deletion" },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200 bg-white/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        {/* Brand */}
        <Link to="/" className="flex items-center gap-3">
          <img src={logoImg} alt="SwasthAI" className="size-8 object-contain" />
          <div className="flex items-baseline gap-2">
            <span className="text-xl font-bold tracking-tight text-slate-900">SwasthAI</span>
            <span className="text-sm font-semibold text-teal-700">Lab</span>
          </div>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-2">
          {navLinks.map((item) => {
            const isActive = location.pathname === item.to;
            return (
              <Link
                key={item.to}
                to={item.to}
                className={`rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-slate-100 text-teal-800 font-semibold"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* Action Button */}
        <div>
          <Link
            to="/login"
            className="inline-flex items-center gap-2 rounded-xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white shadow-xs transition hover:bg-teal-700"
          >
            <span>Lab Sign In</span>
            <ArrowRight className="size-4" />
          </Link>
        </div>
      </div>
    </header>
  );
}
