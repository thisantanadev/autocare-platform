import { api } from './client.js'

export async function listVehicles() {
  const { data } = await api.get('/vehicles')
  return data
}

export async function getVehicle(vehicleId) {
  const { data } = await api.get(`/vehicles/${vehicleId}`)
  return data
}

export async function createVehicle(payload) {
  const { data } = await api.post('/vehicles', payload)
  return data
}

export async function updateVehicle(vehicleId, payload) {
  const { data } = await api.put(`/vehicles/${vehicleId}`, payload)
  return data
}

export async function deleteVehicle(vehicleId) {
  await api.delete(`/vehicles/${vehicleId}`)
}

export async function getVehicleAnalytics(vehicleId) {
  const { data } = await api.get(`/vehicles/${vehicleId}/analytics`)
  return data
}
