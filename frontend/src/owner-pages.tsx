import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowUpRight, CalendarDays, Check, CircleDollarSign, Eye, Mail, Phone, Plus, RefreshCw, Search, Trash2, Wrench, X } from 'lucide-react'
import { api, errorMessage } from './api'
import { useAuth } from './auth-context'
import { ConfirmationDialog, DataTable, EmptyState, Field, InlineError, InlineSuccess, LoadingState, Modal, PageHeading, Panel, StatCard, StatusBadge } from './components'
import { formatCurrency, formatDate } from './format'
import type { Lease, OwnerDashboard, Property, Rent, TenantDetails, User } from './types'

export function OwnerDashboardPage() {
  const { token } = useAuth()
  const [summary, setSummary] = useState<OwnerDashboard | null>(null)
  const [approvals, setApprovals] = useState<Rent[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [rentToApprove, setRentToApprove] = useState<Rent | null>(null)
  const [rentToReject, setRentToReject] = useState<Rent | null>(null)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const [data, pending] = await Promise.all([api.dashboard(token), api.approvals(token)])
      setSummary(data)
      setApprovals(pending)
    } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token])

  useEffect(() => { void load() }, [load])

  async function generate() {
    if (!token) return
    setBusy(-1)
    setMessage('')
    setError('')
    try { setMessage(await api.generateRent(token)); await load() } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(null) }
  }

  async function decide(id: number, accept: boolean): Promise<boolean> {
    if (!token) return false
    setBusy(id)
    setError('')
    try {
      if (accept) await api.approveRent(token, id)
      else await api.rejectRent(token, id)
      await load()
      return true
    } catch (cause) { setError(errorMessage(cause)); return false } finally { setBusy(null) }
  }

  return <section className="page-section">
    <PageHeading eyebrow="OWNER WORKSPACE" title="Overview" subtitle="A current view of your properties, rent and maintenance." action={<button className="button" onClick={() => void load()}><RefreshCw size={14} /> Refresh</button>} />
    {error && <InlineError>{error}</InlineError>}{message && <InlineSuccess>{message}</InlineSuccess>}
    {loading || !summary ? <LoadingState /> : <>
      <div className="stats-grid">
        <StatCard label="Active tenants" value={String(summary.tenantCount)} detail="Across active leases" />
        <StatCard label="Outstanding rent" value={formatCurrency(summary.pendingRent)} detail="Includes accrued fines" tone="accent" />
        <StatCard label="Open issues" value={String(summary.openMaintenanceCount)} detail="Open or in progress" tone={summary.openMaintenanceCount ? 'warning' : 'neutral'} />
      </div>
      <div className="dashboard-grid">
        <Panel title="Cash payment approvals" description={`${approvals.length} awaiting your review`} action={<button className="button button-quiet" onClick={() => void generate()} disabled={busy !== null}>{busy === -1 ? <span className="spinner" /> : <RefreshCw size={14} />} Generate rent</button>}>
          {approvals.length === 0 ? <EmptyState title="You're up to date" detail="Cash claims that need approval will appear here." /> : <DataTable headings={['Month', 'Amount', 'Due', 'Status', 'Actions']}>
            {approvals.map((rent) => <tr key={rent.id}><td>{rent.forMonth}</td><td>{formatCurrency(Number(rent.amount) + Number(rent.fine))}</td><td>{formatDate(rent.dueDate)}</td><td><StatusBadge status={rent.status} /></td><td className="table-actions"><button className="button button-small button-primary" disabled={busy === rent.id} onClick={() => setRentToApprove(rent)}><Check size={13} /> Accept</button><button className="button button-small button-danger" disabled={busy === rent.id} onClick={() => setRentToReject(rent)}><X size={13} /> Reject</button></td></tr>)}
          </DataTable>}
        </Panel>
        <Panel title="Quick links" description="Common owner tasks">
          <div className="quick-links"><Link to="/owner/properties"><Building2Icon /> Add or update a property <ArrowUpRight size={14} /></Link><Link to="/owner/leases"><Search size={16} /> Find a tenant and assign a lease <ArrowUpRight size={14} /></Link><Link to="/owner/maintenance"><Wrench size={16} /> Review maintenance requests <ArrowUpRight size={14} /></Link></div>
        </Panel>
      </div>
    </>}
    {rentToApprove && <ConfirmationDialog title="Approve this cash payment?" description={`The tenant's cash claim for ${rentToApprove.forMonth} will be marked as paid.`} confirmLabel="Approve payment" onCancel={() => setRentToApprove(null)} onConfirm={async () => { await decide(rentToApprove.id, true); setRentToApprove(null) }} />}
    {rentToReject && <ConfirmationDialog title="Reject this cash payment?" description={`The tenant's cash claim for ${rentToReject.forMonth} will be rejected.`} confirmLabel="Reject payment" onCancel={() => setRentToReject(null)} onConfirm={async () => { await decide(rentToReject.id, false); setRentToReject(null) }} />}
  </section>
}

