import { Link } from 'react-router-dom'

import { getDashboardSummary } from '../api/dashboard.js'
import CategoryBarChart from '../components/charts/CategoryBarChart.jsx'
import MonthlyExpensesChart from '../components/charts/MonthlyExpensesChart.jsx'
import EmptyState from '../components/EmptyState.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import StatCard from '../components/StatCard.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { formatCurrency, formatDate, formatKm } from '../utils/format.js'
import { maintenanceCategoryLabel } from '../utils/labels.js'

const ACTIVITY_LABELS = { MAINTENANCE: 'Manutenção', FUEL: 'Abastecimento' }

function ReminderRow({ reminder }) {
  const due = [
    reminder.dueDate ? formatDate(reminder.dueDate) : null,
    reminder.dueMileage ? formatKm(reminder.dueMileage) : null,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <li className="list-row">
      <div>
        <div className="row-title">{reminder.title}</div>
        <div className="row-sub">
          {reminder.vehicleName}
          {due && ` — vence em ${due}`}
        </div>
      </div>
      <span className={`badge ${reminder.overdue ? 'badge-red' : 'badge-amber'}`}>
        {reminder.overdue ? 'Atrasado' : 'A vencer'}
      </span>
    </li>
  )
}

export default function DashboardPage() {
  const { data, loading, error } = useAsyncData(
    getDashboardSummary,
    'Não foi possível carregar o painel.',
  )

  if (loading) {
    return <LoadingBlock label="Carregando seu painel" />
  }

  if (error) {
    return <ErrorAlert message={error} />
  }

  const summary = data
  const categoryData = summary.expensesByCategory.map((item) => ({
    label: maintenanceCategoryLabel(item.category),
    total: Number(item.total),
  }))

  if (summary.vehicleCount === 0) {
    return (
      <>
        <PageHeader title="Painel" subtitle="Sua visão geral de custos e lembretes." />
        <div className="card">
          <EmptyState
            title="Nenhum veículo cadastrado"
            description="Cadastre seu primeiro veículo para começar a registrar manutenções, abastecimentos e lembretes."
            action={
              <Link className="btn" to="/app/vehicles/new">
                Adicionar veículo
              </Link>
            }
          />
        </div>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Painel"
        subtitle="Sua visão geral de custos e lembretes."
        actions={
          <Link className="btn" to="/app/vehicles/new">
            Adicionar veículo
          </Link>
        }
      />

      <div className="grid-cards">
        <StatCard
          label="Gasto total"
          value={formatCurrency(summary.combinedTotal)}
          detail="Manutenções + combustível"
        />
        <StatCard label="Manutenções" value={formatCurrency(summary.maintenanceTotal)} />
        <StatCard label="Combustível" value={formatCurrency(summary.fuelTotal)} />
        <StatCard
          label="Veículos"
          value={summary.vehicleCount}
          detail={
            summary.overdueReminderCount > 0
              ? `${summary.overdueReminderCount} lembrete(s) atrasado(s)`
              : 'Nenhum lembrete atrasado'
          }
        />
      </div>

      <div className="two-col">
        <div className="section-stack">
          <section className="card">
            <h2 className="card-title">Gastos por mês</h2>
            {summary.monthlyExpenses.length === 0 ? (
              <EmptyState
                title="Sem lançamentos ainda"
                description="Os gastos aparecem aqui conforme você registra manutenções e abastecimentos."
              />
            ) : (
              <MonthlyExpensesChart
                data={summary.monthlyExpenses.map((month) => ({
                  month: month.month,
                  maintenance: Number(month.maintenance),
                  fuel: Number(month.fuel),
                }))}
              />
            )}
          </section>

          <section className="card">
            <h2 className="card-title">Manutenções por categoria</h2>
            {categoryData.length === 0 ? (
              <EmptyState
                title="Sem manutenções registradas"
                description="Registre uma manutenção para ver a distribuição por categoria."
              />
            ) : (
              <CategoryBarChart data={categoryData} />
            )}
          </section>
        </div>

        <div className="section-stack">
          <section className="card">
            <h2 className="card-title">Próximos lembretes</h2>
            {summary.upcomingReminders.length === 0 ? (
              <p style={{ color: 'var(--steel)', fontSize: '0.9rem' }}>
                Nada pendente no momento.
              </p>
            ) : (
              <ul className="warning-list">
                {summary.upcomingReminders.map((reminder) => (
                  <ReminderRow key={reminder.id} reminder={reminder} />
                ))}
              </ul>
            )}
          </section>

          <section className="card">
            <h2 className="card-title">Atividade recente</h2>
            {summary.recentActivity.length === 0 ? (
              <p style={{ color: 'var(--steel)', fontSize: '0.9rem' }}>
                Nenhum lançamento nos últimos meses.
              </p>
            ) : (
              <ul className="warning-list">
                {summary.recentActivity.map((item, index) => (
                  <li className="list-row" key={`${item.type}-${item.vehicleId}-${index}`}>
                    <div>
                      <div className="row-title">{item.title}</div>
                      <div className="row-sub">
                        {ACTIVITY_LABELS[item.type] ?? item.type} · {item.vehicleName} ·{' '}
                        {formatDate(item.date)}
                      </div>
                    </div>
                    <span className="money">{formatCurrency(item.amount)}</span>
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
