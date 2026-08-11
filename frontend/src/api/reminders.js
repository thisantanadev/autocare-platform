import { api } from './client.js'

export async function listReminders(vehicleId) {
  const { data } = await api.get(`/vehicles/${vehicleId}/reminders`)
  return data
}

export async function getReminder(reminderId) {
  const { data } = await api.get(`/reminders/${reminderId}`)
  return data
}

export async function createReminder(vehicleId, payload) {
  const { data } = await api.post(`/vehicles/${vehicleId}/reminders`, payload)
  return data
}

export async function updateReminder(reminderId, payload) {
  const { data } = await api.put(`/reminders/${reminderId}`, payload)
  return data
}

export async function completeReminder(reminderId) {
  const { data } = await api.post(`/reminders/${reminderId}/complete`)
  return data
}

export async function reopenReminder(reminderId) {
  const { data } = await api.post(`/reminders/${reminderId}/reopen`)
  return data
}

export async function deleteReminder(reminderId) {
  await api.delete(`/reminders/${reminderId}`)
}
