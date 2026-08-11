import { Link, Navigate } from 'react-router-dom'

import { useAuth } from '../auth/AuthContext.jsx'

const FEATURES = [
  {
    mark: 'KM',
    title: 'Histórico completo por veículo',
    description:
      'Manutenções, abastecimentos e quilometragem em um só lugar, com a linha do tempo de cada carro.',
  },
  {
    mark: 'R$',
    title: 'Gastos sob controle',
    description:
      'Custo por mês, por categoria e por quilômetro rodado, calculados a partir dos seus registros reais.',
  },
  {
    mark: 'AVISO',
    title: 'Lembretes que não falham',
    description:
      'Vencimentos por data ou por quilometragem, com destaque automático para o que já está atrasado.',
  },
]

export default function LandingPage() {
  const { user, initializing } = useAuth()

  if (!initializing && user) {
    return <Navigate to="/app" replace />
  }

  return (
    <>
      <header className="landing-header">
        <span className="brand">
          <span className="tick" aria-hidden="true" />
          AutoCare
        </span>
        <nav style={{ display: 'flex', gap: '0.6rem' }}>
          <Link className="btn btn-outline-light btn-sm" to="/login">
            Entrar
          </Link>
          <Link className="btn btn-amber btn-sm" to="/register">
            Criar conta
          </Link>
        </nav>
      </header>

      <section className="landing-hero">
        <div className="hero-inner">
          <div>
            <p className="eyebrow">Manutenção · Combustível · Lembretes</p>
            <h1>Seu carro com histórico de oficina, sem papelada.</h1>
            <p className="lead">
              O AutoCare registra cada manutenção e abastecimento, acompanha a quilometragem e
              transforma seus gastos em números que fazem sentido: custo por quilômetro, consumo
              real e o que vence em seguida.
            </p>
            <div className="hero-actions">
              <Link className="btn btn-amber" to="/register">
                Começar agora
              </Link>
              <Link className="btn btn-outline-light" to="/login">
                Já tenho conta
              </Link>
            </div>
          </div>

          <div className="gauge-panel" aria-hidden="true">
            <div className="gauge">
              <span className="dial" style={{ '--sweep': '210deg' }} />
              <div>
                <div className="gauge-label">Consumo médio</div>
                <div className="gauge-value">11,4 km/L</div>
              </div>
            </div>
            <div className="gauge">
              <span className="dial" style={{ '--sweep': '150deg' }} />
              <div>
                <div className="gauge-label">Custo por km</div>
                <div className="gauge-value">R$ 0,62</div>
              </div>
            </div>
            <div className="gauge">
              <span className="dial" style={{ '--sweep': '90deg' }} />
              <div>
                <div className="gauge-label">Próxima revisão</div>
                <div className="gauge-value">1.650 km</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="landing-features">
        {FEATURES.map((feature) => (
          <div className="feature" key={feature.title}>
            <span className="feature-mark">{feature.mark}</span>
            <h3>{feature.title}</h3>
            <p>{feature.description}</p>
          </div>
        ))}
      </section>

      <footer className="landing-footer">
        AutoCare — projeto de portfólio de código aberto, licença MIT.
      </footer>
    </>
  )
}
