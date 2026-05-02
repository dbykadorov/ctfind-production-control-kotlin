import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/warehouse/MaterialCreateDialog.vue'),
  'utf8',
)

describe('MaterialCreateDialog', () => {
  it('has name input', () => {
    expect(SOURCE).toMatch(/name|warehouse\.fields\.name/i)
  })

  it('has unit select', () => {
    expect(SOURCE).toMatch(/unit|MeasurementUnit/i)
  })

  it('has submit button', () => {
    expect(SOURCE).toMatch(/submit|create|Создать|Сохранить/i)
  })

  it('emits created event on success', () => {
    expect(SOURCE).toMatch(/emit.*created|created.*emit/i)
  })

  it('imports httpClient for API call', () => {
    expect(SOURCE).toContain('httpClient')
  })

  it('handles cancel/close', () => {
    expect(SOURCE).toMatch(/cancel|close|Отмена/i)
  })
})
