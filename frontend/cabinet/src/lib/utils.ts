/**
 * Утилитарные хелперы для UI-кита Кабинета (shadcn-vue baseline).
 */

import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Классический shadcn `cn` — мерж Tailwind-классов с правильной приоритизацией. */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
