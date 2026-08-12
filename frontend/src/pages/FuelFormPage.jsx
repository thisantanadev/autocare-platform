import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { extractErrorMessage } from '../api/client.js'
import { createFuelEntry, getFuelEntry, updateFuelEntry } from '../api/fuel.js'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import LoadingBlock from '../components/LoadingBlock.jsx'
import PageHeader from '../components/PageHeader.jsx'
import useAsyncData from '../hooks/useAsyncData.js'
import { formatCurrency } from '../utils/format.js'
import { collectErrors, requireNumber, requireText, todayIso } from '../utils/validation.js'

const EMPTY_FORM = {
  refuelDate: todayIso(),
  odometer: '',
  liters: '',
  totalCost: '',
  fullTank: true,
}

export default function FuelFormPage() {
  const { vehicleId: vehicleIdParam, entryId } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(entryId)

  const loadEntry = useCallback(
    () => (entryId ? getFuelEntry(entryId) : Promise.resolve(null)),
    [entryId],
  )
  const { data, loading, error } = useAsyncData(
    loadEntry,
    'Não foi possível carregar o abastecimento.',
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
      refuelDate: data.refuelDate ?? todayIso(),
      odometer: String(data.odometer ?? ''),
      liters: data.liters === null || data.liters === undefined ? '' : String(data.liters),
      totalCost:
        data.totalCost === null || data.totalCost === undefined ? '' : String(data.totalCost),
      fullTank: Boolean(data.fullTank),
    })
  }, [data])

  function update(field) {
    return (event) => setForm((previous) => ({ ...previous, [field]: event.target.value }))
  }

  function validate() {
    return collectErrors({
      refuelDate:
        requireText(form.refuelDate) ??
        (form.refuelDate > todayIso() ? 'A data não pode estar no futuro' : undefined),
      odometer: requireNumber(form.odometer, { min: 0, integer: true }),
      liters: requireNumber(form.liters, { min: 0.001 }),
      totalCost: requireNumber(form.totalCost, { min: 0.01 }),
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
      refuelDate: form.refuelDate,
      odometer: Number(form.odometer),
      liters: Number(form.liters),
      totalCost: Number(form.totalCost),
      fullTank: form.fullTank,
    }

    setSubmitting(true)
    setFailure('')
    try {
      if (editing) {
        await updateFuelEntry(entryId, payload)
      } else {
        await createFuelEntry(vehicleId, payload)
      }
      navigate(`/app/vehicles/${vehicleId}`, { replace: true })
    } catch (requestError) {
      setFailure(extractErrorMessage(requestError, 'Não foi possível salvar o abastecimento.'))
    } finally {
      setSubmitting(false)
    }
  }

  // Shown live so a typo in litres or total is easy to spot before saving.
  const liters = Number(form.liters)
  const totalCost = Number(form.totalCost)
  const pricePerLiter =
    liters > 0 && totalCost > 0 && !Number.isNaN(liters) && !Number.isNaN(totalCost)
      ? totalCost / liters
      : null

  if (editing && loading) {
    return <LoadingBlock label="Carregando abastecimento" />
  }

  if (editing && error) {
    return <ErrorAlert message={error} />
  }

  return (
    <>
      <PageHeader
        title={editing ? 'Editar abastecimento' : 'Registrar abastecimento'}
        subtitle="Tanques cheios permitem calcular o consumo médio real do veículo."
      />

      <ErrorAlert message={failure} />

      <form className="card" onSubmit={handleSubmit} noValidate style={{ maxWidth: '46rem' }}>
        <div className="form-grid">
          <FormField label="Data" error={errors.refuelDate}>
            {(props) => (
              <input
                {...props}
                type="date"
                max={todayIso()}
                value={form.refuelDate}
                onChange={update('refuelDate')}
              />
            )}
          </FormField>

          <FormField
            label="Odômetro (km)"
            error={errors.odometer}
            hint="Atualiza a quilometragem do veículo se for maior"
          >
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min="0"
                value={form.odometer}
                onChange={update('odometer')}
              />
            )}
          </FormField>

          <FormField label="Litros" error={errors.liters}>
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="decimal"
                min="0.001"
                step="0.001"
                value={form.liters}
                onChange={update('liters')}
              />
            )}
          </FormField>

          <FormField
            label="Valor total (R$)"
            error={errors.totalCost}
            hint={pricePerLiter ? `Equivale a ${formatCurrency(pricePerLiter)} por litro` : undefined}
          >
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="decimal"
                min="0.01"
                step="0.01"
                value={form.totalCost}
                onChange={update('totalCost')}
              />
            )}
          </FormField>
        </div>

        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={form.fullTank}
            onChange={(event) =>
              setForm((previous) => ({ ...previous, fullTank: event.target.checked }))
            }
          />
          Completei o tanque
        </label>

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
