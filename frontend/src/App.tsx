import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import {
  ArrowLeftRight,
  Bell,
  Building2,
  Check,
  ChevronRight,
  CircleHelp,
  CreditCard,
  Home,
  LogOut,
  Menu,
  Moon,
  MoreHorizontal,
  Settings,
  Sparkles,
  Sun,
  Users,
  Wrench,
  X,
} from 'lucide-react'
import { useAuth } from './auth-context'
import { ConfirmationDialog, InlineSuccess, LoadingState } from './components'
import { WorkspaceActivationPage } from './WorkspaceActivationPage'
import type { Role } from './types'
import { AuthPage, DashboardPage, LeasesPage, MaintenanceDetailPage, MaintenancePage, NoticeDetailPage, NoticesPage, OwnerTenantDetailPage, ProfilePage, PropertiesPage, RentPage, TenantLeaseDetailPage } from './pages'
import { FaqPage } from './faq'

type Theme = 'dark' | 'light'

interface NavItem {
  to: string
  label: string
  icon: typeof Home
}

interface NavSection {
  title: string
  items: NavItem[]
}

const ownerNavSections: NavSection[] = [
  {
    title: 'MANAGE',
    items: [
      { to: '/owner/overview', label: 'Overview', icon: Home },
      { to: '/owner/properties', label: 'Properties', icon: Building2 },
      { to: '/owner/leases', label: 'Tenants & Leases', icon: Users },
    ],
  },
  {
    title: 'OPERATIONS',
    items: [
      { to: '/owner/rent', label: 'Payments', icon: CreditCard },
      { to: '/owner/maintenance', label: 'Maintenance', icon: Wrench },
    ],
  },
  {
    title: 'ACCOUNT',
    items: [
      { to: '/settings', label: 'Settings', icon: Settings },
      { to: '/faq', label: 'FAQ', icon: CircleHelp },
    ],
  },
]

const tenantNavSections: NavSection[] = [
  {
    title: 'MANAGE',
    items: [
      { to: '/tenant/overview', label: 'My home', icon: Home },
    ],
  },
  {
    title: 'OPERATIONS',
    items: [
      { to: '/tenant/rent', label: 'Payments', icon: CreditCard },
      { to: '/tenant/maintenance', label: 'Maintenance', icon: Wrench },
      { to: '/tenant/notices', label: 'Notices', icon: Bell },
    ],
  },
  {
    title: 'ACCOUNT',
    items: [
      { to: '/settings', label: 'Settings', icon: Settings },
      { to: '/faq', label: 'FAQ', icon: CircleHelp },
    ],
  },
]

