import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import { deleteFuelEntry, listFuelEntries } from '../api/fuel.js'
import { deleteMaintenanceRecord, listMaintenanceRecords } from '../api/maintenance.js'
import {
  completeReminder,
  deleteReminder,
  listReminders,
  reopenReminder,
} from '../api/reminders.js'
import { getVehicle, getVehicleAnalytics } from '../api/vehicles.js'
import CategoryBarChart from '../components/charts/CategoryBarChart.jsx'
import MonthlyExpensesChart from '../components/charts/MonthlyExpensesChart.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import EmptyState from '../components/EmptyState.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import PlateBadge from '../components/PlateBadge.jsx'
import StatCard from '../components/StatCard.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import {
  formatConsumption,
  formatCurrency,
  formatDate,
  formatKm,
  formatLiters,
  formatPercentage,
} from '../utils/format.js'
import {
  analyticsWarningMessage,
  fuelTypeLabel,
  maintenanceCategoryLabel,
  TREND_LABELS,
  UPCOMING_STATUS_LABELS,
} from '../utils/labels.js'

const TABS = [
  { key: 'overview', label: 'Visão geral' },
  { key: 'maintenance', label: 'Manutenções' },
  { key: 'fuel', label: 'Abastecimentos' },
  { key: 'reminders', label: 'Lembretes' },
]

const PAGE_SIZE = 10

/** Confirmation + delete plumbing shared by the maintenance and fuel tabs. */
function useDeletion(remove, onDeleted) {
  const [target, setTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function confirm() {
    setBusy(true)
    setError('')
    try {
      await remove(target.id)
      setTarget(null)
      onDeleted()
    } catch (failure) {
      setTarget(null)
      setError(extractErrorMessage(failure, 'Não foi possível excluir o registro.'))
    } finally {
      setBusy(false)
    }
  }

  return { target, setTarget, busy, error, confirm }
}

function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <div className="pagination">
      <button
        type="button"
        className="btn btn-secondary btn-sm"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        Anterior
      </button>
      <span>
        Página {page + 1} de {totalPages}
      </span>
      <button
        type="button"
        className="btn btn-secondary btn-sm"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Próxima
      </button>
    </div>
  )
}

function RowActions({ editTo, onDelete }) {
  return (
    <div style={{ display: 'flex', gap: '0.4rem', justifyContent: 'flex-end' }}>
      <Link className="btn btn-secondary btn-sm" to={editTo}>
        Editar
      </Link>
      <button type="button" className="btn btn-ghost btn-sm" onClick={onDelete}>
        Excluir
      </button>
    </div>
  )
}

