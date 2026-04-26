/**
 * Контрактный тест design-tokens (010-cabinet-layout-rework, T014).
 *
 * Проверяет наличие всех новых layout/brand-токенов в `src/styles/tokens.css`:
 *   - layout: --bg-app, --card-radius, --card-margin, --card-shadow,
 *             --sidebar-width-expanded, --sidebar-width-collapsed,
 *             --header-height, --topbar-icon-size, --transition-base;
 *   - brand: amber-палитра (--c-brand-500 = #f5c842 в обеих темах);
 *   - sidebar preset 'none' (R-008): соответствующий блок [data-sidebar-preset='none'].
 *
 * См. specs/010-cabinet-layout-rework/contracts/design-tokens.contract.md.
 */

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// vitest cwd = project root (cabinet_app/), см. vitest.config.ts.
const TOKENS_PATH = resolve(process.cwd(), 'src/styles/tokens.css')
const tokens = readFileSync(TOKENS_PATH, 'utf-8')

describe('design tokens / layout (010 floating-card)', () => {
  it.each([
    '--bg-app',
    '--card-radius',
    '--card-margin',
    '--card-shadow',
    '--sidebar-width-expanded',
    '--sidebar-width-collapsed',
    '--header-height',
    '--topbar-icon-size',
    '--transition-base',
  ])('содержит токен %s', (token) => {
    expect(tokens).toContain(token)
  })

  it('--card-radius = 24px (UC reference)', () => {
    expect(tokens).toMatch(/--card-radius:\s*24px/)
  })

  it('--sidebar-width-expanded = 280px и -collapsed = 80px', () => {
    expect(tokens).toMatch(/--sidebar-width-expanded:\s*280px/)
    expect(tokens).toMatch(/--sidebar-width-collapsed:\s*80px/)
  })

  it('--header-height = 75px и --topbar-icon-size = 45px', () => {
    expect(tokens).toMatch(/--header-height:\s*75px/)
    expect(tokens).toMatch(/--topbar-icon-size:\s*45px/)
  })

  it('--transition-base = 350ms cubic-bezier(0.4, 0, 0.2, 1)', () => {
    expect(tokens).toMatch(/--transition-base:\s*350ms\s+cubic-bezier\(0\.4,\s*0,\s*0\.2,\s*1\)/)
  })
})

describe('design tokens / brand amber palette (010)', () => {
  it('--c-brand-500 = #f5c842 в обеих темах', () => {
    // Простейший тест: токен встречается минимум дважды (в :root/dark и в [data-theme=light]).
    const matches = tokens.matchAll(/--c-brand-500:\s*#f5c842/gi)
    expect([...matches].length).toBeGreaterThanOrEqual(2)
  })

  it('амбровая палитра 50/100/500/600/700 присутствует', () => {
    expect(tokens).toMatch(/--c-brand-50:\s*#fff8e1/i)
    expect(tokens).toMatch(/--c-brand-100:\s*#ffecb3/i)
    expect(tokens).toMatch(/--c-brand-500:\s*#f5c842/i)
    expect(tokens).toMatch(/--c-brand-600:\s*#c4a035/i)
    expect(tokens).toMatch(/--c-brand-700:\s*#8c6f26/i)
  })
})

describe('design tokens / app background (010 §R-004)', () => {
  it('dark: --bg-app = #1a2235 (navy)', () => {
    expect(tokens).toMatch(/--bg-app:\s*#1a2235/i)
  })

  it('light: --bg-app = #f1f2f8', () => {
    expect(tokens).toMatch(/--bg-app:\s*#f1f2f8/i)
  })

  it('--c-bg алиасит --bg-app для backward-compat (фичи 006/007)', () => {
    expect(tokens).toMatch(/--c-bg:\s*var\(--bg-app\)/)
  })
})

describe('design tokens / sidebar preset \'none\' (010 §R-008)', () => {
  it('блок [data-sidebar-preset=\'none\'] присутствует', () => {
    expect(tokens).toMatch(/\[data-sidebar-preset=['"]none['"]\]/)
  })

  it('пресет \'none\' определяет --sidebar-grad-* как var(--bg-app)', () => {
    // Внутри блока 'none' grad-top/bottom должны указывать на --bg-app, чтобы
    // даже если кто-то применит .cabinet-sidebar-bg к sidebar — фон совпадёт с body.
    const noneBlock = tokens.match(/\[data-sidebar-preset=['"]none['"]\]\s*\{([^}]+)\}/)
    expect(noneBlock).not.toBeNull()
    expect(noneBlock![1]).toMatch(/--sidebar-grad-top:\s*var\(--bg-app\)/)
    expect(noneBlock![1]).toMatch(/--sidebar-grad-bottom:\s*var\(--bg-app\)/)
  })
})

describe('design tokens / scoped fg-перебивка внутри .cabinet-card (010 §R-004)', () => {
  it('правило [data-theme=\'dark\'] .cabinet-card существует', () => {
    expect(tokens).toMatch(/\[data-theme=['"]dark['"]\]\s+\.cabinet-card\s*\{/)
  })

  it('перебивает --c-fg на тёмное значение (#3d4257)', () => {
    const block = tokens.match(/\[data-theme=['"]dark['"]\]\s+\.cabinet-card\s*\{([^}]+)\}/)
    expect(block).not.toBeNull()
    expect(block![1]).toMatch(/--c-fg:\s*#3d4257/i)
    expect(block![1]).toMatch(/--c-fg-strong:\s*#1a1f36/i)
  })
})
