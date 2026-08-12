import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { AuthProvider } from '../auth/AuthContext.jsx'
import LoginPage from './LoginPage.jsx'

vi.mock('../api/auth.js', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  restoreSession: vi.fn(),
  getCurrentUser: vi.fn(),
}))

import * as authApi from '../api/auth.js'

const USER = { id: 'u1', name: 'Motorista Demo', email: 'demo@autocare.dev' }

beforeEach(() => {
  vi.clearAllMocks()
  // No refresh cookie yet: a rejected restore simply means "not logged in".
  authApi.restoreSession.mockRejectedValue(new Error('no session'))
})

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/app" element={<h1>Painel</h1>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  it('blocks submission and reports both fields when the form is empty', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(await screen.findByRole('button', { name: 'Entrar' }))

    expect(await screen.findAllByText('Campo obrigatório')).toHaveLength(2)
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('blocks submission when the e-mail is malformed', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(await screen.findByLabelText('E-mail'), 'sem-arroba')
    await user.type(screen.getByLabelText('Senha'), 'DemoAutoCare123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Informe um e-mail válido')).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('trims the e-mail and lands on the dashboard after signing in', async () => {
    const user = userEvent.setup()
    authApi.login.mockResolvedValue(USER)
    renderLogin()

    await user.type(await screen.findByLabelText('E-mail'), '  demo@autocare.dev  ')
    await user.type(screen.getByLabelText('Senha'), 'DemoAutoCare123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(authApi.login).toHaveBeenCalledWith('demo@autocare.dev', 'DemoAutoCare123')
    expect(await screen.findByRole('heading', { name: 'Painel' })).toBeInTheDocument()
  })

  it('surfaces the message returned by the API', async () => {
    const user = userEvent.setup()
    authApi.login.mockRejectedValue({
      response: { data: { message: 'E-mail ou senha inválidos' } },
    })
    renderLogin()

    await user.type(await screen.findByLabelText('E-mail'), 'demo@autocare.dev')
    await user.type(screen.getByLabelText('Senha'), 'senha-errada')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos')
  })
})
