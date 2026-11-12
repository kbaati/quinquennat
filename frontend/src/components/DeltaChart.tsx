import type { HouseholdResult } from '../api.ts'
import { formatEuros, formatSignedEuros } from '../format.ts'

/**
 * Diverging horizontal bars: yearly change in disposable income per household, around a zero
 * axis. Gains and losses differ by side, by colour and by the signed label, never by colour alone.
 */
export default function DeltaChart({ households }: { households: HouseholdResult[] }) {
  const largest = Math.max(...households.map((household) => Math.abs(household.ecartRevenuDisponible)), 1)

  return (
    <figure className="chart">
      <figcaption className="chart-legend">
        <span>
          <i className="swatch loss" /> Perte
        </span>
        <span>
          <i className="swatch gain" /> Gain
        </span>
        <span className="chart-unit">en euros par an</span>
      </figcaption>
      <ul className="chart-rows">
        {households.map((household) => {
          const delta = Math.round(household.ecartRevenuDisponible)
          const width = `${(Math.abs(delta) / largest) * 50}%`
          const side = delta > 0 ? 'gain' : delta < 0 ? 'loss' : 'none'
          return (
            <li key={household.id} className="chart-row" tabIndex={0}>
              <span className="chart-label">{household.nom}</span>
              <span className="chart-track">
                <span className="chart-axis" />
                {side !== 'none' && <span className={`chart-bar ${side}`} style={{ width }} />}
                <span className={`chart-value ${side}`} style={side === 'none' ? undefined : { ['--bar' as string]: width }}>
                  {formatSignedEuros(delta)}
                </span>
              </span>
              <span className="chart-tooltip" role="tooltip">
                <strong>{household.nom}</strong>
                <span>Revenu disponible avant : {formatEuros(household.avant.revenu_disponible)}</span>
                <span>Revenu disponible après : {formatEuros(household.apres.revenu_disponible)}</span>
                <span>Écart : {formatSignedEuros(delta)} par an</span>
              </span>
            </li>
          )
        })}
      </ul>
    </figure>
  )
}
