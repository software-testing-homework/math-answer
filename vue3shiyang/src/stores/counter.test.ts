import { setActivePinia, createPinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useCounterStore } from './counter'

describe('useCounterStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始值正确', () => {
    const store = useCounterStore()
    expect(store.count).toBe(0)
    expect(store.doubleCount).toBe(0)
  })

  it('increment 会让 count +1，并更新 doubleCount', () => {
    const store = useCounterStore()
    store.increment()
    expect(store.count).toBe(1)
    expect(store.doubleCount).toBe(2)
  })
})
