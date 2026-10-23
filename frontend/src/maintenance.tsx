import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Ban, ChevronLeft, ChevronRight, Eye, Pencil, Plus, Search, Send } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { api, errorMessage } from './api'
import { useAuth } from './auth-context'
import { ConfirmationDialog, EmptyState, Field, InlineError, InlineSuccess, LoadingState, Modal, PageHeading, Panel, StatusBadge } from './components'
import { formatDate } from './format'
import type { Lease, MaintenanceComment, MaintenanceRequest, Property } from './types'

const pageSize = 5

export function MaintenancePage() {
  const { token, user } = useAuth()
  const owner = user?.role === 'OWNER'
  const [searchParams] = useSearchParams()
  const [properties, setProperties] = useState<Property[]>([])
  const [leases, setLeases] = useState<Lease[]>([])
  const [requests, setRequests] = useState<MaintenanceRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editingRequest, setEditingRequest] = useState<MaintenanceRequest | null>(null)
  const [requestToCancel, setRequestToCancel] = useState<MaintenanceRequest | null>(null)
  const [selectedProperty, setSelectedProperty] = useState('')
  const [form, setForm] = useState({ title: '', description: '', priority: 'MEDIUM' })
  const [search, setSearch] = useState('')
  const [propertyFilter, setPropertyFilter] = useState(() => searchParams.get('propertyId') ?? '')
  const [page, setPage] = useState(0)
  const [total, setTotal] = useState(0)
  const [pageCount, setPageCount] = useState(0)
  const [busy, setBusy] = useState<number | null>(null)
  const [pendingStatus, setPendingStatus] = useState<{ request: MaintenanceRequest; status: MaintenanceRequest['status'] } | null>(null)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await api.maintenancePage(token, {
        page,
        size: pageSize,
        search: search.trim() || undefined,
        propertyId: propertyFilter ? Number(propertyFilter) : undefined,
      })
      setRequests(result.content)
      setTotal(result.totalElements)
      setPageCount(result.totalPages)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [token, page, search, propertyFilter])

  useEffect(() => { void load() }, [load])

  useEffect(() => {
    if (!token) return
    const accessToken = token
    async function loadProperties() {
      try {
        if (owner) {
          setProperties(await api.properties(accessToken))
        } else {
          const rows = await api.tenantLeases(accessToken)
          setLeases(rows)
          setProperties(Array.from(new Map(rows.map((lease) => [lease.propertyId, { id: lease.propertyId, title: lease.propertyTitle } as Property])).values()))
        }
      } catch (cause) {
        setError(errorMessage(cause))
      }
    }
    void loadProperties()
  }, [token, owner])

  async function createRequest(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    if (!selectedProperty || !form.title.trim() || !form.description.trim()) {
      setError('Choose a property and complete the title and description.')
      return
    }
    setBusy(-1)
    setError('')
    try {
      await api.createMaintenance(token, {
        propertyId: Number(selectedProperty),
        title: form.title.trim(),
        description: form.description.trim(),
        priority: form.priority,
      })
      setNotice('Maintenance request submitted.')
      setCreateOpen(false)
      setForm({ title: '', description: '', priority: 'MEDIUM' })
      setPage(0)
      if (page === 0) await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(null)
    }
  }

  async function updateStatus(request: MaintenanceRequest, status: MaintenanceRequest['status']) {
    if (!token) return
    setBusy(request.id)
    setError('')
    try {
      await api.updateMaintenanceStatus(token, request.id, status)
      setNotice('Request status updated.')
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(null)
      setPendingStatus(null)
    }
  }

  async function saveRequestChanges(event: FormEvent) {
    event.preventDefault()
    if (!token || !editingRequest) return
    setBusy(editingRequest.id)
    setError('')
    try {
      await api.updateMaintenance(token, editingRequest.id, {
        ...form,
        title: form.title.trim(),
        description: form.description.trim(),
      })
      setNotice('Maintenance request updated.')
      setEditingRequest(null)
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(null)
    }
  }

  async function cancelTenantRequest() {
    if (!token || !requestToCancel) return
    setBusy(requestToCancel.id)
    setError('')
    try {
      await api.cancelMaintenance(token, requestToCancel.id)
      setNotice('Maintenance request cancelled.')
      setRequestToCancel(null)
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(null)
    }
  }

  const hasFilters = Boolean(search.trim() || propertyFilter)
  const shownStart = total === 0 ? 0 : page * pageSize + 1
  const shownEnd = Math.min((page + 1) * pageSize, total)

  return <section className="page-section">
    <PageHeading eyebrow="PROPERTY CARE" title="Maintenance" subtitle={owner ? 'Track and resolve tenant-reported property issues.' : 'Report an issue and follow its progress.'} action={!owner && leases.length > 0 ? <button className="button button-primary" onClick={() => setCreateOpen(true)}><Plus size={15} /> New request</button> : undefined} />
    {error && <InlineError>{error}</InlineError>}{notice && <InlineSuccess>{notice}</InlineSuccess>}
    <Panel title={owner ? 'Requests across your properties' : 'My requests'} description={`${total} requests`} action={<div className="notice-table-filters">
      <label className="notice-search-field" htmlFor="maintenance-search"><Search size={16} /><input id="maintenance-search" type="search" placeholder="Search requests..." value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} /></label>
      <select aria-label="Filter requests by property" value={propertyFilter} onChange={(event) => { setPropertyFilter(event.target.value); setPage(0) }}>
        <option value="">All properties</option>
        {properties.map((property) => <option key={property.id} value={property.id}>{property.title}</option>)}
      </select>
    </div>}>
      {loading ? <LoadingState /> : requests.length === 0 ? <EmptyState title={hasFilters ? 'No matching requests' : 'No maintenance requests'} detail={hasFilters ? 'Try another search or property filter.' : owner ? 'Tenant issues will appear here.' : 'You have not reported an issue for your leased property.'} action={!owner && leases.length > 0 && !hasFilters ? <button className="button button-primary" onClick={() => setCreateOpen(true)}><Plus size={14} /> New request</button> : undefined} /> : <>
        <div className="table-scroll">
          <table className="notice-table maintenance-table">
            <thead><tr><th>Issue</th><th>Property</th><th>Reported by</th><th>Priority</th><th>Status</th><th>Created</th><th>Action</th></tr></thead>
            <tbody>{requests.map((request) => {
              const detailPath = `/${owner ? 'owner' : 'tenant'}/maintenance/${request.id}`
              return <tr key={request.id}>
                <td className="notice-table-title">{request.title}<small className="table-secondary">{request.description}</small></td>
                <td>{request.propertyTitle}</td>
                <td>{owner ? request.tenantName : 'You'}</td>
                <td><StatusBadge status={request.priority} /></td>
                <td>{owner && request.status !== 'CANCELLED' && request.status !== 'RESOLVED' ? <select className="compact-select" aria-label={`Update status for ${request.title}`} value={request.status} disabled={busy === request.id} onChange={(event) => setPendingStatus({ request, status: event.target.value as MaintenanceRequest['status'] })}>{request.status === 'OPEN' && <option value="OPEN">Open</option>}{request.status === 'OPEN' && <option value="IN_PROGRESS">In progress</option>}{request.status === 'IN_PROGRESS' && <option value="IN_PROGRESS">In progress</option>}<option value="RESOLVED">Resolved</option></select> : <StatusBadge status={request.status} />}</td>
                <td>{formatDate(request.createdAt)}</td>
                <td>{owner ? <Link className="notice-table-view" to={detailPath}>View details <ChevronRight size={16} /></Link> : <span className="notice-table-actions"><Link className="notice-table-action notice-table-open" to={detailPath} aria-label={`View request: ${request.title}`} title="View request"><Eye size={16} /></Link><button className="notice-table-action notice-table-edit" type="button" disabled={request.status === 'RESOLVED'} onClick={() => { setEditingRequest(request); setForm({ title: request.title, description: request.description, priority: request.priority }) }} aria-label={`Edit request: ${request.title}`} title={request.status === 'RESOLVED' ? 'Resolved requests cannot be edited' : 'Edit request'}><Pencil size={16} /></button>{request.status === 'OPEN' && <button className="notice-table-action notice-table-delete" type="button" onClick={() => setRequestToCancel(request)} aria-label={`Cancel request: ${request.title}`} title="Cancel request"><Ban size={16} /></button>}</span>}</td>
              </tr>
            })}</tbody>
          </table>
        </div>
        <footer className="notice-table-footer">
          <span>Showing {shownStart}–{shownEnd} of {total} requests</span>
          <nav className="notice-pagination" aria-label="Maintenance request pages">
            <button type="button" className="icon-button" aria-label="Previous page" disabled={page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))}><ChevronLeft size={18} /></button>
            <span className="notice-page-current" aria-current="page">{page + 1}</span>
            <button type="button" className="icon-button" aria-label="Next page" disabled={page + 1 >= pageCount} onClick={() => setPage((current) => Math.min(pageCount - 1, current + 1))}><ChevronRight size={18} /></button>
          </nav>
        </footer>
      </>}
    </Panel>
    {pendingStatus && <ConfirmationDialog title={pendingStatus.status === 'RESOLVED' ? 'Resolve this request?' : 'Move this request in progress?'} description={`The status of “${pendingStatus.request.title}” will change to ${pendingStatus.status === 'RESOLVED' ? 'Resolved' : 'In progress'}. This status cannot be changed back.`} confirmLabel={pendingStatus.status === 'RESOLVED' ? 'Resolve request' : 'Mark in progress'} onCancel={() => setPendingStatus(null)} onConfirm={() => void updateStatus(pendingStatus.request, pendingStatus.status)} />}
    {createOpen && <Modal title="Report a maintenance issue" description="Your property owner will be notified." onClose={() => setCreateOpen(false)}>
      <form className="form-stack" onSubmit={createRequest} noValidate>
        <div className="field-group"><label htmlFor="issue-property">Property <span className="required-mark">*</span></label><select id="issue-property" value={selectedProperty} onChange={(event) => setSelectedProperty(event.target.value)} required><option value="">Choose an active lease</option>{leases.map((lease) => <option key={lease.id} value={lease.propertyId}>{lease.propertyTitle}</option>)}</select></div>
        <Field label="Title" id="issue-title" value={form.title} onChange={(value) => setForm((current) => ({ ...current, title: value }))} required />
        <div className="field-group"><label htmlFor="issue-description">Description <span className="required-mark">*</span></label><textarea id="issue-description" value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} rows={4} required /></div>
        <div className="field-group"><label htmlFor="issue-priority">Priority <span className="required-mark">*</span></label><select id="issue-priority" value={form.priority} onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))} required><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></div>
        <div className="modal-actions"><button type="button" className="button" onClick={() => setCreateOpen(false)}>Cancel</button><button className="button button-primary" disabled={busy === -1}>{busy === -1 ? <span className="spinner" /> : null}Submit request</button></div>
      </form>
    </Modal>}
    {editingRequest && <Modal title="Edit maintenance request" description="Update your issue title, description, or priority." onClose={() => setEditingRequest(null)}>
      <form className="form-stack" onSubmit={saveRequestChanges} noValidate>
        <Field label="Title" id="maintenance-list-title" value={form.title} onChange={(value) => setForm((current) => ({ ...current, title: value }))} required />
        <div className="field-group"><label htmlFor="maintenance-list-description">Description <span className="required-mark">*</span></label><textarea id="maintenance-list-description" rows={4} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} required /></div>
        <div className="field-group"><label htmlFor="maintenance-list-priority">Priority <span className="required-mark">*</span></label><select id="maintenance-list-priority" value={form.priority} onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))} required><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></div>
        <div className="modal-actions"><button type="button" className="button" onClick={() => setEditingRequest(null)}>Cancel</button><button className="button button-primary" disabled={busy === editingRequest.id}>{busy === editingRequest.id ? <span className="spinner" /> : null}Save changes</button></div>
      </form>
    </Modal>}
    {requestToCancel && <ConfirmationDialog title={`Cancel “${requestToCancel.title}”?`} description="You can cancel this request only while it is open." confirmLabel="Cancel request" onCancel={() => setRequestToCancel(null)} onConfirm={() => void cancelTenantRequest()} />}
  </section>
}

