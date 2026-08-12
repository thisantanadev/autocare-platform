import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

import VehiclesPage from './VehiclesPage.jsx'

vi.mock('../api/vehicles.js', () => ({ listVehicles: vi.fn() }))

import { listVehicles } from '../api/vehicles.js'

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

beforeEach(() => vi.clearAllMocks())

function renderPage() {
  return render(
    <MemoryRouter>
      <VehiclesPage />
    </MemoryRouter>,
  )
}

describe('VehiclesPage', () => {
  it('invites the first registration when the garage is empty', async () => {
    listVehicles.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Nenhum veículo cadastrado')).toBeInTheDocument()
  })

  it('shows the plate, mileage and fuel type of each vehicle', async () => {
    listVehicles.mockResolvedValue([VEHICLE])
    renderPage()

    expect(await screen.findByText('Carro da família')).toBeInTheDocument()
    expect(screen.getByText('ABC1D23')).toBeInTheDocument()
    expect(screen.getByText('45.000 km')).toBeInTheDocument()
    expect(screen.getByText('Flex')).toBeInTheDocument()
    // The whole card is the link into the vehicle history.
    expect(screen.getByRole('link', { name: /Carro da família/ })).toHaveAttribute(
      'href',
      '/app/vehicles/v1',
    )
  })

  it('marks vehicles registered without a plate', async () => {
    listVehicles.mockResolvedValue([{ ...VEHICLE, licensePlate: null }])
    renderPage()

    expect(await screen.findByText('Sem placa')).toBeInTheDocument()
  })

  it('reports a failed load instead of rendering an empty garage', async () => {
    listVehicles.mockRejectedValue({ response: { data: { message: 'Sessão expirada' } } })
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Sessão expirada')
    expect(screen.queryByText('Nenhum veículo cadastrado')).not.toBeInTheDocument()
  })
})
