import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

import DashboardPage from './DashboardPage.jsx'

vi.mock('../api/dashboard.js', () => ({ getDashboardSummary: vi.fn() }))

import { getDashboardSummary } from '../api/dashboard.js'

const SUMMARY = {
  vehicleCount: 2,
  maintenanceTotal: 1200,
  fuelTotal: 2400,
  combinedTotal: 3600,
  monthlyExpenses: [
    { month: '2026-07', maintenance: 600, fuel: 1200, total: 1800 },
    { month: '2026-08', maintenance: 600, fuel: 1200, total: 1800 },
  ],
  expensesByCategory: [{ category: 'OIL_CHANGE', total: 600 }],
  overdueReminderCount: 1,
  upcomingReminders: [
    {
      id: 'r1',
      vehicleId: 'v1',
      vehicleName: 'Carro da família',
      title: 'Renovar seguro',
      dueDate: '2026-09-01',
      dueMileage: null,
      overdue: true,
    },
  ],
  recentActivity: [
    {
      type: 'FUEL',
      vehicleId: 'v1',
      vehicleName: 'Carro da família',
      title: 'Abastecimento',
      date: '2026-08-10',
      amount: 280,
    },
  ],
}

beforeEach(() => vi.clearAllMocks())

function renderPage() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  it('summarizes spend, overdue reminders and recent activity', async () => {
    getDashboardSummary.mockResolvedValue(SUMMARY)
    renderPage()

    expect(await screen.findByText('Gasto total')).toBeInTheDocument()
    expect(screen.getByText('1 lembrete(s) atrasado(s)')).toBeInTheDocument()
    expect(screen.getByText('Renovar seguro')).toBeInTheDocument()
    expect(screen.getByText('Atrasado')).toBeInTheDocument()
    // Activity rows translate the backend's type discriminator.
    expect(screen.getByText(/Abastecimento · Carro da família/)).toBeInTheDocument()
  })

  it('translates maintenance categories in the category chart', async () => {
    getDashboardSummary.mockResolvedValue(SUMMARY)
    renderPage()

    expect(await screen.findByText('Manutenções por categoria')).toBeInTheDocument()
    expect(screen.queryByText('OIL_CHANGE')).not.toBeInTheDocument()
  })

  it('onboards a user with no vehicles instead of showing empty charts', async () => {
    getDashboardSummary.mockResolvedValue({
      ...SUMMARY,
      vehicleCount: 0,
      monthlyExpenses: [],
      expensesByCategory: [],
      upcomingReminders: [],
      recentActivity: [],
    })
    renderPage()

    expect(await screen.findByText('Nenhum veículo cadastrado')).toBeInTheDocument()
    expect(screen.queryByText('Gastos por mês')).not.toBeInTheDocument()
  })

  it('reports a failed load', async () => {
    getDashboardSummary.mockRejectedValue({ response: { data: { message: 'Falha no servidor' } } })
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Falha no servidor')
  })
})
