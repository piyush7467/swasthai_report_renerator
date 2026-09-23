import { Check } from "lucide-react";

export type ReportStepIndex = 1 | 2 | 3 | 4 | 5;

interface ReportWorkflowStepperProps {
  currentStep: ReportStepIndex;
  maxAccessibleStep: ReportStepIndex;
  onSelectStep: (step: ReportStepIndex) => void;
}

const STEPS = [
  { step: 1 as ReportStepIndex, title: "Patient", subtitle: "Select / Register" },
  { step: 2 as ReportStepIndex, title: "Reporting Doctor", subtitle: "Assign Pathologist" },
  { step: 3 as ReportStepIndex, title: "Select Tests", subtitle: "Lab Catalog" },
  { step: 4 as ReportStepIndex, title: "Enter Results", subtitle: "Parameters & Flags" },
  { step: 5 as ReportStepIndex, title: "Review & Finalize", subtitle: "PDF & QR Preview" },
];

export function ReportWorkflowStepper({
  currentStep,
  maxAccessibleStep,
  onSelectStep,
}: ReportWorkflowStepperProps) {
  return (
    <div className="w-full bg-white border border-slate-200/80 rounded-xl p-3.5 shadow-xs">
      {/* DESKTOP VIEW (md and up) */}
      <div className="hidden md:flex items-center justify-between">
        {STEPS.map((item, index) => {
          const isCompleted = item.step < currentStep;
          const isCurrent = item.step === currentStep;
          const isClickable = item.step <= maxAccessibleStep;

          return (
            <div key={item.step} className="flex items-center flex-1 last:flex-none">
              <button
                type="button"
                disabled={!isClickable}
                onClick={() => onSelectStep(item.step)}
                className={`flex items-center gap-3 text-left transition-all px-2 py-1.5 rounded-lg group ${
                  isClickable
                    ? "cursor-pointer hover:bg-slate-50/80"
                    : "cursor-not-allowed opacity-50"
                }`}
              >
                <div
                  className={`h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 transition-all ${
                    isCurrent
                      ? "bg-[#0F766E] text-white ring-4 ring-[#E6F4EA] shadow-xs"
                      : isCompleted
                      ? "bg-emerald-600 text-white"
                      : "bg-slate-100 text-slate-400 border border-slate-200"
                  }`}
                >
                  {isCompleted ? <Check className="h-4 w-4 stroke-[2.5]" /> : item.step}
                </div>
                <div className="min-w-0">
                  <p
                    className={`text-[11px] font-bold tracking-wide uppercase leading-none ${
                      isCurrent
                        ? "text-[#0F766E]"
                        : isCompleted
                        ? "text-slate-700"
                        : "text-slate-400"
                    }`}
                  >
                    Step {item.step}
                  </p>
                  <p
                    className={`text-xs font-semibold truncate mt-1 ${
                      isCurrent
                        ? "text-slate-900"
                        : isCompleted
                        ? "text-slate-700"
                        : "text-slate-400"
                    }`}
                  >
                    {item.title}
                  </p>
                </div>
              </button>

              {index < STEPS.length - 1 && (
                <div className="flex-1 mx-3 h-0.5 relative bg-slate-200">
                  <div
                    className="absolute inset-y-0 left-0 bg-emerald-500 transition-all duration-300"
                    style={{
                      width: isCompleted ? "100%" : "0%",
                    }}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* MOBILE COMPACT VIEW (< md) */}
      <div className="md:hidden flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="h-7 w-7 rounded-full bg-[#0F766E] text-white flex items-center justify-center text-xs font-bold shrink-0 shadow-xs">
            {currentStep}
          </div>
          <div>
            <span className="text-[10px] font-bold text-[#0F766E] uppercase tracking-wider">
              Step {currentStep} of 5
            </span>
            <div className="text-xs font-bold text-slate-900">
              {STEPS[currentStep - 1].title}
            </div>
          </div>
        </div>

        {/* Quick Stepper Dots on Mobile */}
        <div className="flex items-center gap-1.5">
          {STEPS.map((item) => (
            <button
              key={item.step}
              type="button"
              disabled={item.step > maxAccessibleStep}
              onClick={() => onSelectStep(item.step)}
              className={`h-2 rounded-full transition-all ${
                item.step === currentStep
                  ? "w-6 bg-[#0F766E]"
                  : item.step < currentStep
                  ? "w-2 bg-emerald-600"
                  : "w-2 bg-slate-200"
              }`}
              title={`Step ${item.step}: ${item.title}`}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
