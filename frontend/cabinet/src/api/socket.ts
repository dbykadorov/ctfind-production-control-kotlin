/**
 * Socket.IO singleton: подключение к Frappe realtime.
 * См. specs/006-spa-cabinet-ui/contracts/socket-events.md.
 *
 * Migration note (002-migrate-cabinet-frontend): socket не создается при import.
 * До новой realtime-интеграции login screen не должен вызывать getSocket().
 */

import type { Socket } from 'socket.io-client'
import { io } from 'socket.io-client'
import { readBoot } from './boot'

let socketInstance: Socket | null = null

function buildSocketUrl(): string {
  if (typeof window === 'undefined')
    return '/'
  const { protocol, hostname, port } = window.location
  if (import.meta.env.DEV) {
    return `${protocol}//${hostname}:${port}`
  }
  return `${protocol}//${hostname}${port ? `:${port}` : ''}`
}

export function getSocket(): Socket {
  if (socketInstance)
    return socketInstance
  const boot = readBoot()
  socketInstance = io(buildSocketUrl(), {
    path: '/socket.io',
    withCredentials: true,
    transports: ['websocket', 'polling'],
    reconnection: true,
    reconnectionAttempts: 10,
    reconnectionDelay: 500,
    reconnectionDelayMax: 5_000,
    auth: {
      sid: typeof document !== 'undefined' ? document.cookie : '',
      user: boot.user,
    },
  })
  return socketInstance
}

export type DocUpdateHandler = () => void

/** Подписаться на изменения конкретного документа: `doc_update:<DocType>:<name>`. */
export function subscribeDocUpdate(
  doctype: string,
  name: string,
  handler: DocUpdateHandler,
): () => void {
  const socket = getSocket()
  const event = `doc_update:${doctype}:${name}`
  socket.on(event, handler)
  return () => socket.off(event, handler)
}

/** Подписаться на изменения списка: `list_update:<DocType>`. */
export function subscribeListUpdate(doctype: string, handler: DocUpdateHandler): () => void {
  const socket = getSocket()
  const event = `list_update:${doctype}`
  socket.on(event, handler)
  return () => socket.off(event, handler)
}

/** Полное закрытие сокета (для logout). */
export function disconnectSocket(): void {
  if (!socketInstance)
    return
  socketInstance.disconnect()
  socketInstance = null
}