export function MaintenanceDetailPage() {
  const { token, user } = useAuth()
  const { requestId } = useParams()
  const owner = user?.role === 'OWNER'
  const id = Number(requestId)
  const [request, setRequest] = useState<MaintenanceRequest | null>(null)
  const [comments, setComments] = useState<MaintenanceComment[]>([])
  const [commentsOpen, setCommentsOpen] = useState(false)
  const [commentDraft, setCommentDraft] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [pendingStatus, setPendingStatus] = useState<MaintenanceRequest['status'] | null>(null)
  const [form, setForm] = useState({ title: '', description: '', priority: 'MEDIUM' })

  const load = useCallback(async () => {
    if (!token || !Number.isInteger(id) || id <= 0) {
      setError('Invalid maintenance request.')
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      const [requestData, commentData] = await Promise.all([api.maintenanceRequest(token, id), api.comments(token, id)])
      setRequest(requestData)
      setComments(commentData)
      setForm({ title: requestData.title, description: requestData.description, priority: requestData.priority })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [token, id])

  useEffect(() => { void load() }, [load])

  async function addComment(event: FormEvent) {
    event.preventDefault()
    if (!token || !commentDraft.trim()) return
    setBusy(true)
    setError('')
    try {
      await api.addComment(token, id, commentDraft.trim())
      setCommentDraft('')
      setComments(await api.comments(token, id))
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  async function changeStatus() {
    if (!token || !request || !pendingStatus) return
    setBusy(true)
    setError('')
    try {
      await api.updateMaintenanceStatus(token, id, pendingStatus)
      setSuccess('Request status updated.')
      setPendingStatus(null)
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  async function saveChanges(event: FormEvent) {
    event.preventDefault()
    if (!token || !request) return
    setBusy(true)
    setError('')
    try {
      await api.updateMaintenance(token, id, { ...form, title: form.title.trim(), description: form.description.trim() })
      setSuccess('Maintenance request updated.')
      setEditOpen(false)
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  async function cancelRequest() {
    if (!token || !request) return
    setBusy(true)
    setError('')
    try {
      await api.cancelMaintenance(token, id)
      setSuccess('Maintenance request cancelled.')
      setCancelOpen(false)
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  return <section className="page-section">
    {loading ? <Panel title="Maintenance request"><LoadingState /></Panel> : error && !request ? <><InlineError>{error}</InlineError><Link className="button" to={`/${owner ? 'owner' : 'tenant'}/maintenance`}>Back to maintenance</Link></> : request ? <>
      <PageHeading eyebrow="PROPERTY CARE" title="Maintenance request" subtitle={`Request #${request.id} · ${request.status.replace('_', ' ')}`} action={!owner ? <div className="notice-table-actions">{request.status === 'OPEN' && <button className="button notice-delete-button" onClick={() => setCancelOpen(true)}><Ban size={14} /> Cancel request</button>}<button className="button" disabled={request.status === 'RESOLVED'} title={request.status === 'RESOLVED' ? 'Resolved requests cannot be edited' : 'Edit request'} onClick={() => setEditOpen(true)}><Pencil size={14} /> Edit</button></div> : undefined} />
      {error && <InlineError>{error}</InlineError>}{success && <InlineSuccess>{success}</InlineSuccess>}
      {!owner && request.status === 'OPEN' && <div className="alert alert-info maintenance-cancel-hint" role="note">You can cancel this request only while its status is Open.</div>}
      <Panel title={request.title} description={`${request.propertyTitle} · Reported by ${owner ? request.tenantName : 'You'}`}>
        <div className="maintenance-detail-meta">
          <span><strong>Priority</strong><StatusBadge status={request.priority} /></span>
          <span><strong>Status</strong>{owner && request.status !== 'RESOLVED' && request.status !== 'CANCELLED' ? <select className="compact-select" aria-label="Update request status" value={request.status} onChange={(event) => setPendingStatus(event.target.value as MaintenanceRequest['status'])}><option value={request.status}>{request.status.replace('_', ' ')}</option>{request.status === 'OPEN' && <option value="IN_PROGRESS">In progress</option>}<option value="RESOLVED">Resolved</option></select> : <StatusBadge status={request.status} />}</span>
          <span><strong>Reported</strong>{formatDate(request.createdAt)}</span>
          {request.resolvedAt && <span><strong>Resolved</strong>{formatDate(request.resolvedAt)}</span>}
        </div>
        <div className="maintenance-detail-description"><h3>Description</h3><p>{request.description}</p></div>
      </Panel>
      <Panel
        title="Discussion"
        description={`${comments.length} ${comments.length === 1 ? 'comment' : 'comments'} · Visible to the tenant and property owner.`}
        action={<button className="comment-toggle-button" type="button" aria-expanded={commentsOpen} aria-controls="maintenance-comments" onClick={() => setCommentsOpen((open) => !open)}>{commentsOpen ? 'Hide Comments' : 'View Comments'}</button>}
      >
        {commentsOpen && <div className="comment-thread" id="maintenance-comments">{comments.length ? comments.map((comment) => <div className="comment-item" key={comment.id}><div><strong>{comment.userName}</strong><time>{formatDate(comment.createdAt)}</time></div><p>{comment.comment}</p></div>) : <p className="muted-copy">No comments yet.</p>}</div>}
        <form className="comment-composer" onSubmit={addComment}>
          <label className="comment-composer-label" htmlFor="maintenance-comment">Add to the conversation <span className="required-mark">*</span></label>
          <div className="comment-composer-field">
            <textarea id="maintenance-comment" aria-label="Write a comment" value={commentDraft} onChange={(event) => setCommentDraft(event.target.value)} placeholder="Share an update or ask a question..." rows={2} required />
            <button className="button button-primary comment-send-button" type="submit" disabled={!commentDraft.trim() || busy}><Send size={15} /><span>Send</span></button>
          </div>
          <small className="comment-composer-hint">Your comment will be visible to the tenant and property owner.</small>
        </form>
      </Panel>
    </> : null}
    {editOpen && request && <Modal title="Edit maintenance request" description="Update the issue title, description, or priority." onClose={() => setEditOpen(false)}>
      <form className="form-stack" onSubmit={saveChanges} noValidate>
        <Field label="Title" id="maintenance-title" value={form.title} onChange={(value) => setForm((current) => ({ ...current, title: value }))} required />
        <div className="field-group"><label htmlFor="maintenance-description">Description <span className="required-mark">*</span></label><textarea id="maintenance-description" rows={4} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} required /></div>
        <div className="field-group"><label htmlFor="maintenance-priority">Priority <span className="required-mark">*</span></label><select id="maintenance-priority" value={form.priority} onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))} required><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></div>
        <div className="modal-actions"><button type="button" className="button" onClick={() => setEditOpen(false)}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? <span className="spinner" /> : null}Save changes</button></div>
      </form>
    </Modal>}
    {cancelOpen && request && <ConfirmationDialog title={`Cancel “${request.title}”?`} description="You can cancel this request only while it is open. Once cancelled, the owner will no longer process it." confirmLabel="Cancel request" onCancel={() => setCancelOpen(false)} onConfirm={() => void cancelRequest()} />}
    {pendingStatus && request && <ConfirmationDialog title={pendingStatus === 'RESOLVED' ? 'Resolve this request?' : 'Move this request in progress?'} description={`The status of “${request.title}” will change to ${pendingStatus === 'RESOLVED' ? 'Resolved' : 'In progress'}. This status cannot be changed back.`} confirmLabel={pendingStatus === 'RESOLVED' ? 'Resolve request' : 'Mark in progress'} onCancel={() => setPendingStatus(null)} onConfirm={() => void changeStatus()} />}
  </section>
}
