export function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  if (diff < 60_000) return 'только что'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} мин назад`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} ч назад`
  if (diff < 172_800_000) return 'вчера'
  if (diff < 604_800_000) return `${Math.floor(diff / 86_400_000)} дн назад`
  return new Date(iso).toLocaleDateString('ru-RU')
}
