import { api } from './client.js'

export async function listFuelEntries(vehicleId, page = 0, size = 20) {
  const { data } = await api.get(`/vehicles/${vehicleId}/fuel-entries`, {
    params: { page, size },
  })
  return data
}

export async function getFuelEntry(entryId) {
  const { data } = await api.get(`/fuel-entries/${entryId}`)
  return data
}

export async function createFuelEntry(vehicleId, payload) {
  const { data } = await api.post(`/vehicles/${vehicleId}/fuel-entries`, payload)
  return data
}

export async function updateFuelEntry(entryId, payload) {
  const { data } = await api.put(`/fuel-entries/${entryId}`, payload)
  return data
}

export async function deleteFuelEntry(entryId) {
  await api.delete(`/fuel-entries/${entryId}`)
}