function Building2Icon() { return <CircleDollarSign size={16} /> }

function PropertyForm({ initial, onCancel, onSave }: { initial?: Property; onCancel: () => void; onSave: (body: Partial<Property>) => Promise<void> }) {
  const [values, setValues] = useState({ title: initial?.title || '', address: initial?.address || '', type: initial?.type || 'FLAT', rentAmount: initial?.rentAmount?.toString() || '', description: initial?.description || '', amenities: initial?.amenities?.join(', ') || '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [serverError, setServerError] = useState('')
  function set(field: keyof typeof values, value: string) { setValues((current) => ({ ...current, [field]: value })); setErrors((current) => ({ ...current, [field]: '' })); setServerError('') }
  async function submit(event: React.FormEvent) {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!values.title.trim()) next.title = 'Enter a property name.'
    if (!values.address.trim()) next.address = 'Enter the property address.'
    if (!values.rentAmount || !Number.isFinite(Number(values.rentAmount)) || Number(values.rentAmount) <= 0) next.rentAmount = 'Enter a rent amount above zero.'
    setErrors(next)
    if (Object.keys(next).length) return
    setBusy(true)
    try {
      await onSave({ title: values.title.trim(), address: values.address.trim(), type: values.type, rentAmount: Number(values.rentAmount), description: values.description.trim(), amenities: values.amenities.split(/[,\n]/).map((item) => item.trim()).filter(Boolean) })
    } catch (cause) { setServerError(errorMessage(cause)) } finally { setBusy(false) }
  }
  return <form className="form-stack" onSubmit={submit} noValidate>
    {serverError && <InlineError>{serverError}</InlineError>}
    <div className="form-grid"><Field label="Property name" id="property-title" value={values.title} onChange={(v) => set('title', v)} error={errors.title} required /><Field label="Type" id="property-type" value={values.type} onChange={(v) => set('type', v)} required /></div>
    <Field label="Address" id="property-address" value={values.address} onChange={(v) => set('address', v)} error={errors.address} required />
    <div className="form-grid"><Field label="Monthly rent" id="property-rent" type="number" min="0.01" step="0.01" value={values.rentAmount} onChange={(v) => set('rentAmount', v)} error={errors.rentAmount} required /><Field label="Amenities" id="property-amenities" value={values.amenities} onChange={(v) => set('amenities', v)} hint="Separate names with commas." /></div>
    <div className="field-group"><label htmlFor="property-description">Description</label><textarea id="property-description" value={values.description} onChange={(e) => set('description', e.target.value)} rows={3} /></div>
    <div className="modal-actions"><button type="button" className="button" onClick={onCancel}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? <span className="spinner" /> : null}{initial ? 'Save changes' : 'Create property'}</button></div>
  </form>
}

