import * as React from "react";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Sparkles, PenLine, ListFilter, X } from "lucide-react";

export interface LabUnitOption {
  value: string;
  label: string;
  description: string;
}

export interface LabUnitCategory {
  category: string;
  items: LabUnitOption[];
}

export const LAB_UNIT_CATEGORIES: LabUnitCategory[] = [
  {
    category: "Mass / Concentration (Serum, Plasma, Blood)",
    items: [
      { value: "mg/dL", label: "mg/dL", description: "Milligram per deciliter (Glucose, Cholesterol, Creatinine, Bilirubin)" },
      { value: "g/dL", label: "g/dL", description: "Gram per deciliter (Hemoglobin, Total Protein, Albumin)" },
      { value: "µg/dL", label: "µg/dL", description: "Microgram per deciliter (Cortisol, Iron, Lead)" },
      { value: "ng/dL", label: "ng/dL", description: "Nanogram per deciliter (Total Testosterone)" },
      { value: "ng/mL", label: "ng/mL", description: "Nanogram per milliliter (Ferritin, PSA, 25-OH Vit D)" },
      { value: "pg/mL", label: "pg/mL", description: "Picogram per milliliter (Vit B12, Free T3/T4, Estradiol)" },
      { value: "µg/mL", label: "µg/mL", description: "Microgram per milliliter (Therapeutic Drug Monitoring)" },
      { value: "mg/L", label: "mg/L", description: "Milligram per liter (hs-CRP, C-Reactive Protein)" },
      { value: "g/L", label: "g/L", description: "Gram per liter (Immunoglobulins, SI Total Protein)" },
      { value: "µg/L", label: "µg/L", description: "Microgram per liter (Trace metals)" },
    ],
  },
  {
    category: "Molar Concentration & Electrolytes (SI Units)",
    items: [
      { value: "mmol/L", label: "mmol/L", description: "Millimole per liter (Sodium, Potassium, Chloride, Glucose SI)" },
      { value: "µmol/L", label: "µmol/L", description: "Micromole per liter (Creatinine SI, Bilirubin SI, Uric Acid)" },
      { value: "nmol/L", label: "nmol/L", description: "Nanomole per liter (25-OH Vit D SI, Testosterone SI)" },
      { value: "pmol/L", label: "pmol/L", description: "Picomole per liter (Free T3/T4 SI, Insulin SI)" },
      { value: "mEq/L", label: "mEq/L", description: "Milliequivalent per liter (Electrolytes, Anion Gap)" },
      { value: "mOsm/kg", label: "mOsm/kg", description: "Milliosmoles per kg (Serum / Urine Osmolality)" },
    ],
  },
  {
    category: "Enzymatic Activity & Hormones",
    items: [
      { value: "U/L", label: "U/L", description: "Units per liter (ALT/SGPT, AST/SGOT, ALP, GGT, Amylase, LDH)" },
      { value: "IU/L", label: "IU/L", description: "International Units per liter (Lipase, CK-MB)" },
      { value: "mIU/L", label: "mIU/L", description: "Milli-IU per liter (LH, FSH)" },
      { value: "µIU/mL", label: "µIU/mL", description: "Micro-IU per milliliter (TSH, Insulin)" },
      { value: "IU/mL", label: "IU/mL", description: "International Units per mL (Antibody titers, Hepatitis)" },
      { value: "mIU/mL", label: "mIU/mL", description: "Milli-IU per mL (Beta-hCG, Prolactin)" },
    ],
  },
  {
    category: "Hematology & Cell Counts",
    items: [
      { value: "10^3/µL", label: "10^3/µL", description: "Thousand per microliter (WBC count, Platelets)" },
      { value: "10^6/µL", label: "10^6/µL", description: "Million per microliter (RBC count)" },
      { value: "cells/µL", label: "cells/µL", description: "Cells per microliter (Absolute Neutrophils, Lymphocytes)" },
      { value: "/mm³", label: "/mm³", description: "Per cubic millimeter (Total WBC, Platelets)" },
      { value: "fL", label: "fL", description: "Femtoliter (Mean Corpuscular Volume - MCV, MPV)" },
      { value: "pg", label: "pg", description: "Picogram (Mean Corpuscular Hemoglobin - MCH)" },
      { value: "mm/hr", label: "mm/hr", description: "Millimeters per hour (ESR - Erythrocyte Sedimentation Rate)" },
      { value: "%", label: "%", description: "Percentage (Differential Count, Hematocrit, HbA1c)" },
    ],
  },
  {
    category: "Urinalysis, Microscopy & Molecular",
    items: [
      { value: "/hpf", label: "/hpf", description: "Per high power field (Urine RBCs, WBCs, Epithelial cells)" },
      { value: "/lpf", label: "/lpf", description: "Per low power field (Urine Casts, Crystals)" },
      { value: "copies/mL", label: "copies/mL", description: "Copies per mL (Viral load: HIV, HBV, HCV)" },
      { value: "log copies/mL", label: "log copies/mL", description: "Logarithmic viral load" },
      { value: "Index", label: "Index", description: "Sample-to-Cutoff Index (ELISA serology)" },
      { value: "ratio", label: "ratio", description: "Ratio (e.g. Albumin/Creatinine, BUN/Creatinine)" },
      { value: "pH units", label: "pH units", description: "pH Scale (Urine / Blood pH)" },
    ],
  },
  {
    category: "Coagulation, Clearance & Time",
    items: [
      { value: "seconds", label: "seconds", description: "Seconds (Prothrombin Time PT, aPTT, Thrombin Time)" },
      { value: "INR", label: "INR", description: "International Normalized Ratio (PT/INR)" },
      { value: "minutes", label: "minutes", description: "Bleeding time, Clotting time" },
      { value: "hours", label: "hours", description: "Timed collections (24h urine)" },
      { value: "mL/min", label: "mL/min", description: "Clearance rate (Creatinine Clearance)" },
      { value: "mL/min/1.73m²", label: "mL/min/1.73m²", description: "Estimated GFR (eGFR CKD-EPI)" },
    ],
  },
];

