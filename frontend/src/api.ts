export type ServiceStatus = 'ok' | 'unavailable'

export interface Health {
  backend: ServiceStatus
  engine: {
    status: ServiceStatus
    openfiscaCore: string | null
    openfiscaFrance: string | null
  }
}

export async function fetchHealth(): Promise<Health> {
  const response = await fetch('/api/sante')
  if (!response.ok) {
    throw new Error(`GET /api/sante: HTTP ${response.status}`)
  }
  return response.json()
}
