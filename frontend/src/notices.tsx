import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, CalendarDays, ChevronLeft, ChevronRight, Eye, FileText, Home, Info, Megaphone, Pencil, Plus, RefreshCw, Search, Trash2 } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, errorMessage } from './api'
import { useAuth } from './auth-context'
import { ConfirmationDialog, EmptyState, Field, InlineError, InlineSuccess, LoadingState, Modal, NoticeUnreadDot, PageHeading, Panel } from './components'
import { formatDate } from './format'
import { markNoticeRead } from './notice-status'
import type { Notice, Property } from './types'

export function NoticesPage() {
  const { token, user } = useAuth()
  const owner = user?.role === 'OWNER'
  const [notices, setNotices] = useState<Notice[]>([])
  const [properties, setProperties] = useState<Array<{ id: number; title: string }>>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [noticeToDelete, setNoticeToDelete] = useState<Notice | null>(null)
  const [noticeToEdit, setNoticeToEdit] = useState<Notice | null>(null)
  const [search, setSearch] = useState('')
  const [propertyFilter, setPropertyFilter] = useState('')
  const [page, setPage] = useState(1)
  const [form, setForm] = useState({ title: '', message: '', propertyId: '' })
  const [totalNotices, setTotalNotices] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const pageSize = 5

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true); setError('')
    try {
      const noticeRequest = owner
        ? api.ownerNoticePage(token, { page: page - 1, size: pageSize, ...(search.trim() ? { search: search.trim() } : {}), ...(propertyFilter ? { propertyId: Number(propertyFilter) } : {}) })
        : api.tenantNoticePage(token, { page: page - 1, size: pageSize, ...(search.trim() ? { search: search.trim() } : {}), ...(propertyFilter ? { propertyId: Number(propertyFilter) } : {}) })
      const filterPropertiesRequest = owner
        ? api.properties(token).then((rows) => rows.map(({ id, title }) => ({ id, title })))
        : api.tenantLeases(token).then((rows) => rows.map(({ propertyId, propertyTitle }) => ({ id: propertyId, title: propertyTitle })))
      const [noticePage, propertyRows] = await Promise.all([noticeRequest, filterPropertiesRequest])
      setNotices(noticePage.content)
      setTotalNotices(noticePage.totalElements)
      setTotalPages(noticePage.totalPages)
      setProperties(propertyRows)
    } catch (cause) { setError(errorMessage(cause)) } finally { setLoading(false) }
  }, [token, owner, page, search, propertyFilter])
  useEffect(() => { void load() }, [load])

  function openEdit(notice: Notice) {
    setNoticeToEdit(notice)
    setForm({ title: notice.title, message: notice.message, propertyId: notice.propertyId ? String(notice.propertyId) : '' })
    setError('')
    setOpen(true)
  }

  function closeForm() {
    setOpen(false)
    setNoticeToEdit(null)
    setForm({ title: '', message: '', propertyId: '' })
  }

  async function saveNotice(event: React.FormEvent) {
    event.preventDefault()
    if (!token) return
    if (!form.title.trim() || !form.message.trim()) { setError('Enter a title and message.'); return }
    setBusy(true); setError('')
    try {
      const body = { title: form.title.trim(), message: form.message.trim(), ...(form.propertyId ? { propertyId: Number(form.propertyId) } : {}) }
      if (noticeToEdit) {
        await api.updateNotice(token, noticeToEdit.id, body)
        setSuccess('Notice updated.')
        closeForm()
        await load()
      } else {
        await api.createNotice(token, body)
        setSuccess('Notice posted.')
        closeForm()
        const shouldResetFilters = page !== 1 || Boolean(search || propertyFilter)
        setPage(1); setSearch(''); setPropertyFilter('')
        if (!shouldResetFilters) await load()
      }
    } catch (cause) { setError(errorMessage(cause)) } finally { setBusy(false) }
  }

  async function removeNotice(notice: Notice) {
    if (!token) return
    setError('')
    setSuccess('')
    try {
      await api.deleteNotice(token, notice.id)
      setSuccess('Notice deleted.')
      await load()
    } catch (cause) {
      setError(errorMessage(cause))
    }
  }

  const pageCount = Math.max(1, totalPages)
  useEffect(() => {
    if (totalPages > 0 && page > totalPages) setPage(totalPages)
  }, [page, totalPages])

  return <section className="page-section">
    <PageHeading eyebrow="COMMUNICATIONS" title="Notices" subtitle={owner ? 'Share property updates with tenants.' : 'Updates from your property owner.'} action={owner ? <button className="button button-primary" onClick={() => { setNoticeToEdit(null); setForm({ title: '', message: '', propertyId: '' }); setOpen(true) }}><Plus size={15} /> Post notice</button> : <button className="button" onClick={() => void load()}><RefreshCw size={14} /> Refresh</button>} />
    {error && <InlineError>{error}</InlineError>}{success && <InlineSuccess>{success}</InlineSuccess>}
    <Panel
      title={owner ? 'Your notices' : 'Notices for your home'}
      description={`${totalNotices} notices`}
      action={totalNotices > 0 ? <div className="notice-table-filters">
        <label className="notice-search-field" htmlFor={owner ? 'owner-notice-search' : 'tenant-notice-search'}><Search size={16} /><input id={owner ? 'owner-notice-search' : 'tenant-notice-search'} type="search" placeholder="Search notices..." value={search} onChange={(event) => { setSearch(event.target.value); setPage(1) }} /></label>
        <select aria-label="Filter notices by property" value={propertyFilter} onChange={(event) => { setPropertyFilter(event.target.value); setPage(1) }}>
          <option value="">All properties</option>
          {properties.map((property) => <option key={property.id} value={property.id}>{property.title}</option>)}
        </select>
      </div> : undefined}
    >
      {loading ? <LoadingState /> : totalNotices === 0 ? search.trim() || propertyFilter ? <EmptyState title="No matching notices" detail="Try another search or property filter." /> : <EmptyState title="No notices yet" detail={owner ? 'Your announcements will be listed here.' : 'Your owner has not posted any notices.'} /> : owner && notices.length === 0 ? <EmptyState title="No matching notices" detail="Try another search or property filter." /> : owner ? <>
        <div className="table-scroll">
          <table className="notice-table">
            <thead><tr><th>Title</th><th>Property</th><th>Date</th><th>Actions</th></tr></thead>
            <tbody>{notices.map((notice) => {
              const property = notice.propertyTitle || (notice.propertyId ? `Property #${notice.propertyId}` : 'All properties')
              const noticePath = `/owner/notices/${notice.id}`
              const readNotice = () => { if (user) markNoticeRead(user.id, notice.id) }
              return <tr key={notice.id}>
                <td className="notice-table-title">{notice.title}</td>
                <td>{property}</td>
                <td><span className="notice-table-date-content">{user && <NoticeUnreadDot createdAt={notice.createdAt} noticeId={notice.id} userId={user.id} />}{formatDate(notice.createdAt)}</span></td>
                <td><span className="notice-table-actions"><Link className="notice-table-action notice-table-open" to={noticePath} onClick={readNotice} aria-label={`Open notice: ${notice.title}`} title="Open notice"><Eye size={16} /></Link><button className="notice-table-action notice-table-edit" type="button" onClick={() => openEdit(notice)} aria-label={`Edit notice: ${notice.title}`} title="Edit notice"><Pencil size={16} /></button><button className="notice-table-action notice-table-delete" type="button" onClick={() => setNoticeToDelete(notice)} aria-label={`Delete notice: ${notice.title}`} title="Delete notice"><Trash2 size={16} /></button></span></td>
              </tr>
            })}</tbody>
          </table>
        </div>
        <footer className="notice-table-footer">
          <span>Showing {Math.min((page - 1) * pageSize + notices.length, totalNotices)} of {totalNotices} notices</span>
          <nav className="notice-pagination" aria-label="Notice pages">
            <button type="button" className="icon-button" aria-label="Previous page" disabled={page <= 1} onClick={() => setPage((current) => Math.max(1, current - 1))}><ChevronLeft size={18} /></button>
            <span className="notice-page-current" aria-current="page">{page}</span>
            <button type="button" className="icon-button" aria-label="Next page" disabled={page >= pageCount} onClick={() => setPage((current) => Math.min(pageCount, current + 1))}><ChevronRight size={18} /></button>
          </nav>
        </footer>
      </> : <>
        <div className="table-scroll">
          <table className="notice-table tenant-notice-table">
            <thead><tr><th>Title</th><th>Property</th><th>Date</th><th>Action</th></tr></thead>
            <tbody>{notices.map((notice) => {
              const property = notice.propertyTitle || (notice.propertyId ? `Property #${notice.propertyId}` : 'All properties')
              const noticePath = `/tenant/notices/${notice.id}`
              const readNotice = () => { if (user) markNoticeRead(user.id, notice.id) }
              return <tr key={notice.id}>
                <td className="notice-table-title">{notice.title}</td>
                <td>{property}</td>
                <td><span className="notice-table-date-content">{user && <NoticeUnreadDot createdAt={notice.createdAt} noticeId={notice.id} userId={user.id} />}{formatDate(notice.createdAt)}</span></td>
                <td><Link className="notice-table-view" to={noticePath} onClick={readNotice}>View details <ChevronRight size={16} /></Link></td>
              </tr>
            })}</tbody>
          </table>
        </div>
        <footer className="notice-table-footer">
          <span>Showing {Math.min((page - 1) * pageSize + notices.length, totalNotices)} of {totalNotices} notices</span>
          <nav className="notice-pagination" aria-label="Notice pages">
            <button type="button" className="icon-button" aria-label="Previous page" disabled={page <= 1} onClick={() => setPage((current) => Math.max(1, current - 1))}><ChevronLeft size={18} /></button>
            <span className="notice-page-current" aria-current="page">{page}</span>
            <button type="button" className="icon-button" aria-label="Next page" disabled={page >= pageCount} onClick={() => setPage((current) => Math.min(pageCount, current + 1))}><ChevronRight size={18} /></button>
          </nav>
        </footer>
      </>}
    </Panel>
    {open && <Modal title={noticeToEdit ? 'Edit notice' : 'Post a notice'} description={noticeToEdit ? 'Update the notice details for your tenants.' : 'Choose one property or leave it as a general notice for all your leased properties.'} onClose={closeForm}>
      <form className="form-stack" onSubmit={saveNotice} noValidate>
        <Field label="Title" id="notice-title" value={form.title} onChange={(value) => setForm((current) => ({ ...current, title: value }))} required />
        <div className="field-group"><label htmlFor="notice-message">Message <span className="required-mark">*</span></label><textarea id="notice-message" rows={4} value={form.message} onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))} required /></div>
        <div className="field-group"><label htmlFor="notice-property">Audience</label><select id="notice-property" value={form.propertyId} onChange={(event) => setForm((current) => ({ ...current, propertyId: event.target.value }))}><option value="">All properties</option>{properties.map((property) => <option key={property.id} value={property.id}>{property.title}</option>)}</select></div>
        <div className="modal-actions"><button type="button" className="button" onClick={closeForm}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? <span className="spinner" /> : null}{noticeToEdit ? 'Save changes' : 'Publish notice'}</button></div>
      </form>
    </Modal>}
    {noticeToDelete && <ConfirmationDialog title={`Delete "${noticeToDelete.title}"?`} description="This notice will be permanently deleted and will no longer be visible to tenants." confirmLabel="Delete notice" onCancel={() => setNoticeToDelete(null)} onConfirm={async () => { await removeNotice(noticeToDelete); setNoticeToDelete(null) }} />}
  </section>
}

