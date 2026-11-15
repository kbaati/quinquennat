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
    parametre: 'impot_revenu.bareme_ir_depuis_1945.bareme.brackets[1].rate',
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
    parametre: 'prestations_sociales.solidarite_insertion.minima_sociaux.rsa.rsa_m.montant_de_base_du_rsa',
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

const reactions = {
  actif: true,
  generateur: 'ia',
  reactions: [
    {
      foyer: 'seul-smic',
      ressenti: -1,
      temoignage: 'Trois cents euros de moins, il faudra surveiller les courses.',
      raison: 'Le revenu disponible baisse de 300 € par an.',
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
    if (url === '/api/foyers') return json(simulation.foyers.map(({ id, nom, description }) => ({ id, nom, description })))
    if (url === '/api/sante')
      return json({ backend: 'ok', engine: { status: 'ok', openfiscaCore: '44.7.1', openfiscaFrance: '176.1.10' } })
    if (url === '/api/simulations' && init?.method === 'POST') return json(simulation, 201)
    if (url === '/api/simulations/abcd2345/reactions') return json(reactions)
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

test('testimonials are labelled as fictional and AI-generated, never as a poll', async () => {
  window.history.pushState(null, '', '/s/abcd2345')

  render(<App />)

  expect(await screen.findByText(/Trois cents euros de moins/)).toBeTruthy()
  expect(screen.getByText('Témoignage fictif généré par IA')).toBeTruthy()
  expect(screen.getByText(/Plutôt défavorable pour le budget du foyer/)).toBeTruthy()
  expect(document.body.textContent).toContain('ni un sondage')
})

test('template testimonials are not presented as AI-generated', async () => {
  fetchMock.mockImplementation((url: string) =>
    url.endsWith('/reactions') ? json({ ...reactions, generateur: 'factice' }) : json(simulation),
  )
  window.history.pushState(null, '', '/s/abcd2345')

  render(<App />)

  expect(await screen.findByText("Témoignage fictif — texte d'exemple automatique")).toBeTruthy()
  expect(document.body.textContent).not.toContain('généré par IA')
})

test('results stay readable when testimonials are switched off or fail', async () => {
  fetchMock.mockImplementation((url: string) =>
    url.endsWith('/reactions') ? json({ actif: false, generateur: 'factice', reactions: [] }) : json(simulation),
  )
  window.history.pushState(null, '', '/s/abcd2345')

  render(<App />)

  await screen.findByText('Effet du programme sur 2 foyers types')
  await waitFor(() => expect(screen.queryByText('Témoignages fictifs')).toBeNull())
})

test('the method page traces every lever to its OpenFisca parameter and states the limits', async () => {
  window.history.pushState(null, '', '/methode')

  render(<App />)

  expect(await screen.findByText('impot_revenu.bareme_ir_depuis_1945.bareme.brackets[1].rate')).toBeTruthy()
  expect(await screen.findByText(/OpenFisca-France 176\.1\.10/)).toBeTruthy()
  expect(await screen.findByText(/Les 2 foyers types/)).toBeTruthy()
  expect(screen.getByText('Pas un échantillon représentatif.')).toBeTruthy()
  expect(screen.getByText('Pas un sondage.')).toBeTruthy()
  expect(screen.getByText('Pas un chiffrage budgétaire.')).toBeTruthy()
  expect(screen.getByText('Ces témoignages sont fictifs')).toBeTruthy()
  expect(document.title).toBe('Méthode — Quinquennat')
})

test('the header links to the method page without reloading', async () => {
  render(<App />)
  await screen.findByLabelText('Taux de la 1re tranche imposée')

  fireEvent.click(screen.getByRole('link', { name: 'Méthode' }))

  expect(await screen.findByRole('heading', { name: 'Méthode' })).toBeTruthy()
  expect(window.location.pathname).toBe('/methode')
})
