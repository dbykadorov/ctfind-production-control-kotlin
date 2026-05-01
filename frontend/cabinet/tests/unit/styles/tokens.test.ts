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
  it('dark: --bg-app = #101014 (PAM-like neutral dark)', () => {
    expect(tokens).toMatch(/--bg-app:\s*#101014/i)
  })

  it('light: --bg-app = #f8f9fc', () => {
    expect(tokens).toMatch(/--bg-app:\s*#f8f9fc/i)
  })

  it('--c-bg определён отдельно для dark и light', () => {
    expect(tokens).toMatch(/--c-bg:\s*#16161d/i)
    expect(tokens).toMatch(/--c-bg:\s*#f1f3f8/i)
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

describe('design tokens / surfaces sync with PAM dark (015)', () => {
  it('dark: --c-surface и --c-elevated используют тёмные нейтральные значения', () => {
    expect(tokens).toMatch(/--c-surface:\s*#19191f/i)
    expect(tokens).toMatch(/--c-elevated:\s*#25252d/i)
  })

  it('light: surface/elevated остаются светлыми', () => {
    const surfaceMatches = [...tokens.matchAll(/--c-surface:\s*#ffffff/gi)]
    const elevatedMatches = [...tokens.matchAll(/--c-elevated:\s*#ffffff/gi)]
    expect(surfaceMatches.length).toBeGreaterThanOrEqual(1)
    expect(elevatedMatches.length).toBeGreaterThanOrEqual(1)
  })
})
