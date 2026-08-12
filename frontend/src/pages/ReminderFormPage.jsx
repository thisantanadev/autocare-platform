import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import { createReminder, getReminder, updateReminder } from '../api/reminders.js'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { collectErrors, requireNumber, requireText } from '../utils/validation.js'

const EMPTY_FORM = { title: '', description: '', dueDate: '', dueMileage: '' }

const MISSING_TARGET_MESSAGE = 'Informe uma data, uma quilometragem, ou as duas'

export default function ReminderFormPage() {
  const { vehicleId: vehicleIdParam, reminderId } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(reminderId)

  const loadReminder = useCallback(
    () => (reminderId ? getReminder(reminderId) : Promise.resolve(null)),
    [reminderId],
  )
  const { data, loading, error } = useAsyncData(
    loadReminder,
    'Não foi possível carregar o lembrete.',
  )

  const vehicleId = vehicleIdParam ?? data?.vehicleId
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!data) {
      return
    }
    setForm({
      title: data.title ?? '',
      description: data.description ?? '',
      dueDate: data.dueDate ?? '',
      dueMileage: data.dueMileage ? String(data.dueMileage) : '',
    })
  }, [data])

  function update(field) {
    return (event) => setForm((previous) => ({ ...previous, [field]: event.target.value }))
  }

  function validate() {
    // ReminderService rejects a reminder with neither target, so flag it here too.
    const missingTarget = !form.dueDate && form.dueMileage === ''
    return collectErrors({
      title: requireText(form.title, { max: 120 }),
      description:
        form.description.trim().length > 2000 ? 'Use no máximo 2000 caracteres' : undefined,
      dueDate: missingTarget ? MISSING_TARGET_MESSAGE : undefined,
      dueMileage: missingTarget
        ? MISSING_TARGET_MESSAGE
        : requireNumber(form.dueMileage, { required: false, min: 0, integer: true }),
    })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const nextErrors = validate()
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      dueDate: form.dueDate || null,
      dueMileage: form.dueMileage === '' ? null : Number(form.dueMileage),
    }

    setSubmitting(true)
    setFailure('')
    try {
      if (editing) {
        await updateReminder(reminderId, payload)
      } else {
        await createReminder(vehicleId, payload)
      }
      navigate(`/app/vehicles/${vehicleId}`, { replace: true })
    } catch (requestError) {
      setFailure(extractErrorMessage(requestError, 'Não foi possível salvar o lembrete.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (editing && loading) {
    return <LoadingBlock label="Carregando lembrete" />
  }

  if (editing && error) {
    return <ErrorAlert message={error} />
  }

  return (
    <>
      <PageHeader
        title={editing ? 'Editar lembrete' : 'Novo lembrete'}
        subtitle="O lembrete fica atrasado quando a data passa ou a quilometragem é atingida."
      />

      <ErrorAlert message={failure} />

      <form className="card" onSubmit={handleSubmit} noValidate style={{ maxWidth: '46rem' }}>
        <FormField label="Título" error={errors.title}>
          {(props) => (
            <input
              {...props}
              type="text"
              placeholder="Ex.: Renovar seguro"
              value={form.title}
              onChange={update('title')}
            />
          )}
        </FormField>

        <div className="form-grid">
          <FormField label="Vence em (data)" error={errors.dueDate} hint="Opcional">
            {(props) => (
              <input {...props} type="date" value={form.dueDate} onChange={update('dueDate')} />
            )}
          </FormField>

          <FormField label="Vence em (km)" error={errors.dueMileage} hint="Opcional">
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="0"
                value={form.dueMileage}
                onChange={update('dueMileage')}
              />
            )}
          </FormField>
        </div>

        <FormField label="Observações" error={errors.description} hint="Opcional">
          {(props) => (
            <textarea
              {...props}
              rows={4}
              value={form.description}
              onChange={update('description')}
            />
          )}
        </FormField>

        <div className="form-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate(`/app/vehicles/${vehicleId}`)}
          >
            Cancelar
          </button>
          <button className="btn" type="submit" disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </>
  )
}
