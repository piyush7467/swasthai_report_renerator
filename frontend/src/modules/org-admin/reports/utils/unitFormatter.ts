/**
 * Formats diagnostic lab units for presentation.
 * Transforms verbose units into standard compact scientific notation.
 *
 * Examples:
 * - "million/µL", "million/uL", "10^6/µL" -> "10⁶/µL"
 * - "thousand/µL", "thousand/uL", "10^3/µL" -> "10³/µL"
 *
 * Preserves clinical units such as:
 * - "g/dL", "mg/dL", "fL", "%", "IU/L", "cells/µL", "/mm³", etc.
 */
export function formatDisplayUnit(unit?: string | null): string {
  if (!unit) return "—";
  const trimmed = unit.trim();
  if (!trimmed || trimmed === "-") return "—";

  const lower = trimmed.toLowerCase();

  // Million variations -> 10⁶/µL
  if (
    lower === "million/µl" ||
    lower === "million/ul" ||
    lower === "million / µl" ||
    lower === "million / ul" ||
    lower === "10^6/µl" ||
    lower === "10^6/ul" ||
    lower === "10^6 / µl" ||
    lower === "10^6 / ul" ||
    lower === "106/µl" ||
    lower === "106/ul" ||
    lower === "m/µl" ||
    lower === "m/ul"
  ) {
    return "10⁶/µL";
  }

  // Thousand variations -> 10³/µL
  if (
    lower === "thousand/µl" ||
    lower === "thousand/ul" ||
    lower === "thousand / µl" ||
    lower === "thousand / ul" ||
    lower === "10^3/µl" ||
    lower === "10^3/ul" ||
    lower === "10^3 / µl" ||
    lower === "10^3 / ul" ||
    lower === "103/µl" ||
    lower === "103/ul" ||
    lower === "k/µl" ||
    lower === "k/ul"
  ) {
    return "10³/µL";
  }

  // Handle generic replacements for embedded 10^6 or 10^3 notation
  let formatted = trimmed
    .replace(/10\^6/g, "10⁶")
    .replace(/10\^3/g, "10³")
    .replace(/million\/µl/gi, "10⁶/µL")
    .replace(/million\/ul/gi, "10⁶/µL")
    .replace(/thousand\/µl/gi, "10³/µL")
    .replace(/thousand\/ul/gi, "10³/µL");

  return formatted;
}
