import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  ArrowLeft,
  Calculator,
  FileSpreadsheet,
  Gauge,
  Loader2,
  Save,
  Sliders,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import type { ApiErrorResponse } from "@/core/auth/authTypes";

import { useTestQuery } from "../hooks/useTests";
import { useCreateParameterMutation } from "../hooks/useParameters";
import { ParameterUnitSelect } from "../components/ParameterUnitSelect";
import type {
  CalculationType,
  ParameterInputType,
  TestParameterDataType,
} from "../types/parameterTypes";

const createParameterSchema = z
  .object({
    code: z
      .string()
      .trim()
      .min(1, "Parameter code is required")
      .max(50, "Parameter code must not exceed 50 characters"),
    name: z
      .string()
      .trim()
      .min(1, "Parameter name is required")
      .max(150, "Parameter name must not exceed 150 characters"),
    description: z
      .string()
      .trim()
      .max(500, "Description must not exceed 500 characters")
      .optional()
      .or(z.literal("")),
    dataType: z.enum(
      ["INTEGER", "DECIMAL", "TEXT", "BOOLEAN", "DATE", "DATETIME", "ENUM"],
      { message: "Data type is required" },
    ),
    inputType: z.enum(["MANUAL", "CALCULATED"], {
      message: "Input type is required",
    }),
    calculationType: z.enum(["NONE", "MCV", "MCH", "MCHC"]).default("NONE"),
    unit: z
      .string()
      .trim()
      .max(50, "Unit must not exceed 50 characters")
      .optional()
      .or(z.literal("")),
    required: z.boolean().default(true),
    displayOrder: z
      .preprocess(
        (val) => (val === "" || val == null ? 1 : Number(val)),
        z.number().int().min(1, "Display order must be at least 1"),
      ),

    // Ranges (numeric only)
    referenceMin: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().min(0, "Reference minimum cannot be negative").optional(),
      ),
    referenceMax: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().min(0, "Reference maximum cannot be negative").optional(),
      ),
    criticalLow: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().min(0, "Critical low cannot be negative").optional(),
      ),
    criticalHigh: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().min(0, "Critical high cannot be negative").optional(),
      ),

    // Reporting
    reportDescription: z
      .string()
      .trim()
      .max(500, "Report description must not exceed 500 characters")
      .optional()
      .or(z.literal("")),
    interpretationGuidance: z
      .string()
      .trim()
      .optional()
      .or(z.literal("")),
  })
  .superRefine((data, ctx) => {
    const isNumeric = data.dataType === "INTEGER" || data.dataType === "DECIMAL";

    if (data.inputType === "CALCULATED") {
      if (!isNumeric) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["dataType"],
          message: "Calculated parameter must have numeric data type (INTEGER or DECIMAL)",
        });
      }
      if (data.calculationType === "NONE") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["calculationType"],
          message: "Please select a clinical calculation formula (MCV, MCH, or MCHC)",
        });
      }
    } else {
      if (data.calculationType !== "NONE") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["calculationType"],
          message: "Manual parameter must have calculation type NONE",
        });
      }
    }

    if (!isNumeric) {
      if (
        data.referenceMin != null ||
        data.referenceMax != null ||
        data.criticalLow != null ||
        data.criticalHigh != null
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["referenceMin"],
          message: "Ranges are allowed only for INTEGER or DECIMAL parameters",
        });
      }
    } else {
      if (
        data.referenceMin != null &&
        data.referenceMax != null &&
        data.referenceMin > data.referenceMax
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["referenceMin"],
          message: "Reference min cannot exceed reference max",
        });
      }
      if (
        data.criticalLow != null &&
        data.criticalHigh != null &&
        data.criticalLow > data.criticalHigh
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["criticalLow"],
          message: "Critical low cannot exceed critical high",
        });
      }
    }
  });

type CreateParameterFormValues = z.infer<typeof createParameterSchema>;

