import { useEffect, useState } from 'react'
import { fetchReactions, type HouseholdResult, type Reactions } from '../api.ts'
import { formatSignedEuros } from '../format.ts'

const FEELINGS: Record<number, string> = {
  [-2]: 'Très défavorable pour le budget du foyer',
  [-1]: 'Plutôt défavorable pour le budget du foyer',
  0: 'Sans effet notable sur le budget du foyer',
  1: 'Plutôt favorable pour le budget du foyer',
  2: 'Très favorable pour le budget du foyer',
}

/** The label every testimonial carries, truthful about how the text was produced. */
function fictionLabel(generator: Reactions['generateur']): string {
  return generator === 'ia'
    ? 'Témoignage fictif généré par IA'
    : "Témoignage fictif — texte d'exemple automatique"
}

/**
 * Fictional testimonials, loaded after the figures so that a slow or failed generation never
 * hides the results. Renders nothing when the feature is switched off.
 */
export default function Testimonials({ id, households }: { id: string; households: HouseholdResult[] }) {
  const [reactions, setReactions] = useState<Reactions | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let cancelled = false
    fetchReactions(id)
      .then((loaded) => !cancelled && setReactions(loaded))
      .catch(() => !cancelled && setFailed(true))
    return () => {
      cancelled = true
    }
  }, [id])

  if (reactions && !reactions.actif) {
    return null
  }

  const byId = new Map(households.map((household) => [household.id, household]))
  const label = reactions ? fictionLabel(reactions.generateur) : ''

  return (
    <section>
      <h2>Témoignages fictifs</h2>
      <p className="note">
        Ces textes sont inventés{reactions?.generateur === 'ia' ? ' par une IA' : ''} à partir d'une seule
        information : le foyer type et son écart en euros. Ce ne sont ni des personnes réelles, ni un sondage, ni
        une mesure de l'opinion.
      </p>
      {failed && <p className="note">Les témoignages fictifs ne sont pas disponibles pour le moment.</p>}
      {!reactions && !failed && <p>Écriture des témoignages fictifs…</p>}
      {reactions && reactions.reactions.length === 0 && (
        <p>Aucun foyer type ne voit son budget changer : il n'y a pas de témoignage à afficher.</p>
      )}
      {reactions && reactions.reactions.length > 0 && (
        <ul className="testimonials">
          {reactions.reactions.map((reaction) => {
            const household = byId.get(reaction.foyer)
            if (!household) return null
            return (
              <li key={reaction.foyer} className="testimonial">
                <div className="testimonial-head">
                  <span>{household.nom}</span>
                  <span className="testimonial-delta">
                    {formatSignedEuros(household.ecartRevenuDisponible)} par an
                  </span>
                </div>
                <blockquote>« {reaction.temoignage} »</blockquote>
                <p className="testimonial-meta">
                  {FEELINGS[reaction.ressenti] ?? FEELINGS[0]}. {reaction.raison}
                </p>
                <span className="fiction-tag">{label}</span>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