export function OwnerPropertiesPage() {
  const { token } = useAuth()
  const [properties, setProperties] = useState<Property[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState<'new' | Property | null>(null)
  const [notice, setNotice] = useState('')
  const [deleting, setDeleting] = useState<number | null>(null)
  const [propertyToDelete, setPropertyToDelete] = useState<Property | null>(null)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true); setError('')
    try { setProperties(await api.properties(token)) } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token])
  useEffect(() => { void load() }, [load])

  async function save(body: Partial<Property>) {
    if (!token) return
    if (modal && modal !== 'new') await api.updateProperty(token, modal.id, body)
    else await api.createProperty(token, body)
    setModal(null); setNotice('Property saved.'); await load()
  }

  async function remove(property: Property): Promise<boolean> {
    if (!token) return false
    setDeleting(property.id); setError('')
    try { await api.deleteProperty(token, property.id); setNotice('Property deleted.'); await load(); return true } catch (cause) { setError(errorMessage(cause)); return false } finally { setDeleting(null) }
  }

  return <section className="page-section">
    <PageHeading eyebrow="PORTFOLIO" title="Properties" subtitle="Manage rental details and amenities for your portfolio." action={<button className="button button-primary" onClick={() => setModal('new')}><Plus size={15} /> Add property</button>} />
    {error && <InlineError>{error}</InlineError>}{notice && <InlineSuccess>{notice}</InlineSuccess>}
    <Panel title="Your properties" description={`${properties.length} properties`}>
      {loading ? <LoadingState /> : properties.length === 0 ? <EmptyState title="No properties yet" detail="Add your first property to start managing leases and rent." action={<button className="button button-primary" onClick={() => setModal('new')}><Plus size={14} /> Add property</button>} /> : <DataTable headings={['Property', 'Type', 'Rent / month', 'Amenities', 'Status', '']}>
        {properties.map((property) => <tr key={property.id}><td><strong>{property.title}</strong><small className="table-secondary">{property.address}</small></td><td>{property.type}</td><td>{formatCurrency(property.rentAmount)}</td><td><span className="amenity-summary">{property.amenities?.length ? property.amenities.join(', ') : '—'}</span></td><td><StatusBadge status={property.status} /></td><td className="table-actions"><button className="button button-small" onClick={() => setModal(property)}>Edit</button><button className="icon-action" aria-label={`Delete ${property.title}`} disabled={deleting === property.id} title="Delete property" onClick={() => setPropertyToDelete(property)}><Trash2 size={15} /></button></td></tr>)}
      </DataTable>}
    </Panel>
    {modal && <Modal title={modal === 'new' ? 'Add property' : 'Edit property'} description="Property details and amenities" onClose={() => setModal(null)}><PropertyForm initial={modal === 'new' ? undefined : modal} onCancel={() => setModal(null)} onSave={save} /></Modal>}
    {propertyToDelete && <ConfirmationDialog title={`Delete ${propertyToDelete.title}?`} description="This permanently deletes the property and cannot be undone." confirmLabel="Delete property" onCancel={() => setPropertyToDelete(null)} onConfirm={async () => { await remove(propertyToDelete); setPropertyToDelete(null) }} />}
  </section>
}

