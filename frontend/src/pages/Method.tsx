import { useEffect, useState } from 'react'
import { fetchHealth, fetchHouseholds, fetchLevers, type Health, type Household, type Lever } from '../api.ts'
import { formatLeverValue } from '../format.ts'

const SOURCE_URL = 'https://github.com/kbaati/quinquennat'
const OPENFISCA_URL = 'https://github.com/openfisca/openfisca-france'

/** Displayed amount -> OpenFisca-France variable it is read from. */
const AMOUNTS: [string, string, string][] = [
  ['Revenu disponible', 'revenu_disponible', 'Ce qui reste au foyer après impôts directs et prestations sociales.'],
  ['Impôt sur le revenu', 'impot_revenu_restant_a_payer', "Impôt dû sur les revenus de l'année."],
  ['CSG', 'csg', 'Contribution sociale généralisée, tous revenus du foyer confondus.'],
  ['RSA', 'rsa', 'Revenu de solidarité active.'],
  ["Prime d'activité", 'ppa', "Complément de revenu versé aux travailleurs modestes."],
  ['Allocations familiales', 'af', 'Versées à partir de deux enfants à charge.'],
  ['Aides au logement', 'aide_logement', 'APL, ALF ou ALS selon la situation.'],
]

/**
 * How every figure is produced. The lever and household lists come from the same catalogue the
 * simulations use, so this page cannot drift away from what is actually computed.
 */
