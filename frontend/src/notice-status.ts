const NOTICE_NEW_WINDOW_MS = 24 * 60 * 60 * 1000
const NOTICE_READ_EVENT = 'tom:notice-read'

function readNoticeReadTimes(userId: number): Record<string, number> {
  const stored = localStorage.getItem(`tom-read-notices-${userId}`)
  if (!stored) return {}

  try {
    const parsed: unknown = JSON.parse(stored)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
    return Object.entries(parsed).reduce<Record<string, number>>((readTimes, [noticeId, readAt]) => {
      if (typeof readAt === 'number' && Date.now() - readAt < NOTICE_NEW_WINDOW_MS) readTimes[noticeId] = readAt
      return readTimes
    }, {})
  } catch (cause) {
    console.error('Unable to read notice read status from local storage.', cause)
    return {}
  }
}

export function isNoticeUnread(userId: number, noticeId: number, createdAt: string): boolean {
  const createdAtMs = Date.parse(createdAt)
  const noticeAge = Date.now() - createdAtMs
  if (!Number.isFinite(createdAtMs) || noticeAge < 0 || noticeAge >= NOTICE_NEW_WINDOW_MS) return false
  return !readNoticeReadTimes(userId)[String(noticeId)]
}

export function timeUntilNoticeExpires(createdAt: string): number {
  const createdAtMs = Date.parse(createdAt)
  if (!Number.isFinite(createdAtMs)) return 0
  return Math.max(0, createdAtMs + NOTICE_NEW_WINDOW_MS - Date.now())
}

export function markNoticeRead(userId: number, noticeId: number): void {
  const readTimes = readNoticeReadTimes(userId)
  if (readTimes[String(noticeId)]) return
  readTimes[String(noticeId)] = Date.now()
  localStorage.setItem(`tom-read-notices-${userId}`, JSON.stringify(readTimes))
  window.dispatchEvent(new Event(NOTICE_READ_EVENT))
}

export { NOTICE_READ_EVENT }
