import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiMock = {
  get: vi.fn(),
  post: vi.fn(),
  delete: vi.fn()
}

vi.mock('./user', () => {
  return {
    default: apiMock
  }
})

describe('favoriteApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('add/remove/status 使用正确 path', async () => {
    const favoriteApi = (await import('./favorite')).default
    favoriteApi.addFavorite(1)
    expect(apiMock.post).toHaveBeenCalledWith('/favorite/article/1')

    favoriteApi.removeFavorite(1)
    expect(apiMock.delete).toHaveBeenCalledWith('/favorite/article/1')

    favoriteApi.getFavoriteStatus(2)
    expect(apiMock.get).toHaveBeenCalledWith('/favorite/article/2/status')
  })

  it('getFavoriteArticles 传递 params', async () => {
    const favoriteApi = (await import('./favorite')).default
    const params = { pageNum: 2, pageSize: 20 }
    favoriteApi.getFavoriteArticles(params)
    expect(apiMock.get).toHaveBeenCalledWith('/favorite/articles', { params })
  })
})

