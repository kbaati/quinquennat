import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import App from './App.tsx'

const levers = [
  {
    id: 'ir-taux-tranche-1',
    groupe: 'Impôt sur le revenu',
    libelle: 'Taux de la 1re tranche imposée',
    description: 'Taux appliqué à la première tranche.',
    unite: '%',
    valeurActuelle: 11,
    min: 0,
    max: 25,
    pas: 0.5,
  },
  {
    id: 'rsa-montant-forfaitaire',
    groupe: 'Minima sociaux',
    libelle: 'Montant forfaitaire du RSA',
    description: 'Montant mensuel de base.',
    unite: '€/mois',
    valeurActuelle: 646.52,
    min: 400,
    max: 1000,
    pas: 5,
  },
]

const simulation = {
  id: 'abcd2345',
  creeLe: '2026-11-12T10:00:00Z',
  annee: 2025,
  programme: [
    {
      levier: 'ir-taux-tranche-1',
      groupe: 'Impôt sur le revenu',
      libelle: 'Taux de la 1re tranche imposée',
      unite: '%',
      valeurActuelle: 11,
      valeur: 15,
      parametre: 'x',
    },
  ],
  versions: { openfiscaCore: '44.7.1', openfiscaFrance: '176.1.10' },
  foyers: [
    {
      id: 'seul-smic',
      nom: 'Personne seule au SMIC',
      description: 'Salariée au SMIC.',
      avant: { revenu_disponible: 20000, impot_revenu: 0 },
      apres: { revenu_disponible: 19700, impot_revenu: 300 },
      ecartRevenuDisponible: -300,
    },
    {
      id: 'seul-sans-revenu',
      nom: 'Personne seule sans revenu',
      description: 'Sans emploi.',
      avant: { revenu_disponible: 10000, impot_revenu: 0 },
      apres: { revenu_disponible: 10000, impot_revenu: 0 },
      ecartRevenuDisponible: 0,
    },
  ],
}

function json(body: unknown, status = 200) {
  return Promise.resolve({ ok: status < 400, status, json: async () => body })
}

let fetchMock: ReturnType<typeof vi.fn>

beforeEach(() => {
  fetchMock = vi.fn((url: string, init?: RequestInit) => {
    if (url === '/api/leviers') return json(levers)
    if (url === '/api/simulations' && init?.method === 'POST') return json(simulation, 201)
    if (url === '/api/simulations/abcd2345') return json(simulation)
    return json({ detail: 'Simulation introuvable.' }, 404)
  })
  vi.stubGlobal('fetch', fetchMock)
  vi.stubGlobal('scrollTo', vi.fn())
  window.history.pushState(null, '', '/')
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

test('compose, simulate and land on the results', async () => {
  render(<App />)

  const slider = await screen.findByLabelText('Taux de la 1re tranche imposée')
  expect(screen.getByText('Impôt sur le revenu')).toBeTruthy()
  expect(screen.getByText(/Aucun levier modifié/)).toBeTruthy()

  fireEvent.change(slider, { target: { value: '15' } })
  expect(screen.getByText('1 levier modifié.')).toBeTruthy()
  fireEvent.submit(screen.getByRole('button', { name: 'Simuler' }).closest('form')!)

  expect(await screen.findByText('Effet du programme sur 2 foyers types')).toBeTruthy()
  // Only the moved lever is sent, by id: never a raw OpenFisca parameter path.
  const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')!
  expect(JSON.parse(post[1].body)).toEqual({ leviers: { 'ir-taux-tranche-1': 15 } })
  expect(window.location.pathname).toBe('/s/abcd2345')
})

test('results show the programme, gains and losses, and the share link', async () => {
  window.history.pushState(null, '', '/s/abcd2345')

  render(<App />)

  await screen.findByText('Effet du programme sur 2 foyers types')
  expect(screen.getByText('0 foyer y gagne, 1 y perd, 1 ne voit pas de différence.')).toBeTruthy()
  expect(screen.getAllByText('Personne seule au SMIC').length).toBeGreaterThan(0)
  expect(screen.getByText(/Impôt sur le revenu : 0\s€ → 300\s€/)).toBeTruthy()
  expect((screen.getByLabelText('Lien de partage') as HTMLInputElement).value).toMatch(/\/s\/abcd2345$/)
  expect(screen.getByText(/OpenFisca-France 176\.1\.10/)).toBeTruthy()
  expect(document.body.textContent).toContain('pas un échantillon représentatif')
})

test('an unknown simulation gets a clear message', async () => {
  window.history.pushState(null, '', '/s/zzzz9999')

  render(<App />)

  expect(await screen.findByText("Cette simulation n'existe pas ou plus.")).toBeTruthy()
})

test('a failed simulation shows the error and lets the user retry', async () => {
  fetchMock.mockImplementation((url: string, init?: RequestInit) =>
    url === '/api/leviers'
      ? json(levers)
      : init?.method === 'POST'
        ? json({ detail: 'Trop de simulations en peu de temps.' }, 429)
        : json({}, 404),
  )
  render(<App />)

  fireEvent.submit((await screen.findByRole('button', { name: 'Simuler' })).closest('form')!)

  expect(await screen.findByText('Trop de simulations en peu de temps.')).toBeTruthy()
  await waitFor(() => expect((screen.getByRole('button', { name: 'Simuler' }) as HTMLButtonElement).disabled).toBe(false))
})
