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

// The analytics service explains missing metrics in English; these are the
// pt-BR equivalents keyed by the stable warning code it returns.
export const ANALYTICS_WARNING_MESSAGES = {
  NO_DATA: 'Nenhuma manutenção ou abastecimento registrado até agora.',
  INSUFFICIENT_FUEL_DATA:
    'O consumo médio precisa de pelo menos 3 abastecimentos com tanque cheio.',
  INSUFFICIENT_DISTANCE_DATA:
    'O custo por quilômetro precisa de leituras de odômetro cobrindo uma distância maior.',
}

export function analyticsWarningMessage(warning) {
  return ANALYTICS_WARNING_MESSAGES[warning.code] ?? warning.message
}

export function fuelTypeLabel(value) {
  return FUEL_TYPE_LABELS[value] ?? value
}

export function maintenanceCategoryLabel(value) {
  return MAINTENANCE_CATEGORY_LABELS[value] ?? value
}
