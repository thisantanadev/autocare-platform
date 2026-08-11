import { api } from './client.js'

export async function getDashboardSummary() {
  const { data } = await api.get('/dashboard')
  return data
}
