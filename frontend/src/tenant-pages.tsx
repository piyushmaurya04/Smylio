import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowUpRight, Bike, CalendarDays, Camera, Clock3, Copy, CreditCard, Home, LayoutGrid, Mail, MessageCircle, Package, Phone, Receipt, RefreshCw, Send, Snowflake, Sofa, Table2, Tv, Wallet } from 'lucide-react'
import { api, errorMessage } from './api'
import { useAuth } from './auth-context'
import { ConfirmationDialog, DataTable, EmptyState, InlineError, InlineSuccess, LoadingState, NoticeUnreadDot, PageHeading, Panel, StatCard, StatusBadge } from './components'
import { formatCurrency, formatDate } from './format'
import { markNoticeRead } from './notice-status'
import type { Lease, Notice, Rent, TenantRents } from './types'

export function TenantDashboardPage() {
  const { token, user } = useAuth()
  const [leases, setLeases] = useState<Lease[]>([])
  const [rents, setRents] = useState<TenantRents | null>(null)
  const [notices, setNotices] = useState<Notice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [shareMessage, setShareMessage] = useState('')
  const [shareError, setShareError] = useState('')
  const tenantCode = user?.tenantCode
  const tenantIdMessage = tenantCode ? `My Smylio tenant ID is ${tenantCode}.` : ''

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true); setError('')
    try {
      const [leaseRows, rentSummary, noticeRows] = await Promise.all([api.tenantLeases(token), api.tenantRents(token), api.tenantNotices(token)])
      setLeases(leaseRows); setRents(rentSummary); setNotices(noticeRows)
    } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token])
  useEffect(() => { void load() }, [load])

  async function copyTenantId(): Promise<boolean> {
    if (!tenantCode) return false
    setShareMessage('')
    setShareError('')
    try {
      await navigator.clipboard.writeText(tenantCode)
      setShareMessage('Tenant ID copied. Share it with your property owner to connect your lease.')
      return true
    } catch (cause) {
      setShareError(errorMessage(cause))
      return false
    }
  }

  function copyForPlatform(platform: string) {
    void copyTenantId().then((copied) => {
      if (copied) setShareMessage(`Tenant ID copied. Paste it into a message on ${platform}.`)
    })
  }

  return <section className="page-section">
    <PageHeading eyebrow="TENANT PORTAL" title="My home" subtitle="Your lease, rent and property updates in one place." action={<button className="button" onClick={() => void load()}><RefreshCw size={14} /> Refresh</button>} />
    {error && <InlineError>{error}</InlineError>}
    <Panel className="tenant-id-panel" title="Your Smylio tenant ID" description="Share this ID with your property owner to connect your lease.">
      {tenantCode ? <>
        <div className="tenant-id-row">
          <strong className="tenant-id-value">{tenantCode}</strong>
          <button className="button" type="button" onClick={() => void copyTenantId()}><Copy size={14} /> Copy ID</button>
        </div>
        <div className="tenant-share-actions" aria-label="Share tenant ID">
          <span>Share via</span>
          <a className="button" href={`https://wa.me/?text=${encodeURIComponent(tenantIdMessage)}`} target="_blank" rel="noreferrer"><MessageCircle size={14} /> WhatsApp</a>
          <a className="button" href={`https://t.me/share/url?url=&text=${encodeURIComponent(tenantIdMessage)}`} target="_blank" rel="noreferrer"><Send size={14} /> Telegram</a>
          <a className="button" href="https://www.instagram.com/direct/inbox/" target="_blank" rel="noreferrer" onClick={() => copyForPlatform('Instagram')}><Camera size={14} /> Instagram</a>
          <a className="button" href="https://www.snapchat.com/" target="_blank" rel="noreferrer" onClick={() => copyForPlatform('Snapchat')}><Camera size={14} /> Snapchat</a>
        </div>
        <p className="tenant-share-help">WhatsApp and Telegram open a ready-to-send message. For Instagram and Snapchat, your ID is copied first so you can paste it into a message.</p>
        {shareError && <InlineError>{shareError}</InlineError>}
        {shareMessage && <InlineSuccess>{shareMessage}</InlineSuccess>}
      </> : <InlineError>Your tenant ID is unavailable. Sign out and sign in again to refresh your account details.</InlineError>}
    </Panel>
    {loading || !rents ? <LoadingState /> : <>
      <div className="stats-grid stats-grid-tenant">
        <StatCard label="Current balance" value={formatCurrency(rents.totalDue)} detail={rents.breakdown.length ? `${rents.breakdown.length} unpaid rent item${rents.breakdown.length === 1 ? '' : 's'}` : 'No outstanding rent'} tone={rents.totalDue ? 'warning' : 'accent'} />
        <StatCard label="Next due date" value={formatDate(rents.nextDueDate)} detail="Based on your oldest unpaid rent" />
        <StatCard label="Active homes" value={String(leases.length)} detail="Properties linked to your account" />
      </div>
      <div className="tenant-home-grid">
        <Panel title="My active leases" description="Property details and amenities">
          {leases.length === 0 ? <EmptyState title="No active lease linked" detail="Your owner will link a property to your tenant code when your lease is ready." /> : <div className="lease-cards">{leases.map((lease) => <Link className="lease-card lease-card-link tenant-active-lease-card" key={lease.id} to={`/tenant/leases/${lease.id}`} aria-label={`View lease ${lease.id} for ${lease.propertyTitle}`}><div className="lease-card-head"><div className="property-symbol"><Home size={17} /></div><StatusBadge status={lease.status} /></div><h3>{lease.propertyTitle}</h3><div className="lease-card-rent"><span>Monthly rent</span><strong>{formatCurrency(lease.monthlyRent)}</strong></div></Link>)}</div>}
        </Panel>
        <Panel title="Recent notices" description="Updates from your property owner" action={<Link className="inline-link" to="/tenant/notices">All notices <ArrowUpRight size={13} /></Link>}>
          {notices.length === 0 ? <EmptyState title="No new notices" detail="Owner announcements will appear here." /> : <div className="notice-list">{notices.slice(0, 4).map((notice) => <Link className="notice-row" key={notice.id} to={`/tenant/notices/${notice.id}`} onClick={() => { if (user) markNoticeRead(user.id, notice.id) }}><div className="notice-date">{user && <NoticeUnreadDot createdAt={notice.createdAt} noticeId={notice.id} userId={user.id} />}{formatDate(notice.createdAt)}</div><strong>{notice.title}</strong><p>{notice.message}</p>{notice.propertyTitle && <small>{notice.propertyTitle}</small>}</Link>)}</div>}
        </Panel>
      </div>
    </>}
  </section>
}

