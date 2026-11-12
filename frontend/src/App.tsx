import type { MouseEvent } from 'react'
import Compose from './pages/Compose.tsx'
import Results from './pages/Results.tsx'
import { navigate, usePath } from './router.ts'

function Page({ path }: { path: string }) {
  const result = path.match(/^\/s\/([a-z0-9]+)\/?$/)
  if (result) {
    return <Results key={result[1]} id={result[1]} />
  }
  if (path === '/') {
    return <Compose />
  }
  return (
    <>
      <h1>Page introuvable</h1>
      <p>Cette adresse ne correspond à aucune page.</p>
    </>
  )
}

function App() {
  const path = usePath()

  function goHome(event: MouseEvent) {
    event.preventDefault()
    navigate('/')
  }

  return (
    <>
      <header className="site-header">
        <a href="/" onClick={goHome} className="site-title">
          Quinquennat
        </a>
        <span className="site-tagline">Un programme, des foyers types, des euros</span>
      </header>
      <main>
        <Page path={path} />
      </main>
      <footer className="site-footer">
        Outil indépendant et non partisan. Aucune donnée personnelle n'est collectée. Code source sous licence
        AGPL-3.0.
      </footer>
    </>
  )
}

export default App
