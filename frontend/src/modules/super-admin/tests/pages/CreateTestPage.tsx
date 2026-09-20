import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  ArrowLeft,
  Calendar,
  Clock,
  Coins,
  FileSpreadsheet,
  FlaskConical,
  Layers,
  Loader2,
  Save,
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
import type { ApiErrorResponse } from "@/core/auth/authTypes";

import { useCreateTestMutation } from "../hooks/useTests";
import { useActiveCategoriesQuery } from "../hooks/useCategories";
import type { SampleType, TestType } from "../types/testTypes";

const createTestSchema = z
  .object({
    code: z
      .string()
      .trim()
      .min(1, "Test code is required")
      .max(50, "Test code must not exceed 50 characters"),
    name: z
      .string()
      .trim()
      .min(1, "Test name is required")
      .max(150, "Test name must not exceed 150 characters"),
    shortName: z
      .string()
      .trim()
      .max(75, "Short name must not exceed 75 characters")
      .optional()
      .or(z.literal("")),
    categoryRefId: z
      .string()
      .min(1, "Test category is required"),
    testType: z.enum(["INDIVIDUAL", "PANEL", "PROFILE"], {
      message: "Test type is required",
    }),
    description: z
      .string()
      .trim()
      .max(1000, "Description must not exceed 1000 characters")
      .optional()
      .or(z.literal("")),

    // Sample
    sampleType: z.enum(
      [
        "WHOLE_BLOOD",
        "SERUM",
        "PLASMA",
        "URINE",
        "STOOL",
        "CSF",
        "SWAB",
        "SEMEN",
        "SALIVA",
        "OTHER",
      ],
      { message: "Sample type is required" },
    ),
    customSampleType: z
      .string()
      .trim()
      .max(100, "Custom sample type must not exceed 100 characters")
      .optional()
      .or(z.literal("")),
    specimenContainer: z
      .string()
      .trim()
      .max(150, "Specimen container must not exceed 150 characters")
      .optional()
      .or(z.literal("")),
    sampleVolume: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().positive("Volume must be greater than 0").optional(),
      ),
    sampleVolumeUnit: z
      .string()
      .trim()
      .max(20, "Sample volume unit must not exceed 20 characters")
      .optional()
      .or(z.literal("")),
    fastingRequired: z.boolean().default(false),
    patientPreparation: z
      .string()
      .trim()
      .max(1000, "Patient preparation must not exceed 1000 characters")
      .optional()
      .or(z.literal("")),
    collectionInstructions: z
      .string()
      .trim()
      .max(1500, "Collection instructions must not exceed 1500 characters")
      .optional()
      .or(z.literal("")),

    // Processing
    turnaroundTimeHours: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().int().min(1, "Turnaround time must be at least 1 hour").optional(),
      ),
    prioritySupported: z.boolean().default(false),
    outsourced: z.boolean().default(false),
    laboratoryInstructions: z
      .string()
      .trim()
      .max(1500, "Laboratory instructions must not exceed 1500 characters")
      .optional()
      .or(z.literal("")),

    // Reporting
    reportSection: z
      .string()
      .trim()
      .max(100, "Report section must not exceed 100 characters")
      .optional()
      .or(z.literal("")),
    displayOrder: z
      .preprocess(
        (val) => (val === "" || val == null ? 0 : Number(val)),
        z.number().int().min(0, "Display order cannot be negative").default(0),
      ),
    reportDescription: z
      .string()
      .trim()
      .max(1000, "Report description must not exceed 1000 characters")
      .optional()
      .or(z.literal("")),
    interpretationGuidance: z
      .string()
      .trim()
      .max(2000, "Interpretation guidance must not exceed 2000 characters")
      .optional()
      .or(z.literal("")),

    // Commercial
    basePrice: z
      .preprocess(
        (val) => (val === "" || val == null ? undefined : Number(val)),
        z.number().min(0, "Base price cannot be negative").optional(),
      ),
    currency: z
      .string()
      .trim()
      .length(3, "Currency must be 3-character ISO code")
      .default("INR"),
    billingCode: z
      .string()
      .trim()
      .max(50, "Billing code must not exceed 50 characters")
      .optional()
      .or(z.literal("")),

    // Dates
    effectiveFrom: z.string().optional().or(z.literal("")),
    effectiveUntil: z.string().optional().or(z.literal("")),
  })
  .superRefine((data, ctx) => {
    if (data.sampleType === "OTHER" && (!data.customSampleType || !data.customSampleType.trim())) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["customSampleType"],
        message: "Custom sample type is required when sample type is OTHER",
      });
    }
    if (data.sampleType !== "OTHER" && data.customSampleType && data.customSampleType.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["customSampleType"],
        message: "Custom sample type is allowed only when sample type is OTHER",
      });
    }
    if (data.effectiveFrom && data.effectiveUntil) {
      if (new Date(data.effectiveUntil) < new Date(data.effectiveFrom)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["effectiveUntil"],
          message: "Effective until date cannot be before effective from date",
        });
      }
    }
  });

