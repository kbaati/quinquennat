import { useEffect, useState } from 'react'
import { fetchHealth, type Health } from './api.ts'

type State = { kind: 'loading' } | { kind: 'loaded'; health: Health } | { kind: 'backend-down' }

function Status({ ok }: { ok: boolean }) {
  return <span className={ok ? 'status-ok' : 'status-ko'}>{ok ? 'ok' : 'indisponible'}</span>
}

function App() {
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    let cancelled = false
    fetchHealth()
      .then((health) => !cancelled && setState({ kind: 'loaded', health }))
      .catch(() => !cancelled && setState({ kind: 'backend-down' }))
    return () => {
      cancelled = true
    }
  }, [])

  const engine = state.kind === 'loaded' ? state.health.engine : null

  return (
    <main>
      <h1>Quinquennat</h1>
      <p className="subtitle">État des services</p>
      {state.kind === 'loading' ? (
        <p>Vérification en cours…</p>
      ) : (
        <ul className="status-list">
          <li data-testid="backend">
            <span>Backend</span>
            <Status ok={state.kind === 'loaded'} />
          </li>
          <li data-testid="engine">
            <span>Moteur de calcul</span>
            <Status ok={engine?.status === 'ok'} />
            {engine?.status === 'ok' && (
              <span className="versions">
                OpenFisca-France {engine.openfiscaFrance} · OpenFisca-Core {engine.openfiscaCore}
              </span>
            )}
          </li>
        </ul>
      )}
    </main>
  )
}

export default App
