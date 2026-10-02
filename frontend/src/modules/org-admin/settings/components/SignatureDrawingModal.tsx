import React, { useRef, useState, useEffect, useCallback } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  RotateCcw,
  Trash2,
  Check,
  Loader2,
  FileSignature,
} from "lucide-react";

interface Point {
  x: number;
  y: number;
}

interface Stroke {
  points: Point[];
  color: string;
  width: number;
}

interface SignatureDrawingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (file: File) => Promise<void>;
  isSaving: boolean;
}

const INK_COLORS = [
  { id: "navy", label: "Navy Blue", value: "#0F172A" },
  { id: "royal", label: "Royal Blue", value: "#1E3A8A" },
  { id: "black", label: "Pure Black", value: "#000000" },
];

export function SignatureDrawingModal({
  isOpen,
  onClose,
  onSave,
  isSaving,
}: SignatureDrawingModalProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const [strokes, setStrokes] = useState<Stroke[]>([]);
  const [currentStroke, setCurrentStroke] = useState<Point[] | null>(null);
  const [selectedColor, setSelectedColor] = useState<string>("#0F172A");
  const [strokeWidth, setStrokeWidth] = useState<number>(2.5);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Redraw all strokes on canvas
  const redraw = useCallback(
    (allStrokes: Stroke[], ongoing?: Point[]) => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      ctx.save();
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.restore();

      const drawStroke = (pts: Point[], color: string, width: number) => {
        if (pts.length === 0) return;
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = width;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";

        if (pts.length === 1) {
          ctx.arc(pts[0].x, pts[0].y, width / 2, 0, Math.PI * 2);
          ctx.fillStyle = color;
          ctx.fill();
          return;
        }

        ctx.moveTo(pts[0].x, pts[0].y);
        for (let i = 1; i < pts.length; i++) {
          const prev = pts[i - 1];
          const curr = pts[i];
          const midX = (prev.x + curr.x) / 2;
          const midY = (prev.y + curr.y) / 2;
          ctx.quadraticCurveTo(prev.x, prev.y, midX, midY);
        }
        const last = pts[pts.length - 1];
        ctx.lineTo(last.x, last.y);
        ctx.stroke();
      };

      allStrokes.forEach((s) => drawStroke(s.points, s.color, s.width));
      if (ongoing && ongoing.length > 0) {
        drawStroke(ongoing, selectedColor, strokeWidth);
      }
    },
    [selectedColor, strokeWidth]
  );

  // Setup High-DPI canvas whenever dialog opens
  useEffect(() => {
    if (!isOpen) return;

    const timer = setTimeout(() => {
      const canvas = canvasRef.current;
      const container = containerRef.current;
      if (!canvas || !container) return;

      const rect = container.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;

      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      canvas.style.width = `${rect.width}px`;
      canvas.style.height = `${rect.height}px`;

      const ctx = canvas.getContext("2d");
      if (ctx) {
        ctx.scale(dpr, dpr);
      }
      setStrokes([]);
      setCurrentStroke(null);
      setErrorMessage(null);
    }, 50);

    return () => clearTimeout(timer);
  }, [isOpen]);

  const getCanvasCoordinates = (e: React.PointerEvent<HTMLCanvasElement>): Point => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    };
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    e.preventDefault();
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
    const pt = getCanvasCoordinates(e);
    setCurrentStroke([pt]);
    setErrorMessage(null);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!currentStroke) return;
    e.preventDefault();
    const pt = getCanvasCoordinates(e);
    const updated = [...currentStroke, pt];
    setCurrentStroke(updated);
    redraw(strokes, updated);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!currentStroke) return;
    e.preventDefault();
    try {
      (e.target as HTMLElement).releasePointerCapture(e.pointerId);
    } catch {
      // Ignore pointer capture release error if already released
    }

    if (currentStroke.length > 0) {
      const newStroke: Stroke = {
        points: currentStroke,
        color: selectedColor,
        width: strokeWidth,
      };
      const updatedStrokes = [...strokes, newStroke];
      setStrokes(updatedStrokes);
      redraw(updatedStrokes);
    }
    setCurrentStroke(null);
  };

  const handleClear = () => {
    setStrokes([]);
    setCurrentStroke(null);
    setErrorMessage(null);
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.save();
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.restore();
  };

  const handleUndo = () => {
    if (strokes.length === 0) return;
    const updated = strokes.slice(0, -1);
    setStrokes(updated);
    redraw(updated);
  };

  const handleSave = () => {
    if (strokes.length === 0) {
      setErrorMessage("Please sign inside the box before saving.");
      return;
    }

    const canvas = canvasRef.current;
    if (!canvas) return;

    canvas.toBlob(
      async (blob) => {
        if (!blob) {
          setErrorMessage("Failed to export signature image.");
          return;
        }
        const file = new File([blob], "digital-signature.png", {
          type: "image/png",
        });
        try {
          await onSave(file);
          onClose();
        } catch {
          // Error is handled in parent
        }
      },
      "image/png"
    );
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && !isSaving && onClose()}>
      <DialogContent className="sm:max-w-xl p-0 overflow-hidden bg-white border border-slate-200">
        <DialogHeader className="p-5 pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-teal-50 text-teal-700">
              <FileSignature className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-bold text-slate-900">
                Digital Signature Pad
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 mt-0.5">
                Draw your official signature specimen using mouse, touch, or stylus.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="p-5 space-y-4">
          {/* Controls Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
            {/* Color selection */}
            <div className="flex items-center gap-2">
              <span className="text-slate-500 font-medium">Ink:</span>
              <div className="flex items-center gap-1.5">
                {INK_COLORS.map((ink) => (
                  <button
                    key={ink.id}
                    type="button"
                    onClick={() => setSelectedColor(ink.value)}
                    className={`size-6 rounded-full border-2 transition-all flex items-center justify-center cursor-pointer ${
                      selectedColor === ink.value
                        ? "border-teal-600 scale-110 shadow-xs"
                        : "border-transparent hover:scale-105"
                    }`}
                    style={{ backgroundColor: ink.value }}
                    title={ink.label}
                  >
                    {selectedColor === ink.value && (
                      <Check className="size-3 text-white" />
                    )}
                  </button>
                ))}
              </div>
            </div>

            {/* Pen Stroke Width */}
            <div className="flex items-center gap-2">
              <span className="text-slate-500 font-medium">Thickness:</span>
              <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg">
                {[
                  { label: "Fine", width: 2 },
                  { label: "Normal", width: 2.8 },
                  { label: "Bold", width: 4 },
                ].map((s) => (
                  <button
                    key={s.label}
                    type="button"
                    onClick={() => setStrokeWidth(s.width)}
                    className={`px-2 py-0.5 text-[11px] rounded-md font-medium transition-colors cursor-pointer ${
                      strokeWidth === s.width
                        ? "bg-white text-slate-900 shadow-xs font-semibold"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Undo & Clear */}
            <div className="flex items-center gap-1.5">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleUndo}
                disabled={strokes.length === 0 || isSaving}
                className="h-7 text-xs px-2.5 text-slate-600 border-slate-200 hover:bg-slate-50 cursor-pointer"
                title="Undo last stroke"
              >
                <RotateCcw className="size-3.5 mr-1" />
                Undo
              </Button>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleClear}
                disabled={strokes.length === 0 || isSaving}
                className="h-7 text-xs px-2.5 text-rose-600 border-rose-200 hover:bg-rose-50 cursor-pointer"
                title="Clear all"
              >
                <Trash2 className="size-3.5 mr-1" />
                Clear
              </Button>
            </div>
          </div>

          {/* Interactive Drawing Pad */}
          <div
            ref={containerRef}
            className="relative w-full h-[220px] rounded-xl border-2 border-dashed border-teal-200/80 bg-slate-50/70 overflow-hidden select-none touch-none cursor-crosshair shadow-inner"
          >
            <canvas
              ref={canvasRef}
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerCancel={handlePointerUp}
              className="absolute inset-0 w-full h-full"
              style={{ touchAction: "none" }}
            />

            {/* Baseline guideline */}
            <div className="absolute left-8 right-8 bottom-12 border-b border-dashed border-slate-300 pointer-events-none flex items-center justify-between">
              <span className="text-[10px] uppercase font-mono text-slate-400 tracking-wider">
                Sign above line
              </span>
              <span className="text-[10px] text-slate-400 font-mono">✕</span>
            </div>

            {strokes.length === 0 && !currentStroke && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none text-slate-400 text-xs">
                Draw signature here with mouse or touch
              </div>
            )}
          </div>

          {errorMessage && (
            <p className="text-xs text-rose-600 font-medium">{errorMessage}</p>
          )}

          <div className="rounded-lg bg-teal-50/80 border border-teal-100 p-2.5 text-[11px] text-teal-800 leading-relaxed">
            <strong>Notice:</strong> Once submitted, your specimen will enter the administrative verification queue before being stamped on issued reports.
          </div>
        </div>

        <DialogFooter className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-between sm:justify-between">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onClose}
            disabled={isSaving}
            className="text-xs text-slate-600 hover:text-slate-900 cursor-pointer"
          >
            Cancel
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={handleSave}
            disabled={isSaving || strokes.length === 0}
            className="bg-[#0F766E] hover:bg-[#115E59] text-white text-xs px-4 h-8 rounded-lg shadow-xs cursor-pointer font-semibold"
          >
            {isSaving ? (
              <>
                <Loader2 className="size-3.5 mr-1.5 animate-spin" />
                Saving Signature...
              </>
            ) : (
              <>
                <Check className="size-3.5 mr-1.5" />
                Save & Apply Signature
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