export function CreateParameterPage() {
  const { refId: testRefId } = useParams<{ refId: string }>();
  const navigate = useNavigate();
  const [generalError, setGeneralError] = useState<string | null>(null);

  const { data: test, isLoading: testLoading, isError: testError } = useTestQuery(testRefId);
  const createMutation = useCreateParameterMutation(testRefId || "");

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateParameterFormValues>({
    resolver: zodResolver(createParameterSchema) as any,
    defaultValues: {
      code: "",
      name: "",
      description: "",
      dataType: "DECIMAL",
      inputType: "MANUAL",
      calculationType: "NONE",
      unit: "",
      required: true,
      displayOrder: 1,
      referenceMin: undefined,
      referenceMax: undefined,
      criticalLow: undefined,
      criticalHigh: undefined,
      reportDescription: "",
      interpretationGuidance: "",
    },
    mode: "onBlur",
  });

  const selectedInputType = watch("inputType");
  const selectedDataType = watch("dataType");
  const isNumeric = selectedDataType === "INTEGER" || selectedDataType === "DECIMAL";

  if (testLoading) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (testError || !test) {
    return (
      <div className="mx-auto max-w-4xl space-y-4">
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Parent Test Not Found</AlertTitle>
          <AlertDescription>
            The parent test could not be loaded. Please return to the test catalog.
          </AlertDescription>
        </Alert>
        <Button variant="outline" onClick={() => navigate("/super-admin/tests")}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Tests
        </Button>
      </div>
    );
  }

  const onSubmit = async (values: CreateParameterFormValues) => {
    setGeneralError(null);
    try {
      await createMutation.mutateAsync({
        code: values.code.trim().toUpperCase(),
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
        dataType: values.dataType as TestParameterDataType,
        inputType: values.inputType as ParameterInputType,
        calculationType:
          values.inputType === "CALCULATED"
            ? (values.calculationType as CalculationType)
            : "NONE",
        unit: values.unit?.trim() || undefined,
        required: values.required,
        displayOrder: Number(values.displayOrder),
        referenceMin:
          isNumeric && values.referenceMin != null ? Number(values.referenceMin) : undefined,
        referenceMax:
          isNumeric && values.referenceMax != null ? Number(values.referenceMax) : undefined,
        criticalLow:
          isNumeric && values.criticalLow != null ? Number(values.criticalLow) : undefined,
        criticalHigh:
          isNumeric && values.criticalHigh != null ? Number(values.criticalHigh) : undefined,
        reportDescription: values.reportDescription?.trim() || undefined,
        interpretationGuidance: values.interpretationGuidance?.trim() || undefined,
      });

      navigate(`/super-admin/tests/${test.refId}`);
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError) {
          if (apiError.code === "RESOURCE_ALREADY_EXISTS" || apiError.message.includes("already exists")) {
            setError("code", { message: apiError.message });
            return;
          }
          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              setError(field as any, { message: msg });
            });
            return;
          }
          setGeneralError(apiError.message || "Failed to create parameter.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while creating the parameter.");
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6 pb-12">
      {/* Back button */}
      <div className="flex items-center gap-3">
        <Link
          to={`/super-admin/tests/${test.refId}`}
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Add Test Parameter
          </h1>
          <p className="text-sm text-slate-500">
            Test: <span className="font-semibold text-slate-800">{test.name}</span> (
            <span className="font-mono text-xs">{test.code}</span>)
          </p>
        </div>
      </div>

      {generalError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Cannot Create Parameter</AlertTitle>
          <AlertDescription className="mt-1">{generalError}</AlertDescription>
        </Alert>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Section 1: Parameter Information */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Sliders className="h-5 w-5 text-blue-600" />
              <CardTitle className="text-base font-semibold">
                Parameter Identification
              </CardTitle>
            </div>
            <CardDescription>
              Code, name, and ordering within the diagnostic report.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="code" className="text-xs font-semibold">
                  Parameter Code <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="code"
                  placeholder="e.g. HGB"
                  className="font-mono uppercase"
                  {...register("code")}
                />
                {errors.code && (
                  <p className="text-xs text-red-500">{errors.code.message}</p>
                )}
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="name" className="text-xs font-semibold">
                  Parameter Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="name"
                  placeholder="e.g. Hemoglobin"
                  {...register("name")}
                />
                {errors.name && (
                  <p className="text-xs text-red-500">{errors.name.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="displayOrder" className="text-xs font-semibold">
                  Display Order <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="displayOrder"
                  type="number"
                  placeholder="1"
                  {...register("displayOrder")}
                />
                {errors.displayOrder && (
                  <p className="text-xs text-red-500">
                    {errors.displayOrder.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5 sm:col-span-2 flex flex-col justify-center pt-3">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    {...register("required")}
                  />
                  <span className="text-sm font-medium text-slate-700">
                    Mandatory parameter (required for report validation)
                  </span>
                </label>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="description" className="text-xs font-semibold">
                Parameter Description
              </Label>
              <Textarea
                id="description"
                placeholder="Diagnostic definition and clinical meaning..."
                rows={2}
                {...register("description")}
              />
              {errors.description && (
                <p className="text-xs text-red-500">
                  {errors.description.message}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Section 2: Data & Calculation Configuration */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Calculator className="h-5 w-5 text-indigo-600" />
              <CardTitle className="text-base font-semibold">
                Data Type & Calculation Configuration
              </CardTitle>
            </div>
            <CardDescription>
              Specify whether this parameter is entered manually or calculated by the clinical engine.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="dataType" className="text-xs font-semibold">
                  Data Type <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="dataType"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger id="dataType">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="DECIMAL">Decimal Number</SelectItem>
                        <SelectItem value="INTEGER">Integer Number</SelectItem>
                        <SelectItem value="TEXT">Text String</SelectItem>
                        <SelectItem value="BOOLEAN">Boolean (Positive/Negative)</SelectItem>
                        <SelectItem value="DATE">Date</SelectItem>
                        <SelectItem value="DATETIME">Date & Time</SelectItem>
                        <SelectItem value="ENUM">Enumerated Values</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.dataType && (
                  <p className="text-xs text-red-500">
                    {errors.dataType.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="inputType" className="text-xs font-semibold">
                  Input Mode <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="inputType"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={(val) => {
                        field.onChange(val);
                        if (val === "MANUAL") {
                          setValue("calculationType", "NONE");
                        } else if (val === "CALCULATED") {
                          setValue("calculationType", "MCV");
                          if (!isNumeric) {
                            setValue("dataType", "DECIMAL");
                          }
                        }
                      }}
                    >
                      <SelectTrigger id="inputType">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="MANUAL">Manual Input</SelectItem>
                        <SelectItem value="CALCULATED">Calculated by Backend</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.inputType && (
                  <p className="text-xs text-red-500">
                    {errors.inputType.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-1.5 pt-2 border-t border-slate-100">
              <Label htmlFor="unit" className="text-xs font-semibold">
                Measurement Unit
              </Label>
              <Controller
                name="unit"
                control={control}
                render={({ field }) => (
                  <ParameterUnitSelect
                    id="unit"
                    value={field.value}
                    onChange={field.onChange}
                    error={errors.unit?.message}
                  />
                )}
              />
            </div>

            {/* Conditionally exposed calculation type */}
            {selectedInputType === "CALCULATED" && (
              <div className="rounded-lg border border-indigo-100 bg-indigo-50/50 p-4 space-y-2">
                <Label htmlFor="calculationType" className="text-xs font-semibold text-indigo-900">
                  Clinical Calculation Formula <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="calculationType"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger id="calculationType" className="bg-white">
                        <SelectValue placeholder="Select calculation..." />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="MCV">
                          MCV — Mean Corpuscular Volume (HCT × 10 / RBC)
                        </SelectItem>
                        <SelectItem value="MCH">
                          MCH — Mean Corpuscular Hemoglobin (HGB × 10 / RBC)
                        </SelectItem>
                        <SelectItem value="MCHC">
                          MCHC — Mean Corpuscular Hemoglobin Concentration (HGB × 100 / HCT)
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                <p className="text-xs text-indigo-600">
                  Calculations are computed deterministically by the Spring Boot backend calculation engine.
                </p>
                {errors.calculationType && (
                  <p className="text-xs text-red-500">
                    {errors.calculationType.message}
                  </p>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Section 3: Reference & Critical Ranges */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Gauge className="h-5 w-5 text-emerald-600" />
              <CardTitle className="text-base font-semibold">
                Biological Reference Ranges & Critical Limits
              </CardTitle>
            </div>
            <CardDescription>
              {isNumeric
                ? "Configure normal biological benchmarks and clinical panic limits."
                : "Reference ranges apply only to numeric (DECIMAL or INTEGER) parameters."}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {!isNumeric ? (
              <p className="text-xs text-slate-500 italic">
                Non-numeric parameters do not take numerical minimum and maximum bounds.
              </p>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
                <div className="space-y-1.5">
                  <Label htmlFor="referenceMin" className="text-xs font-semibold">
                    Reference Min
                  </Label>
                  <Input
                    id="referenceMin"
                    type="number"
                    step="0.01"
                    placeholder="e.g. 12.0"
                    {...register("referenceMin")}
                  />
                  {errors.referenceMin && (
                    <p className="text-xs text-red-500">
                      {errors.referenceMin.message}
                    </p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="referenceMax" className="text-xs font-semibold">
                    Reference Max
                  </Label>
                  <Input
                    id="referenceMax"
                    type="number"
                    step="0.01"
                    placeholder="e.g. 16.0"
                    {...register("referenceMax")}
                  />
                  {errors.referenceMax && (
                    <p className="text-xs text-red-500">
                      {errors.referenceMax.message}
                    </p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="criticalLow" className="text-xs font-semibold text-rose-700">
                    Critical Low (Panic)
                  </Label>
                  <Input
                    id="criticalLow"
                    type="number"
                    step="0.01"
                    placeholder="e.g. 7.0"
                    {...register("criticalLow")}
                  />
                  {errors.criticalLow && (
                    <p className="text-xs text-red-500">
                      {errors.criticalLow.message}
                    </p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="criticalHigh" className="text-xs font-semibold text-rose-700">
                    Critical High (Panic)
                  </Label>
                  <Input
                    id="criticalHigh"
                    type="number"
                    step="0.01"
                    placeholder="e.g. 20.0"
                    {...register("criticalHigh")}
                  />
                  {errors.criticalHigh && (
                    <p className="text-xs text-red-500">
                      {errors.criticalHigh.message}
                    </p>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Section 4: Reporting & Guidance */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FileSpreadsheet className="h-5 w-5 text-amber-600" />
              <CardTitle className="text-base font-semibold">
                Reporting & Guidance
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="reportDescription" className="text-xs font-semibold">
                Report Description (Printed on Report)
              </Label>
              <Textarea
                id="reportDescription"
                placeholder="Printed notes for this specific analyte..."
                rows={2}
                {...register("reportDescription")}
              />
              {errors.reportDescription && (
                <p className="text-xs text-red-500">
                  {errors.reportDescription.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="interpretationGuidance" className="text-xs font-semibold">
                Interpretation Guidance
              </Label>
              <Textarea
                id="interpretationGuidance"
                placeholder="Clinical interpretation guidance for staff..."
                rows={2}
                {...register("interpretationGuidance")}
              />
              {errors.interpretationGuidance && (
                <p className="text-xs text-red-500">
                  {errors.interpretationGuidance.message}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Actions */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(`/super-admin/tests/${test.refId}`)}
            disabled={isSubmitting || createMutation.isPending}
          >
            Cancel
          </Button>

          <Button
            type="submit"
            disabled={isSubmitting || createMutation.isPending}
            className="bg-blue-600 hover:bg-blue-700 text-white min-w-32"
          >
            {isSubmitting || createMutation.isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Creating...
              </>
            ) : (
              <>
                <Save className="mr-2 h-4 w-4" />
                Create Parameter
              </>
            )}
          </Button>
        </div>
      </form>
    </div>
  );
}

export default CreateParameterPage;
