import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import ConfirmDialog from './ConfirmDialog.jsx'

function setup(overrides = {}) {
  const props = {
    open: true,
    title: 'Excluir veículo?',
    description: 'Esta ação não pode ser desfeita.',
    onConfirm: vi.fn(),
    onCancel: vi.fn(),
    ...overrides,
  }
  render(<ConfirmDialog {...props} />)
  return props
}

describe('ConfirmDialog', () => {
  it('renders nothing while closed', () => {
    setup({ open: false })
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('focuses cancel on open so a stray Enter never confirms a deletion', async () => {
    setup()
    expect(await screen.findByRole('button', { name: 'Cancelar' })).toHaveFocus()
  })

  it('cancels on Escape', async () => {
    const user = userEvent.setup()
    const props = setup()

    await user.keyboard('{Escape}')

    expect(props.onCancel).toHaveBeenCalled()
    expect(props.onConfirm).not.toHaveBeenCalled()
  })

  it('confirms only on the explicit destructive action', async () => {
    const user = userEvent.setup()
    const props = setup()

    await user.click(screen.getByRole('button', { name: 'Excluir' }))

    expect(props.onConfirm).toHaveBeenCalled()
  })

  it('disables the confirm button and shows progress while deleting', () => {
    setup({ busy: true })
    expect(screen.getByRole('button', { name: 'Excluindo…' })).toBeDisabled()
  })
})