// Flat list of all known preset values for rapid lookup
const ALL_PRESET_VALUES = new Set<string>(
  LAB_UNIT_CATEGORIES.flatMap((c) => c.items.map((i) => i.value)),
);

// Top 8 most commonly used laboratory units for quick-click chips
const POPULAR_UNITS = [
  "mg/dL",
  "g/dL",
  "mmol/L",
  "U/L",
  "10^3/µL",
  "%",
  "µIU/mL",
  "seconds",
];

const NONE_VALUE = "__NONE__";
const CUSTOM_VALUE = "__CUSTOM__";

interface ParameterUnitSelectProps {
  id?: string;
  value?: string | null;
  onChange: (val: string) => void;
  error?: string;
  disabled?: boolean;
}

export function ParameterUnitSelect({
  id = "unit",
  value = "",
  onChange,
  error,
  disabled = false,
}: ParameterUnitSelectProps) {
  const normalizedValue = value ?? "";
  const isKnownPreset = ALL_PRESET_VALUES.has(normalizedValue);
  const isEmpty = normalizedValue.trim() === "";

  // Track if user explicitly selected "Custom" mode
  const [isCustomMode, setIsCustomMode] = React.useState<boolean>(
    !isEmpty && !isKnownPreset,
  );

  // Sync custom mode if incoming external value is not in presets and non-empty
  React.useEffect(() => {
    if (!isEmpty && !isKnownPreset) {
      setIsCustomMode(true);
    } else if (isEmpty || isKnownPreset) {
      setIsCustomMode(false);
    }
  }, [normalizedValue, isEmpty, isKnownPreset]);

  // Determine current select value
  const selectValue = React.useMemo(() => {
    if (isCustomMode) return CUSTOM_VALUE;
    if (isEmpty) return NONE_VALUE;
    if (isKnownPreset) return normalizedValue;
    return CUSTOM_VALUE;
  }, [isCustomMode, isEmpty, isKnownPreset, normalizedValue]);

  const handleSelectChange = (newVal: string) => {
    if (newVal === NONE_VALUE) {
      setIsCustomMode(false);
      onChange("");
    } else if (newVal === CUSTOM_VALUE) {
      setIsCustomMode(true);
      // Keep existing custom value or clear if it was a preset
      if (isKnownPreset) {
        onChange("");
      }
    } else {
      setIsCustomMode(false);
      onChange(newVal);
    }
  };

  const handleQuickSelect = (unitVal: string) => {
    if (disabled) return;
    setIsCustomMode(false);
    onChange(unitVal);
  };

  return (
    <div className="space-y-2">
      {/* Quick popular unit badges */}
      <div className="flex flex-wrap items-center gap-1.5">
        <span className="text-[11px] font-medium text-slate-500 flex items-center gap-1">
          <Sparkles className="h-3 w-3 text-amber-500" />
          Quick:
        </span>
        {POPULAR_UNITS.map((unit) => {
          const isSelected = normalizedValue === unit;
          return (
            <Badge
              key={unit}
              variant={isSelected ? "default" : "outline"}
              className={`cursor-pointer text-[11px] px-2 py-0.5 transition-all select-none ${
                isSelected
                  ? "bg-slate-900 text-white hover:bg-slate-800 font-semibold shadow-xs"
                  : "bg-slate-50 hover:bg-slate-100 hover:border-slate-400 text-slate-700"
              }`}
              onClick={() => handleQuickSelect(unit)}
            >
              {unit}
            </Badge>
          );
        })}
        <Badge
          variant={isEmpty && !isCustomMode ? "default" : "outline"}
          className={`cursor-pointer text-[11px] px-2 py-0.5 transition-all select-none ${
            isEmpty && !isCustomMode
              ? "bg-slate-700 text-white hover:bg-slate-800 font-semibold"
              : "bg-slate-50 hover:bg-slate-100 text-slate-500"
          }`}
          onClick={() => {
            if (disabled) return;
            setIsCustomMode(false);
            onChange("");
          }}
        >
          No unit
        </Badge>
      </div>

      {/* Main Selector & Custom Input */}
      {!isCustomMode ? (
        <div className="flex items-center gap-2">
          <div className="flex-1">
            <Select
              value={selectValue}
              onValueChange={handleSelectChange}
              disabled={disabled}
            >
              <SelectTrigger id={id} className="bg-white">
                <SelectValue placeholder="Select clinical unit...">
                  {isEmpty ? (
                    <span className="text-slate-400 font-normal">
                      No Unit (Qualitative / Descriptive)
                    </span>
                  ) : (
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-900">
                        {normalizedValue}
                      </span>
                    </div>
                  )}
                </SelectValue>
              </SelectTrigger>
              <SelectContent className="max-h-80">
                <SelectItem value={NONE_VALUE} className="text-slate-600 font-medium">
                  None (No unit / Qualitative / Ratio)
                </SelectItem>
                <SelectItem
                  value={CUSTOM_VALUE}
                  className="text-indigo-600 font-medium hover:text-indigo-700"
                >
                  <span className="flex items-center gap-1.5">
                    <PenLine className="h-3.5 w-3.5" />
                    Custom unit (Type manually)...
                  </span>
                </SelectItem>

                {LAB_UNIT_CATEGORIES.map((cat) => (
                  <React.Fragment key={cat.category}>
                    <SelectSeparator />
                    <SelectGroup>
                      <SelectLabel className="text-xs font-bold text-slate-800 tracking-wide">
                        {cat.category}
                      </SelectLabel>
                      {cat.items.map((item) => (
                        <SelectItem key={item.value} value={item.value}>
                          <div className="flex items-center gap-2 text-left">
                            <span className="font-semibold text-slate-900 w-16 shrink-0">
                              {item.label}
                            </span>
                            <span className="text-xs text-slate-500 truncate max-w-[280px]">
                              {item.description}
                            </span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  </React.Fragment>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            className="shrink-0 text-xs h-9 px-2.5 text-slate-600 hover:text-slate-900"
            onClick={() => {
              setIsCustomMode(true);
            }}
            disabled={disabled}
            title="Enter custom unit"
          >
            <PenLine className="h-3.5 w-3.5 mr-1" />
            Custom
          </Button>
        </div>
      ) : (
        <div className="space-y-1.5">
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <Input
                id={id}
                type="text"
                maxLength={50}
                placeholder="Enter custom lab unit (e.g. CFU/mL, mg/24h, OD)"
                value={normalizedValue}
                onChange={(e) => onChange(e.target.value)}
                disabled={disabled}
                autoFocus
                className="bg-white pr-8"
              />
              {normalizedValue && (
                <button
                  type="button"
                  onClick={() => onChange("")}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  tabIndex={-1}
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="shrink-0 text-xs h-9 px-2.5 text-indigo-600 hover:text-indigo-700 bg-indigo-50/50 hover:bg-indigo-100/50 border-indigo-200"
              onClick={() => {
                setIsCustomMode(false);
              }}
              disabled={disabled}
            >
              <ListFilter className="h-3.5 w-3.5 mr-1" />
              Presets
            </Button>
          </div>
          <p className="text-[11px] text-slate-500">
            Typing custom unit. Click <strong>Presets</strong> to switch back to standard clinical dropdown.
          </p>
        </div>
      )}

      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}
