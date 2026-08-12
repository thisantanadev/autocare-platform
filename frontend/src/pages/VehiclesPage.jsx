import { Link } from 'react-router-dom'

import { listVehicles } from '../api/vehicles.js'
import EmptyState from '../components/EmptyState.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import PlateBadge from '../components/PlateBadge.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { formatKm } from '../utils/format.js'
import { fuelTypeLabel } from '../utils/labels.js'

export default function VehiclesPage() {
  const { data, loading, error } = useAsyncData(
    listVehicles,
    'Não foi possível carregar seus veículos.',
  )

  return (
    <>
      <PageHeader
        title="Veículos"
        subtitle="Cada veículo tem seu próprio histórico de manutenções, abastecimentos e lembretes."
        actions={
          <Link className="btn" to="/app/vehicles/new">
            Adicionar veículo
          </Link>
        }
      />

      <ErrorAlert message={error} />

      {loading && <LoadingBlock label="Carregando veículos" />}

      {!loading && !error && data.length === 0 && (
        <div className="card">
          <EmptyState
            title="Nenhum veículo cadastrado"
            description="Cadastre um veículo para começar a acompanhar custos, consumo e vencimentos."
            action={
              <Link className="btn" to="/app/vehicles/new">
                Adicionar veículo
              </Link>
            }
          />
        </div>
      )}

      {!loading && !error && data.length > 0 && (
        <div className="vehicle-grid">
          {data.map((vehicle) => (
            <Link
              className="card vehicle-card"
              key={vehicle.id}
              to={`/app/vehicles/${vehicle.id}`}
            >
              <div className="vehicle-name">{vehicle.displayName}</div>
              <div className="vehicle-sub">
                {vehicle.brand} {vehicle.model} · {vehicle.manufacturingYear}
                {vehicle.modelYear && vehicle.modelYear !== vehicle.manufacturingYear
                  ? `/${vehicle.modelYear}`
                  : ''}
              </div>
              <div className="vehicle-meta">
                {vehicle.licensePlate ? (
                  <PlateBadge plate={vehicle.licensePlate} />
                ) : (
                  <span className="badge badge-neutral">Sem placa</span>
                )}
                <span className="odometer">{formatKm(vehicle.currentMileage)}</span>
              </div>
              <div style={{ marginTop: '0.6rem' }}>
                <span className="badge badge-neutral">{fuelTypeLabel(vehicle.fuelType)}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  )
}
