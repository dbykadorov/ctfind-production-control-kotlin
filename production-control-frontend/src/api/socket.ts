/**
 * Realtime boundary placeholder.
 *
 * The Spring/Kotlin backend does not expose realtime events yet, so subscriptions
 * are intentionally no-ops. Keeping this module preserves composable contracts
 * without opening a websocket to an unavailable legacy service.
 */

export type DocUpdateHandler = () => void

export function subscribeDocUpdate(
  _doctype: string,
  _name: string,
  _handler: DocUpdateHandler,
): () => void {
  return () => {}
}

export function subscribeListUpdate(_doctype: string, _handler: DocUpdateHandler): () => void {
  return () => {}
}

export function disconnectSocket(): void {
  // No active realtime connection in the current Spring-only runtime.
}
