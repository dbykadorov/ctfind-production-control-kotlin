/**
 * URL-утилиты Кабинета (009-cabinet-custom-login).
 *
 * Главная цель — безопасный разбор query-параметра `?from=…` на странице
 * `/cabinet/login`, чтобы открытый редирект (open redirect) невозможен.
 *
 * См. data-model.md §2.3 + research.md §R-006.
 */

/**
 * Очищает значение `from`, ожидаемое из query-string или router.
 *
 * Возвращает строку, если она безопасна для `window.location.assign(value)`:
 *   - типа `string`
 *   - равна `"/cabinet"` ровно или начинается с `"/cabinet/"`
 *   - не содержит `protocol-relative` префикса `//`
 *   - не содержит обратного слеша `\` (защита от анти-патчей IE)
 *   - не парсится как absolute URL с другим origin
 *
 * Иначе возвращает `null` — caller должен сделать fallback на `'/cabinet'`.
 */
export function sanitizeFrom(value: unknown): string | null {
  if (typeof value !== 'string')
    return null

  if (value.length === 0)
    return null

  if (value.includes('\\'))
    return null

  if (value.startsWith('//'))
    return null

  if (value !== '/cabinet' && !value.startsWith('/cabinet/'))
    return null

  // Дополнительная защита: попытаться распарсить как absolute URL — если получится,
  // значит начало вида "/cabinet" было обманом (например, "/cabinet/../../etc/passwd"
  // нормализуется браузером к чему-то странному; URL ctor не примет относительный путь
  // без base, поэтому здесь это всегда выбрасывает TypeError, что нам и нужно).
  try {
    // eslint-disable-next-line no-new
    new URL(value)
    return null
  }
  catch {
    return value
  }
}
