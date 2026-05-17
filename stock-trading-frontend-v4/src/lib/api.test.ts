import { describe, expect, it } from 'vitest'
import { z } from 'zod'

import { parseApiData } from './api'

describe('parseApiData', () => {
  it('unwraps a valid API envelope', () => {
    const schema = z.object({ stockCode: z.string(), price: z.number() })

    const data = parseApiData(
      JSON.stringify({ success: true, data: { stockCode: '005930', price: 75000 } }),
      schema,
    )

    expect(data).toEqual({ stockCode: '005930', price: 75000 })
  })

  it('rejects an invalid response shape', () => {
    const schema = z.object({ stockCode: z.string(), price: z.number() })

    expect(() =>
      parseApiData(JSON.stringify({ success: true, data: { stockCode: '005930', price: '75000' } }), schema),
    ).toThrow()
  })
})