function AuthenticatedLayout() {
  const { user, badges, switchRole, signOut } = useAuth()
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem('tom-theme') as Theme) || 'dark')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [confirmSignOut, setConfirmSignOut] = useState(false)
  const [workspaceExpanded, setWorkspaceExpanded] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [switching, setSwitching] = useState(false)
  const [workspaceNotice, setWorkspaceNotice] = useState('')
  const prevRoleRef = useRef(user?.role)
  const userMenuRef = useRef<HTMLDivElement>(null)
  const workspaceRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()

  const navSections = user?.role === 'OWNER' ? ownerNavSections : tenantNavSections
  const allNavItems = navSections.flatMap((section) => section.items)
  const matchedNavItem = allNavItems.find((link) => location.pathname.startsWith(link.to))
  const pageTitle = matchedNavItem?.label || 'Overview'

  const noticesPath = user?.role === 'OWNER' ? '/owner/notices' : '/tenant/notices'
  const isNoticeDetail = location.pathname.startsWith(`${noticesPath}/`)
  const maintenancePath = user?.role === 'OWNER' ? '/owner/maintenance' : '/tenant/maintenance'
  const isMaintenanceDetail = location.pathname.startsWith(`${maintenancePath}/`)
  const isTenantLeaseDetail = location.pathname.startsWith('/tenant/leases/')
  const isOwnerTenantDetail = location.pathname.startsWith('/owner/leases/tenants/')
  const isActivationPage = location.pathname.startsWith('/workspace/activate/')

  const userRoles = user?.roles ?? (user ? [user.role] : [])
  const hasMultipleRoles = userRoles.length > 1

  useEffect(() => {
    if (prevRoleRef.current && user?.role && prevRoleRef.current !== user.role) {
      setWorkspaceNotice(
        user.role === 'OWNER' ? 'Switched to Owner Workspace.' : 'Switched to Tenant Portal.'
      )
    }
    prevRoleRef.current = user?.role
  }, [user?.role])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('tom-theme', theme)
  }, [theme])

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      const target = event.target as Node
      if (userMenuRef.current && !userMenuRef.current.contains(target)) {
        setUserMenuOpen(false)
      }
      if (workspaceRef.current && !workspaceRef.current.contains(target)) {
        setWorkspaceExpanded(false)
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setUserMenuOpen(false)
        setWorkspaceExpanded(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [])

  useEffect(() => {
    setUserMenuOpen(false)
    setWorkspaceExpanded(false)
    setMobileOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (mobileOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileOpen])

  // Alt + W keyboard shortcut to quickly toggle workspaces
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.altKey && (event.key === 'w' || event.key === 'W')) {
        if (hasMultipleRoles && user && !switching) {
          event.preventDefault()
          const nextRole: Role = user.role === 'OWNER' ? 'TENANT' : 'OWNER'
          setSwitching(true)
          void switchRole(nextRole)
            .then(() => {
              navigate(nextRole === 'OWNER' ? '/owner/overview' : '/tenant/overview')
              setMobileOpen(false)
            })
            .finally(() => setSwitching(false))
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [hasMultipleRoles, user, switching, switchRole, navigate])

  if (!user) return <Navigate to="/login" replace />

  async function handleSelectWorkspace(targetRole: Role) {
    if (user?.role === targetRole) {
      setWorkspaceExpanded(false)
      return
    }

    const isActivated = userRoles.includes(targetRole)
    if (!isActivated) {
      setWorkspaceExpanded(false)
      navigate(`/workspace/activate/${targetRole.toLowerCase()}`)
      setMobileOpen(false)
      return
    }

    setSwitching(true)
    setWorkspaceExpanded(false)
    try {
      await switchRole(targetRole)
      navigate(targetRole === 'OWNER' ? '/owner/overview' : '/tenant/overview')
      setMobileOpen(false)
    } finally {
      setSwitching(false)
    }
  }

  return (
    <div className="app-shell">
      <button className={`mobile-scrim ${mobileOpen ? 'is-visible' : ''}`} aria-label="Close navigation" onClick={() => setMobileOpen(false)} />
      <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-header-row">
          <Link
            to={user.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'}
            className="brand-lockup"
            title="Smylio Home"
            aria-label="Go to Smylio Home"
          >
            <img className="brand-logo theme-logo-dark" src="/brand/smylio-logo.png" alt="" />
            <img className="brand-logo theme-logo-light" src="/brand/smylio-logo-light.png" alt="" />
            <div><strong>Smylio</strong><span>Renting Made Happier</span></div>
          </Link>
          <button
            type="button"
            className="icon-button mobile-drawer-close"
            aria-label="Close navigation menu"
            onClick={() => setMobileOpen(false)}
          >
            <X size={18} />
          </button>
        </div>

        <div className="sidebar-workspace-wrap" ref={workspaceRef}>
          <button
            className={`workspace-card ${workspaceExpanded ? 'workspace-card-expanded' : ''}`}
            type="button"
            aria-expanded={workspaceExpanded}
            onClick={() => setWorkspaceExpanded((expanded) => !expanded)}
          >
            <div className="workspace-card-icon">
              {user.role === 'OWNER' ? <Building2 size={22} /> : <Home size={22} />}
            </div>
            <div className="workspace-card-details">
              <div className="workspace-card-title-row">
                <span className="workspace-card-title">
                  {user.role === 'OWNER' ? 'Owner workspace' : 'Tenant portal'}
                </span>
                <ChevronRight size={15} className="workspace-card-chevron" />
              </div>
              <div className="workspace-card-subtitle">{user.name}</div>
            </div>
          </button>

          {workspaceExpanded && (
            <div className="workspace-switcher-menu" role="menu">
              <div className="workspace-menu-header">SWITCH WORKSPACE</div>

              {/* Owner Workspace Item */}
              <button
                type="button"
                className={`workspace-menu-item ${user.role === 'OWNER' ? 'workspace-menu-item-active' : ''}`}
                onClick={() => void handleSelectWorkspace('OWNER')}
              >
                <div className="workspace-menu-item-icon"><Building2 size={16} /></div>
                <div className="workspace-menu-item-copy">
                  <strong>Owner workspace</strong>
                  <small>{userRoles.includes('OWNER') ? 'Manage properties & leases' : 'Activate property management'}</small>
                </div>
                {user.role === 'OWNER' ? (
                  <span className="workspace-active-badge"><Check size={14} /> Active</span>
                ) : userRoles.includes('OWNER') ? (
                  badges?.ownerPendingApprovals ? (
                    <span className="workspace-badge workspace-badge-alert" title="Pending rent approvals">{badges.ownerPendingApprovals} pending</span>
                  ) : badges?.ownerOpenTickets ? (
                    <span className="workspace-badge" title="Open maintenance tickets">{badges.ownerOpenTickets} open</span>
                  ) : null
                ) : (
                  <span className="workspace-badge workspace-badge-activate"><Sparkles size={11} /> Activate</span>
                )}
              </button>

              {/* Tenant Portal Item */}
              <button
                type="button"
                className={`workspace-menu-item ${user.role === 'TENANT' ? 'workspace-menu-item-active' : ''}`}
                onClick={() => void handleSelectWorkspace('TENANT')}
              >
                <div className="workspace-menu-item-icon"><Home size={16} /></div>
                <div className="workspace-menu-item-copy">
                  <strong>Tenant portal</strong>
                  <small>{userRoles.includes('TENANT') ? 'My home, rents & tickets' : 'Activate tenant capabilities'}</small>
                </div>
                {user.role === 'TENANT' ? (
                  <span className="workspace-active-badge"><Check size={14} /> Active</span>
                ) : userRoles.includes('TENANT') ? (
                  badges?.tenantPendingRents ? (
                    <span className="workspace-badge workspace-badge-alert" title="Outstanding rent due">{badges.tenantPendingRents} rent due</span>
                  ) : badges?.tenantUnreadNotices ? (
                    <span className="workspace-badge" title="Unread notices">{badges.tenantUnreadNotices} notice</span>
                  ) : null
                ) : (
                  <span className="workspace-badge workspace-badge-activate"><Sparkles size={11} /> Activate</span>
                )}
              </button>
            </div>
          )}
        </div>

        <nav className="side-nav" aria-label="Main navigation">
          {navSections.map((section) => (
            <div key={section.title} className="nav-section">
              <div className="nav-section-title">{section.title}</div>
              {section.items.map(({ to, label, icon: Icon }) => {
                const isSettings = to === '/settings'
                const isPayments = to === '/owner/rent' || to === '/tenant/rent'
                const isFaq = to === '/faq'

                return (
                  <NavLink
                    key={to}
                    to={to}
                    className={({ isActive }) => {
                      const active =
                        isActive ||
                        (isSettings && (location.pathname === '/profile' || location.pathname === '/settings')) ||
                        (isPayments && (location.pathname.startsWith('/owner/payments') || location.pathname.startsWith('/tenant/payments'))) ||
                        (isFaq && (location.pathname === '/faq' || location.pathname === '/help'))
                      return `nav-link ${active ? 'nav-link-active' : ''}`
                    }}
                  >
                    <Icon size={18} strokeWidth={1.8} />
                    <span>{label}</span>
                  </NavLink>
                )
              })}
            </div>
          ))}
        </nav>

        <div className="sidebar-bottom-user-wrap" ref={userMenuRef}>
          {userMenuOpen && (
            <div className="sidebar-user-menu" role="menu">
              <button
                type="button"
                className="sidebar-user-menu-item"
                onClick={() => {
                  setUserMenuOpen(false)
                  navigate('/settings')
                }}
              >
                <Settings size={16} />
                <span>Profile & Settings</span>
              </button>

              <div className="workspace-menu-divider" />

              <button
                type="button"
                className="sidebar-user-menu-item sidebar-user-menu-danger"
                onClick={() => {
                  setUserMenuOpen(false)
                  setConfirmSignOut(true)
                }}
              >
                <LogOut size={16} />
                <span>Sign out</span>
              </button>
            </div>
          )}

          <div
            className="sidebar-bottom-user"
            onClick={() => setUserMenuOpen((open) => !open)}
            role="button"
            tabIndex={0}
            aria-haspopup="menu"
            aria-expanded={userMenuOpen}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                setUserMenuOpen((open) => !open)
              }
            }}
          >
            <div className="sidebar-user-avatar">
              {user.name.slice(0, 1).toUpperCase()}
            </div>
            <div className="sidebar-user-copy">
              <strong>{user.name}</strong>
              <small>{user.role === 'OWNER' ? 'Owner account' : 'Tenant account'}</small>
            </div>
            <button
              type="button"
              className="sidebar-user-more"
              aria-label="User account actions"
              title="User account actions"
              onClick={(e) => {
                e.stopPropagation()
                setUserMenuOpen((open) => !open)
              }}
            >
              <MoreHorizontal size={18} />
            </button>
          </div>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <button className="icon-button menu-trigger" aria-label="Open navigation" onClick={() => setMobileOpen(true)}><Menu size={19} /></button>
          <div className="breadcrumbs">
            <span className="crumb-root">Workspace</span>
            <span className="crumb-separator crumb-root-separator">/</span>
            {location.pathname === '/profile' || location.pathname === '/settings' ? (
              <>
                <NavLink to={user.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'}>Overview</NavLink>
                <span className="crumb-separator">/</span>
                <strong>Profile & Settings</strong>
              </>
            ) : isActivationPage ? (
              <>
                <NavLink to={user.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'}>Overview</NavLink>
                <span className="crumb-separator">/</span>
                <strong>Activate {location.pathname.endsWith('tenant') ? 'Tenant Portal' : 'Owner Workspace'}</strong>
              </>
            ) : location.pathname === '/faq' ? (
              <strong>FAQ</strong>
            ) : isNoticeDetail ? (
              <>
                <NavLink to={noticesPath}>Notices</NavLink>
                <span className="crumb-separator">/</span>
                <strong>Notice details</strong>
              </>
            ) : isMaintenanceDetail ? (
              <>
                <NavLink to={maintenancePath}>Maintenance</NavLink>
                <span className="crumb-separator">/</span>
                <strong>Request details</strong>
              </>
            ) : isTenantLeaseDetail ? (
              <>
                <NavLink to="/tenant/overview">My home</NavLink>
                <span className="crumb-separator">/</span>
                <strong>Lease details</strong>
              </>
            ) : isOwnerTenantDetail ? (
              <>
                <NavLink to="/owner/leases">Tenants & Leases</NavLink>
                <span className="crumb-separator">/</span>
                <strong>Tenant details</strong>
              </>
            ) : (
              <strong>{pageTitle}</strong>
            )}
          </div>

          <div className="topbar-actions">
            {/* Topbar Workspace Badge */}
            <div className={`topbar-workspace-pill ${user.role === 'OWNER' ? 'topbar-pill-owner' : 'topbar-pill-tenant'}`}>
              {user.role === 'OWNER' ? (
                <>
                  <Building2 size={13} />
                  <span>Owner Workspace</span>
                </>
              ) : (
                <>
                  <Home size={13} />
                  <span>Tenant Portal</span>
                </>
              )}
            </div>

            {hasMultipleRoles && (
              <button
                type="button"
                className="button button-subtle topbar-switch-button"
                title={`Switch to ${user.role === 'OWNER' ? 'Tenant Portal' : 'Owner Workspace'}`}
                aria-label={`Switch to ${user.role === 'OWNER' ? 'Tenant Portal' : 'Owner Workspace'}`}
                disabled={switching}
                onClick={() => void handleSelectWorkspace(user.role === 'OWNER' ? 'TENANT' : 'OWNER')}
              >
                <ArrowLeftRight size={13} />
                <span className="topbar-switch-text">{user.role === 'OWNER' ? 'Tenant' : 'Owner'}</span>
              </button>
            )}

            <button
              className="icon-button theme-toggle"
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
            >
              {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
            </button>
            <div className="profile-menu">
              <Link to="/profile" className="profile-link" title="Profile & Account" aria-label="Go to Profile & Account">
                <span className="profile-avatar">{user.name.slice(0, 1).toUpperCase()}</span>
                <div className="profile-copy">
                  <strong>{user.name}</strong>
                  <small>{user.role === 'OWNER' ? 'Owner' : 'Tenant'}</small>
                </div>
              </Link>
              <button
                className="icon-button logout-button"
                aria-label="Sign out"
                title="Sign out"
                onClick={() => setConfirmSignOut(true)}
              >
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </header>
        <main className="page-content">
          {workspaceNotice && (
            <div className="workspace-switch-notice-wrap" style={{ marginBottom: '18px' }}>
              <InlineSuccess onDismiss={() => setWorkspaceNotice('')} duration={5000}>
                {workspaceNotice}
              </InlineSuccess>
            </div>
          )}
          <Outlet />
        </main>
      </div>

      {confirmSignOut && (
        <ConfirmationDialog
          title="Sign out?"
          description="You will need to sign in again to return to your workspace."
          confirmLabel="Sign out"
          onCancel={() => setConfirmSignOut(false)}
          onConfirm={() => {
            signOut()
            navigate('/login')
          }}
        />
      )}
    </div>
  )
}

function DefaultRoute() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'} replace />
}

function App() {
  const { user, initializing } = useAuth()
  const defaultPage = user?.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'

  if (initializing) {
    return <main className="page-content"><LoadingState label="Restoring session…" /></main>
  }

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={defaultPage} replace /> : <AuthPage mode="login" />} />
      <Route path="/register" element={user ? <Navigate to={defaultPage} replace /> : <AuthPage mode="register" />} />
      <Route element={<AuthenticatedLayout />}>
        <Route index element={<DefaultRoute />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="settings" element={<ProfilePage />} />
        <Route path="workspace/activate/:role" element={<WorkspaceActivationPage />} />
        <Route path="owner/overview" element={user?.role === 'OWNER' ? <DashboardPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/properties" element={user?.role === 'OWNER' ? <PropertiesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/leases" element={user?.role === 'OWNER' ? <LeasesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/leases/tenants/:tenantId" element={user?.role === 'OWNER' ? <OwnerTenantDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/rent" element={user?.role === 'OWNER' ? <RentPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/payments" element={<Navigate to="/owner/rent" replace />} />
        <Route path="owner/maintenance" element={user?.role === 'OWNER' ? <MaintenancePage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/maintenance/:requestId" element={user?.role === 'OWNER' ? <MaintenanceDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/notices" element={user?.role === 'OWNER' ? <NoticesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/notices/:noticeId" element={user?.role === 'OWNER' ? <NoticeDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/overview" element={user?.role === 'TENANT' ? <DashboardPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/leases/:leaseId" element={user?.role === 'TENANT' ? <TenantLeaseDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/rent" element={user?.role === 'TENANT' ? <RentPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/payments" element={<Navigate to="/tenant/rent" replace />} />
        <Route path="tenant/maintenance" element={user?.role === 'TENANT' ? <MaintenancePage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/maintenance/:requestId" element={user?.role === 'TENANT' ? <MaintenanceDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/notices" element={user?.role === 'TENANT' ? <NoticesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/notices/:noticeId" element={user?.role === 'TENANT' ? <NoticeDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="faq" element={<FaqPage />} />
        <Route path="help" element={<Navigate to="/faq" replace />} />
      </Route>
      <Route path="*" element={<DefaultRoute />} />
    </Routes>
  )
}

export default App