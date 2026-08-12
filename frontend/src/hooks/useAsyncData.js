import { useCallback, useEffect, useState } from 'react'

import { extractErrorMessage } from '../api/client.js'

/**
 * Loads data on mount and whenever `reload()` is called.
 *
 * `loader` must be referentially stable (wrap it in `useCallback`), otherwise
 * every render would trigger a new request. Responses from a superseded call
 * are discarded, so a fast reload can never overwrite fresher data.
 */
export default function useAsyncData(loader, fallbackMessage) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    loader()
      .then((result) => {
        if (active) {
          setData(result)
        }
      })
      .catch((failure) => {
        if (active) {
          setError(extractErrorMessage(failure, fallbackMessage))
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [loader, fallbackMessage, reloadToken])

  const reload = useCallback(() => setReloadToken((token) => token + 1), [])

  return { data, loading, error, reload, setData }
}
