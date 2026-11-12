import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App.tsx'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function stubFetch(response: Promise<Partial<Response>>) {
  vi.stubGlobal('fetch', vi.fn(() => response))
}

test('shows both services as ok with OpenFisca versions', async () => {
  stubFetch(
    Promise.resolve({
      ok: true,
      json: async () => ({
        backend: 'ok',
        engine: { status: 'ok', openfiscaCore: '46.0.1', openfiscaFrance: '176.1.10' },
      }),
    }),
  )

  render(<App />)

  expect((await screen.findByTestId('backend')).textContent).toContain('ok')
  expect(screen.getByTestId('engine').textContent).toContain('ok')
  expect(screen.getByTestId('engine').textContent).toContain('176.1.10')
})

test('shows the engine as unavailable when the backend cannot reach it', async () => {
  stubFetch(
    Promise.resolve({
      ok: true,
      json: async () => ({
        backend: 'ok',
        engine: { status: 'unavailable', openfiscaCore: null, openfiscaFrance: null },
      }),
    }),
  )

  render(<App />)

  expect((await screen.findByTestId('backend')).textContent).toContain('ok')
  expect(screen.getByTestId('engine').textContent).toContain('indisponible')
})

test('shows both services as unavailable when the backend is down', async () => {
  stubFetch(Promise.reject(new Error('network')))

  render(<App />)

  expect((await screen.findByTestId('backend')).textContent).toContain('indisponible')
  expect(screen.getByTestId('engine').textContent).toContain('indisponible')
})
