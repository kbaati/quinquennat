import { useEffect, useState } from 'react'
import { ApiError, fetchSimulation, type HouseholdResult, type Simulation } from '../api.ts'
import DeltaChart from '../components/DeltaChart.tsx'
import Testimonials from '../components/Testimonials.tsx'
import { AMOUNT_LABELS, formatEuros, formatLeverValue, formatSignedEuros } from '../format.ts'
import { navigate } from '../router.ts'

/** Amounts other than disposable income that the programme changes for this household. */
function changedAmounts(household: HouseholdResult): string[] {
  return Object.keys(household.avant)
    .filter((name) => name !== 'revenu_disponible' && household.avant[name] !== household.apres[name])
    .map(
      (name) =>
        `${AMOUNT_LABELS[name] ?? name} : ${formatEuros(household.avant[name])} → ${formatEuros(household.apres[name])}`,
    )
}

function ShareLink({ id }: { id: string }) {
  const [copied, setCopied] = useState(false)
  const url = `${window.location.origin}/s/${id}`

  async function copy() {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <section>
      <h2>Partager cette simulation</h2>
      <div className="share">
        <input type="text" readOnly value={url} aria-label="Lien de partage" onFocus={(event) => event.target.select()} />
        <button type="button" onClick={copy}>
          {copied ? 'Lien copié' : 'Copier le lien'}
        </button>
      </div>
    </section>
  )
}

export default function Results({ id }: { id: string }) {
  const [simulation, setSimulation] = useState<Simulation | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchSimulation(id)
      .then((loaded) => !cancelled && setSimulation(loaded))
      .catch(
        (cause: ApiError) =>
          !cancelled && setError(cause.status === 404 ? "Cette simulation n'existe pas ou plus." : cause.message),
      )
    return () => {
      cancelled = true
    }
  }, [id])

  if (error) {
    return (
      <>
        <h1>Résultats</h1>
        <p className="error" role="alert">
          {error}
        </p>
        <button type="button" className="primary" onClick={() => navigate('/')}>
          Composer un programme
        </button>
      </>
    )
  }
  if (!simulation) {
    return <p>Chargement de la simulation…</p>
  }

  const winners = simulation.foyers.filter((household) => Math.round(household.ecartRevenuDisponible) > 0).length
  const losers = simulation.foyers.filter((household) => Math.round(household.ecartRevenuDisponible) < 0).length
  const unchanged = simulation.foyers.length - winners - losers

  return (
    <>
      <h1>Effet du programme sur {simulation.foyers.length} foyers types</h1>
      <p className="lead">
        Écart de revenu disponible sur une année pleine, avec la législation de {simulation.annee} comme point de
        départ.
      </p>

      <section>
        <h2>Le programme simulé</h2>
        {simulation.programme.length === 0 ? (
          <p>Aucun levier modifié : c'est la situation actuelle.</p>
        ) : (
          <ul className="program">
            {simulation.programme.map((item) => (
              <li key={item.levier}>
                <span>{item.libelle}</span>
                <span className="program-values">
                  {formatLeverValue(item.valeurActuelle, item.unite)} →{' '}
                  <strong>{formatLeverValue(item.valeur, item.unite)}</strong>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>Gains et pertes par foyer type</h2>
        <p className="summary">
          {winners} foyer{winners > 1 ? 's' : ''} y gagne{winners > 1 ? 'nt' : ''}, {losers} y perd
          {losers > 1 ? 'ent' : ''}, {unchanged} ne voi{unchanged > 1 ? 'ent' : 't'} pas de différence.
        </p>
        <DeltaChart households={simulation.foyers} />
      </section>

      <section>
        <h2>Détail par foyer</h2>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Foyer type</th>
                <th scope="col" className="number">
                  Avant
                </th>
                <th scope="col" className="number">
                  Après
                </th>
                <th scope="col" className="number">
                  Écart par an
                </th>
              </tr>
            </thead>
            <tbody>
              {simulation.foyers.map((household) => (
                <tr key={household.id}>
                  <th scope="row">
                    {household.nom}
                    <span className="household-description">{household.description}</span>
                    {changedAmounts(household).map((line) => (
                      <span key={line} className="household-change">
                        {line}
                      </span>
                    ))}
                  </th>
                  <td className="number">{formatEuros(household.avant.revenu_disponible)}</td>
                  <td className="number">{formatEuros(household.apres.revenu_disponible)}</td>
                  <td className="number strong">{formatSignedEuros(household.ecartRevenuDisponible)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="note">
          Montants annuels en euros. Le revenu disponible est ce qui reste au foyer après impôts directs et
          prestations sociales.
        </p>
      </section>

      <Testimonials id={simulation.id} households={simulation.foyers} />

      <ShareLink id={simulation.id} />

      <p className="note">
        Ces foyers types sont des exemples, pas un échantillon représentatif de la population. Calculs réalisés avec
        OpenFisca-France {simulation.versions.openfiscaFrance} (OpenFisca-Core {simulation.versions.openfiscaCore}).
      </p>
      <button type="button" onClick={() => navigate('/')}>
        Composer un autre programme
      </button>
    </>
  )
}
