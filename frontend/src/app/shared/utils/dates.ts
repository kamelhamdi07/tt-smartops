/** Dates locales au format attendu par le backend (LocalDate / LocalDateTime, sans fuseau). */

const pad = (n: number) => String(n).padStart(2, '0');

export function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function toIsoDateTime(d: Date): string {
  return `${toIsoDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
}

/** Valeur d'un <input type="datetime-local"> -> LocalDateTime backend. */
export function fromInputDateTime(v: string | null | undefined): string | null {
  if (!v) return null;
  return v.length === 16 ? `${v}:00` : v;
}

/** LocalDateTime backend -> valeur d'un <input type="datetime-local">. */
export function toInputDateTime(v: string | null | undefined): string {
  return v ? v.substring(0, 16) : '';
}

export function startOfWeek(d: Date): Date {
  const r = new Date(d);
  const day = (r.getDay() + 6) % 7; // lundi = 0
  r.setDate(r.getDate() - day);
  r.setHours(0, 0, 0, 0);
  return r;
}

export function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}
