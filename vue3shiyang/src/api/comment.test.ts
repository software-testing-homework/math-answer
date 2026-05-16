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

describe('commentApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getCommentList 传递 params', async () => {
    const { commentApi } = await import('./comment')
    const params = { articleId: 1, pageNum: 2, pageSize: 5 }
    commentApi.getCommentList(params)
    expect(apiMock.get).toHaveBeenCalledWith('/comment/list', { params })
  })

  it('create/delete/like/unlike 使用正确 path', async () => {
    const { commentApi } = await import('./comment')
    commentApi.createComment({ articleId: 2, content: 'c' })
    expect(apiMock.post).toHaveBeenCalledWith('/comment/create', { articleId: 2, content: 'c' })

    commentApi.deleteComment(3)
    expect(apiMock.delete).toHaveBeenCalledWith('/comment/3')

    commentApi.likeComment(4)
    expect(apiMock.post).toHaveBeenCalledWith('/comment/4/like')

    commentApi.unlikeComment(4)
    expect(apiMock.delete).toHaveBeenCalledWith('/comment/4/like')
  })
})

