import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/AuthContext.jsx'

function Brand() {
  return (
    <NavLink to="/app" className="brand">
      <span className="tick" aria-hidden="true" />
      AutoCare
    </NavLink>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [navOpen, setNavOpen] = useState(false)

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  const closeNav = () => setNavOpen(false)

  return (
    <div className="shell">
      <header className="mobile-topbar">
        <Brand />
        <button
          type="button"
          className="menu-button"
          aria-expanded={navOpen}
          onClick={() => setNavOpen((open) => !open)}
        >
          Menu
        </button>
      </header>

      {navOpen && <div className="nav-backdrop" onClick={closeNav} aria-hidden="true" />}

      <aside className={`sidebar ${navOpen ? 'open' : ''}`}>
        <Brand />
        <nav className="side-nav" aria-label="Navegação principal">
          <NavLink to="/app" end onClick={closeNav}>
            Painel
          </NavLink>
          <NavLink to="/app/vehicles" onClick={closeNav}>
            Veículos
          </NavLink>
          <NavLink to="/app/profile" onClick={closeNav}>
            Perfil
          </NavLink>
        </nav>
        <div className="sidebar-footer">
          <span className="user-name">{user?.name}</span>
          <button type="button" onClick={handleLogout}>
            Sair
          </button>
        </div>
      </aside>

      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
