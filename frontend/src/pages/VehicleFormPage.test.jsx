import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import VehicleFormPage from './VehicleFormPage.jsx'

vi.mock('../api/vehicles.js', () => ({
  getVehicle: vi.fn(),
  createVehicle: vi.fn(),
  updateVehicle: vi.fn(),
  deleteVehicle: vi.fn(),
}))

import { createVehicle, getVehicle, updateVehicle } from '../api/vehicles.js'

beforeEach(() => vi.clearAllMocks())

function renderForm(route = '/app/vehicles/new') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <Routes>
        <Route path="/app/vehicles/new" element={<VehicleFormPage />} />
        <Route path="/app/vehicles/:vehicleId/edit" element={<VehicleFormPage />} />
        <Route path="/app/vehicles/:vehicleId" element={<h1>Detalhe</h1>} />
      </Routes>
    </MemoryRouter>,
  )
}

async function fillRequiredFields(user) {
  await user.type(screen.getByLabelText('Marca'), 'Fiat')
  await user.type(screen.getByLabelText('Modelo'), 'Argo')
  await user.type(screen.getByLabelText('Ano de fabricação'), '2021')
  await user.type(screen.getByLabelText('Quilometragem atual'), '45000')
}

describe('VehicleFormPage — creating', () => {
  it('rejects a plate that does not match the Brazilian format', async () => {
    const user = userEvent.setup()
    renderForm()

    await fillRequiredFields(user)
    await user.type(screen.getByLabelText('Placa'), 'AB123')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(
      await screen.findByText('Use o formato brasileiro, como ABC1D23'),
    ).toBeInTheDocument()
    expect(createVehicle).not.toHaveBeenCalled()
  })

  it('normalizes the plate and sends optional fields as null', async () => {
    const user = userEvent.setup()
    createVehicle.mockResolvedValue({ id: 'v1' })
    renderForm()

    await fillRequiredFields(user)
    await user.type(screen.getByLabelText('Placa'), 'abc-1d23')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(createVehicle).toHaveBeenCalledWith({
      brand: 'Fiat',
      model: 'Argo',
      manufacturingYear: 2021,
      modelYear: null,
      licensePlate: 'ABC1D23',
      currentMileage: 45000,
      fuelType: 'FLEX',
      nickname: null,
    })
    expect(await screen.findByRole('heading', { name: 'Detalhe' })).toBeInTheDocument()
  })

  it('refuses a model year earlier than the manufacturing year', async () => {
    const user = userEvent.setup()
    renderForm()

    await fillRequiredFields(user)
    await user.type(screen.getByLabelText('Ano do modelo'), '2020')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(
      await screen.findByText('Não pode ser anterior ao ano de fabricação'),
    ).toBeInTheDocument()
    expect(createVehicle).not.toHaveBeenCalled()
  })

  it('reports the business rule raised by the backend', async () => {
    const user = userEvent.setup()
    createVehicle.mockRejectedValue({
      response: { data: { message: 'A vehicle with this license plate is already registered' } },
    })
    renderForm()

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'A vehicle with this license plate is already registered',
    )
  })
})

describe('VehicleFormPage — editing', () => {
  it('loads the vehicle into the form and updates it', async () => {
    const user = userEvent.setup()
    getVehicle.mockResolvedValue({
      id: 'v1',
      brand: 'Fiat',
      model: 'Argo',
      manufacturingYear: 2021,
      modelYear: 2022,
      licensePlate: 'ABC1D23',
      currentMileage: 45000,
      fuelType: 'FLEX',
      nickname: 'Carro da família',
    })
    updateVehicle.mockResolvedValue({ id: 'v1' })
    renderForm('/app/vehicles/v1/edit')

    expect(await screen.findByDisplayValue('Argo')).toBeInTheDocument()
    expect(screen.getByLabelText('Placa')).toHaveValue('ABC1D23')

    const mileage = screen.getByLabelText('Quilometragem atual')
    await user.clear(mileage)
    await user.type(mileage, '46200')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(updateVehicle).toHaveBeenCalledWith(
      'v1',
      expect.objectContaining({ currentMileage: 46200, nickname: 'Carro da família' }),
    )
  })

  it('asks for confirmation before deleting', async () => {
    const user = userEvent.setup()
    getVehicle.mockResolvedValue({
      id: 'v1',
      brand: 'Fiat',
      model: 'Argo',
      manufacturingYear: 2021,
      modelYear: null,
      licensePlate: null,
      currentMileage: 45000,
      fuelType: 'FLEX',
      nickname: null,
    })
    renderForm('/app/vehicles/v1/edit')

    await user.click(await screen.findByRole('button', { name: 'Excluir' }))

    expect(await screen.findByRole('alertdialog')).toHaveAccessibleName('Excluir veículo?')
  })
})
