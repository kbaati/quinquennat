import { useEffect, type MouseEvent, type ReactNode } from 'react'
import Compose from './pages/Compose.tsx'
import Method from './pages/Method.tsx'
import Results from './pages/Results.tsx'
import { navigate, usePath } from './router.ts'

const SITE_NAME = 'Quinquennat'

function route(path: string): { title: string; page: ReactNode } {
  const result = path.match(/^\/s\/([a-z0-9]+)\/?$/)
  if (result) {
    return { title: 'Résultats de la simulation', page: <Results key={result[1]} id={result[1]} /> }
  }
  if (path === '/') {
    return { title: 'Composez votre programme', page: <Compose /> }
  }
  if (path === '/methode' || path === '/methode/') {
    return { title: 'Méthode', page: <Method /> }
  }
  return {
    title: 'Page introuvable',
    page: (
      <>
        <h1>Page introuvable</h1>
        <p>Cette adresse ne correspond à aucune page.</p>
      </>
    ),
  }
}

/** In-app link: a real href (open in a new tab, copy the address) without a full page reload. */
function Link({ to, current, children }: { to: string; current?: boolean; children: ReactNode }) {
  function follow(event: MouseEvent) {
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) return
    event.preventDefault()
    navigate(to)
  }
  return (
    <a href={to} onClick={follow} aria-current={current ? 'page' : undefined}>
      {children}
    </a>
  )
}

function App() {
  const path = usePath()
  const { title, page } = route(path)

  useEffect(() => {
    document.title = `${title} — ${SITE_NAME}`
  }, [title])

  return (
    <>
      <header className="site-header">
        <a
          href="/"
          className="site-title"
          onClick={(event) => {
            event.preventDefault()
            navigate('/')
          }}
        >
          {SITE_NAME}
        </a>
        <span className="site-tagline">Un programme, des foyers types, des euros</span>
        <nav className="site-nav" aria-label="Navigation principale">
          <Link to="/" current={path === '/'}>
            Composer
          </Link>
          <Link to="/methode" current={path.startsWith('/methode')}>
            Méthode
          </Link>
        </nav>
      </header>
      <main>{page}</main>
      <footer className="site-footer">
        Outil indépendant et non partisan. Aucune donnée personnelle n'est collectée. Code source sous licence
        AGPL-3.0. <Link to="/methode">Comment les chiffres sont calculés</Link>
      </footer>
    </>
  )
}

export default App
