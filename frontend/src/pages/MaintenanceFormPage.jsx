import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import {
  createMaintenanceRecord,
  getMaintenanceRecord,
  updateMaintenanceRecord,
} from '../api/maintenance.js'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { MAINTENANCE_CATEGORY_LABELS } from '../utils/labels.js'
import { collectErrors, requireNumber, requireText, todayIso } from '../utils/validation.js'

const EMPTY_FORM = {
  category: 'OIL_CHANGE',
  title: '',
  description: '',
  serviceDate: todayIso(),
  mileageAtService: '',
  cost: '',
  workshop: '',
  nextServiceDate: '',
  nextServiceMileage: '',
}

export default function MaintenanceFormPage() {
  const { vehicleId: vehicleIdParam, recordId } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(recordId)

  const loadRecord = useCallback(
    () => (recordId ? getMaintenanceRecord(recordId) : Promise.resolve(null)),
    [recordId],
  )
  const { data, loading, error } = useAsyncData(
    loadRecord,
    'Não foi possível carregar a manutenção.',
  )

  // On edit the vehicle is only known once the record has loaded.
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
      category: data.category ?? 'OIL_CHANGE',
      title: data.title ?? '',
      description: data.description ?? '',
      serviceDate: data.serviceDate ?? todayIso(),
      mileageAtService: String(data.mileageAtService ?? ''),
      cost: data.cost === null || data.cost === undefined ? '' : String(data.cost),
      workshop: data.workshop ?? '',
      nextServiceDate: data.nextServiceDate ?? '',
      nextServiceMileage: data.nextServiceMileage ? String(data.nextServiceMileage) : '',
    })
  }, [data])

  function update(field) {
    return (event) => setForm((previous) => ({ ...previous, [field]: event.target.value }))
  }

  function validate() {
    const mileage = Number(form.mileageAtService)
    return collectErrors({
      title: requireText(form.title, { max: 120 }),
      description:
        form.description.trim().length > 2000 ? 'Use no máximo 2000 caracteres' : undefined,
      serviceDate:
        requireText(form.serviceDate) ??
        (form.serviceDate > todayIso() ? 'A data não pode estar no futuro' : undefined),
      mileageAtService: requireNumber(form.mileageAtService, { min: 0, integer: true }),
      cost: requireNumber(form.cost, { min: 0 }),
      workshop: form.workshop.trim().length > 120 ? 'Use no máximo 120 caracteres' : undefined,
      // Both rules below are enforced again by MaintenanceRecordService.
      nextServiceDate:
        form.nextServiceDate && form.serviceDate && form.nextServiceDate <= form.serviceDate
          ? 'Deve ser depois da data do serviço'
          : undefined,
      nextServiceMileage:
        requireNumber(form.nextServiceMileage, { required: false, min: 0, integer: true }) ??
        (form.nextServiceMileage !== '' &&
        Number.isFinite(mileage) &&
        Number(form.nextServiceMileage) <= mileage
          ? 'Deve ser maior que a quilometragem do serviço'
          : undefined),
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
      category: form.category,
      title: form.title.trim(),
      description: form.description.trim() || null,
      serviceDate: form.serviceDate,
      mileageAtService: Number(form.mileageAtService),
      cost: Number(form.cost),
      workshop: form.workshop.trim() || null,
      nextServiceDate: form.nextServiceDate || null,
      nextServiceMileage: form.nextServiceMileage === '' ? null : Number(form.nextServiceMileage),
    }

    setSubmitting(true)
    setFailure('')
    try {
      if (editing) {
        await updateMaintenanceRecord(recordId, payload)
      } else {
        await createMaintenanceRecord(vehicleId, payload)
      }
      navigate(`/app/vehicles/${vehicleId}`, { replace: true })
    } catch (requestError) {
      setFailure(extractErrorMessage(requestError, 'Não foi possível salvar a manutenção.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (editing && loading) {
    return <LoadingBlock label="Carregando manutenção" />
  }

  if (editing && error) {
    return <ErrorAlert message={error} />
  }

  return (
    <>
      <PageHeader
        title={editing ? 'Editar manutenção' : 'Registrar manutenção'}
        subtitle="Serviços realizados, custo e a próxima revisão prevista."
      />

      <ErrorAlert message={failure} />

      <form className="card" onSubmit={handleSubmit} noValidate style={{ maxWidth: '46rem' }}>
        <div className="form-grid">
          <FormField label="Categoria" error={errors.category}>
            {(props) => (
              <select {...props} value={form.category} onChange={update('category')}>
                {Object.entries(MAINTENANCE_CATEGORY_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            )}
          </FormField>

          <FormField label="Descrição curta" error={errors.title}>
            {(props) => (
              <input
                {...props}
                type="text"
                placeholder="Ex.: Troca de óleo e filtro"
                value={form.title}
                onChange={update('title')}
              />
            )}
          </FormField>

          <FormField label="Data do serviço" error={errors.serviceDate}>
            {(props) => (
              <input
                {...props}
                type="date"
                max={todayIso()}
                value={form.serviceDate}
                onChange={update('serviceDate')}
              />
            )}
          </FormField>

          <FormField
            label="Quilometragem no serviço"
            error={errors.mileageAtService}
            hint="Atualiza a quilometragem do veículo se for maior"
          >
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="0"
                value={form.mileageAtService}
                onChange={update('mileageAtService')}
              />
            )}
          </FormField>

          <FormField label="Custo (R$)" error={errors.cost} hint="Use 0 para serviço em garantia">
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="decimal"
                min="0"
                step="0.01"
                value={form.cost}
                onChange={update('cost')}
              />
            )}
          </FormField>

          <FormField label="Oficina" error={errors.workshop} hint="Opcional">
            {(props) => (
              <input {...props} type="text" value={form.workshop} onChange={update('workshop')} />
            )}
          </FormField>

          <FormField label="Próximo serviço (data)" error={errors.nextServiceDate} hint="Opcional">
            {(props) => (
              <input
                {...props}
                type="date"
                value={form.nextServiceDate}
                onChange={update('nextServiceDate')}
              />
            )}
          </FormField>

          <FormField label="Próximo serviço (km)" error={errors.nextServiceMileage} hint="Opcional">
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="0"
                value={form.nextServiceMileage}
                onChange={update('nextServiceMileage')}
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