export function OwnerLeasesPage() {
  const { token } = useAuth()
  const [leases, setLeases] = useState<Lease[]>([])
  const [properties, setProperties] = useState<Property[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [open, setOpen] = useState(false)
  const [code, setCode] = useState('')
  const [tenant, setTenant] = useState<User | null>(null)
  const [form, setForm] = useState({ propertyId: '', startDate: '', monthlyRent: '', fineGraceDays: '5', finePerDay: '0' })
  const [busy, setBusy] = useState(false)
  const [leaseToEnd, setLeaseToEnd] = useState<Lease | null>(null)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true); setError('')
    try { const [leaseRows, propertyRows] = await Promise.all([api.leases(token), api.properties(token)]); setLeases(leaseRows); setProperties(propertyRows) } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token])
  useEffect(() => { void load() }, [load])

  async function searchTenant() {
    if (!token || !code.trim()) { setError('Enter a tenant code.'); return }
    setBusy(true); setError(''); setTenant(null)
    try { setTenant(await api.findTenant(token, code.trim())) } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(false) }
  }

  async function assign(event: React.FormEvent) {
    event.preventDefault()
    if (!token || !tenant) return
    if (!form.propertyId || !form.startDate || Number(form.monthlyRent) <= 0) { setError('Select a property, start date, and valid monthly rent.'); return }
    setBusy(true); setError('')
    try {
      await api.createLease(token, { propertyId: Number(form.propertyId), tenantCode: tenant.tenantCode || code.trim(), startDate: form.startDate, monthlyRent: Number(form.monthlyRent), fineGraceDays: Number(form.fineGraceDays), finePerDay: Number(form.finePerDay) })
      setNotice(`Lease created for ${tenant.name}.`); setOpen(false); setTenant(null); setCode(''); await load()
    } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(false) }
  }

  async function end(id: number): Promise<boolean> {
    if (!token) return false
    setBusy(true); setError('')
    try { await api.endLease(token, id); setNotice('Lease ended.'); await load(); return true } catch (cause) { setError(errorMessage(cause)); return false } finally { setBusy(false) }
  }

  return <section className="page-section">
    <PageHeading eyebrow="OCCUPANCY" title="Tenants & leases" subtitle="Find a registered tenant by code and assign them to one of your properties." action={<button className="button button-primary" onClick={() => setOpen(true)}><Plus size={15} /> Assign tenant</button>} />
    {error && <InlineError>{error}</InlineError>}{notice && <InlineSuccess>{notice}</InlineSuccess>}
    <Panel title="Lease register" description={`${leases.length} leases, including history`}>
      {loading ? <LoadingState /> : leases.length === 0 ? <EmptyState title="No leases yet" detail="Assign a registered tenant to one of your properties." /> : <DataTable headings={['Tenant', 'Property', 'Start date', 'Monthly rent', 'Lease', 'Action']}>
        {leases.map((lease) => <tr key={lease.id}><td><Link className="tenant-lease-link" to={`/owner/leases/tenants/${lease.tenantId}?propertyId=${lease.propertyId}`}><strong>{lease.tenantName}</strong><small className="table-secondary">{lease.tenantCode}</small></Link></td><td><strong>{lease.propertyTitle}</strong>{lease.amenities?.length > 0 && <small className="table-secondary">{lease.amenities.join(' · ')}</small>}</td><td>{formatDate(lease.startDate)}</td><td>{formatCurrency(lease.monthlyRent)}</td><td><StatusBadge status={lease.status} /></td><td><span className="notice-table-actions"><Link className="notice-table-action notice-table-open" to={`/owner/leases/tenants/${lease.tenantId}?propertyId=${lease.propertyId}`} aria-label={`View ${lease.tenantName} details for ${lease.propertyTitle}`} title="View tenant details"><Eye size={16} /></Link>{lease.status === 'ACTIVE' && <button className="button button-small button-danger" disabled={busy} onClick={() => setLeaseToEnd(lease)}>End lease</button>}</span></td></tr>)}
      </DataTable>}
    </Panel>
    {open && <Modal title="Assign a tenant" description="Tenant assignment creates an active lease." onClose={() => { setOpen(false); setTenant(null); setError('') }}>
      <form className="form-stack" onSubmit={assign} noValidate>
        <div className="field-group"><label htmlFor="tenant-code">Tenant code <span className="required-mark">*</span></label><div className="inline-field"><input id="tenant-code" value={code} onChange={(event) => { setCode(event.target.value); setTenant(null) }} placeholder="TNT-123" required /><button type="button" className="button" disabled={busy} onClick={() => void searchTenant()}><Search size={14} /> Find</button></div><span className="field-help">The tenant must have a registered account.</span></div>
        {tenant && <div className="tenant-confirm"><span className="workspace-avatar">{tenant.name.slice(0, 1)}</span><div><strong>{tenant.name}</strong><small>{tenant.email} · {tenant.tenantCode}</small></div></div>}
        <div className="field-group"><label htmlFor="lease-property">Property <span className="required-mark">*</span></label><select id="lease-property" value={form.propertyId} onChange={(event) => { const selected = properties.find((property) => property.id === Number(event.target.value)); setForm((current) => ({ ...current, propertyId: event.target.value, monthlyRent: selected?.rentAmount?.toString() || current.monthlyRent })) }} required><option value="">Choose available property</option>{properties.filter((property) => property.status === 'AVAILABLE').map((property) => <option key={property.id} value={property.id}>{property.title} · {property.address}</option>)}</select></div>
        <div className="form-grid"><Field label="Start date" id="lease-start" type="date" value={form.startDate} onChange={(value) => setForm((current) => ({ ...current, startDate: value }))} required /><Field label="Monthly rent" id="lease-rent" type="number" min="0.01" step="0.01" value={form.monthlyRent} onChange={(value) => setForm((current) => ({ ...current, monthlyRent: value }))} required /></div>
        <div className="form-grid"><Field label="Fine grace days" id="lease-grace" type="number" min="0" step="1" value={form.fineGraceDays} onChange={(value) => setForm((current) => ({ ...current, fineGraceDays: value }))} /><Field label="Fine per day" id="lease-fine" type="number" min="0" step="0.01" value={form.finePerDay} onChange={(value) => setForm((current) => ({ ...current, finePerDay: value }))} /></div>
        <div className="modal-actions"><button type="button" className="button" onClick={() => setOpen(false)}>Cancel</button><button className="button button-primary" disabled={busy || !tenant}>{busy ? <span className="spinner" /> : null}Create lease</button></div>
      </form>
    </Modal>}
    {leaseToEnd && <ConfirmationDialog title="End this lease?" description={`The lease for ${leaseToEnd.tenantName} at ${leaseToEnd.propertyTitle} will end. Its history will be retained.`} confirmLabel="End lease" onCancel={() => setLeaseToEnd(null)} onConfirm={async () => { await end(leaseToEnd.id); setLeaseToEnd(null) }} />}
  </section>
}

