import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import VehicleDetailPage from './VehicleDetailPage.jsx'

vi.mock('../api/vehicles.js', () => ({
  getVehicle: vi.fn(),
  getVehicleAnalytics: vi.fn(),
}))
vi.mock('../api/maintenance.js', () => ({
  listMaintenanceRecords: vi.fn(),
  deleteMaintenanceRecord: vi.fn(),
}))
vi.mock('../api/fuel.js', () => ({
  listFuelEntries: vi.fn(),
  deleteFuelEntry: vi.fn(),
}))
vi.mock('../api/reminders.js', () => ({
  listReminders: vi.fn(),
  deleteReminder: vi.fn(),
  completeReminder: vi.fn(),
  reopenReminder: vi.fn(),
}))

import { listFuelEntries } from '../api/fuel.js'
import { listMaintenanceRecords } from '../api/maintenance.js'
import { completeReminder, listReminders } from '../api/reminders.js'
import { getVehicle, getVehicleAnalytics } from '../api/vehicles.js'

const VEHICLE = {
  id: 'v1',
  brand: 'Fiat',
  model: 'Argo',
  manufacturingYear: 2021,
  modelYear: 2022,
  licensePlate: 'ABC1D23',
  currentMileage: 45000,
  fuelType: 'FLEX',
  nickname: 'Carro da família',
  displayName: 'Carro da família',
}

const ANALYTICS = {
  totals: { maintenanceCost: 1200, fuelCost: 2400, operatingCost: 3600 },
  monthlyCosts: [{ month: '2026-07', maintenanceCost: 600, fuelCost: 1200, total: 1800 }],
  costByCategory: [{ category: 'OIL_CHANGE', total: 600, percentage: 50 }],
  fuelStats: {
    totalLiters: 300,
    averagePricePerLiter: 6.2,
    averageConsumptionKmPerLiter: 11.44,
    costPerKm: 0.62,
  },
  trend: { direction: 'STABLE', currentMonthTotal: 900, previousThreeMonthAverage: 880 },
  periodComparison: {
    periodDays: 90,
    currentPeriodTotal: 2000,
    previousPeriodTotal: 1800,
    changePercentage: 11.1,
  },
  upcomingMaintenance: [
    {
      title: 'Revisão dos 50.000 km',
      nextServiceDate: '2026-09-01',
      nextServiceMileage: 50000,
      status: 'DUE_SOON',
    },
  ],
  warnings: [],
}

beforeEach(() => {
  vi.clearAllMocks()
  getVehicle.mockResolvedValue(VEHICLE)
  getVehicleAnalytics.mockResolvedValue(ANALYTICS)
  listMaintenanceRecords.mockResolvedValue({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  })
  listFuelEntries.mockResolvedValue({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  })
  listReminders.mockResolvedValue([])
})

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/app/vehicles/v1']}>
      <Routes>
        <Route path="/app/vehicles/:vehicleId" element={<VehicleDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('VehicleDetailPage', () => {
  it('shows the vehicle identity and its analytics overview', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Carro da família' })).toBeInTheDocument()
    expect(screen.getByText('ABC1D23')).toBeInTheDocument()
    expect(await screen.findByText('11,44 km/L')).toBeInTheDocument()
    expect(screen.getByText('Revisão dos 50.000 km')).toBeInTheDocument()
    expect(screen.getByText('Em breve')).toBeInTheDocument()
  })

  it('translates the analytics warning codes to pt-BR', async () => {
    getVehicleAnalytics.mockResolvedValue({
      ...ANALYTICS,
      warnings: [{ code: 'INSUFFICIENT_FUEL_DATA', message: 'Fuel efficiency needs at least 3' }],
    })
    renderPage()

    expect(
      await screen.findByText(
        'O consumo médio precisa de pelo menos 3 abastecimentos com tanque cheio.',
      ),
    ).toBeInTheDocument()
  })

  it('keeps the history usable when the analytics service is unavailable', async () => {
    const user = userEvent.setup()
    getVehicleAnalytics.mockRejectedValue({
      response: { data: { message: 'Analytics service is unavailable' } },
    })
    listMaintenanceRecords.mockResolvedValue({
      content: [
        {
          id: 'm1',
          vehicleId: 'v1',
          category: 'OIL_CHANGE',
          // Deliberately distinct from the "Troca de óleo" category label.
          title: 'Troca de óleo e filtro',
          serviceDate: '2026-07-15',
          mileageAtService: 44000,
          cost: 320,
          workshop: 'Oficina do Zé',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    })
    renderPage()

    expect(await screen.findByText(/Analytics service is unavailable/)).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'Manutenções' }))

    expect(await screen.findByText('Troca de óleo e filtro')).toBeInTheDocument()
    expect(screen.getByText('Oficina do Zé')).toBeInTheDocument()
    // The category badge still renders its translated label alongside the title.
    expect(screen.getByText('Troca de óleo')).toBeInTheDocument()
  })

  it('loads fuel entries only once its tab is opened', async () => {
    const user = userEvent.setup()
    renderPage()

    await screen.findByRole('heading', { name: 'Carro da família' })
    expect(listFuelEntries).not.toHaveBeenCalled()

    await user.click(screen.getByRole('tab', { name: 'Abastecimentos' }))

    expect(await screen.findByText('Nenhum abastecimento registrado')).toBeInTheDocument()
    expect(listFuelEntries).toHaveBeenCalledWith('v1', 0, 10)
  })

  it('completes a reminder and reloads the list', async () => {
    const user = userEvent.setup()
    listReminders.mockResolvedValue([
      {
        id: 'r1',
        vehicleId: 'v1',
        title: 'Renovar seguro',
        dueDate: '2026-09-01',
        dueMileage: null,
        status: 'ACTIVE',
        overdue: false,
      },
    ])
    completeReminder.mockResolvedValue({ id: 'r1', status: 'COMPLETED' })
    renderPage()

    await user.click(await screen.findByRole('tab', { name: 'Lembretes' }))
    await user.click(await screen.findByRole('button', { name: 'Concluir' }))

    expect(completeReminder).toHaveBeenCalledWith('r1')
    expect(listReminders).toHaveBeenCalledTimes(2)
  })
})
