import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CalendarDays, Eye, EyeOff, IdCard, LockKeyhole, Mail, Pencil, Phone, UserRound, Users } from 'lucide-react'
import { api, errorMessage } from './api'
import { useAuth } from './auth-context'
import type { Role } from './types'
import { Field, InlineError, InlineSuccess, LoadingState, PageHeading, Panel } from './components'
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
    password: !values.password ? 'Password is required.' : registering && values.password.length < 8 ? 'Use at least 8 characters.' : registering && passwordBytes > 72 ? 'Password must be 72 UTF-8 bytes or fewer.' : '',
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
        <div className="auth-visual-top"><img className="auth-brand-logo theme-logo-dark" src="/brand/smylio-logo.png" alt="" /><img className="auth-brand-logo theme-logo-light" src="/brand/smylio-logo-light.png" alt="" /><span>Smylio <small>RENTING MADE HAPPIER</small></span></div>
        <div className="auth-visual-content">
          <p className="eyebrow">TENANT & OWNER MANAGEMENT</p>
          <h1>Every property,<br /><em>in good order.</em></h1>
          <p>One clear place to manage homes, leases, rent and the details that keep things running.</p>
        </div>
      </div>
      <div className="auth-panel-wrap">
        <div className="auth-mobile-brand"><img className="auth-brand-logo theme-logo-dark" src="/brand/smylio-logo.png" alt="" /><img className="auth-brand-logo theme-logo-light" src="/brand/smylio-logo-light.png" alt="" /><span>Smylio</span></div>
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
                {touched.password && errors.password ? <span className="field-error" id="password-error">{errors.password}</span> : registering ? <span className="field-help" id="password-help">At least 8 characters, up to 72 UTF-8 bytes.</span> : null}
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
  const [profile, setProfile] = useState<User | null>(null)
  const [form, setForm] = useState({ name: '', email: '', phone: '' })
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

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

  return <section className="page-section">
    <PageHeading eyebrow="ACCOUNT" title="Your profile" subtitle="View and update your personal contact details." action={profile && !editing ? <button className="button button-primary" onClick={startEditing}><Pencil size={14} /> Edit profile</button> : undefined} />
    {error && <InlineError>{error}</InlineError>}
    {message && <InlineSuccess>{message}</InlineSuccess>}
    {loading ? <LoadingState label="Loading your profile…" /> : profile ? <Panel title="Profile information" description={profile.role === 'TENANT' ? 'Your tenant ID and account creation date are read-only.' : 'Your account creation date is read-only.'}>
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
          <div className="account-profile-detail"><span className="account-profile-detail-icon"><Users size={21} /></span><div><span>Role</span><strong>{profile.role === 'OWNER' ? 'Owner' : 'Tenant'}</strong></div></div>
          {profile.role === 'TENANT' && <div className="account-profile-detail"><span className="account-profile-detail-icon"><IdCard size={21} /></span><div><span>Tenant ID</span><strong>{profile.tenantCode}</strong></div></div>}
          <div className="account-profile-detail"><span className="account-profile-detail-icon"><CalendarDays size={21} /></span><div><span>Created at</span><strong>{formatDate(profile.createdAt)}</strong></div></div>
        </div>
      </div>}
    </Panel> : null}
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
