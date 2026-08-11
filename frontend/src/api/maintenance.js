import { api } from './client.js'

export async function listMaintenanceRecords(vehicleId, page = 0, size = 20) {
  const { data } = await api.get(`/vehicles/${vehicleId}/maintenance-records`, {
    params: { page, size },
  })
  return data
}

export async function getMaintenanceRecord(recordId) {
  const { data } = await api.get(`/maintenance-records/${recordId}`)
  return data
}

export async function createMaintenanceRecord(vehicleId, payload) {
  const { data } = await api.post(`/vehicles/${vehicleId}/maintenance-records`, payload)
  return data
}

export async function updateMaintenanceRecord(recordId, payload) {
  const { data } = await api.put(`/maintenance-records/${recordId}`, payload)
  return data
}

export async function deleteMaintenanceRecord(recordId) {
  await api.delete(`/maintenance-records/${recordId}`)
}