export function TenantLeaseDetailPage() {
  const { token } = useAuth()
  const { leaseId } = useParams()
  const id = Number(leaseId)
  const [lease, setLease] = useState<Lease | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token || !Number.isInteger(id) || id <= 0) {
      setError('Invalid lease.')
      setLoading(false)
      return
    }
    let active = true
    async function loadLease() {
      setLoading(true)
      setError('')
      try {
        const leases = await api.tenantLeases(token!)
        if (active) {
          const currentLease = leases.find((item) => item.id === id)
          if (currentLease) setLease(currentLease)
          else setError('This lease is not available in your tenant account.')
        }
      } catch (cause) {
        if (active) setError(errorMessage(cause))
      } finally {
        if (active) setLoading(false)
      }
    }
    void loadLease()
    return () => { active = false }
  }, [token, id])

  return <section className="page-section tenant-lease-detail-page">
    <PageHeading eyebrow="TENANT PORTAL" title="Lease details" subtitle="View complete details about your lease and property." action={<Link className="button" to="/tenant/overview"><ArrowLeft size={14} /> Back to My home</Link>} />
    {loading ? <Panel title="Lease details"><LoadingState /></Panel> : error ? <Panel title="Lease details"><InlineError>{error}</InlineError></Panel> : lease ? <article className="tenant-lease-detail surface">
      <header className="tenant-lease-summary">
        <div className="tenant-lease-home-icon"><Home size={26} /></div>
        <div className="tenant-lease-summary-copy">
          <div className="tenant-lease-name-row"><h2>{lease.propertyTitle}</h2><StatusBadge status={lease.status} /></div>
          <p>Lease #{lease.id}<span aria-hidden="true">·</span>Started {formatDate(lease.startDate)}</p>
        </div>
      </header>
      <div className="tenant-lease-metrics">
        <div className="tenant-lease-metric">
          <div className="tenant-lease-metric-icon"><Wallet size={23} /></div>
          <div><span>Monthly rent</span><strong>{formatCurrency(lease.monthlyRent)}</strong><small>Amount to be paid every month</small></div>
        </div>
        <div className="tenant-lease-metric">
          <div className="tenant-lease-metric-icon"><CalendarDays size={23} /></div>
          <div><span>Lease started date</span><strong>{formatDate(lease.startDate)}</strong></div>
        </div>
        <div className="tenant-lease-metric">
          <div className="tenant-lease-metric-icon"><Clock3 size={23} /></div>
          <div><span>Fine grace period</span><strong>{lease.fineGraceDays} days</strong><small>Allowed days after due date</small></div>
        </div>
        <div className="tenant-lease-metric">
          <div className="tenant-lease-metric-icon"><Receipt size={23} /></div>
          <div><span>Fine per day</span><strong>{formatCurrency(lease.finePerDay)}</strong><small>Amount charged per day after grace period</small></div>
        </div>
      </div>
      <section className="tenant-lease-amenities">
        <h3><LayoutGrid size={22} /> Amenities included</h3>
        {lease.amenities?.length ? <div className="tenant-lease-amenity-list">{lease.amenities.map((amenity) => {
          const label = amenity.toLowerCase()
          const Icon = label.includes('ac') || label.includes('air') ? Snowflake
            : label.includes('bike') || label.includes('parking') ? Bike
              : label.includes('sofa') ? Sofa
                : label.includes('table') ? Table2
                  : label.includes('tv') || label.includes('television') ? Tv
                    : Package
          return <span key={amenity}><Icon size={16} />{amenity}</span>
        })}</div> : <p className="tenant-lease-no-amenities">No amenities listed</p>}
      </section>
      <section className="tenant-lease-owner">
        <h3>Owner details</h3>
        <strong>{lease.ownerName}</strong>
        <div className="tenant-lease-owner-contacts">
          <a href={`mailto:${lease.ownerEmail}`}><Mail size={15} />{lease.ownerEmail}</a>
          {lease.ownerPhone && <a href={`tel:${lease.ownerPhone}`}><Phone size={15} />{lease.ownerPhone}</a>}
        </div>
      </section>
    </article> : null}
  </section>
}

