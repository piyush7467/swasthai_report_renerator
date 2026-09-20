import {
  AlertTriangle,
  Calculator,
  Edit,
  Sliders,
} from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ParameterDataTypeBadge } from "./ParameterDataTypeBadge";
import { TestStatusBadge } from "./TestStatusBadge";
import type { TestParameterResponse } from "../types/parameterTypes";

interface ParameterDetailsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  parameter: TestParameterResponse | null;
  testRefId?: string;
}

export function ParameterDetailsDialog({
  open,
  onOpenChange,
  parameter,
  testRefId,
}: ParameterDetailsDialogProps) {
  if (!parameter) return null;

  const getCalculationExplanation = (type: string) => {
    switch (type) {
      case "MCV":
        return "Mean Corpuscular Volume: (Hematocrit [%] × 10) / Red Blood Cells [10^12/L]";
      case "MCH":
        return "Mean Corpuscular Hemoglobin: (Hemoglobin [g/dL] × 10) / Red Blood Cells [10^12/L]";
      case "MCHC":
        return "Mean Corpuscular Hemoglobin Concentration: (Hemoglobin [g/dL] × 100) / Hematocrit [%]";
      default:
        return null;
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between pr-4">
            <div className="flex items-center gap-2">
              <Sliders className="h-5 w-5 text-blue-600" />
              <DialogTitle className="text-base font-bold text-slate-900">
                {parameter.name}
              </DialogTitle>
            </div>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            Parameter Code: <span className="font-mono font-semibold text-slate-700">{parameter.code}</span>
            {" • "}
            Display Order: <span className="font-semibold text-slate-700">#{parameter.displayOrder}</span>
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 pt-1 text-sm text-slate-700">
          {/* Status & Types Row */}
          <div className="flex flex-wrap items-center gap-2">
            <TestStatusBadge status={parameter.status} />
            <ParameterDataTypeBadge dataType={parameter.dataType} />
            {parameter.inputType === "CALCULATED" ? (
              <Badge
                variant="outline"
                className="border-indigo-200 bg-indigo-50 text-indigo-700 font-mono text-xs flex items-center gap-1"
              >
                <Calculator className="h-3 w-3" />
                CALCULATED: {parameter.calculationType}
              </Badge>
            ) : (
              <Badge variant="outline" className="border-slate-200 text-slate-600 text-xs">
                MANUAL ENTRY
              </Badge>
            )}
            {parameter.required ? (
              <Badge className="bg-emerald-100 text-emerald-800 text-xs font-semibold">
                Required
              </Badge>
            ) : (
              <Badge variant="secondary" className="text-xs text-slate-500">
                Optional
              </Badge>
            )}
          </div>

          {/* Description */}
          {parameter.description && (
            <div className="rounded-md bg-slate-50 p-2.5 text-xs text-slate-600 border border-slate-100">
              <span className="font-semibold text-slate-700 block mb-0.5">Description:</span>
              {parameter.description}
            </div>
          )}

          {/* Calculation Formula Details */}
          {parameter.inputType === "CALCULATED" && parameter.calculationType !== "NONE" && (
            <div className="rounded-md bg-indigo-50/70 border border-indigo-100 p-3 text-xs text-indigo-900">
              <div className="flex items-center gap-1.5 font-semibold text-indigo-800 mb-1">
                <Calculator className="h-3.5 w-3.5" />
                Calculation Formula
              </div>
              <p className="font-mono font-medium">{parameter.calculationType}</p>
              <p className="mt-1 text-indigo-700 text-[11px]">
                {getCalculationExplanation(parameter.calculationType)}
              </p>
            </div>
          )}

          {/* Clinical Reference & Critical Ranges */}
          <div className="rounded-lg border border-slate-200 p-3 space-y-3 bg-white">
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Reference & Clinical Limits
            </div>
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="rounded bg-slate-50 p-2 border border-slate-100">
                <span className="text-slate-400 block text-[11px]">Measurement Unit:</span>
                <span className="font-mono font-semibold text-slate-800 text-sm">
                  {parameter.unit || "None / Ratio"}
                </span>
              </div>
              <div className="rounded bg-slate-50 p-2 border border-slate-100">
                <span className="text-slate-400 block text-[11px]">Normal Reference Range:</span>
                <span className="font-semibold text-slate-800 text-sm">
                  {parameter.referenceMin != null || parameter.referenceMax != null
                    ? `${parameter.referenceMin ?? 0} – ${parameter.referenceMax ?? "∞"} ${parameter.unit || ""}`
                    : "Not specified"}
                </span>
              </div>
            </div>

            {(parameter.criticalLow != null || parameter.criticalHigh != null) && (
              <div className="rounded-md bg-amber-50/60 border border-amber-200 p-2.5 text-xs text-amber-900">
                <div className="flex items-center gap-1.5 font-semibold text-amber-800 mb-1">
                  <AlertTriangle className="h-3.5 w-3.5 text-amber-600" />
                  Critical / Panic Limits
                </div>
                <div className="grid grid-cols-2 gap-2 text-[11px]">
                  <div>
                    <span className="text-amber-700">Critical Low:</span>{" "}
                    <span className="font-mono font-bold text-amber-900">
                      {parameter.criticalLow != null ? `${parameter.criticalLow} ${parameter.unit || ""}` : "None"}
                    </span>
                  </div>
                  <div>
                    <span className="text-amber-700">Critical High:</span>{" "}
                    <span className="font-mono font-bold text-amber-900">
                      {parameter.criticalHigh != null ? `${parameter.criticalHigh} ${parameter.unit || ""}` : "None"}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Reporting Description & Guidance */}
          {(parameter.reportDescription || parameter.interpretationGuidance) && (
            <div className="space-y-2 text-xs">
              {parameter.reportDescription && (
                <div>
                  <span className="font-semibold text-slate-700 block mb-0.5">
                    Patient Report Text:
                  </span>
                  <p className="rounded bg-slate-50 p-2 text-slate-600 border border-slate-100 whitespace-pre-wrap">
                    {parameter.reportDescription}
                  </p>
                </div>
              )}

              {parameter.interpretationGuidance && (
                <div>
                  <span className="font-semibold text-slate-700 block mb-0.5">
                    Clinical Interpretation Guidance:
                  </span>
                  <p className="rounded bg-slate-50 p-2 text-slate-600 border border-slate-100 whitespace-pre-wrap">
                    {parameter.interpretationGuidance}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Metadata Footer */}
          <div className="border-t border-slate-100 pt-2 flex items-center justify-between text-[11px] text-slate-400">
            <span>Ref: <span className="font-mono text-slate-500">{parameter.refId}</span></span>
            <span>Version: {parameter.version}</span>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          {testRefId && (
            <Button
              asChild
              variant="outline"
              size="sm"
              onClick={() => onOpenChange(false)}
            >
              <Link to={`/super-admin/tests/${testRefId}/parameters/${parameter.refId}/edit`}>
                <Edit className="mr-1.5 h-3.5 w-3.5" />
                Edit Parameter
              </Link>
            </Button>
          )}
          <Button
            type="button"
            size="sm"
            onClick={() => onOpenChange(false)}
            className="bg-slate-900 text-white hover:bg-slate-800"
          >
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
