export interface Lever {
  id: string
  groupe: string
  libelle: string
  description: string
  unite: string
  valeurActuelle: number
  min: number
  max: number
  pas: number
}

export type Amounts = Record<string, number>

export interface HouseholdResult {
  id: string
  nom: string
  description: string
  avant: Amounts
  apres: Amounts
  ecartRevenuDisponible: number
}

export interface ProgramItem {
  levier: string
  groupe: string
  libelle: string
  unite: string
  valeurActuelle: number
  valeur: number
  parametre: string
}

export interface Simulation {
  id: string
  creeLe: string
  annee: number
  programme: ProgramItem[]
  versions: { openfiscaCore: string; openfiscaFrance: string }
  foyers: HouseholdResult[]
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, init)
  } catch {
    throw new ApiError(0, 'Le service est injoignable. Vérifiez votre connexion puis réessayez.')
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(response.status, problem?.detail ?? `Le service a répondu par une erreur (${response.status}).`)
  }
  return response.json()
}

export function fetchLevers(): Promise<Lever[]> {
  return request('/api/leviers')
}

export function createSimulation(levers: Record<string, number>): Promise<Simulation> {
  return request('/api/simulations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ leviers: levers }),
  })
}

export function fetchSimulation(id: string): Promise<Simulation> {
  return request(`/api/simulations/${encodeURIComponent(id)}`)
}

/** A fictional testimonial. `ressenti` goes from -2 (very unfavourable for the budget) to 2. */
export interface Reaction {
  foyer: string
  ressenti: number
  temoignage: string
  raison: string
}

export interface Reactions {
  actif: boolean
  /** How the texts were produced: template sentences ("factice") or an AI model ("ia"). */
  generateur: 'factice' | 'ia'
  reactions: Reaction[]
}

export function fetchReactions(id: string): Promise<Reactions> {
  return request(`/api/simulations/${encodeURIComponent(id)}/reactions`)
}
