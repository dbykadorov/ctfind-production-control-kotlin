import type { IncomingMessage, ServerResponse } from 'node:http'

type NextFunction = () => void

export function redirectCabinetBaseWithoutTrailingSlash() {
  return (req: IncomingMessage, res: ServerResponse, next: NextFunction): void => {
    const url = new URL(req.url ?? '/', 'http://localhost')

    if (url.pathname !== '/cabinet') {
      next()
      return
    }

    res.statusCode = 302
    res.setHeader('Location', `/cabinet/${url.search}`)
    res.end()
  }
}
