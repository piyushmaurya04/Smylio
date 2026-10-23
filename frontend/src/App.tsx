import { useEffect, useState } from 'react'
import { Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell,
  Building2,
  ChevronDown,
  CircleHelp,
  CreditCard,
  Home,
  LogOut,
  Menu,
  Moon,
  UserRound,
  Sun,
  Users,
  Wrench,
} from 'lucide-react'
import { useAuth } from './auth-context'
import { ConfirmationDialog } from './components'
import { AuthPage, DashboardPage, LeasesPage, MaintenanceDetailPage, MaintenancePage, NoticeDetailPage, NoticesPage, OwnerTenantDetailPage, ProfilePage, PropertiesPage, RentPage, TenantLeaseDetailPage } from './pages'

type Theme = 'dark' | 'light'

const ownerLinks = [
  { to: '/owner/overview', label: 'Overview', icon: Home },
  { to: '/owner/properties', label: 'Properties', icon: Building2 },
  { to: '/owner/leases', label: 'Tenants & leases', icon: Users },
  { to: '/owner/rent', label: 'Rent', icon: CreditCard },
  { to: '/owner/maintenance', label: 'Maintenance', icon: Wrench },
  { to: '/owner/notices', label: 'Notices', icon: Bell },
]

const tenantLinks = [
  { to: '/tenant/overview', label: 'My home', icon: Home },
  { to: '/tenant/rent', label: 'Rent', icon: CreditCard },
  { to: '/tenant/maintenance', label: 'Maintenance', icon: Wrench },
  { to: '/tenant/notices', label: 'Notices', icon: Bell },
]

function AuthenticatedLayout() {
  const { user, signOut } = useAuth()
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem('tom-theme') as Theme) || 'dark')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [confirmSignOut, setConfirmSignOut] = useState(false)
  const [workspaceExpanded, setWorkspaceExpanded] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const links = user?.role === 'OWNER' ? ownerLinks : tenantLinks
  const pageTitle = links.find((link) => location.pathname.startsWith(link.to))?.label || 'Overview'
  const noticesPath = user?.role === 'OWNER' ? '/owner/notices' : '/tenant/notices'
  const isNoticeDetail = location.pathname.startsWith(`${noticesPath}/`)
  const maintenancePath = user?.role === 'OWNER' ? '/owner/maintenance' : '/tenant/maintenance'
  const isMaintenanceDetail = location.pathname.startsWith(`${maintenancePath}/`)
  const isTenantLeaseDetail = location.pathname.startsWith('/tenant/leases/')
  const isOwnerTenantDetail = location.pathname.startsWith('/owner/leases/tenants/')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('tom-theme', theme)
  }, [theme])

  useEffect(() => setMobileOpen(false), [location.pathname])

  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="app-shell">
      <button className={`mobile-scrim ${mobileOpen ? 'is-visible' : ''}`} aria-label="Close navigation" onClick={() => setMobileOpen(false)} />
      <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
        <div className="brand-lockup">
          <img className="brand-logo theme-logo-dark" src="/smylio-logo.png" alt="" />
          <img className="brand-logo theme-logo-light" src="/smylio-logo-light.png" alt="" />
          <div><strong>Smylio</strong><span>Renting Made Happier</span></div>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        <button className={`workspace-switcher ${workspaceExpanded ? 'workspace-switcher-expanded' : ''}`} type="button" aria-expanded={workspaceExpanded} onClick={() => setWorkspaceExpanded((expanded) => !expanded)}>
          <span className="workspace-avatar">{user.name.slice(0, 1).toUpperCase()}</span>
          <span className="workspace-copy"><strong>{user.role === 'OWNER' ? 'Owner workspace' : 'Tenant portal'}</strong><small>{user.name}</small></span>
          <ChevronDown size={15} />
        </button>
        {workspaceExpanded && <div className="workspace-switcher-menu"><button type="button" className="workspace-profile-button" onClick={() => { setWorkspaceExpanded(false); navigate('/profile') }}><UserRound size={16} />Profile</button></div>}
        <nav className="side-nav" aria-label="Main navigation">
          <div className="workspace-label">MANAGE</div>
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `nav-link ${isActive ? 'nav-link-active' : ''}`}>
              <Icon size={17} strokeWidth={1.8} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-note"><span className="connection-dot" /> API connected</div>
          <button className="nav-link nav-link-button" onClick={() => navigate('/help')}><CircleHelp size={17} /><span>Help & support</span></button>
          <div className="sidebar-footer">Smylio <span>•</span> v1.0</div>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <button className="icon-button menu-trigger" aria-label="Open navigation" onClick={() => setMobileOpen(true)}><Menu size={19} /></button>
          <div className="breadcrumbs"><span>Workspace</span><span className="crumb-separator">/</span>{location.pathname === '/profile' ? <><NavLink to={user.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'}>Overview</NavLink><span className="crumb-separator">/</span><strong>Profile</strong></> : isNoticeDetail ? <><NavLink to={noticesPath}>Notices</NavLink><span className="crumb-separator">/</span><strong>Notice details</strong></> : isMaintenanceDetail ? <><NavLink to={maintenancePath}>Maintenance</NavLink><span className="crumb-separator">/</span><strong>Request details</strong></> : isTenantLeaseDetail ? <><NavLink to="/tenant/overview">My home</NavLink><span className="crumb-separator">/</span><strong>Lease details</strong></> : isOwnerTenantDetail ? <><NavLink to="/owner/leases">Tenants & leases</NavLink><span className="crumb-separator">/</span><strong>Tenant details</strong></> : <strong>{pageTitle}</strong>}</div>
          <div className="topbar-actions">
            <span className="topbar-date">Tenant & owner management</span>
            <button className="icon-button theme-toggle" aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`} title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`} onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
              {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
            </button>
            <div className="profile-menu">
              <span className="profile-avatar">{user.name.slice(0, 1).toUpperCase()}</span>
              <div className="profile-copy"><strong>{user.name}</strong><small>{user.role === 'OWNER' ? 'Owner' : 'Tenant'}</small></div>
              <button className="icon-button logout-button" aria-label="Sign out" title="Sign out" onClick={() => setConfirmSignOut(true)}><LogOut size={16} /></button>
            </div>
          </div>
        </header>
        <main className="page-content"><Outlet /></main>
      </div>
      {confirmSignOut && <ConfirmationDialog title="Sign out?" description="You will need to sign in again to return to your workspace." confirmLabel="Sign out" onCancel={() => setConfirmSignOut(false)} onConfirm={() => { signOut(); navigate('/login') }} />}
    </div>
  )
}