function OverviewTab({ vehicleId }) {
  const loadAnalytics = useCallback(() => getVehicleAnalytics(vehicleId), [vehicleId])
  const { data, loading, error } = useAsyncData(
    loadAnalytics,
    'As análises estão indisponíveis no momento.',
  )

  if (loading) {
    return <LoadingBlock label="Calculando análises" />
  }

  if (error) {
    // The analytics service is optional: the rest of the vehicle history
    // stays usable when it is unreachable.
    return (
      <div className="alert alert-warning" role="status">
        {error} Os registros de manutenção e abastecimento continuam disponíveis nas outras abas.
      </div>
    )
  }

  const { totals, fuelStats, trend, periodComparison, monthlyCosts, costByCategory } = data
  const categoryData = costByCategory.map((item) => ({
    label: maintenanceCategoryLabel(item.category),
    total: Number(item.total),
  }))

  return (
    <>
      {data.warnings.length > 0 && (
        <div className="alert alert-warning" role="status">
          <ul style={{ listStyle: 'none', display: 'grid', gap: '0.25rem' }}>
            {data.warnings.map((warning) => (
              <li key={warning.code}>{analyticsWarningMessage(warning)}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid-cards">
        <StatCard
          label="Custo operacional"
          value={formatCurrency(totals.operatingCost)}
          detail="Manutenções + combustível"
        />
        <StatCard label="Manutenções" value={formatCurrency(totals.maintenanceCost)} />
        <StatCard label="Combustível" value={formatCurrency(totals.fuelCost)} />
        <StatCard
          label="Custo por km"
          value={formatCurrency(fuelStats.costPerKm)}
          detail={fuelStats.costPerKm === null ? 'Dados insuficientes' : undefined}
        />
      </div>

      <div className="grid-cards">
        <StatCard
          label="Consumo médio"
          value={formatConsumption(fuelStats.averageConsumptionKmPerLiter)}
          detail={
            fuelStats.averageConsumptionKmPerLiter === null
              ? 'Precisa de 3 tanques cheios'
              : 'Com base nos tanques cheios'
          }
        />
        <StatCard
          label="Preço médio do litro"
          value={formatCurrency(fuelStats.averagePricePerLiter)}
        />
        <StatCard label="Total abastecido" value={formatLiters(fuelStats.totalLiters)} />
        <StatCard
          label="Tendência do mês"
          value={TREND_LABELS[trend.direction] ?? trend.direction}
          mono={false}
          detail={`${formatCurrency(trend.currentMonthTotal)} vs. média de ${formatCurrency(
            trend.previousThreeMonthAverage,
          )}`}
        />
      </div>

      <div className="two-col">
        <div className="section-stack">
          <section className="card">
            <h2 className="card-title">Custos por mês</h2>
            {monthlyCosts.length === 0 ? (
              <EmptyState
                title="Sem lançamentos"
                description="Registre manutenções ou abastecimentos para ver a evolução mensal."
              />
            ) : (
              <MonthlyExpensesChart
                data={monthlyCosts.map((month) => ({
                  month: month.month,
                  maintenance: Number(month.maintenanceCost),
                  fuel: Number(month.fuelCost),
                }))}
              />
            )}
          </section>

          <section className="card">
            <h2 className="card-title">Manutenções por categoria</h2>
            {categoryData.length === 0 ? (
              <EmptyState
                title="Sem manutenções"
                description="A distribuição por categoria aparece após o primeiro registro."
              />
            ) : (
              <CategoryBarChart data={categoryData} />
            )}
          </section>
        </div>

        <div className="section-stack">
          <section className="card">
            <h2 className="card-title">Comparação de períodos</h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--steel)', marginBottom: '0.75rem' }}>
              Últimos {periodComparison.periodDays} dias contra os {periodComparison.periodDays}{' '}
              anteriores.
            </p>
            <div className="list-row">
              <div className="row-title">Período atual</div>
              <span className="money">{formatCurrency(periodComparison.currentPeriodTotal)}</span>
            </div>
            <div className="list-row">
              <div className="row-title">Período anterior</div>
              <span className="money">{formatCurrency(periodComparison.previousPeriodTotal)}</span>
            </div>
            <div className="list-row">
              <div className="row-title">Variação</div>
              <span
                className="money"
                style={{
                  color:
                    periodComparison.changePercentage === null
                      ? 'var(--steel)'
                      : Number(periodComparison.changePercentage) > 0
                        ? 'var(--red)'
                        : 'var(--green)',
                }}
              >
                {formatPercentage(periodComparison.changePercentage)}
              </span>
            </div>
          </section>

          <section className="card">
            <h2 className="card-title">Próximas manutenções</h2>
            {data.upcomingMaintenance.length === 0 ? (
              <p style={{ color: 'var(--steel)', fontSize: '0.9rem' }}>
                Nenhuma revisão programada nos registros.
              </p>
            ) : (
              <ul className="warning-list">
                {data.upcomingMaintenance.map((item, index) => (
                  <li className="list-row" key={`${item.title}-${index}`}>
                    <div>
                      <div className="row-title">{item.title}</div>
                      <div className="row-sub">
                        {[
                          item.nextServiceDate ? formatDate(item.nextServiceDate) : null,
                          item.nextServiceMileage ? formatKm(item.nextServiceMileage) : null,
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </div>
                    </div>
                    <span
                      className={`badge ${
                        item.status === 'OVERDUE'
                          ? 'badge-red'
                          : item.status === 'DUE_SOON'
                            ? 'badge-amber'
                            : 'badge-neutral'
                      }`}
                    >
                      {UPCOMING_STATUS_LABELS[item.status] ?? item.status}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
    </>
  )
}

function MaintenanceTab({ vehicleId }) {
  const [page, setPage] = useState(0)
  const loadRecords = useCallback(
    () => listMaintenanceRecords(vehicleId, page, PAGE_SIZE),
    [vehicleId, page],
  )
  const { data, loading, error, reload } = useAsyncData(
    loadRecords,
    'Não foi possível carregar as manutenções.',
  )
  const deletion = useDeletion(deleteMaintenanceRecord, reload)

  if (loading) {
    return <LoadingBlock label="Carregando manutenções" />
  }

  return (
    <>
      <ErrorAlert message={error || deletion.error} />

      {data && data.content.length === 0 ? (
        <div className="card">
          <EmptyState
            title="Nenhuma manutenção registrada"
            description="Registre serviços, trocas e revisões para montar o histórico do veículo."
            action={
              <Link className="btn" to={`/app/vehicles/${vehicleId}/maintenance/new`}>
                Registrar manutenção
              </Link>
            }
          />
        </div>
      ) : (
        data && (
          <div className="card">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Categoria</th>
                    <th>Serviço</th>
                    <th className="num">Km</th>
                    <th className="num">Custo</th>
                    <th>Oficina</th>
                    <th>
                      <span className="visually-hidden">Ações</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((record) => (
                    <tr key={record.id}>
                      <td>{formatDate(record.serviceDate)}</td>
                      <td>
                        <span className="badge badge-neutral">
                          {maintenanceCategoryLabel(record.category)}
                        </span>
                      </td>
                      <td>{record.title}</td>
                      <td className="num odometer">{formatKm(record.mileageAtService)}</td>
                      <td className="num money">{formatCurrency(record.cost)}</td>
                      <td>{record.workshop ?? '—'}</td>
                      <td>
                        <RowActions
                          editTo={`/app/maintenance/${record.id}/edit`}
                          onDelete={() => deletion.setTarget(record)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        )
      )}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title="Excluir manutenção?"
        description={`"${deletion.target?.title}" será removida do histórico. Esta ação não pode ser desfeita.`}
        busy={deletion.busy}
        onConfirm={deletion.confirm}
        onCancel={() => deletion.setTarget(null)}
      />
    </>
  )
}

function FuelTab({ vehicleId }) {
  const [page, setPage] = useState(0)
  const loadEntries = useCallback(
    () => listFuelEntries(vehicleId, page, PAGE_SIZE),
    [vehicleId, page],
  )
  const { data, loading, error, reload } = useAsyncData(
    loadEntries,
    'Não foi possível carregar os abastecimentos.',
  )
  const deletion = useDeletion(deleteFuelEntry, reload)

  if (loading) {
    return <LoadingBlock label="Carregando abastecimentos" />
  }

  return (
    <>
      <ErrorAlert message={error || deletion.error} />

      {data && data.content.length === 0 ? (
        <div className="card">
          <EmptyState
            title="Nenhum abastecimento registrado"
            description="Registre abastecimentos com tanque cheio para calcular o consumo médio real."
            action={
              <Link className="btn" to={`/app/vehicles/${vehicleId}/fuel/new`}>
                Registrar abastecimento
              </Link>
            }
          />
        </div>
      ) : (
        data && (
          <div className="card">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th className="num">Odômetro</th>
                    <th className="num">Litros</th>
                    <th className="num">Total</th>
                    <th className="num">R$/L</th>
                    <th>Tanque</th>
                    <th>
                      <span className="visually-hidden">Ações</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((entry) => (
                    <tr key={entry.id}>
                      <td>{formatDate(entry.refuelDate)}</td>
                      <td className="num odometer">{formatKm(entry.odometer)}</td>
                      <td className="num odometer">{formatLiters(entry.liters)}</td>
                      <td className="num money">{formatCurrency(entry.totalCost)}</td>
                      <td className="num money">{formatCurrency(entry.pricePerLiter)}</td>
                      <td>
                        <span className={`badge ${entry.fullTank ? 'badge-green' : 'badge-neutral'}`}>
                          {entry.fullTank ? 'Cheio' : 'Parcial'}
                        </span>
                      </td>
                      <td>
                        <RowActions
                          editTo={`/app/fuel/${entry.id}/edit`}
                          onDelete={() => deletion.setTarget(entry)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        )
      )}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title="Excluir abastecimento?"
        description="O abastecimento será removido do histórico e deixará de contar no consumo médio."
        busy={deletion.busy}
        onConfirm={deletion.confirm}
        onCancel={() => deletion.setTarget(null)}
      />
    </>
  )
}

function RemindersTab({ vehicleId }) {
  const loadReminders = useCallback(() => listReminders(vehicleId), [vehicleId])
  const { data, loading, error, reload } = useAsyncData(
    loadReminders,
    'Não foi possível carregar os lembretes.',
  )
  const deletion = useDeletion(deleteReminder, reload)
  const [updatingId, setUpdatingId] = useState(null)
  const [actionError, setActionError] = useState('')

  async function toggleStatus(reminder) {
    setUpdatingId(reminder.id)
    setActionError('')
    try {
      if (reminder.status === 'COMPLETED') {
        await reopenReminder(reminder.id)
      } else {
        await completeReminder(reminder.id)
      }
      reload()
    } catch (failure) {
      setActionError(extractErrorMessage(failure, 'Não foi possível atualizar o lembrete.'))
    } finally {
      setUpdatingId(null)
    }
  }

  if (loading) {
    return <LoadingBlock label="Carregando lembretes" />
  }

  return (
    <>
      <ErrorAlert message={error || deletion.error || actionError} />

      {data && data.length === 0 ? (
        <div className="card">
          <EmptyState
            title="Nenhum lembrete"
            description="Crie lembretes por data ou por quilometragem para não perder revisões, seguro e licenciamento."
            action={
              <Link className="btn" to={`/app/vehicles/${vehicleId}/reminders/new`}>
                Novo lembrete
              </Link>
            }
          />
        </div>
      ) : (
        data && (
          <div className="card">
            <ul className="warning-list">
              {data.map((reminder) => {
                const due = [
                  reminder.dueDate ? formatDate(reminder.dueDate) : null,
                  reminder.dueMileage ? formatKm(reminder.dueMileage) : null,
                ]
                  .filter(Boolean)
                  .join(' · ')

                return (
                  <li className="list-row" key={reminder.id}>
                    <div>
                      <div className="row-title">{reminder.title}</div>
                      <div className="row-sub">{due || 'Sem vencimento definido'}</div>
                    </div>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                        flexWrap: 'wrap',
                      }}
                    >
                      {reminder.status === 'COMPLETED' ? (
                        <span className="badge badge-green">Concluído</span>
                      ) : (
                        <span className={`badge ${reminder.overdue ? 'badge-red' : 'badge-amber'}`}>
                          {reminder.overdue ? 'Atrasado' : 'Ativo'}
                        </span>
                      )}
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        disabled={updatingId === reminder.id}
                        onClick={() => toggleStatus(reminder)}
                      >
                        {reminder.status === 'COMPLETED' ? 'Reabrir' : 'Concluir'}
                      </button>
                      <RowActions
                        editTo={`/app/reminders/${reminder.id}/edit`}
                        onDelete={() => deletion.setTarget(reminder)}
                      />
                    </div>
                  </li>
                )
              })}
            </ul>
          </div>
        )
      )}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title="Excluir lembrete?"
        description={`"${deletion.target?.title}" será removido. Esta ação não pode ser desfeita.`}
        busy={deletion.busy}
        onConfirm={deletion.confirm}
        onCancel={() => deletion.setTarget(null)}
      />
    </>
  )
}

export default function VehicleDetailPage() {
  const { vehicleId } = useParams()
  const [activeTab, setActiveTab] = useState('overview')

  const loadVehicle = useCallback(() => getVehicle(vehicleId), [vehicleId])
  const { data: vehicle, loading, error } = useAsyncData(
    loadVehicle,
    'Não foi possível carregar o veículo.',
  )

  if (loading) {
    return <LoadingBlock label="Carregando veículo" />
  }

  if (error) {
    return (
      <>
        <ErrorAlert message={error} />
        <Link className="btn btn-secondary" to="/app/vehicles">
          Voltar para veículos
        </Link>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title={vehicle.displayName}
        subtitle={`${vehicle.brand} ${vehicle.model} · ${vehicle.manufacturingYear}${
          vehicle.modelYear && vehicle.modelYear !== vehicle.manufacturingYear
            ? `/${vehicle.modelYear}`
            : ''
        } · ${fuelTypeLabel(vehicle.fuelType)}`}
        actions={
          <>
            <Link className="btn btn-secondary" to={`/app/vehicles/${vehicleId}/edit`}>
              Editar veículo
            </Link>
            <Link className="btn btn-secondary" to={`/app/vehicles/${vehicleId}/fuel/new`}>
              + Abastecimento
            </Link>
            <Link className="btn btn-secondary" to={`/app/vehicles/${vehicleId}/reminders/new`}>
              + Lembrete
            </Link>
            <Link className="btn" to={`/app/vehicles/${vehicleId}/maintenance/new`}>
              + Manutenção
            </Link>
          </>
        }
      />

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          flexWrap: 'wrap',
          marginBottom: '1.25rem',
        }}
      >
        {vehicle.licensePlate && <PlateBadge plate={vehicle.licensePlate} />}
        <span className="odometer" style={{ color: 'var(--steel)' }}>
          {formatKm(vehicle.currentMileage)}
        </span>
      </div>

      <div className="tabs" role="tablist" aria-label="Seções do veículo">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.key}
            className={activeTab === tab.key ? 'active' : ''}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && <OverviewTab vehicleId={vehicleId} />}
      {activeTab === 'maintenance' && <MaintenanceTab vehicleId={vehicleId} />}
      {activeTab === 'fuel' && <FuelTab vehicleId={vehicleId} />}
      {activeTab === 'reminders' && <RemindersTab vehicleId={vehicleId} />}
    </>
  )
}
