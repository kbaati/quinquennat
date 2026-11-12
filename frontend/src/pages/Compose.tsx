import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, createSimulation, fetchLevers, type Lever } from '../api.ts'
import { formatLeverValue } from '../format.ts'
import { navigate } from '../router.ts'

function groupLevers(levers: Lever[]): [string, Lever[]][] {
  const groups = new Map<string, Lever[]>()
  for (const lever of levers) {
    groups.set(lever.groupe, [...(groups.get(lever.groupe) ?? []), lever])
  }
  return [...groups]
}

function LeverControl({ lever, value, onChange }: { lever: Lever; value: number; onChange: (value: number) => void }) {
  const changed = value !== lever.valeurActuelle
  return (
    <div className="lever">
      <div className="lever-head">
        <label htmlFor={lever.id}>{lever.libelle}</label>
        <output htmlFor={lever.id} className={changed ? 'lever-value changed' : 'lever-value'}>
          {formatLeverValue(value, lever.unite)}
        </output>
      </div>
      <input
        id={lever.id}
        type="range"
        min={lever.min}
        max={lever.max}
        step={lever.pas}
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
        aria-describedby={`${lever.id}-help`}
      />
      <p id={`${lever.id}-help`} className="lever-help">
        {lever.description} Aujourd'hui : <strong>{formatLeverValue(lever.valeurActuelle, lever.unite)}</strong>
        {changed && (
          <>
            {' · '}
            <button type="button" className="link" onClick={() => onChange(lever.valeurActuelle)}>
              rétablir
            </button>
          </>
        )}
      </p>
    </div>
  )
}

export default function Compose() {
  const [levers, setLevers] = useState<Lever[] | null>(null)
  const [values, setValues] = useState<Record<string, number>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    fetchLevers()
      .then((loaded) => {
        if (cancelled) return
        setLevers(loaded)
        setValues(Object.fromEntries(loaded.map((lever) => [lever.id, lever.valeurActuelle])))
      })
      .catch((cause: ApiError) => !cancelled && setError(cause.message))
    return () => {
      cancelled = true
    }
  }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!levers) return
    setSubmitting(true)
    setError(null)
    // Only the levers that were moved are sent: the others keep the current legislation.
    const moved = Object.fromEntries(
      levers.filter((lever) => values[lever.id] !== lever.valeurActuelle).map((lever) => [lever.id, values[lever.id]]),
    )
    try {
      const simulation = await createSimulation(moved)
      navigate(`/s/${simulation.id}`)
    } catch (cause) {
      setError((cause as ApiError).message)
      setSubmitting(false)
    }
  }

  const movedCount = levers ? levers.filter((lever) => values[lever.id] !== lever.valeurActuelle).length : 0

  return (
    <>
      <h1>Composez votre programme</h1>
      <p className="lead">
        Réglez quelques leviers fiscaux et sociaux, puis regardez l'effet sur le budget annuel d'une vingtaine de
        foyers types.
      </p>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!levers && !error && <p>Chargement des leviers…</p>}
      {levers && (
        <form onSubmit={submit}>
          {groupLevers(levers).map(([group, groupLeverList]) => (
            <fieldset key={group}>
              <legend>{group}</legend>
              {groupLeverList.map((lever) => (
                <LeverControl
                  key={lever.id}
                  lever={lever}
                  value={values[lever.id]}
                  onChange={(value) => setValues((current) => ({ ...current, [lever.id]: value }))}
                />
              ))}
            </fieldset>
          ))}
          <div className="actions">
            <button type="submit" className="primary" disabled={submitting}>
              {submitting ? 'Calcul en cours…' : 'Simuler'}
            </button>
            <span className="actions-note" aria-live="polite">
              {submitting
                ? 'Le calcul peut prendre quelques secondes.'
                : movedCount === 0
                  ? 'Aucun levier modifié : la simulation montrera la situation actuelle.'
                  : `${movedCount} levier${movedCount > 1 ? 's' : ''} modifié${movedCount > 1 ? 's' : ''}.`}
            </span>
          </div>
        </form>
      )}
    </>
  )
}
