import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <main className="not-found">
      <div>
        <p className="code">404</p>
        <h1>Página não encontrada</h1>
        <p style={{ color: 'var(--steel)', margin: '0.5rem 0 1.5rem' }}>
          O endereço que você abriu não existe ou foi movido.
        </p>
        <Link className="btn" to="/">
          Voltar ao início
        </Link>
      </div>
    </main>
  )
}
