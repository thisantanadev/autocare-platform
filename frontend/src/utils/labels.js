export const FUEL_TYPE_LABELS = {
  GASOLINE: 'Gasolina',
  ETHANOL: 'Etanol',
  FLEX: 'Flex',
  DIESEL: 'Diesel',
  HYBRID: 'Híbrido',
  ELECTRIC: 'Elétrico',
}

export const MAINTENANCE_CATEGORY_LABELS = {
  OIL_CHANGE: 'Troca de óleo',
  FILTERS: 'Filtros',
  BRAKES: 'Freios',
  TIRES: 'Pneus',
  ENGINE: 'Motor',
  TRANSMISSION: 'Transmissão',
  SUSPENSION: 'Suspensão',
  ELECTRICAL: 'Elétrica',
  BATTERY: 'Bateria',
  COOLING: 'Arrefecimento',
  INSPECTION: 'Revisão',
  BODYWORK: 'Funilaria',
  OTHER: 'Outros',
}

export const UPCOMING_STATUS_LABELS = {
  OVERDUE: 'Atrasada',
  DUE_SOON: 'Em breve',
  SCHEDULED: 'Agendada',
}

export const TREND_LABELS = {
  UP: 'Em alta',
  DOWN: 'Em queda',
  STABLE: 'Estável',
}

export function fuelTypeLabel(value) {
  return FUEL_TYPE_LABELS[value] ?? value
}

export function maintenanceCategoryLabel(value) {
  return MAINTENANCE_CATEGORY_LABELS[value] ?? value
}
