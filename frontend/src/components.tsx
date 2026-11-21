import { useEffect, useRef, useState, type ReactNode } from 'react'
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from 'lucide-react'
import { errorMessage } from './api'
import { isNoticeUnread, NOTICE_READ_EVENT, timeUntilNoticeExpires } from './notice-status'

export function PageHeading({ eyebrow, title, subtitle, action }: { eyebrow: string; title: string; subtitle?: string; action?: ReactNode }) {
  return <div className="page-heading"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{subtitle && <p className="page-subtitle">{subtitle}</p>}</div>{action && <div className="page-heading-action">{action}</div>}</div>
}

export function Panel({ title, description, action, children, className = '' }: { title?: string; description?: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return <section className={`surface ${className}`}>
    {(title || action) && <header className="surface-header"><div>{title && <h2>{title}</h2>}{description && <p>{description}</p>}</div>{action}</header>}
    <div className="surface-body">{children}</div>
  </section>
}

export function StatCard({ label, value, detail, tone = 'neutral' }: { label: string; value: string; detail?: string; tone?: 'neutral' | 'accent' | 'warning' }) {
  return <div className={`stat-card stat-${tone}`}><span>{label}</span><strong>{value}</strong>{detail && <small>{detail}</small>}</div>
}

export function EmptyState({ title, detail, action }: { title: string; detail: string; action?: ReactNode }) {
  return <div className="empty-state"><span className="empty-rule" /><strong>{title}</strong><p>{detail}</p>{action}</div>
}

export function LoadingState({ label = 'Loading your workspace…' }: { label?: string }) {
  return <div className="loading-state"><span className="spinner" />{label}</div>
}

export function AutoDismissAlert({
  children,
  className = '',
  role = 'status',
  autoDismiss = true,
  duration = 10000,
  icon,
  onDismiss,
}: {
  children: ReactNode
  className?: string
  role?: string
  autoDismiss?: boolean
  duration?: number
  icon?: ReactNode
  onDismiss?: () => void
}) {
  const [phase, setPhase] = useState<'visible' | 'dissolving' | 'hidden'>('visible')
  const onDismissRef = useRef(onDismiss)

  useEffect(() => {
    onDismissRef.current = onDismiss
  }, [onDismiss])

  useEffect(() => {
    setPhase('visible')
    if (!autoDismiss) return

    let dissolveTimeout: number | undefined
    const timer = window.setTimeout(() => {
      setPhase('dissolving')
      dissolveTimeout = window.setTimeout(() => {
        setPhase('hidden')
        onDismissRef.current?.()
      }, 700)
    }, duration)

    return () => {
      window.clearTimeout(timer)
      if (dissolveTimeout) window.clearTimeout(dissolveTimeout)
    }
  }, [children, autoDismiss, duration])

  if (phase === 'hidden') return null

  function dismissNow() {
    setPhase('dissolving')
    window.setTimeout(() => {
      setPhase('hidden')
      onDismissRef.current?.()
    }, 300)
  }

  const animationClass = phase === 'dissolving' ? 'alert-dissolving' : 'alert-enter'

  return (
    <div className={`alert ${className} ${animationClass}`.trim()} role={role}>
      {icon && <span className="alert-icon-wrap">{icon}</span>}
      <div className="alert-body">{children}</div>
      {onDismiss && (
        <button
          type="button"
          className="alert-dismiss"
          onClick={dismissNow}
          aria-label="Dismiss notification"
          title="Dismiss notification"
        >
          <X size={15} />
        </button>
      )}
      {autoDismiss && (
        <span
          className="alert-progress-line"
          style={{ animationDuration: `${duration}ms` }}
        />
      )}
    </div>
  )
}

export function InlineError({ children, autoDismiss = false, duration = 10000, onDismiss }: {
  children: ReactNode
  autoDismiss?: boolean
  duration?: number
  onDismiss?: () => void
}) {
  return (
    <AutoDismissAlert
      className="alert-error"
      role="alert"
      autoDismiss={autoDismiss}
      duration={duration}
      icon={<AlertCircle size={17} className="alert-icon" />}
      onDismiss={onDismiss}
    >
      {children}
    </AutoDismissAlert>
  )
}

export function InlineSuccess({ children, autoDismiss = true, duration = 10000, onDismiss }: {
  children: ReactNode
  autoDismiss?: boolean
  duration?: number
  onDismiss?: () => void
}) {
  return (
    <AutoDismissAlert
      className="alert-success"
      role="status"
      autoDismiss={autoDismiss}
      duration={duration}
      icon={<CheckCircle2 size={17} className="alert-icon" />}
      onDismiss={onDismiss}
    >
      {children}
    </AutoDismissAlert>
  )
}

export function InlineWarning({ children, autoDismiss = true, duration = 10000, onDismiss }: {
  children: ReactNode
  autoDismiss?: boolean
  duration?: number
  onDismiss?: () => void
}) {
  return (
    <AutoDismissAlert
      className="alert-warning"
      role="alert"
      autoDismiss={autoDismiss}
      duration={duration}
      icon={<AlertTriangle size={17} className="alert-icon" />}
      onDismiss={onDismiss}
    >
      {children}
    </AutoDismissAlert>
  )
}

export function InlineNotice({ children, autoDismiss = true, duration = 10000, onDismiss }: {
  children: ReactNode
  autoDismiss?: boolean
  duration?: number
  onDismiss?: () => void
}) {
  return (
    <AutoDismissAlert
      className="alert-info"
      role="status"
      autoDismiss={autoDismiss}
      duration={duration}
      icon={<Info size={17} className="alert-icon" />}
      onDismiss={onDismiss}
    >
      {children}
    </AutoDismissAlert>
  )
}

export function StatusBadge({ status }: { status: string }) {
  const style = status.toLowerCase().replace(/_/g, '-')
  return <span className={`status-badge status-${style}`}>{status.replace(/_/g, ' ')}</span>
}

export function NoticeUnreadDot({ createdAt, noticeId, userId }: { createdAt: string; noticeId: number; userId: number }) {
  const [isUnread, setIsUnread] = useState(() => isNoticeUnread(userId, noticeId, createdAt))

  useEffect(() => {
    const updateUnreadStatus = () => setIsUnread(isNoticeUnread(userId, noticeId, createdAt))
    updateUnreadStatus()
    window.addEventListener(NOTICE_READ_EVENT, updateUnreadStatus)
    window.addEventListener('storage', updateUnreadStatus)
    const timeout = window.setTimeout(updateUnreadStatus, timeUntilNoticeExpires(createdAt))
    return () => {
      window.clearTimeout(timeout)
      window.removeEventListener(NOTICE_READ_EVENT, updateUnreadStatus)
      window.removeEventListener('storage', updateUnreadStatus)
    }
  }, [createdAt, noticeId, userId])

  return isUnread ? <span className="notice-unread-dot" role="img" aria-label="Unread new notice" title="Unread new notice" /> : null
}

export function DataTable({ children, headings }: { children: ReactNode; headings: string[] }) {
  return <div className="table-scroll"><table><thead><tr>{headings.map((heading) => <th key={heading}>{heading}</th>)}</tr></thead><tbody>{children}</tbody></table></div>
}

export function Modal({ title, description, onClose, children, wide = false, confirmation = false, error, warning }: {
  title: string
  description?: string
  onClose: () => void
  children: ReactNode
  wide?: boolean
  confirmation?: boolean
  error?: string
  warning?: string
}) {
  useEffect(() => {
    function onKey(event: KeyboardEvent) { if (event.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => { document.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
  }, [onClose])

  return <div className={`modal-backdrop ${confirmation ? 'modal-confirmation-backdrop' : ''}`} role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className={`modal ${wide ? 'modal-wide' : ''} ${confirmation ? 'modal-confirmation' : ''}`} role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <header className="modal-header"><div><h2 id="modal-title">{title}</h2>{description && <p>{description}</p>}</div><button type="button" className="icon-button" aria-label="Close dialog" onClick={onClose}><X size={17} /></button></header>
      <div className="modal-content">
        {error && <InlineError>{error}</InlineError>}
        {warning && <InlineWarning autoDismiss={false}>{warning}</InlineWarning>}
        {children}
      </div>
    </section>
  </div>
}

export function ConfirmationDialog({ title, description, confirmLabel, onConfirm, onCancel }: { title: string; description: string; confirmLabel: string; onConfirm: () => void | Promise<void>; onCancel: () => void }) {
  const [busy, setBusy] = useState(false)
  const [dialogError, setDialogError] = useState('')

  async function confirm() {
    setBusy(true)
    setDialogError('')
    try {
      await onConfirm()
    } catch (cause) {
      setDialogError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={title} onClose={onCancel} confirmation error={dialogError}>
      {description && (
        <div style={{ marginBottom: '16px' }}>
          <InlineWarning autoDismiss={false}>{description}</InlineWarning>
        </div>
      )}
      <div className="modal-actions">
        <button type="button" className="button" disabled={busy} onClick={onCancel}>
          Cancel
        </button>
        <button
          type="button"
          className="button button-danger"
          disabled={busy}
          onClick={() => void confirm()}
        >
          {busy ? <span className="spinner" /> : null}
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}

export function Field({ label, id, type = 'text', value, onChange, onBlur, error, hint, required = false, min, step, placeholder, autoComplete }: {
  label: string; id: string; type?: string; value: string | number; onChange: (value: string) => void; onBlur?: () => void; error?: string; hint?: string; required?: boolean; min?: string | number; step?: string | number; placeholder?: string; autoComplete?: string
}) {
  return <div className="field-group"><label htmlFor={id}>{label}{required && <span className="required-mark"> *</span>}</label><input id={id} type={type} value={value} onChange={(event) => onChange(event.target.value)} onBlur={onBlur} min={min} step={step} placeholder={placeholder} autoComplete={autoComplete} required={required} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined} />{error ? <span className="field-error" id={`${id}-error`}>{error}</span> : hint ? <span className="field-help" id={`${id}-hint`}>{hint}</span> : null}</div>
}
