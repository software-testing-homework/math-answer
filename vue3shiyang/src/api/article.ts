import api from './user'

export interface Category {
  id: number
  name: string
  code: string
  parentId: number
  level: number
  sort: number
  description: string
  icon: string
  status: number
}

export interface Article {
  id: number
  title: string
  summary: string
  content: string
  contentType: number
  coverImage: string
  categoryId: number
  categoryName: string
  authorId: number
  authorName: string
  authorAvatar: string
  tags: string
  viewCount: number
  likeCount: number
  commentCount: number
  status: number
  isTop: number
  publishTime: string
  createTime: string
}

export interface ArticleListResponse {
  list: Article[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface ArticleCreateRequest {
  title: string
  summary: string
  content: string
  contentType?: number
  coverImage?: string
  categoryId: number
  tags?: string
}

export interface ArticleUpdateRequest {
  id: number
  title?: string
  summary?: string
  content?: string
  contentType?: number
  coverImage?: string
  categoryId?: number
  tags?: string
}

export const categoryApi = {
  getAllCategories() {
    return api.get<any, { code: number; message: string; data: Category[] }>('/category/list')
  },

  getCategoryById(id: number) {
    return api.get<any, { code: number; message: string; data: Category }>(`/category/${id}`)
  },

  getCategoriesByLevel(level: number) {
    return api.get<any, { code: number; message: string; data: Category[] }>(`/category/level/${level}`)
  },

  getCategoriesByParentId(parentId: number) {
    return api.get<any, { code: number; message: string; data: Category[] }>(`/category/parent/${parentId}`)
  }
}

export const articleApi = {
  getArticleList(params: {
    pageNum?: number
    pageSize?: number
    categoryId?: number
    keyword?: string
    status?: number
  }) {
    return api.get<any, { code: number; message: string; data: ArticleListResponse }>('/article/list', { params })
  },

  getMyArticles(params: {
    pageNum?: number
    pageSize?: number
    status?: number
  }) {
    return api.get<any, { code: number; message: string; data: ArticleListResponse }>('/article/my', { params })
  },

  getArticleById(id: number) {
    return api.get<any, { code: number; message: string; data: Article }>(`/article/${id}`)
  },

  createArticle(data: ArticleCreateRequest) {
    return api.post<any, { code: number; message: string; data: Article }>('/article/create', data)
  },

  updateArticle(data: ArticleUpdateRequest) {
    return api.put<any, { code: number; message: string; data: Article }>('/article/update', data)
  },

  deleteArticle(id: number) {
    return api.delete<any, { code: number; message: string; data: string }>(`/article/${id}`)
  },

  likeArticle(id: number) {
    return api.post<any, { code: number; message: string; data: string }>(`/article/${id}/like`)
  },

  unlikeArticle(id: number) {
    return api.delete<any, { code: number; message: string; data: string }>(`/article/${id}/like`)
  },

  getHotArticles(limit?: number) {
    return api.get<any, { code: number; message: string; data: Article[] }>('/article/hot', { params: { limit } })
  },

  getHotArticlesByCategory(categoryId: number, limit?: number) {
    return api.get<any, { code: number; message: string; data: Article[] }>(`/article/hot/category/${categoryId}`, { params: { limit } })
  },

  getLatestArticles(limit?: number) {
    return api.get<any, { code: number; message: string; data: Article[] }>('/article/latest', { params: { limit } })
  },

  getLatestArticlesByCategory(categoryId: number, limit?: number) {
    return api.get<any, { code: number; message: string; data: Article[] }>(`/article/latest/category/${categoryId}`, { params: { limit } })
  },

  uploadCover(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<any, { code: number; message: string; data: { url: string } }>('/article/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  getFavoriteStatus(id: number) {
    return api.get<any, { code: number; message: string; data: boolean }>(`/article/${id}/favorite/status`)
  }
}

export default { categoryApi, articleApi }
