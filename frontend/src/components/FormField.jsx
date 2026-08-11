import { useId } from 'react'

/**
 * Labeled form control with inline validation. `children` receives the
 * generated id/aria props so the label is always associated correctly.
 */
export default function FormField({ label, error, hint, children }) {
  const id = useId()
  const errorId = `${id}-error`

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children({
        id,
        'aria-invalid': error ? 'true' : undefined,
        'aria-describedby': error ? errorId : undefined,
      })}
      {hint && !error && <span className="hint">{hint}</span>}
      {error && (
        <span className="field-error" id={errorId}>
          {error}
        </span>
      )}
    </div>
  )
}
