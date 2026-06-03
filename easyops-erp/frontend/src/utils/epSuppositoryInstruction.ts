/** Required instruction when dosage form is suppository (Rx template & prescription). */
export const SUPPOSITORY_INSTRUCTION = 'Anal Use (পায়ু পথে ব্যবহার)';

export function isSuppositoryDosageForm(form?: string | null): boolean {
  return (form ?? '').trim().toUpperCase() === 'SUPPOSITORY';
}

/** Apply required suppository instruction when the effective dosage form is SUPPOSITORY. */
export function mergeMedicationLinePatchWithSuppositoryInstruction<
  T extends { dosageForm?: string; instructions?: string },
>(line: T, patch: Partial<T>): Partial<T> {
  const nextForm = (patch.dosageForm ?? line.dosageForm) as string | undefined;
  if (!isSuppositoryDosageForm(nextForm)) {
    return patch;
  }
  return { ...patch, instructions: SUPPOSITORY_INSTRUCTION };
}

export function withSuppositoryInstruction<T extends { dosageForm?: string; instructions?: string }>(
  line: T,
): T {
  if (!isSuppositoryDosageForm(line.dosageForm)) return line;
  return { ...line, instructions: SUPPOSITORY_INSTRUCTION };
}

export function ensureSuppositoryInstructionsOnMedicationLines<
  T extends { dosageForm?: string; instructions?: string },
>(lines: T[]): T[] {
  return lines.map(withSuppositoryInstruction);
}