function DefaultRoute() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'} replace />
}

function App() {
  const { user } = useAuth()
  const defaultPage = user?.role === 'OWNER' ? '/owner/overview' : '/tenant/overview'

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={defaultPage} replace /> : <AuthPage mode="login" />} />
      <Route path="/register" element={user ? <Navigate to={defaultPage} replace /> : <AuthPage mode="register" />} />
      <Route element={<AuthenticatedLayout />}>
        <Route index element={<DefaultRoute />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="owner/overview" element={user?.role === 'OWNER' ? <DashboardPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/properties" element={user?.role === 'OWNER' ? <PropertiesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/leases" element={user?.role === 'OWNER' ? <LeasesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/leases/tenants/:tenantId" element={user?.role === 'OWNER' ? <OwnerTenantDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/rent" element={user?.role === 'OWNER' ? <RentPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/maintenance" element={user?.role === 'OWNER' ? <MaintenancePage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/maintenance/:requestId" element={user?.role === 'OWNER' ? <MaintenanceDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/notices" element={user?.role === 'OWNER' ? <NoticesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="owner/notices/:noticeId" element={user?.role === 'OWNER' ? <NoticeDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/overview" element={user?.role === 'TENANT' ? <DashboardPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/leases/:leaseId" element={user?.role === 'TENANT' ? <TenantLeaseDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/rent" element={user?.role === 'TENANT' ? <RentPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/maintenance" element={user?.role === 'TENANT' ? <MaintenancePage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/maintenance/:requestId" element={user?.role === 'TENANT' ? <MaintenanceDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/notices" element={user?.role === 'TENANT' ? <NoticesPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="tenant/notices/:noticeId" element={user?.role === 'TENANT' ? <NoticeDetailPage /> : <Navigate to={defaultPage} replace />} />
        <Route path="help" element={<HelpPage />} />
      </Route>
      <Route path="*" element={<DefaultRoute />} />
    </Routes>
  )
}

function HelpPage() {
  return <section className="page-section"><div className="page-heading"><div><p className="eyebrow">SUPPORT</p><h1>Help & support</h1><p className="page-subtitle">For help with your property account, contact your workspace administrator.</p></div></div></section>
}

export default App