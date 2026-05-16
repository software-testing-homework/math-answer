import api from './user'

export interface Comment {
  id: number
  articleId: number
  userId: number
  userName: string
  userAvatar: string
  content: string
  likeCount: number
  replyCount: number
  status: number
  createTime: string
  updateTime: string
  isLiked: boolean
}

export interface CommentListResponse {
  list: Comment[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface CommentCreateRequest {
  articleId: number
  content: string
}

export const commentApi = {
  getCommentList(params: {
    articleId: number
    pageNum?: number
    pageSize?: number
  }) {
    return api.get<any, { code: number; message: string; data: CommentListResponse }>('/comment/list', { params })
  },

  getTopComments(params: {
    articleId: number
    limit?: number
  }) {
    return api.get<any, { code: number; message: string; data: Comment[] }>('/comment/top', { params })
  },

  createComment(data: CommentCreateRequest) {
    return api.post<any, { code: number; message: string; data: Comment }>('/comment/create', data)
  },

  deleteComment(id: number) {
    return api.delete<any, { code: number; message: string; data: string }>(`/comment/${id}`)
  },

  likeComment(id: number) {
    return api.post<any, { code: number; message: string; data: string }>(`/comment/${id}/like`)
  },

  unlikeComment(id: number) {
    return api.delete<any, { code: number; message: string; data: string }>(`/comment/${id}/like`)
  }
}

export default { commentApi }
