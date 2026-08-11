import { useEffect, useRef } from 'react'

/**
 * Confirmation step before destructive actions. Focus moves to the cancel
 * button on open so a stray Enter never confirms a deletion.
 */
export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Excluir',
  busy = false,
  onConfirm,
  onCancel,
}) {
  const cancelRef = useRef(null)

  useEffect(() => {
    if (open) {
      cancelRef.current?.focus()
    }
  }, [open])

  useEffect(() => {
    if (!open) {
      return undefined
    }
    function onKeyDown(event) {
      if (event.key === 'Escape') {
        onCancel()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open, onCancel])

  if (!open) {
    return null
  }

  return (
    <div className="dialog-backdrop" onClick={onCancel}>
      <div
        className="dialog"
        role="alertdialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <h3>{title}</h3>
        <p>{description}</p>
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" ref={cancelRef} onClick={onCancel}>
            Cancelar
          </button>
          <button type="button" className="btn btn-danger" disabled={busy} onClick={onConfirm}>
            {busy ? 'Excluindo…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