export function NoticeDetailPage() {
  const { token, user } = useAuth()
  const { noticeId: noticeIdParam } = useParams()
  const navigate = useNavigate()
  const owner = user?.role === 'OWNER'
  const noticeId = Number(noticeIdParam)
  const [notice, setNotice] = useState<Notice | null>(null)
  const [properties, setProperties] = useState<Property[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({ title: '', message: '', propertyId: '' })
  const noticesPath = owner ? '/owner/notices' : '/tenant/notices'

  useEffect(() => {
    if (!token) return
    let active = true
    setLoading(true)
    setError('')
    setNotice(null)

    if (!Number.isSafeInteger(noticeId) || noticeId <= 0) {
      setError('Notice not found.')
      setLoading(false)
      return () => { active = false }
    }

    const request = owner ? api.ownerNotices(token) : api.tenantNotices(token)
    const propertyRequest = owner ? api.properties(token) : Promise.resolve([] as Property[])
    void Promise.all([request, propertyRequest])
      .then(([rows, propertyRows]) => {
        if (active) {
          setProperties(propertyRows)
          const selectedNotice = rows.find((row) => row.id === noticeId)
          if (selectedNotice) {
            setNotice(selectedNotice)
            if (user) markNoticeRead(user.id, selectedNotice.id)
          }
          else setError('Notice not found or is no longer available.')
        }
      })
      .catch((cause: unknown) => {
        if (active) setError(errorMessage(cause))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [token, owner, noticeId, user])

  function openEdit() {
    if (!notice) return
    setForm({
      title: notice.title,
      message: notice.message,
      propertyId: notice.propertyId ? String(notice.propertyId) : '',
    })
    setError('')
    setEditOpen(true)
  }

  async function saveNotice(event: React.FormEvent) {
    event.preventDefault()
    if (!token || !notice) return
    if (!form.title.trim() || !form.message.trim()) {
      setError('Enter a title and message.')
      return
    }

    setBusy(true)
    setError('')
    try {
      const updatedNotice = await api.updateNotice(token, notice.id, {
        title: form.title.trim(),
        message: form.message.trim(),
        ...(form.propertyId ? { propertyId: Number(form.propertyId) } : {}),
      })
      setNotice(updatedNotice)
      setEditOpen(false)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  async function deleteNotice() {
    if (!token || !notice) return
    setBusy(true)
    setError('')
    try {
      await api.deleteNotice(token, notice.id)
      navigate(noticesPath, { replace: true })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
      setDeleteOpen(false)
    }
  }

  return <section className="page-section">
    <PageHeading eyebrow="COMMUNICATIONS" title="Notice details" subtitle={owner ? 'View the complete notice information you shared.' : 'View the complete notice information shared by your property owner.'} action={<button className="button" onClick={() => navigate(noticesPath)}><ArrowLeft size={15} /> Back to notices</button>} />
    {error && <InlineError>{error}</InlineError>}
    {loading ? <div className="tenant-notice-detail-card tenant-notice-detail-loading"><LoadingState /></div> : notice ? <article className="tenant-notice-detail-card">
      <header className="tenant-notice-summary">
        <div className="tenant-notice-title-block">
          <span className="tenant-notice-megaphone"><Megaphone size={34} strokeWidth={1.7} /></span>
          <div className="tenant-notice-title-copy">
            <h2>{notice.title}</h2>
            <p><Home size={20} /><span>{notice.propertyTitle || (notice.propertyId ? `Property #${notice.propertyId}` : 'All properties')}</span></p>
          </div>
        </div>
        <div className="tenant-notice-date">
          <CalendarDays size={24} />
          <div><span>Date</span><strong>{formatDate(notice.createdAt)}</strong><small>{owner ? 'Posted by You' : 'Posted by Property Owner'}</small></div>
        </div>
      </header>
      {owner && <div className="tenant-notice-owner-actions">
        <button className="button" type="button" onClick={openEdit}><Pencil size={15} /> Edit notice</button>
        <button className="button button-danger" type="button" onClick={() => setDeleteOpen(true)}><Trash2 size={15} /> Delete notice</button>
      </div>}
      <section className="tenant-notice-message-panel">
        <h3><FileText size={23} />Notice message</h3>
        <div className="tenant-notice-message-body">{notice.message}</div>
        <p className="tenant-notice-context"><Info size={19} />{owner
          ? notice.propertyId ? 'This notice was shared with tenants of the selected property.' : 'This notice was shared with tenants across all your properties.'
          : notice.propertyId ? 'This notice was shared by your property owner for the selected property.' : 'This notice was shared by your property owner with all your properties.'}</p>
      </section>
    </article> : null}
    {editOpen && notice && <Modal title="Edit notice" description="Update the notice details for your tenants." onClose={() => setEditOpen(false)}>
      <form className="form-stack" onSubmit={saveNotice} noValidate>
        <Field label="Title" id="notice-detail-title" value={form.title} onChange={(value) => setForm((current) => ({ ...current, title: value }))} required />
        <div className="field-group"><label htmlFor="notice-detail-message">Message <span className="required-mark">*</span></label><textarea id="notice-detail-message" rows={4} value={form.message} onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))} required /></div>
        <div className="field-group"><label htmlFor="notice-detail-property">Property</label><select id="notice-detail-property" value={form.propertyId} onChange={(event) => setForm((current) => ({ ...current, propertyId: event.target.value }))}><option value="">All properties</option>{properties.map((property) => <option key={property.id} value={property.id}>{property.title}</option>)}</select></div>
        <div className="modal-actions"><button type="button" className="button" onClick={() => setEditOpen(false)}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? <span className="spinner" /> : null}Save changes</button></div>
      </form>
    </Modal>}
    {deleteOpen && notice && <ConfirmationDialog title={`Delete "${notice.title}"?`} description="This notice will be permanently deleted and will no longer be visible to tenants." confirmLabel="Delete notice" onCancel={() => setDeleteOpen(false)} onConfirm={deleteNotice} />}
  </section>
}