type CreateTestFormValues = z.infer<typeof createTestSchema>;

export function CreateTestPage() {
  const navigate = useNavigate();
  const [generalError, setGeneralError] = useState<string | null>(null);

  const createMutation = useCreateTestMutation();
  const { data: categoriesData, isLoading: categoriesLoading } = useActiveCategoriesQuery();

  const {
    register,
    handleSubmit,
    control,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateTestFormValues>({
    resolver: zodResolver(createTestSchema) as any,
    defaultValues: {
      code: "",
      name: "",
      shortName: "",
      categoryRefId: "",
      testType: "INDIVIDUAL",
      description: "",
      sampleType: "SERUM",
      customSampleType: "",
      specimenContainer: "",
      sampleVolume: undefined,
      sampleVolumeUnit: "mL",
      fastingRequired: false,
      patientPreparation: "",
      collectionInstructions: "",
      turnaroundTimeHours: 24,
      prioritySupported: false,
      outsourced: false,
      laboratoryInstructions: "",
      reportSection: "",
      displayOrder: 0,
      reportDescription: "",
      interpretationGuidance: "",
      basePrice: undefined,
      currency: "INR",
      billingCode: "",
      effectiveFrom: "",
      effectiveUntil: "",
    },
    mode: "onBlur",
  });

  const selectedSampleType = watch("sampleType");

  const onSubmit = async (values: CreateTestFormValues) => {
    setGeneralError(null);
    try {
      const response = await createMutation.mutateAsync({
        code: values.code.trim().toUpperCase(),
        name: values.name.trim(),
        shortName: values.shortName?.trim() || undefined,
        categoryRefId: values.categoryRefId.trim(),
        testType: values.testType as TestType,
        description: values.description?.trim() || undefined,
        sampleType: values.sampleType as SampleType,
        customSampleType:
          values.sampleType === "OTHER"
            ? values.customSampleType?.trim() || undefined
            : undefined,
        specimenContainer: values.specimenContainer?.trim() || undefined,
        sampleVolume: values.sampleVolume != null ? Number(values.sampleVolume) : undefined,
        sampleVolumeUnit: values.sampleVolumeUnit?.trim() || undefined,
        fastingRequired: values.fastingRequired,
        patientPreparation: values.patientPreparation?.trim() || undefined,
        collectionInstructions: values.collectionInstructions?.trim() || undefined,
        turnaroundTimeHours:
          values.turnaroundTimeHours != null
            ? Number(values.turnaroundTimeHours)
            : undefined,
        prioritySupported: values.prioritySupported,
        outsourced: values.outsourced,
        laboratoryInstructions: values.laboratoryInstructions?.trim() || undefined,
        reportSection: values.reportSection?.trim() || undefined,
        displayOrder:
          values.displayOrder != null ? Number(values.displayOrder) : 0,
        reportDescription: values.reportDescription?.trim() || undefined,
        interpretationGuidance: values.interpretationGuidance?.trim() || undefined,
        basePrice: values.basePrice != null ? Number(values.basePrice) : undefined,
        currency: values.currency?.trim().toUpperCase() || "INR",
        billingCode: values.billingCode?.trim().toUpperCase() || undefined,
        effectiveFrom: values.effectiveFrom || undefined,
        effectiveUntil: values.effectiveUntil || undefined,
      });

      navigate(`/super-admin/tests/${response.refId}`);
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const apiError = error.response?.data;
        if (apiError) {
          if (apiError.code === "RESOURCE_ALREADY_EXISTS") {
            const msg = apiError.message.toLowerCase();
            if (msg.includes("code")) {
              setError("code", { message: apiError.message });
            } else if (msg.includes("name")) {
              setError("name", { message: apiError.message });
            } else {
              setGeneralError(apiError.message);
            }
            return;
          }

          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              setError(field as any, { message: msg });
            });
            return;
          }

          setGeneralError(apiError.message || "Failed to create test specification.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while creating the test.");
    }
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6 pb-12">
      {/* Back & Breadcrumb */}
      <div className="flex items-center gap-3">
        <Link
          to="/super-admin/tests"
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Create Master Test
          </h1>
          <p className="text-sm text-slate-500">
            Configure a new diagnostic test specification for the master catalog.
          </p>
        </div>
      </div>

      {generalError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Cannot Create Test</AlertTitle>
          <AlertDescription className="mt-1">{generalError}</AlertDescription>
        </Alert>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Section 1: Basic Information */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FlaskConical className="h-5 w-5 text-blue-600" />
              <CardTitle className="text-base font-semibold">
                Basic Information
              </CardTitle>
            </div>
            <CardDescription>
              Core identifiers, classification, and test category.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="code" className="text-xs font-semibold">
                  Test Code <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="code"
                  placeholder="e.g. CBC"
                  className="font-mono uppercase"
                  {...register("code")}
                />
                {errors.code && (
                  <p className="text-xs text-red-500">{errors.code.message}</p>
                )}
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="name" className="text-xs font-semibold">
                  Test Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="name"
                  placeholder="e.g. Complete Blood Count"
                  {...register("name")}
                />
                {errors.name && (
                  <p className="text-xs text-red-500">{errors.name.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="shortName" className="text-xs font-semibold">
                  Short Name / Abbreviation
                </Label>
                <Input
                  id="shortName"
                  placeholder="e.g. Hemogram"
                  {...register("shortName")}
                />
                {errors.shortName && (
                  <p className="text-xs text-red-500">
                    {errors.shortName.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="categoryRefId" className="text-xs font-semibold">
                  Category <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="categoryRefId"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      disabled={categoriesLoading}
                    >
                      <SelectTrigger id="categoryRefId">
                        <SelectValue placeholder="Select category..." />
                      </SelectTrigger>
                      <SelectContent>
                        {categoriesData?.content.map((cat) => (
                          <SelectItem key={cat.refId} value={cat.refId}>
                            {cat.name} ({cat.code})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.categoryRefId && (
                  <p className="text-xs text-red-500">
                    {errors.categoryRefId.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="testType" className="text-xs font-semibold">
                  Test Type <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="testType"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger id="testType">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="INDIVIDUAL">Individual Test</SelectItem>
                        <SelectItem value="PANEL">Panel (Multiple Parameters)</SelectItem>
                        <SelectItem value="PROFILE">Health Profile</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.testType && (
                  <p className="text-xs text-red-500">
                    {errors.testType.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="description" className="text-xs font-semibold">
                Clinical Description
              </Label>
              <Textarea
                id="description"
                placeholder="Clinical indications and description of this test..."
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

        {/* Section 2: Sample & Collection */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Layers className="h-5 w-5 text-indigo-600" />
              <CardTitle className="text-base font-semibold">
                Sample & Specimen Requirements
              </CardTitle>
            </div>
            <CardDescription>
              Specimen types, collection volumes, containers, and preparation.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="sampleType" className="text-xs font-semibold">
                  Sample Type <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="sampleType"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger id="sampleType">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="WHOLE_BLOOD">Whole Blood</SelectItem>
                        <SelectItem value="SERUM">Serum</SelectItem>
                        <SelectItem value="PLASMA">Plasma</SelectItem>
                        <SelectItem value="URINE">Urine</SelectItem>
                        <SelectItem value="STOOL">Stool</SelectItem>
                        <SelectItem value="CSF">CSF (Cerebrospinal Fluid)</SelectItem>
                        <SelectItem value="SWAB">Swab</SelectItem>
                        <SelectItem value="SEMEN">Semen</SelectItem>
                        <SelectItem value="SALIVA">Saliva</SelectItem>
                        <SelectItem value="OTHER">Other Specimen</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.sampleType && (
                  <p className="text-xs text-red-500">
                    {errors.sampleType.message}
                  </p>
                )}
              </div>

              {selectedSampleType === "OTHER" && (
                <div className="space-y-1.5">
                  <Label htmlFor="customSampleType" className="text-xs font-semibold">
                    Custom Sample Type <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="customSampleType"
                    placeholder="Specify specimen type"
                    {...register("customSampleType")}
                  />
                  {errors.customSampleType && (
                    <p className="text-xs text-red-500">
                      {errors.customSampleType.message}
                    </p>
                  )}
                </div>
              )}

              <div className="space-y-1.5">
                <Label htmlFor="specimenContainer" className="text-xs font-semibold">
                  Specimen Container
                </Label>
                <Input
                  id="specimenContainer"
                  placeholder="e.g. EDTA Lavender Top Tube"
                  {...register("specimenContainer")}
                />
                {errors.specimenContainer && (
                  <p className="text-xs text-red-500">
                    {errors.specimenContainer.message}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1.5">
                  <Label htmlFor="sampleVolume" className="text-xs font-semibold">
                    Volume
                  </Label>
                  <Input
                    id="sampleVolume"
                    type="number"
                    step="0.01"
                    placeholder="e.g. 3.0"
                    {...register("sampleVolume")}
                  />
                  {errors.sampleVolume && (
                    <p className="text-xs text-red-500">
                      {errors.sampleVolume.message}
                    </p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="sampleVolumeUnit" className="text-xs font-semibold">
                    Unit
                  </Label>
                  <Input
                    id="sampleVolumeUnit"
                    placeholder="mL"
                    {...register("sampleVolumeUnit")}
                  />
                  {errors.sampleVolumeUnit && (
                    <p className="text-xs text-red-500">
                      {errors.sampleVolumeUnit.message}
                    </p>
                  )}
                </div>
              </div>
            </div>

            <div className="pt-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                  {...register("fastingRequired")}
                />
                <span className="text-sm font-medium text-slate-700">
                  Fasting Required (patient must fast before collection)
                </span>
              </label>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="patientPreparation" className="text-xs font-semibold">
                  Patient Preparation Instructions
                </Label>
                <Textarea
                  id="patientPreparation"
                  placeholder="e.g. 8-12 hours overnight fasting required. Water permitted."
                  rows={2}
                  {...register("patientPreparation")}
                />
                {errors.patientPreparation && (
                  <p className="text-xs text-red-500">
                    {errors.patientPreparation.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="collectionInstructions" className="text-xs font-semibold">
                  Collection Instructions
                </Label>
                <Textarea
                  id="collectionInstructions"
                  placeholder="e.g. Invert tube gently 8-10 times immediately after collection."
                  rows={2}
                  {...register("collectionInstructions")}
                />
                {errors.collectionInstructions && (
                  <p className="text-xs text-red-500">
                    {errors.collectionInstructions.message}
                  </p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Section 3: Processing */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-amber-600" />
              <CardTitle className="text-base font-semibold">
                Processing & Laboratory Specifications
              </CardTitle>
            </div>
            <CardDescription>
              Turnaround duration, priority routing, and outsourcing flags.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="turnaroundTimeHours" className="text-xs font-semibold">
                  Turnaround Time (Hours)
                </Label>
                <Input
                  id="turnaroundTimeHours"
                  type="number"
                  placeholder="24"
                  {...register("turnaroundTimeHours")}
                />
                {errors.turnaroundTimeHours && (
                  <p className="text-xs text-red-500">
                    {errors.turnaroundTimeHours.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col justify-center space-y-2 pt-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    {...register("prioritySupported")}
                  />
                  <span className="text-sm font-medium text-slate-700">
                    STAT / Priority Supported
                  </span>
                </label>
              </div>

              <div className="flex flex-col justify-center space-y-2 pt-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    {...register("outsourced")}
                  />
                  <span className="text-sm font-medium text-slate-700">
                    Outsourced to Reference Lab
                  </span>
                </label>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="laboratoryInstructions" className="text-xs font-semibold">
                Laboratory Processing Instructions
              </Label>
              <Textarea
                id="laboratoryInstructions"
                placeholder="Internal bench protocol and handling instructions..."
                rows={2}
                {...register("laboratoryInstructions")}
              />
              {errors.laboratoryInstructions && (
                <p className="text-xs text-red-500">
                  {errors.laboratoryInstructions.message}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Section 4: Reporting & Layout */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FileSpreadsheet className="h-5 w-5 text-emerald-600" />
              <CardTitle className="text-base font-semibold">
                Reporting & Presentation
              </CardTitle>
            </div>
            <CardDescription>
              Report headers, display ordering, and interpretation guidance.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="reportSection" className="text-xs font-semibold">
                  Report Section Header
                </Label>
                <Input
                  id="reportSection"
                  placeholder="e.g. HEMATOLOGY / COMPLETE HEMOGRAM"
                  {...register("reportSection")}
                />
                {errors.reportSection && (
                  <p className="text-xs text-red-500">
                    {errors.reportSection.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="displayOrder" className="text-xs font-semibold">
                  Display Order
                </Label>
                <Input
                  id="displayOrder"
                  type="number"
                  placeholder="0"
                  {...register("displayOrder")}
                />
                {errors.displayOrder && (
                  <p className="text-xs text-red-500">
                    {errors.displayOrder.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="reportDescription" className="text-xs font-semibold">
                Report Description (Printed on Report)
              </Label>
              <Textarea
                id="reportDescription"
                placeholder="Methodology and clinical overview printed directly on final patient report..."
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
                Clinical Interpretation Guidance
              </Label>
              <Textarea
                id="interpretationGuidance"
                placeholder="Interpretation benchmarks and diagnostic notes..."
                rows={3}
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

        {/* Section 5: Commercial & Billing */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Coins className="h-5 w-5 text-amber-500" />
              <CardTitle className="text-base font-semibold">
                Commercial & Billing
              </CardTitle>
            </div>
            <CardDescription>
              Catalog baseline pricing, currency, and reimbursement codes.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="basePrice" className="text-xs font-semibold">
                  Base Price
                </Label>
                <Input
                  id="basePrice"
                  type="number"
                  step="0.01"
                  placeholder="e.g. 450.00"
                  {...register("basePrice")}
                />
                {errors.basePrice && (
                  <p className="text-xs text-red-500">
                    {errors.basePrice.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="currency" className="text-xs font-semibold">
                  Currency (ISO 3-Letter)
                </Label>
                <Input
                  id="currency"
                  placeholder="INR"
                  className="uppercase font-mono"
                  maxLength={3}
                  {...register("currency")}
                />
                {errors.currency && (
                  <p className="text-xs text-red-500">
                    {errors.currency.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="billingCode" className="text-xs font-semibold">
                  Billing / CPT Code
                </Label>
                <Input
                  id="billingCode"
                  placeholder="e.g. 85025"
                  className="font-mono uppercase"
                  {...register("billingCode")}
                />
                {errors.billingCode && (
                  <p className="text-xs text-red-500">
                    {errors.billingCode.message}
                  </p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Section 6: Effective Period */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-teal-600" />
              <CardTitle className="text-base font-semibold">
                Effective Validity Period
              </CardTitle>
            </div>
            <CardDescription>
              Optional date bounds for when this test specification is active.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="effectiveFrom" className="text-xs font-semibold">
                  Effective From
                </Label>
                <Input
                  id="effectiveFrom"
                  type="date"
                  {...register("effectiveFrom")}
                />
                {errors.effectiveFrom && (
                  <p className="text-xs text-red-500">
                    {errors.effectiveFrom.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="effectiveUntil" className="text-xs font-semibold">
                  Effective Until
                </Label>
                <Input
                  id="effectiveUntil"
                  type="date"
                  {...register("effectiveUntil")}
                />
                {errors.effectiveUntil && (
                  <p className="text-xs text-red-500">
                    {errors.effectiveUntil.message}
                  </p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Form Actions */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate("/super-admin/tests")}
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
                Create Test
              </>
            )}
          </Button>
        </div>
      </form>
    </div>
  );
}

export default CreateTestPage;
