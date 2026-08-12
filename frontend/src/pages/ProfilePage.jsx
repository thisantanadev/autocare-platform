import { useNavigate } from 'react-router-dom'

import { getCurrentUser } from '../api/auth.js'
import { useAuth } from '../auth/AuthContext.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { formatDate } from '../utils/format.js'

export default function ProfilePage() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const { data, loading, error } = useAsyncData(
    getCurrentUser,
    'Não foi possível carregar seu perfil.',
  )

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <>
      <PageHeader title="Perfil" subtitle="Dados da sua conta no AutoCare." />

      <ErrorAlert message={error} />

      {loading && <LoadingBlock label="Carregando perfil" />}

      {!loading && !error && (
        <div className="card" style={{ maxWidth: '30rem' }}>
          <h2 className="card-title">Conta</h2>

          <div className="list-row">
            <div className="row-title">Nome</div>
            <span>{data.name}</span>
          </div>
          <div className="list-row">
            <div className="row-title">E-mail</div>
            <span>{data.email}</span>
          </div>
          <div className="list-row">
            <div className="row-title">Conta criada em</div>
            <span>{formatDate(data.createdAt)}</span>
          </div>

          <div className="form-actions" style={{ marginTop: '1.25rem' }}>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>
              Sair da conta
            </button>
          </div>
        </div>
      )}
    </>
  )
}
