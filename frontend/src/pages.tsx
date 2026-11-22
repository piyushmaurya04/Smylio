import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import {
  Building2,
  CalendarDays,
  Check,
  Copy,
  Eye,
  EyeOff,
  Home,
  IdCard,
  KeyRound,
  LockKeyhole,
  Mail,
  Pencil,
  Phone,
  Sparkles,
  UserRound,
  Users,
} from 'lucide-react'
import { api, errorMessage } from './api'
import { useAuth } from './auth-context'
import type { Role } from './types'
import { Field, InlineError, InlineSuccess, LoadingState, Modal, PageHeading, Panel } from './components'
import { formatDate } from './format'
import type { User } from './types'
import { OwnerDashboardPage, OwnerLeasesPage, OwnerPropertiesPage, OwnerRentPage, OwnerTenantDetailPage } from './owner-pages'
import { TenantDashboardPage, TenantLeaseDetailPage, TenantRentPage } from './tenant-pages'
import { MaintenanceDetailPage, MaintenancePage } from './maintenance'
import { NoticeDetailPage, NoticesPage } from './notices'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { signIn, signUp } = useAuth()
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [values, setValues] = useState({ name: '', email: '', password: '', phone: '', role: 'TENANT' as Role })
  const [touched, setTouched] = useState<Record<string, boolean>>({})
  const [registrationStep, setRegistrationStep] = useState(1)
  const registering = mode === 'register'
  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())
  const passwordBytes = new TextEncoder().encode(values.password).length
  const errors = {
    name: registering && !values.name.trim() ? 'Enter your name.' : '',
    email: !values.email.trim() ? 'Email is required.' : !emailValid ? 'Enter a valid email address.' : '',
    password: !values.password ? 'Password is required.' : registering && values.password.length < 8 ? 'Use at least 8 characters.' : registering && passwordBytes > 72 ? 'Password must be 72 characters or fewer.' : '',
    phone: registering && !values.phone.trim() ? 'Phone number is required.' : '',
  }

  function update(field: keyof typeof values, value: string) {
    setValues((current) => ({ ...current, [field]: value }))
    setError('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (registering && registrationStep === 1) {
      setTouched((current) => ({ ...current, name: true }))
      if (errors.name) return
      setRegistrationStep(2)
      return
    }
    setTouched({ name: true, email: true, password: true, phone: true })
    if (errors.name || errors.email || errors.password || errors.phone) return
    setSubmitting(true)
    setError('')
    try {
      if (registering) await signUp({ ...values, email: values.email.trim() })
      else await signIn(values.email.trim(), values.password)
      navigate('/', { replace: true })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-visual">
        <Link to="/" className="auth-visual-top"><img className="auth-brand-logo theme-logo-dark" src="/brand/smylio-logo.png" alt="" /><img className="auth-brand-logo theme-logo-light" src="/brand/smylio-logo-light.png" alt="" /><span>Smylio <small>RENTING MADE HAPPIER</small></span></Link>
        <div className="auth-visual-content">
          <p className="eyebrow">TENANT & OWNER MANAGEMENT</p>
          <h1>Every property,<br /><em>in good order.</em></h1>
          <p>One clear place to manage homes, leases, rent and the details that keep things running.</p>
        </div>
      </div>
      <div className="auth-panel-wrap">
        <Link to="/" className="auth-mobile-brand"><img className="auth-brand-logo theme-logo-dark" src="/brand/smylio-logo.png" alt="" /><img className="auth-brand-logo theme-logo-light" src="/brand/smylio-logo-light.png" alt="" /><span>Smylio</span></Link>
        <section className="auth-panel">
          <div className="auth-heading"><div className="auth-icon"><LockKeyhole size={18} /></div><p className="eyebrow">{registering ? `CREATE ACCOUNT · STEP ${registrationStep} OF 2` : 'WELCOME BACK'}</p><h2>{registering ? 'Create your account' : 'Sign in to Smylio'}</h2><p>{registering ? registrationStep === 1 ? "Let's start with your full name." : 'Complete your account details.' : 'Use your account credentials to continue.'}</p></div>
          {error && <InlineError>{error}</InlineError>}
          <form className="form-stack" onSubmit={submit} noValidate>
            {registering && registrationStep === 1 && <Field label="Full name" id="name" value={values.name} onChange={(value) => update('name', value)} onBlur={() => setTouched((current) => ({ ...current, name: true }))} error={touched.name ? errors.name : ''} required autoComplete="name" />}
            {(!registering || registrationStep === 2) && <>
              <Field label="Email address" id="email" type="email" value={values.email} onChange={(value) => update('email', value)} onBlur={() => setTouched((current) => ({ ...current, email: true }))} error={touched.email ? errors.email : ''} required autoComplete="email" />
              <div className="field-group">
                <label htmlFor="password">Password <span className="required-mark">*</span></label>
                <div className={`password-wrap ${touched.password && errors.password ? 'field-invalid' : ''}`}>
                  <input id="password" type={showPassword ? 'text' : 'password'} value={values.password} onChange={(event) => update('password', event.target.value)} onBlur={() => setTouched((current) => ({ ...current, password: true }))} autoComplete={registering ? 'new-password' : 'current-password'} required aria-invalid={Boolean(touched.password && errors.password)} aria-describedby={touched.password && errors.password ? 'password-error' : registering ? 'password-help' : undefined} />
                  <button type="button" className="field-icon-button" onClick={() => setShowPassword((current) => !current)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={16} /> : <Eye size={16} />}</button>
                </div>
                {touched.password && errors.password ? <span className="field-error" id="password-error">{errors.password}</span> : registering ? <span className="field-help" id="password-help">At least 8 characters.</span> : null}
              </div>
              {registering && <>
                <Field label="Phone number" id="phone" type="tel" value={values.phone} onChange={(value) => update('phone', value)} onBlur={() => setTouched((current) => ({ ...current, phone: true }))} error={touched.phone ? errors.phone : ''} required autoComplete="tel" />
                <div className="field-group"><label htmlFor="role">Account type <span className="required-mark">*</span></label><select id="role" value={values.role} onChange={(event) => update('role', event.target.value as Role)} required><option value="TENANT">Tenant</option><option value="OWNER">Owner</option></select></div>
              </>}
            </>}
            {registering && registrationStep === 2 && <button className="button button-wide" type="button" onClick={() => setRegistrationStep(1)}>Back</button>}
            <button className="button button-primary button-wide" type="submit" disabled={submitting}>{submitting ? <><span className="spinner" /> {registering ? 'Creating account…' : 'Signing in…'}</> : registering && registrationStep === 1 ? 'Next' : registering ? 'Create account' : 'Sign in'}</button>
          </form>
          <p className="auth-switch">{registering ? 'Already registered?' : 'New to Smylio?'} <Link to={registering ? '/login' : '/register'}>{registering ? 'Sign in' : 'Create an account'}</Link></p>
        </section>
      </div>
    </div>
  )
}

