import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiMock = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn()
}

vi.mock('./user', () => {
  return {
    default: apiMock
  }
})

class MockFormData {
  entries: Array<{ key: string; value: any }> = []

  append(key: string, value: any) {
    this.entries.push({ key, value })
  }
}

describe('articleApi/categoryApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('categoryApi.getAllCategories 调用 /category/list', async () => {
    const { categoryApi } = await import('./article')
    categoryApi.getAllCategories()
    expect(apiMock.get).toHaveBeenCalledWith('/category/list')
  })

  it('articleApi.getArticleList 传递 params', async () => {
    const { articleApi } = await import('./article')
    const params = { pageNum: 1, pageSize: 10, keyword: 'k' }
    articleApi.getArticleList(params)
    expect(apiMock.get).toHaveBeenCalledWith('/article/list', { params })
  })

  it('articleApi.like/unlike 使用正确 method 与 path', async () => {
    const { articleApi } = await import('./article')
    articleApi.likeArticle(9)
    expect(apiMock.post).toHaveBeenCalledWith('/article/9/like')
    articleApi.unlikeArticle(9)
    expect(apiMock.delete).toHaveBeenCalledWith('/article/9/like')
  })

  it('articleApi.uploadCover 使用 multipart/form-data', async () => {
    vi.stubGlobal('FormData', MockFormData as any)
    const { articleApi } = await import('./article')
    const file = { name: 'a.png' } as any
    articleApi.uploadCover(file)

    const call = apiMock.post.mock.calls.find(c => c[0] === '/article/upload')
    expect(call).toBeTruthy()
    const [, body, config] = call as any
    expect(body).toBeInstanceOf(MockFormData)
    expect(body.entries).toEqual([{ key: 'file', value: file }])
    expect(config?.headers?.['Content-Type']).toBe('multipart/form-data')
  })
})

