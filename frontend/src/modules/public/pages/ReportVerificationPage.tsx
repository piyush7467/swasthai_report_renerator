import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { ShieldCheck, ShieldAlert, Building2, Calendar, FileText, CheckCircle2, Loader2, ArrowLeft } from "lucide-react";
import { PublicHeader } from "../components/PublicHeader";
import { PublicFooter } from "../components/PublicFooter";
import axios from "axios";

interface VerificationResponse {
  valid: boolean;
  reportRefId?: string;
  organizationName?: string;
  reportStatus?: string;
  finalizedAt?: string;
  message?: string;
}

export default function ReportVerificationPage() {
  const { reportRefId } = useParams<{ reportRefId: string }>();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<VerificationResponse | null>(null);

  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL || "https://swasthaireportrenerator-production-2370.up.railway.app/api/v1";

  useEffect(() => {
    let isMounted = true;

    async function checkVerification() {
      if (!reportRefId) {
        setData({
          valid: false,
          reportRefId: "",
          message: "No report reference ID provided.",
        });
        setLoading(false);
        return;
      }

      setLoading(true);

      try {
        const cleanBaseUrl = apiBaseUrl.replace(/\/+$/, "");
        const url = `${cleanBaseUrl}/public/reports/${encodeURIComponent(reportRefId.trim())}/verify`;
        const res = await axios.get<VerificationResponse>(url, {
          timeout: 10000,
        });

        if (isMounted) {
          setData(res.data);
        }
      } catch (err: any) {
        if (isMounted) {
          if (err.response?.data) {
            setData(err.response.data);
          } else {
            setData({
              valid: false,
              reportRefId: reportRefId,
              message: "Report record could not be verified on the diagnostic server.",
            });
          }
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    checkVerification();

    return () => {
      isMounted = false;
    };
  }, [reportRefId, apiBaseUrl]);

  const formatDate = (isoString?: string) => {
    if (!isoString) return "Certified Record";
    try {
      return new Date(isoString).toLocaleString("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <PublicHeader />

      <main className="flex-1 flex flex-col items-center justify-center px-4 py-12">
        <div className="w-full max-w-xl">
          {loading ? (
            <div className="bg-white rounded-2xl border border-slate-200 p-10 text-center shadow-sm">
              <Loader2 className="w-10 h-10 text-teal-600 animate-spin mx-auto mb-4" />
              <h2 className="text-lg font-bold text-slate-800">Verifying Diagnostic Authenticity...</h2>
              <p className="text-xs text-slate-500 mt-1">
                Checking cryptographic laboratory registry for Ref: {reportRefId}
              </p>
            </div>
          ) : data?.valid ? (
            <div className="bg-white rounded-2xl border border-teal-200 shadow-md overflow-hidden">
              {/* Green Verified Header Banner */}
              <div className="bg-gradient-to-r from-teal-700 to-emerald-600 p-6 text-white text-center">
                <div className="w-16 h-16 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center mx-auto mb-3 shadow-inner">
                  <ShieldCheck className="w-9 h-9 text-white" />
                </div>
                <div className="inline-block bg-white/25 text-white text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full mb-1">
                  ✓ Verified Authentic Report
                </div>
                <h1 className="text-2xl font-black tracking-tight">SwasthAI Clinical Verification</h1>
                <p className="text-teal-100 text-xs mt-1">Official Digital Pathology Record</p>
              </div>

              {/* Certificate Details */}
              <div className="p-6 sm:p-8 space-y-6">
                <div className="bg-teal-50/70 border border-teal-100 rounded-xl p-4 flex items-start gap-3">
                  <CheckCircle2 className="w-5 h-5 text-teal-700 shrink-0 mt-0.5" />
                  <div className="text-xs text-teal-900 leading-relaxed">
                    This official verification confirms that this diagnostic investigation was validated, digitally signed, and certified by an authorized clinical laboratory on the SwasthAI diagnostic network.
                  </div>
                </div>

                <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl overflow-hidden bg-slate-50/50">
                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 flex items-center gap-2">
                      <FileText className="w-4 h-4 text-slate-400" /> Report Reference
                    </span>
                    <span className="text-xs font-mono font-bold text-slate-900">{data.reportRefId || reportRefId}</span>
                  </div>

                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 flex items-center gap-2">
                      <Building2 className="w-4 h-4 text-slate-400" /> Issuing Laboratory
                    </span>
                    <span className="text-xs font-bold text-teal-700">{data.organizationName || "Authorized Diagnostic Center"}</span>
                  </div>

                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 flex items-center gap-2">
                      <Calendar className="w-4 h-4 text-slate-400" /> Certification Date
                    </span>
                    <span className="text-xs font-semibold text-slate-700">{formatDate(data.finalizedAt)}</span>
                  </div>

                  <div className="p-3.5 flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500">Status</span>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-extrabold bg-emerald-100 text-emerald-800">
                      {data.reportStatus || "FINALIZED"}
                    </span>
                  </div>
                </div>

                <div className="text-center pt-2">
                  <Link
                    to="/login"
                    className="inline-flex items-center gap-2 text-xs font-bold text-teal-700 hover:text-teal-800 hover:underline"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" /> SwasthAI Laboratory Portal
                  </Link>
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-2xl border border-red-200 shadow-md overflow-hidden">
              <div className="bg-rose-600 p-6 text-white text-center">
                <div className="w-16 h-16 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center mx-auto mb-3">
                  <ShieldAlert className="w-9 h-9 text-white" />
                </div>
                <div className="inline-block bg-white/20 text-white text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full mb-1">
                  ✕ Verification Unsuccessful
                </div>
                <h1 className="text-2xl font-black tracking-tight">Report Record Not Found</h1>
                <p className="text-rose-100 text-xs mt-1">Ref: {reportRefId || "Unknown"}</p>
              </div>

              <div className="p-6 sm:p-8 space-y-5 text-center">
                <p className="text-sm text-slate-600">
                  {data?.message || "No authentic diagnostic record matching this reference ID was found on the official laboratory registry."}
                </p>
                <div className="p-3.5 bg-rose-50 border border-rose-100 rounded-xl text-xs text-rose-800 text-left">
                  <strong>Notice for Patients & Clinicians:</strong> If this document was provided as a certified medical report, it may be invalid, pending finalization, or tampered with. Please contact the testing laboratory directly for verification.
                </div>
                <div className="pt-2">
                  <Link
                    to="/help"
                    className="inline-flex items-center justify-center gap-2 text-xs font-bold bg-slate-900 text-white px-4 py-2.5 rounded-xl hover:bg-slate-800 transition"
                  >
                    Contact Support & Help Desk
                  </Link>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      <PublicFooter />
    </div>
  );
}
