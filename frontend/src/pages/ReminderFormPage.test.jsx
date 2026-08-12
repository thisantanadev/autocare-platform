import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import ReminderFormPage from './ReminderFormPage.jsx'

vi.mock('../api/reminders.js', () => ({
  getReminder: vi.fn(),
  createReminder: vi.fn(),
  updateReminder: vi.fn(),
}))

import { createReminder } from '../api/reminders.js'

beforeEach(() => vi.clearAllMocks())

function renderForm() {
  return render(
    <MemoryRouter initialEntries={['/app/vehicles/v1/reminders/new']}>
      <Routes>
        <Route path="/app/vehicles/:vehicleId/reminders/new" element={<ReminderFormPage />} />
        <Route path="/app/vehicles/:vehicleId" element={<h1>Detalhe</h1>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ReminderFormPage', () => {
  it('requires a due date or a due mileage, mirroring ReminderService', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.type(screen.getByLabelText('Título'), 'Renovar seguro')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(
      await screen.findAllByText('Informe uma data, uma quilometragem, ou as duas'),
    ).toHaveLength(2)
    expect(createReminder).not.toHaveBeenCalled()
  })

  it('accepts a mileage-only reminder', async () => {
    const user = userEvent.setup()
    createReminder.mockResolvedValue({ id: 'r1' })
    renderForm()

    await user.type(screen.getByLabelText('Título'), 'Trocar correia dentada')
    await user.type(screen.getByLabelText('Vence em (km)'), '60000')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(createReminder).toHaveBeenCalledWith('v1', {
      title: 'Trocar correia dentada',
      description: null,
      dueDate: null,
      dueMileage: 60000,
    })
  })

  it('accepts a date-only reminder', async () => {
    const user = userEvent.setup()
    createReminder.mockResolvedValue({ id: 'r1' })
    renderForm()

    await user.type(screen.getByLabelText('Título'), 'Licenciamento')
    await user.type(screen.getByLabelText('Vence em (data)'), '2026-11-30')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(createReminder).toHaveBeenCalledWith('v1', {
      title: 'Licenciamento',
      description: null,
      dueDate: '2026-11-30',
      dueMileage: null,
    })
  })
})
