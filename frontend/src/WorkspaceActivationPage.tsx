import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  ArrowLeftRight,
  Building2,
  Check,
  Home,
  KeyRound,
  ShieldCheck,
  Sparkles,
  Users,
} from 'lucide-react'
import { errorMessage } from './api'
import { useAuth } from './auth-context'
import { InlineError, PageHeading, Panel } from './components'
import type { Role } from './types'

export function WorkspaceActivationPage() {
  const { role } = useParams<{ role: string }>()
  const navigate = useNavigate()
  const { user, activateRole } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const targetRole: Role = role?.toLowerCase() === 'tenant' ? 'TENANT' : 'OWNER'
  const isActivatingTenant = targetRole === 'TENANT'

  const userRoles = user?.roles ?? (user ? [user.role] : [])
  const alreadyActivated = userRoles.includes(targetRole)

  async function handleConfirm() {
    setBusy(true)
    setError('')
    try {
      await activateRole(targetRole)
      sessionStorage.setItem('tom-just-activated', targetRole)
      navigate(targetRole === 'OWNER' ? '/owner/overview' : '/tenant/overview', { replace: true })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  function handleCancel() {
    if (window.history.length > 1) {
      navigate(-1)
    } else {
      navigate(user?.role === 'OWNER' ? '/owner/overview' : '/tenant/overview')
    }
  }

  return (
    <section className="page-section">
      <PageHeading
        eyebrow="WORKSPACE ONBOARDING"
        title={isActivatingTenant ? 'Activate Tenant Portal' : 'Activate Owner Workspace'}
        subtitle={
          isActivatingTenant
            ? 'Enable tenant capabilities on your account while keeping your owner workspace completely safe.'
            : 'Enable property owner capabilities on your account while keeping your tenant portal completely safe.'
        }
        action={
          <button type="button" className="button" onClick={handleCancel}>
            <ArrowLeft size={14} /> Back
          </button>
        }
      />

      {error && <InlineError>{error}</InlineError>}

      {alreadyActivated ? (
        <Panel
          title="Workspace already enabled"
          description="Your account already has access to this workspace."
        >
          <div className="activation-already-enabled">
            <p>
              You can switch to {isActivatingTenant ? 'Tenant Portal' : 'Owner Workspace'} at any time from the sidebar
              dropdown or by pressing <kbd className="shortcut-kbd">Alt+W</kbd>.
            </p>
            <button
              type="button"
              className="button button-primary"
              onClick={() =>
                navigate(targetRole === 'OWNER' ? '/owner/overview' : '/tenant/overview')
              }
            >
              Go to {isActivatingTenant ? 'Tenant Portal' : 'Owner Workspace'}
            </button>
          </div>
        </Panel>
      ) : (
        <Panel className="activation-page-panel">
          <div className="role-activation-body">
            <div className="role-activation-banner">
              <Sparkles size={22} className="role-activation-banner-icon" />
              <div>
                <strong>Single account, two workspaces</strong>
                <p>
                  Smylio allows you to be both an owner and a tenant under the same account. Switching is instant and keeps
                  your data strictly separated.
                </p>
              </div>
            </div>

            <div className="role-activation-grid">
              {isActivatingTenant ? (
                <>
                  <div className="activation-feature-card">
                    <div className="activation-feature-icon">
                      <KeyRound size={20} />
                    </div>
                    <div>
                      <strong>Unique Tenant Code</strong>
                      <p>
                        A unique Crockford Base32 Tenant ID (e.g. TNT-...) will be generated immediately for you to share
                        with your landlords.
                      </p>
                    </div>
                  </div>

                  <div className="activation-feature-card">
                    <div className="activation-feature-icon">
                      <Home size={20} />
                    </div>
                    <div>
                      <strong>Leases, Rent & Tickets</strong>
                      <p>
                        Get added to property leases, review rent schedules, make cash/UPI payments, and report maintenance
                        issues.
                      </p>
                    </div>
                  </div>

                  <div className="activation-feature-card activation-feature-highlight">
                    <div className="activation-feature-icon">
                      <ShieldCheck size={20} />
                    </div>
                    <div>
                      <strong>Owner Data Is 100% Safe</strong>
                      <p>
                        Your existing properties, leases, tenant records, and finances remain completely untouched and
                        secure.
                      </p>
                    </div>
                  </div>

                  <div className="activation-feature-card">
                    <div className="activation-feature-icon">
                      <ArrowLeftRight size={20} />
                    </div>
                    <div>
                      <strong>Instant Switching Anytime</strong>
                      <p>
                        Freely toggle between Owner Workspace and Tenant Portal anytime from the sidebar or using shortcut
                        Alt + W.
                      </p>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div className="activation-feature-card">
                    <div className="activation-feature-icon">
                      <Building2 size={20} />
                    </div>
                    <div>
                      <strong>Property Management</strong>
                      <p>
                        Add properties, configure rental units, set amenities, and manage your real estate portfolio.
                      </p>
                    </div>
                  </div>

                  <div className="activation-feature-card">
                    <div className="activation-feature-icon">
                      <Users size={20} />
                    </div>
                    <div>
                      <strong>Leases & Rent Approvals</strong>
                      <p>
                        Search tenants by Tenant ID, issue leases, generate monthly rent, and approve rent payments.
                      </p>
                    </div>
                  </div>

                  <div className="activation-feature-card activation-feature-highlight">
                    <div className="activation-feature-icon">
                      <ShieldCheck size={20} />
                    </div>
                    <div>
                      <strong>Tenant Data Is 100% Safe</strong>
                      <p>
                        Your current tenant home, rent payment receipts, and maintenance history remain untouched and
                        secure.
                      </p>
                    </div>
                  </div>

                  <div className="activation-feature-card">
                    <div className="activation-feature-icon">
                      <ArrowLeftRight size={20} />
                    </div>
                    <div>
                      <strong>Instant Switching Anytime</strong>
                      <p>
                        Freely toggle between Tenant Portal and Owner Workspace anytime from the sidebar or using shortcut
                        Alt + W.
                      </p>
                    </div>
                  </div>
                </>
              )}
            </div>

            <div className="activation-page-actions-bar">
              <p className="role-activation-guarantee">
                <Check size={16} /> No credit card required. No separate password or login needed.
              </p>
              <div className="activation-page-buttons">
                <button
                  type="button"
                  className="button"
                  disabled={busy}
                  onClick={handleCancel}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="button button-primary"
                  disabled={busy}
                  onClick={() => void handleConfirm()}
                >
                  {busy ? (
                    <>
                      <span className="spinner" /> Activating…
                    </>
                  ) : isActivatingTenant ? (
                    'Confirm & Activate Tenant Portal'
                  ) : (
                    'Confirm & Activate Owner Workspace'
                  )}
                </button>
              </div>
            </div>
          </div>
        </Panel>
      )}
    </section>
  )
}