export function TenantRentPage() {
  const { token } = useAuth()
  const [summary, setSummary] = useState<TenantRents | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [paymentConfirmation, setPaymentConfirmation] = useState<{ rent: Rent; method: 'online' | 'cash' } | null>(null)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true); setError('')
    try { setSummary(await api.tenantRents(token)) } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token])
  useEffect(() => { void load() }, [load])

  async function pay(rent: Rent, method: 'online' | 'cash') {
    if (!token) return
    setBusy(rent.id); setError(''); setMessage('')
    try {
      const response = method === 'online' ? await api.payOnline(token, rent.id) : await api.payCash(token, rent.id)
      setMessage(method === 'online' ? `Payment recorded for ${response.forMonth}.` : `Cash claim submitted for ${response.forMonth}. It will appear as awaiting approval.`)
      await load()
    } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(null) }
  }

  return <section className="page-section">
    <PageHeading eyebrow="TENANT PORTAL" title="Rent & payments" subtitle="Your outstanding balance and complete payment history." action={<button className="button" onClick={() => void load()}><RefreshCw size={14} /> Refresh</button>} />
    {error && <InlineError>{error}</InlineError>}{message && <div className="alert alert-success" role="status">{message}</div>}
    {loading || !summary ? <LoadingState /> : <>
      <div className="stats-grid stats-grid-tenant"><StatCard label="Total due" value={formatCurrency(summary.totalDue)} detail="Rent plus accrued fines" tone={summary.totalDue ? 'warning' : 'accent'} /><StatCard label="Next due date" value={formatDate(summary.nextDueDate)} detail="Earliest unpaid item" /><StatCard label="Payment records" value={String(summary.history.length)} detail="Across your leases" /></div>
      <Panel title="Outstanding rent" description="Pay online now or submit a cash claim for owner approval.">
        {summary.breakdown.length === 0 ? <EmptyState title="No rent due" detail="Rent records are created on each lease's monthly due date. Your balance will appear here once the first record is generated." /> : <DataTable headings={['Billing period', 'Due date', 'Rent', 'Fine', 'Total', 'Status', 'Action']}>
          {summary.breakdown.map((rent) => <tr key={rent.id}><td>{rent.forMonth}</td><td>{formatDate(rent.dueDate)}</td><td>{formatCurrency(rent.amount)}</td><td>{formatCurrency(rent.fine)}</td><td><strong>{formatCurrency(Number(rent.amount) + Number(rent.fine))}</strong></td><td><StatusBadge status={rent.status} /></td><td className="table-actions">{rent.status === 'PENDING' || rent.status === 'LATE' ? <><button className="button button-small button-primary" disabled={busy === rent.id} onClick={() => setPaymentConfirmation({ rent, method: 'online' })}>{busy === rent.id ? <span className="spinner" /> : <CreditCard size={13} />} Pay</button><button className="button button-small" disabled={busy === rent.id} onClick={() => setPaymentConfirmation({ rent, method: 'cash' })}>Cash claim</button></> : <span className="table-secondary">Awaiting owner</span>}</td></tr>)}
        </DataTable>}
      </Panel>
      <Panel title="Payment history" description="Paid and unpaid rent records, newest due date first.">
        {summary.history.length === 0 ? <EmptyState title="No payment history" detail="Payment history appears here after rent is generated for your lease." /> : <DataTable headings={['Billing period', 'Due date', 'Paid date', 'Amount', 'Method', 'Status']}>
          {summary.history.map((rent) => <tr key={rent.id}><td>{rent.forMonth}</td><td>{formatDate(rent.dueDate)}</td><td>{formatDate(rent.paidDate)}</td><td>{formatCurrency(Number(rent.amount) + Number(rent.fine))}</td><td>{rent.method || '—'}</td><td><StatusBadge status={rent.status} /></td></tr>)}
        </DataTable>}
      </Panel>
    </>}
    {paymentConfirmation && <ConfirmationDialog
      title={paymentConfirmation.method === 'online' ? 'Confirm payment?' : 'Confirm cash claim?'}
      description={paymentConfirmation.method === 'online'
        ? `Confirm payment of ${formatCurrency(Number(paymentConfirmation.rent.amount) + Number(paymentConfirmation.rent.fine))} for ${paymentConfirmation.rent.forMonth}?`
        : `Submit a cash claim for ${formatCurrency(Number(paymentConfirmation.rent.amount) + Number(paymentConfirmation.rent.fine))} for ${paymentConfirmation.rent.forMonth}? Your owner will need to approve it.`}
      confirmLabel={paymentConfirmation.method === 'online' ? 'Pay now' : 'Submit cash claim'}
      onCancel={() => setPaymentConfirmation(null)}
      onConfirm={async () => {
        await pay(paymentConfirmation.rent, paymentConfirmation.method)
        setPaymentConfirmation(null)
      }}
    />}
  </section>
}
