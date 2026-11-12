const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
const signedEuros = new Intl.NumberFormat('fr-FR', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
  signDisplay: 'exceptZero',
})
const decimal = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 })

export function formatEuros(amount: number): string {
  return euros.format(amount)
}

export function formatSignedEuros(amount: number): string {
  return signedEuros.format(Math.round(amount))
}

/** A lever value in its display unit, e.g. "11 %", "646,52 €/mois". */
export function formatLeverValue(value: number, unit: string): string {
  return `${decimal.format(value)} ${unit}`
}

/** Labels of the yearly amounts returned for each household. */
export const AMOUNT_LABELS: Record<string, string> = {
  revenu_disponible: 'Revenu disponible',
  impot_revenu: 'Impôt sur le revenu',
  csg: 'CSG',
  rsa: 'RSA',
  prime_activite: "Prime d'activité",
  allocations_familiales: 'Allocations familiales',
  aides_logement: 'Aides au logement',
}
