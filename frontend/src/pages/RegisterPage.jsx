import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import {
  collectErrors,
  PASSWORD_MIN_LENGTH,
  requireEmail,
  requirePassword,
  requireText,
} from '../utils/validation.js'

export default function RegisterPage() {
  const { user, initializing, register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ name: '', email: '', password: '', confirmation: '' })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function update(field) {
    return (event) => setForm((previous) => ({ ...previous, [field]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const nextErrors = collectErrors({
      name: requireText(form.name, { max: 120 }),
      email: requireEmail(form.email),
      password: requirePassword(form.password),
      confirmation:
        form.confirmation === form.password ? undefined : 'As senhas não são iguais',
    })
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    setSubmitting(true)
    setFailure('')
    try {
      await register(form.name.trim(), form.email.trim(), form.password)
      navigate('/app', { replace: true })
    } catch (error) {
      setFailure(extractErrorMessage(error, 'Não foi possível criar sua conta.'))
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

        <h1>Criar sua conta</h1>
        <p className="auth-sub">
          Comece registrando seu primeiro veículo — leva menos de um minuto.
        </p>

        <ErrorAlert message={failure} />

        <form onSubmit={handleSubmit} noValidate>
          <FormField label="Nome" error={errors.name}>
            {(props) => (
              <input
                {...props}
                type="text"
                autoComplete="name"
                value={form.name}
                onChange={update('name')}
              />
            )}
          </FormField>

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

          <FormField
            label="Senha"
            error={errors.password}
            hint={`Pelo menos ${PASSWORD_MIN_LENGTH} caracteres`}
          >
            {(props) => (
              <input
                {...props}
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={update('password')}
              />
            )}
          </FormField>

          <FormField label="Confirmar senha" error={errors.confirmation}>
            {(props) => (
              <input
                {...props}
                type="password"
                autoComplete="new-password"
                value={form.confirmation}
                onChange={update('confirmation')}
              />
            )}
          </FormField>

          <button className="btn" type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? 'Criando conta…' : 'Criar conta'}
          </button>
        </form>

        <p className="auth-alt">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </div>
    </main>
  )
}