export function OwnerTenantDetailPage() {
  const { token } = useAuth()
  const { tenantId } = useParams()
  const { search } = useLocation()
  const [tenant, setTenant] = useState<TenantDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const selectedPropertyId = new URLSearchParams(search).get('propertyId')

  const load = useCallback(async () => {
    if (!token || !tenantId || !/^\d+$/.test(tenantId)) {
      setError('Tenant details could not be loaded.')
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      setTenant(await api.tenantDetails(token, Number(tenantId)))
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [tenantId, token])

  useEffect(() => { void load() }, [load])

  return <section className="page-section">
    <PageHeading eyebrow="OCCUPANCY" title={tenant?.name || 'Tenant details'} subtitle="Tenant contact information, lease history, and maintenance activity." action={<button className="button" onClick={() => navigate('/owner/leases')}><ArrowLeft size={14} /> Back to tenants & leases</button>} />
    {error && <InlineError>{error}</InlineError>}
    {loading ? <LoadingState label="Loading tenant details…" /> : tenant && <>
      <div className="stats-grid stats-grid-tenant">
        <StatCard label="Leases" value={String(tenant.leases.length)} detail="Including ended leases" />
        <div className="stat-card stat-neutral">
          <span>Maintenance requests</span>
          <Link
            className="owner-tenant-maintenance-count"
            to={`/owner/maintenance?propertyId=${selectedPropertyId && tenant.maintenanceRequestsByProperty[Number(selectedPropertyId)] !== undefined
              ? Number(selectedPropertyId)
              : tenant.leases[0].propertyId}`}
            aria-label={`View ${tenant.maintenanceRequestsByProperty[selectedPropertyId ? Number(selectedPropertyId) : tenant.leases[0].propertyId] ?? tenant.maintenanceRequestCount} maintenance requests for ${tenant.leases.find((lease) => lease.propertyId === Number(selectedPropertyId))?.propertyTitle ?? tenant.leases[0].propertyTitle}`}
          >
            {tenant.maintenanceRequestsByProperty[selectedPropertyId && tenant.maintenanceRequestsByProperty[Number(selectedPropertyId)] !== undefined
              ? Number(selectedPropertyId)
              : tenant.leases[0].propertyId] ?? 0}
          </Link>
          <small>For {tenant.leases.find((lease) => lease.propertyId === Number(selectedPropertyId))?.propertyTitle ?? tenant.leases[0].propertyTitle}</small>
        </div>
        <StatCard label="Tenant since" value={formatDate(tenant.createdAt)} detail="Account registration date" />
      </div>
      <Panel title="Tenant information" description={`Tenant ID ${tenant.tenantCode}`}>
        <div className="owner-tenant-contact">
          <div><span>Name</span><strong>{tenant.name}</strong></div>
          <div><span>Email</span><a href={`mailto:${tenant.email}`}><Mail size={15} />{tenant.email}</a></div>
          <div><span>Phone</span>{tenant.phone ? <a href={`tel:${tenant.phone}`}><Phone size={15} />{tenant.phone}</a> : <strong>Not provided</strong>}</div>
        </div>
      </Panel>
      <Panel title="Leased properties" description="Lease start and end dates for this tenant at your properties.">
        {tenant.leases.length ? <DataTable headings={['Property', 'Lease started', 'Lease ended', 'Monthly rent', 'Status']}>
          {tenant.leases.map((lease) => <tr key={lease.id}><td><strong>{lease.propertyTitle}</strong></td><td><CalendarDays size={14} className="inline-icon" />{formatDate(lease.startDate)}</td><td>{formatDate(lease.endDate)}</td><td>{formatCurrency(lease.monthlyRent)}</td><td><StatusBadge status={lease.status} /></td></tr>)}
        </DataTable> : <EmptyState title="No lease history" detail="This tenant has no leases for your properties." />}
      </Panel>
    </>}
  </section>
}

export function OwnerRentPage() {
  const { token } = useAuth()
  const [leases, setLeases] = useState<Lease[]>([])
  const [selected, setSelected] = useState('')
  const [history, setHistory] = useState<Rent[]>([])
  const [approvals, setApprovals] = useState<Rent[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [rentToApprove, setRentToApprove] = useState<Rent | null>(null)
  const [rentToReject, setRentToReject] = useState<Rent | null>(null)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true); setError('')
    try { const [leaseRows, pending] = await Promise.all([api.leases(token), api.approvals(token)]); setLeases(leaseRows); setApprovals(pending) } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token])
  useEffect(() => { void load() }, [load])

  async function selectLease(id: string) {
    setSelected(id); setHistory([])
    if (!token || !id) return
    setBusy(true); setError('')
    try { setHistory(await api.leaseHistory(token, Number(id))) } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(false) }
  }

  async function generate() {
    if (!token) return
    setBusy(true); setError(''); setMessage('')
    try { setMessage(await api.generateRent(token)); await load(); if (selected) await selectLease(selected) } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(false) }
  }

  async function decide(id: number, accept: boolean): Promise<boolean> {
    if (!token) return false
    setBusy(true); setError('')
    try {
      if (accept) await api.approveRent(token, id)
      else await api.rejectRent(token, id)
      await load()
      if (selected) await selectLease(selected)
      return true
    } catch (cause) { setError(errorMessage(cause)); return false } finally { setBusy(false) }
  }

  return <section className="page-section">
    <PageHeading eyebrow="RENT OPERATIONS" title="Rent" subtitle="Review cash claims, generate rent and inspect lease payment history." action={<button className="button button-primary" disabled={busy} onClick={() => void generate()}>{busy ? <span className="spinner" /> : <RefreshCw size={14} />} Generate rent</button>} />
    {error && <InlineError>{error}</InlineError>}{message && <InlineSuccess>{message}</InlineSuccess>}
    <Panel title="Cash payment approvals" description={`${approvals.length} awaiting review`}>
      {loading ? <LoadingState /> : approvals.length === 0 ? <EmptyState title="No approvals pending" detail="Tenant cash claims will appear here." /> : <DataTable headings={['Month', 'Lease', 'Amount', 'Due date', 'Actions']}>
        {approvals.map((rent) => <tr key={rent.id}><td>{rent.forMonth}</td><td>#{rent.leaseId}</td><td>{formatCurrency(Number(rent.amount) + Number(rent.fine))}</td><td>{formatDate(rent.dueDate)}</td><td className="table-actions"><button className="button button-small button-primary" disabled={busy} onClick={() => setRentToApprove(rent)}>Accept</button><button className="button button-small button-danger" disabled={busy} onClick={() => setRentToReject(rent)}>Reject</button></td></tr>)}
      </DataTable>}
    </Panel>
    <Panel title="Payment history" description="Choose a lease to inspect its rent records.">
      <div className="filter-row"><div className="field-group"><label htmlFor="rent-lease">Lease</label><select id="rent-lease" value={selected} onChange={(event) => void selectLease(event.target.value)}><option value="">Select a lease</option>{leases.map((lease) => <option key={lease.id} value={lease.id}>{lease.propertyTitle} · {lease.tenantName} · #{lease.id}</option>)}</select></div></div>
      {busy ? <LoadingState /> : selected && history.length ? <DataTable headings={['Month', 'Due date', 'Amount', 'Fine', 'Paid', 'Status']}>
        {history.map((rent) => <tr key={rent.id}><td>{rent.forMonth}</td><td>{formatDate(rent.dueDate)}</td><td>{formatCurrency(rent.amount)}</td><td>{formatCurrency(rent.fine)}</td><td>{formatDate(rent.paidDate)}</td><td><StatusBadge status={rent.status} /></td></tr>)}
      </DataTable> : selected ? <EmptyState title="No rent records" detail="Rent records appear after a billing cycle is generated." /> : null}
    </Panel>
    {rentToApprove && <ConfirmationDialog title="Approve this cash payment?" description={`The tenant's cash claim for ${rentToApprove.forMonth} will be marked as paid.`} confirmLabel="Approve payment" onCancel={() => setRentToApprove(null)} onConfirm={async () => { await decide(rentToApprove.id, true); setRentToApprove(null) }} />}
    {rentToReject && <ConfirmationDialog title="Reject this cash payment?" description={`The tenant's cash claim for ${rentToReject.forMonth} will be rejected.`} confirmLabel="Reject payment" onCancel={() => setRentToReject(null)} onConfirm={async () => { await decide(rentToReject.id, false); setRentToReject(null) }} />}
  </section>
}