export default function Method() {
  const [levers, setLevers] = useState<Lever[] | null>(null)
  const [households, setHouseholds] = useState<Household[] | null>(null)
  const [health, setHealth] = useState<Health | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchLevers()
      .then((loaded) => !cancelled && setLevers(loaded))
      .catch(() => undefined)
    fetchHouseholds()
      .then((loaded) => !cancelled && setHouseholds(loaded))
      .catch(() => undefined)
    fetchHealth()
      .then((loaded) => !cancelled && setHealth(loaded))
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [])

  const engine = health?.engine
  const version = engine?.openfiscaFrance
    ? `OpenFisca-France ${engine.openfiscaFrance} (OpenFisca-Core ${engine.openfiscaCore})`
    : 'OpenFisca-France'

  return (
    <article className="method">
      <h1>Méthode</h1>
      <p className="lead">D'où viennent les chiffres, ce qu'ils disent, et ce qu'ils ne disent pas.</p>

      <h2>Ce que fait l'outil</h2>
      <p>
        Vous réglez quelques leviers fiscaux et sociaux. L'outil recalcule alors, pour une série de foyers types,
        leurs impôts et leurs prestations sur une année, et affiche l'écart en euros avec la situation actuelle.
      </p>

      <h2>Ce que l'outil n'est pas</h2>
      <ul>
        <li>
          <strong>Pas un échantillon représentatif.</strong> Les foyers types sont des exemples choisis pour
          illustrer des situations variées. Le nombre de foyers gagnants ou perdants ne dit rien de la part de la
          population concernée.
        </li>
        <li>
          <strong>Pas un chiffrage budgétaire.</strong> L'outil ne calcule ni le coût ni le rendement d'un
          programme pour les finances publiques.
        </li>
        <li>
          <strong>Pas un sondage.</strong> Il ne mesure aucune opinion et ne recueille aucune intention de vote.
        </li>
        <li>
          <strong>Pas un avis.</strong> L'outil ne soutient aucun parti ni aucun candidat et ne porte aucun
          jugement sur les programmes composés.
        </li>
      </ul>

      <h2>Comment les chiffres sont calculés</h2>
      <p>
        Les calculs sont réalisés par{' '}
        <a href={OPENFISCA_URL} target="_blank" rel="noreferrer">
          OpenFisca-France
        </a>
        , un logiciel libre qui traduit en code la législation fiscale et sociale française. Ce site utilise{' '}
        {version}.
      </p>
      <ul>
        <li>
          <strong>Année simulée : 2025</strong>, la dernière pour laquelle la législation est complète dans cette
          version. Chaque foyer est calculé deux fois : avec la législation de 2025, puis avec votre programme.
        </li>
        <li>
          <strong>Année pleine.</strong> Vos réglages s'appliquent à partir du 1er janvier, comme si la mesure
          était en vigueur toute l'année.
        </li>
        <li>
          <strong>Situation stable.</strong> Certaines prestations dépendent des revenus des années précédentes.
          Chaque foyer est donc supposé avoir eu les mêmes revenus et la même composition les deux années
          d'avant.
        </li>
        <li>
          <strong>Comportements inchangés.</strong> Les foyers ne modifient ni leur travail, ni leur logement, ni
          leur consommation en réaction au programme.
        </li>
      </ul>

      <h2>Les montants affichés</h2>
      <p>Tous les montants sont annuels, en euros. Chacun correspond à une variable d'OpenFisca-France.</p>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th scope="col">Montant</th>
              <th scope="col">Variable OpenFisca</th>
            </tr>
          </thead>
          <tbody>
            {AMOUNTS.map(([label, variable, description]) => (
              <tr key={variable}>
                <th scope="row">
                  {label}
                  <span className="household-description">{description}</span>
                </th>
                <td>
                  <code>{variable}</code>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="note">
        L'écart affiché pour un foyer est la différence de revenu disponible entre les deux calculs. Les autres
        prélèvements (TVA, taxes locales, cotisations sociales) ne sont pas modifiés par les leviers.
      </p>

      <h2>Les leviers</h2>
      <p>
        Chaque levier modifie un seul paramètre de la législation. La valeur « aujourd'hui » est celle
        d'OpenFisca-France pour 2025 ; un test automatique vérifie qu'elle n'a pas dérivé.
      </p>
      {!levers && <p>Chargement des leviers…</p>}
      {levers && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Levier</th>
                <th scope="col" className="number">
                  Aujourd'hui
                </th>
                <th scope="col" className="number">
                  Réglable de… à…
                </th>
              </tr>
            </thead>
            <tbody>
              {levers.map((lever) => (
                <tr key={lever.id}>
                  <th scope="row">
                    {lever.libelle}
                    <span className="household-description">{lever.description}</span>
                    <code className="parameter">{lever.parametre}</code>
                  </th>
                  <td className="number">{formatLeverValue(lever.valeurActuelle, lever.unite)}</td>
                  <td className="number">
                    {formatLeverValue(lever.min, lever.unite)} – {formatLeverValue(lever.max, lever.unite)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="note">
        Le levier sur la CSG ne porte que sur la part déductible de la CSG sur les revenus d'activité ; la part
        non déductible reste inchangée.
      </p>

      <h2>Les foyers types</h2>
      <p>
        {households ? `Les ${households.length} foyers types` : 'Les foyers types'} sont décrits dans un fichier
        public du dépôt, <code>donnees/foyers.json</code>, avec toutes les valeurs transmises au calcul.
      </p>
      {households && (
        <ul className="household-list">
          {households.map((household) => (
            <li key={household.id}>
              <strong>{household.nom}.</strong> {household.description}
            </li>
          ))}
        </ul>
      )}

      <h2>Les témoignages fictifs</h2>
      <p>
        Sous les chiffres, chaque foyer dont le budget change est accompagné d'un court texte.{' '}
        <strong>Ces témoignages sont fictifs</strong> : personne ne les a prononcés, et ils ne mesurent aucune
        opinion.
      </p>
      <ul>
        <li>
          Le texte est écrit à partir de deux informations seulement : la description du foyer type et ses
          montants en euros, avant et après. Le programme lui-même n'est pas transmis.
        </li>
        <li>
          Selon la configuration du site, il est rédigé par un modèle d'intelligence artificielle ou tiré de
          phrases d'exemple. L'étiquette placée sous chaque témoignage indique lequel des deux.
        </li>
        <li>
          La consigne interdit toute mention d'un parti, d'un candidat, d'une personne réelle ou d'une opinion
          politique. Un texte généré par IA peut malgré tout être maladroit ou inexact : seuls les chiffres font
          foi.
        </li>
        <li>
          Le « ressenti », de −2 à +2, décrit l'effet sur le budget du foyer. Ce n'est ni une approbation ni une
          intention de vote.
        </li>
      </ul>

      <h2>Limites à garder en tête</h2>
      <ul>
        <li>Les montants sont des ordres de grandeur pour des situations simplifiées, pas un calcul personnalisé.</li>
        <li>
          Seuls certains dispositifs sont détaillés. Des aides locales ou des situations particulières (handicap,
          pensions alimentaires, revenus du patrimoine) ne sont pas représentées.
        </li>
        <li>
          Les résultats dépendent de la version d'OpenFisca-France utilisée ; elle est rappelée sous chaque
          simulation.
        </li>
      </ul>

      <h2>Vos données</h2>
      <p>
        Le site ne demande aucun compte, ne dépose aucun cookie de suivi et n'utilise aucun outil de mesure
        d'audience. Une simulation enregistrée contient uniquement les réglages choisis, les résultats et la date ;
        elle est accessible à toute personne qui en connaît le lien. Votre adresse IP sert, en mémoire et pendant
        une minute, à limiter le nombre de simulations ; elle n'est pas enregistrée avec la simulation.
      </p>

      <h2>Code source</h2>
      <p>
        Le code, les foyers types et les leviers sont publics, sous licence AGPL-3.0 :{' '}
        <a href={SOURCE_URL} target="_blank" rel="noreferrer">
          github.com/kbaati/quinquennat
        </a>
        . Chacun peut vérifier les calculs, signaler une erreur ou proposer une correction.
      </p>
    </article>
  )
}