export function ProfilePage() {
  const { token, updateUser } = useAuth()
  const navigate = useNavigate()
  const [profile, setProfile] = useState<User | null>(null)
  const [form, setForm] = useState({ name: '', email: '', phone: '' })
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [passwordModalError, setPasswordModalError] = useState('')
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [copiedTenantCode, setCopiedTenantCode] = useState(false)

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const current = await api.profile(token)
      setProfile(current)
      setForm({ name: current.name, email: current.email, phone: current.phone ?? '' })
      updateUser(current)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [token, updateUser])

  useEffect(() => { void load() }, [load])

  async function copyTenantCode(code: string) {
    try {
      await navigator.clipboard.writeText(code)
      setCopiedTenantCode(true)
      setTimeout(() => setCopiedTenantCode(false), 2000)
    } catch {
      // non-blocking
    }
  }

  function startEditing() {
    if (!profile) return
    setForm({ name: profile.name, email: profile.email, phone: profile.phone ?? '' })
    setError('')
    setMessage('')
    setEditing(true)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) return
    if (!form.name.trim() || !form.email.trim() || !form.phone.trim()) {
      setError('Name, email, and phone number are required.')
      return
    }
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const updated = await api.updateProfile(token, {
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
      })
      setProfile(updated)
      setForm({ name: updated.name, email: updated.email, phone: updated.phone ?? '' })
      updateUser(updated)
      setEditing(false)
      setMessage('Profile updated.')
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  async function handlePasswordChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) return
    if (!passwordForm.currentPassword) {
      setPasswordModalError('Enter your current password.')
      return
    }
    if (!passwordForm.newPassword) {
      setPasswordModalError('Enter a new password.')
      return
    }
    if (passwordForm.newPassword.length < 8) {
      setPasswordModalError('New password must be at least 8 characters.')
      return
    }
    if (new TextEncoder().encode(passwordForm.newPassword).length > 72) {
      setPasswordModalError('New password must be 72 characters or fewer.')
      return
    }
    if (passwordForm.newPassword === passwordForm.currentPassword) {
      setPasswordModalError('New password cannot be the same as your current password.')
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordModalError('New password and confirm password do not match.')
      return
    }

    setPasswordSaving(true)
    setPasswordModalError('')
    try {
      await api.changePassword(token, {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      })
      setPasswordModalOpen(false)
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setShowCurrent(false)
      setShowNew(false)
      setShowConfirm(false)
      setMessage('Password changed successfully.')
    } catch (cause) {
      if (axios.isAxiosError(cause)) {
        const serverMessage = (cause.response?.data as { message?: string; error?: string } | undefined)?.message
          || (cause.response?.data as { message?: string; error?: string } | undefined)?.error
        if (serverMessage) {
          setPasswordModalError(serverMessage)
        } else if (cause.response?.status === 403 || cause.response?.status === 401 || cause.response?.status === 400) {
          setPasswordModalError('Current password is incorrect.')
        } else {
          setPasswordModalError(errorMessage(cause))
        }
      } else {
        setPasswordModalError(errorMessage(cause))
      }
    } finally {
      setPasswordSaving(false)
    }
  }

  return <section className="page-section">
    <PageHeading
      eyebrow="ACCOUNT"
      title="Your profile"
      subtitle="View and update your personal contact details."
      action={profile && !editing ? (
        <div className="profile-heading-actions">
          <button
            className="button"
            type="button"
            onClick={() => {
              setError('')
              setPasswordModalError('')
              setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
              setShowCurrent(false)
              setShowNew(false)
              setShowConfirm(false)
              setPasswordModalOpen(true)
            }}
          >
            <KeyRound size={14} /> Change password
          </button>
          <button className="button button-primary" type="button" onClick={startEditing}>
            <Pencil size={14} /> Edit profile
          </button>
        </div>
      ) : undefined}
    />
    {!passwordModalOpen && error && <InlineError>{error}</InlineError>}
    {message && <InlineSuccess onDismiss={() => setMessage('')}>{message}</InlineSuccess>}
    {loading ? <LoadingState label="Loading your profile…" /> : profile ? <>
      <Panel title="Profile information" description="Your contact information and primary account details.">
        {editing ? <form className="form-stack profile-form" onSubmit={save} noValidate>
          <Field label="Full name" id="profile-name" value={form.name} onChange={(value) => setForm((current) => ({ ...current, name: value }))} required autoComplete="name" />
          <Field label="Email address" id="profile-email" type="email" value={form.email} onChange={(value) => setForm((current) => ({ ...current, email: value }))} required autoComplete="email" />
          <Field label="Phone number" id="profile-phone" type="tel" value={form.phone} onChange={(value) => setForm((current) => ({ ...current, phone: value }))} required autoComplete="tel" />
          <div className="modal-actions"><button className="button" type="button" disabled={saving} onClick={() => { setEditing(false); setError('') }}>Cancel</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? <><span className="spinner" /> Saving…</> : 'Save changes'}</button></div>
        </form> : <div className="account-profile-layout">
          <div className="account-profile-identity">
            <span className="account-profile-avatar">{profile.name.slice(0, 1).toUpperCase()}</span>
            <div><strong>{profile.name}</strong><span>{profile.role === 'OWNER' ? 'Owner' : 'Tenant'}</span></div>
          </div>
          <div className="account-profile-details">
            <div className="account-profile-detail"><span className="account-profile-detail-icon"><UserRound size={21} /></span><div><span>Name</span><strong>{profile.name}</strong></div></div>
            <div className="account-profile-detail"><span className="account-profile-detail-icon"><Mail size={21} /></span><div><span>Email</span><a href={`mailto:${profile.email}`}>{profile.email}</a></div></div>
            <div className="account-profile-detail"><span className="account-profile-detail-icon"><Phone size={21} /></span><div><span>Phone</span>{profile.phone ? <a href={`tel:${profile.phone}`}>{profile.phone}</a> : <strong>Not provided</strong>}</div></div>
            <div className="account-profile-detail"><span className="account-profile-detail-icon"><Users size={21} /></span><div><span>Active workspace</span><strong>{profile.role === 'OWNER' ? 'Owner workspace' : 'Tenant portal'}</strong></div></div>
            {profile.tenantCode && (
              <div className="account-profile-detail">
                <span className="account-profile-detail-icon"><IdCard size={21} /></span>
                <div>
                  <span>Tenant ID</span>
                  <div className="profile-tenant-id-row">
                    <strong>{profile.tenantCode}</strong>
                    <button
                      type="button"
                      className="button button-subtle profile-copy-button"
                      onClick={() => void copyTenantCode(profile.tenantCode!)}
                    >
                      {copiedTenantCode ? <><Check size={12} /> Copied</> : <><Copy size={12} /> Copy ID</>}
                    </button>
                  </div>
                </div>
              </div>
            )}
            <div className="account-profile-detail"><span className="account-profile-detail-icon"><CalendarDays size={21} /></span><div><span>Created at</span><strong>{formatDate(profile.createdAt)}</strong></div></div>
          </div>
        </div>}
      </Panel>

      <Panel
        title="Account Workspaces"
        description="Smylio allows you to seamlessly manage properties as an Owner and rent as a Tenant with a single login."
        className="profile-workspaces-panel"
      >
        <div className="profile-workspaces-grid">
          {/* Owner Workspace Card */}
          <div className={`profile-workspace-card ${(profile.roles ?? [profile.role]).includes('OWNER') ? 'workspace-card-enabled' : ''} ${profile.role === 'OWNER' ? 'workspace-card-active' : ''}`}>
            <div className="workspace-card-header">
              <div className="workspace-card-title">
                <div className="workspace-card-icon"><Building2 size={18} /></div>
                <div>
                  <strong>Owner Workspace</strong>
                  <small>Properties, leases & rent collection</small>
                </div>
              </div>
              <span className={`status-badge ${(profile.roles ?? [profile.role]).includes('OWNER') ? 'status-active' : 'status-inactive'}`}>
                {profile.role === 'OWNER' ? 'Active now' : (profile.roles ?? [profile.role]).includes('OWNER') ? 'Enabled' : 'Not activated'}
              </span>
            </div>
            <p className="workspace-card-desc">
              List rental properties, invite tenants, track incoming rent payments, and coordinate property repairs.
            </p>
            {!(profile.roles ?? [profile.role]).includes('OWNER') && (
              <div className="workspace-card-footer">
                <button
                  type="button"
                  className="button button-primary"
                  onClick={() => navigate('/workspace/activate/owner')}
                >
                  <Sparkles size={14} /> Activate Owner Workspace
                </button>
              </div>
            )}
          </div>

          {/* Tenant Portal Card */}
          <div className={`profile-workspace-card ${(profile.roles ?? [profile.role]).includes('TENANT') ? 'workspace-card-enabled' : ''} ${profile.role === 'TENANT' ? 'workspace-card-active' : ''}`}>
            <div className="workspace-card-header">
              <div className="workspace-card-title">
                <div className="workspace-card-icon"><Home size={18} /></div>
                <div>
                  <strong>Tenant Portal</strong>
                  <small>My home, rent receipts & repair tickets</small>
                </div>
              </div>
              <span className={`status-badge ${(profile.roles ?? [profile.role]).includes('TENANT') ? 'status-active' : 'status-inactive'}`}>
                {profile.role === 'TENANT' ? 'Active now' : (profile.roles ?? [profile.role]).includes('TENANT') ? 'Enabled' : 'Not activated'}
              </span>
            </div>
            <p className="workspace-card-desc">
              Connect to your landlord using your Tenant ID, record rent receipts, and submit maintenance tickets.
            </p>
            {!(profile.roles ?? [profile.role]).includes('TENANT') && (
              <div className="workspace-card-footer">
                <button
                  type="button"
                  className="button button-primary"
                  onClick={() => navigate('/workspace/activate/tenant')}
                >
                  <Sparkles size={14} /> Activate Tenant Portal
                </button>
              </div>
            )}
          </div>
        </div>
      </Panel>
    </> : null}
    {passwordModalOpen && <Modal title="Change password" description="Verify your current password and choose a new one." onClose={() => { setPasswordModalOpen(false); setPasswordModalError('') }} error={passwordModalError}>
      <form className="form-stack" onSubmit={handlePasswordChange} noValidate>
        <div className="field-group">
          <label htmlFor="current-password">Current password <span className="required-mark">*</span></label>
          <div className="password-wrap">
            <input id="current-password" type={showCurrent ? 'text' : 'password'} value={passwordForm.currentPassword} onChange={(e) => setPasswordForm((c) => ({ ...c, currentPassword: e.target.value }))} required autoComplete="current-password" placeholder="Enter current password" />
            <button type="button" className="field-icon-button" onClick={() => setShowCurrent((c) => !c)} aria-label={showCurrent ? 'Hide current password' : 'Show current password'}>{showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}</button>
          </div>
        </div>
        <div className="field-group">
          <label htmlFor="new-password">New password <span className="required-mark">*</span></label>
          <div className="password-wrap">
            <input id="new-password" type={showNew ? 'text' : 'password'} value={passwordForm.newPassword} onChange={(e) => setPasswordForm((c) => ({ ...c, newPassword: e.target.value }))} required autoComplete="new-password" placeholder="At least 8 characters" />
            <button type="button" className="field-icon-button" onClick={() => setShowNew((c) => !c)} aria-label={showNew ? 'Hide new password' : 'Show new password'}>{showNew ? <EyeOff size={16} /> : <Eye size={16} />}</button>
          </div>
          <span className="field-help">Must be at least 8 characters.</span>
        </div>
        <div className="field-group">
          <label htmlFor="confirm-password">Confirm new password <span className="required-mark">*</span></label>
          <div className="password-wrap">
            <input id="confirm-password" type={showConfirm ? 'text' : 'password'} value={passwordForm.confirmPassword} onChange={(e) => setPasswordForm((c) => ({ ...c, confirmPassword: e.target.value }))} required autoComplete="new-password" placeholder="Re-enter new password" />
            <button type="button" className="field-icon-button" onClick={() => setShowConfirm((c) => !c)} aria-label={showConfirm ? 'Hide confirm password' : 'Show confirm password'}>{showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}</button>
          </div>
        </div>
        <div className="modal-actions">
          <button type="button" className="button" onClick={() => { setPasswordModalOpen(false); setPasswordModalError('') }}>Cancel</button>
          <button type="submit" className="button button-primary" disabled={passwordSaving}>{passwordSaving ? <><span className="spinner" /> Updating…</> : 'Update password'}</button>
        </div>
      </form>
    </Modal>}
  </section>
}

export function DashboardPage() {
  const { user } = useAuth()
  return user?.role === 'OWNER' ? <OwnerDashboardPage /> : <TenantDashboardPage />
}

export function PropertiesPage() { return <OwnerPropertiesPage /> }
export function LeasesPage() { return <OwnerLeasesPage /> }
export { OwnerTenantDetailPage }
export function RentPage() {
  const { user } = useAuth()
  return user?.role === 'OWNER' ? <OwnerRentPage /> : <TenantRentPage />
}
export { MaintenanceDetailPage, MaintenancePage, NoticeDetailPage, NoticesPage, TenantLeaseDetailPage }
