import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import { collectErrors, requireEmail, requireText } from '../utils/validation.js'

export default function LoginPage() {
  const { user, initializing, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function update(field) {
    return (event) => setForm((previous) => ({ ...previous, [field]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const nextErrors = collectErrors({
      email: requireEmail(form.email),
      password: requireText(form.password),
    })
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    setSubmitting(true)
    setFailure('')
    try {
      await login(form.email.trim(), form.password)
      // Returning users land back on the page that bounced them to /login.
      navigate(location.state?.from ?? '/app', { replace: true })
    } catch (error) {
      setFailure(extractErrorMessage(error, 'Não foi possível entrar. Confira e-mail e senha.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (!initializing && user) {
    return <Navigate to="/app" replace />
  }

  return (
    <main className="auth-screen">
      <div className="card auth-card">
        <Link to="/" className="brand">
          <span className="tick" aria-hidden="true" />
          AutoCare
        </Link>

        <h1>Entrar na sua conta</h1>
        <p className="auth-sub">Acompanhe manutenções, gastos e lembretes dos seus veículos.</p>

        <ErrorAlert message={failure} />

        <form onSubmit={handleSubmit} noValidate>
          <FormField label="E-mail" error={errors.email}>
            {(props) => (
              <input
                {...props}
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={update('email')}
              />
            )}
          </FormField>

          <FormField label="Senha" error={errors.password}>
            {(props) => (
              <input
                {...props}
                type="password"
                autoComplete="current-password"
                value={form.password}
                onChange={update('password')}
              />
            )}
          </FormField>

          <button className="btn" type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <p className="auth-alt">
          Ainda não tem conta? <Link to="/register">Criar conta</Link>
        </p>
      </div>
    </main>
  )
}
