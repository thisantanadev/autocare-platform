import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import { createVehicle, deleteVehicle, getVehicle, updateVehicle } from '../api/vehicles.js'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { FUEL_TYPE_LABELS } from '../utils/labels.js'
import { collectErrors, normalizePlate, requireNumber, requireText, validatePlate } from '../utils/validation.js'

const EMPTY_FORM = {
  brand: '',
  model: '',
  manufacturingYear: '',
  modelYear: '',
  licensePlate: '',
  currentMileage: '',
  fuelType: 'FLEX',
  nickname: '',
}

// The backend accepts next year's models, which are sold before the year turns.
const MAX_YEAR = new Date().getFullYear() + 1

export default function VehicleFormPage() {
  const { vehicleId } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(vehicleId)

  const loadVehicle = useCallback(
    () => (vehicleId ? getVehicle(vehicleId) : Promise.resolve(null)),
    [vehicleId],
  )
  const { data, loading, error } = useAsyncData(
    loadVehicle,
    'Não foi possível carregar o veículo.',
  )

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    if (!data) {
      return
    }
    setForm({
      brand: data.brand ?? '',
      model: data.model ?? '',
      manufacturingYear: String(data.manufacturingYear ?? ''),
      modelYear: data.modelYear ? String(data.modelYear) : '',
      licensePlate: data.licensePlate ?? '',
      currentMileage: String(data.currentMileage ?? ''),
      fuelType: data.fuelType ?? 'FLEX',
      nickname: data.nickname ?? '',
    })
  }, [data])

  function update(field) {
    return (event) => setForm((previous) => ({ ...previous, [field]: event.target.value }))
  }

  function validate() {
    const manufacturingYear = Number(form.manufacturingYear)
    return collectErrors({
      brand: requireText(form.brand, { max: 60 }),
      model: requireText(form.model, { max: 80 }),
      manufacturingYear: requireNumber(form.manufacturingYear, {
        min: 1900,
        max: MAX_YEAR,
        integer: true,
      }),
      modelYear:
        requireNumber(form.modelYear, {
          required: false,
          min: 1900,
          max: MAX_YEAR + 1,
          integer: true,
        }) ??
        (form.modelYear !== '' &&
        Number.isFinite(manufacturingYear) &&
        Number(form.modelYear) < manufacturingYear
          ? 'Não pode ser anterior ao ano de fabricação'
          : undefined),
      licensePlate: validatePlate(form.licensePlate),
      currentMileage: requireNumber(form.currentMileage, { min: 0, integer: true }),
      nickname: form.nickname.trim().length > 60 ? 'Use no máximo 60 caracteres' : undefined,
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
      brand: form.brand.trim(),
      model: form.model.trim(),
      manufacturingYear: Number(form.manufacturingYear),
      modelYear: form.modelYear === '' ? null : Number(form.modelYear),
      licensePlate: normalizePlate(form.licensePlate) || null,
      currentMileage: Number(form.currentMileage),
      fuelType: form.fuelType,
      nickname: form.nickname.trim() || null,
    }

    setSubmitting(true)
    setFailure('')
    try {
      const saved = editing
        ? await updateVehicle(vehicleId, payload)
        : await createVehicle(payload)
      navigate(`/app/vehicles/${saved.id}`, { replace: true })
    } catch (requestError) {
      setFailure(extractErrorMessage(requestError, 'Não foi possível salvar o veículo.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    setDeleting(true)
    setFailure('')
    try {
      await deleteVehicle(vehicleId)
      navigate('/app/vehicles', { replace: true })
    } catch (requestError) {
      setConfirmingDelete(false)
      setFailure(extractErrorMessage(requestError, 'Não foi possível excluir o veículo.'))
    } finally {
      setDeleting(false)
    }
  }

  if (editing && loading) {
    return <LoadingBlock label="Carregando veículo" />
  }

  if (editing && error) {
    return <ErrorAlert message={error} />
  }

  return (
    <>
      <PageHeader
        title={editing ? 'Editar veículo' : 'Adicionar veículo'}
        subtitle={
          editing
            ? 'Atualize os dados cadastrais e a quilometragem atual.'
            : 'Informe os dados do veículo para começar a registrar o histórico.'
        }
        actions={
          editing && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setConfirmingDelete(true)}
            >
              Excluir
            </button>
          )
        }
      />

      <ErrorAlert message={failure} />

      <form className="card" onSubmit={handleSubmit} noValidate style={{ maxWidth: '46rem' }}>
        <div className="form-grid">
          <FormField label="Marca" error={errors.brand}>
            {(props) => <input {...props} type="text" value={form.brand} onChange={update('brand')} />}
          </FormField>

          <FormField label="Modelo" error={errors.model}>
            {(props) => <input {...props} type="text" value={form.model} onChange={update('model')} />}
          </FormField>

          <FormField label="Ano de fabricação" error={errors.manufacturingYear}>
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="1900"
                max={MAX_YEAR}
                value={form.manufacturingYear}
                onChange={update('manufacturingYear')}
              />
            )}
          </FormField>

          <FormField
            label="Ano do modelo"
            error={errors.modelYear}
            hint="Opcional, quando diferente do ano de fabricação"
          >
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="1900"
                value={form.modelYear}
                onChange={update('modelYear')}
              />
            )}
          </FormField>

          <FormField label="Placa" error={errors.licensePlate} hint="Opcional, formato ABC1D23">
            {(props) => (
              <input
                {...props}
                type="text"
                autoCapitalize="characters"
                maxLength={10}
                value={form.licensePlate}
                onChange={update('licensePlate')}
              />
            )}
          </FormField>

          <FormField
            label="Quilometragem atual"
            error={errors.currentMileage}
            hint="Não pode ser menor que a maior leitura já registrada"
          >
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="0"
                value={form.currentMileage}
                onChange={update('currentMileage')}
              />
            )}
          </FormField>

          <FormField label="Combustível" error={errors.fuelType}>
            {(props) => (
              <select {...props} value={form.fuelType} onChange={update('fuelType')}>
                {Object.entries(FUEL_TYPE_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            )}
          </FormField>

          <FormField label="Apelido" error={errors.nickname} hint="Opcional, ex.: Carro da família">
            {(props) => (
              <input {...props} type="text" value={form.nickname} onChange={update('nickname')} />
            )}
          </FormField>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate(editing ? `/app/vehicles/${vehicleId}` : '/app/vehicles')}
          >
            Cancelar
          </button>
          <button className="btn" type="submit" disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>

      <ConfirmDialog
        open={confirmingDelete}
        title="Excluir veículo?"
        description="Todo o histórico de manutenções, abastecimentos e lembretes deste veículo será excluído. Esta ação não pode ser desfeita."
        busy={deleting}
        onConfirm={handleDelete}
        onCancel={() => setConfirmingDelete(false)}
      />
    </>
  )
}
