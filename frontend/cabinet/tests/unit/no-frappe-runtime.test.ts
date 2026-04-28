import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SRC_ROOT = join(process.cwd(), 'src')
const FORBIDDEN_RUNTIME_PATTERNS = [
  '/api/method',
  'frappeCall',
  'frappe.client',
  'frappe.auth',
  'frappe-client',
  'X-Frappe',
  'socket.io',
]

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry)
    const stat = statSync(path)
    if (stat.isDirectory())
      return sourceFiles(path)
    return /\.(?:ts|vue)$/.test(path) ? [path] : []
  })
}

describe('frontend runtime API boundaries', () => {
  it('does not reference legacy Frappe HTTP or realtime APIs', () => {
    const offenders = sourceFiles(SRC_ROOT).flatMap((path) => {
      const content = readFileSync(path, 'utf8')
      return FORBIDDEN_RUNTIME_PATTERNS
        .filter(pattern => content.includes(pattern))
        .map(pattern => `${path.replace(`${SRC_ROOT}/`, '')}: ${pattern}`)
    })

    expect(offenders).toEqual([])
  })

  it('production task source files use the Spring API client only (T085)', () => {
    const productionPaths = sourceFiles(SRC_ROOT).filter(p =>
      p.includes(`${SRC_ROOT}/pages/production`)
      || p.includes(`${SRC_ROOT}/components/domain/ProductionTask`)
      || /use-production-task/.test(p),
    )
    expect(productionPaths.length).toBeGreaterThan(0)

    const offenders = productionPaths.flatMap((path) => {
      const content = readFileSync(path, 'utf8')
      return FORBIDDEN_RUNTIME_PATTERNS
        .filter(pattern => content.includes(pattern))
        .map(pattern => `${path.replace(`${SRC_ROOT}/`, '')}: ${pattern}`)
    })

    expect(offenders).toEqual([])
  })

  it('production task source files only call Spring `/api/production-tasks` endpoints (T085)', () => {
    const productionPaths = sourceFiles(SRC_ROOT).filter(p =>
      p.includes(`${SRC_ROOT}/pages/production`)
      || p.includes(`${SRC_ROOT}/components/domain/ProductionTask`)
      || /use-production-task/.test(p),
    )
    const apiCalls = productionPaths.flatMap((path) => {
      const content = readFileSync(path, 'utf8')
      const matches = content.match(/['"`]\/api\/[^'"`]+['"`]/g) ?? []
      return matches.map(m => m.replace(/['"`]/g, ''))
    })
    const nonProductionEndpoints = apiCalls.filter(url =>
      !url.startsWith('/api/production-tasks')
      && !url.startsWith('/api/orders'),
    )
    expect(nonProductionEndpoints).toEqual([])
  })
})
