import api from './user'
import type { Article } from './article'

export interface FavoriteListResponse {
  list: Article[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export const favoriteApi = {
  addFavorite(articleId: number) {
    return api.post<any, { code: number; message: string; data: string }>(`/favorite/article/${articleId}`)
  },

  removeFavorite(articleId: number) {
    return api.delete<any, { code: number; message: string; data: string }>(`/favorite/article/${articleId}`)
  },

  getFavoriteStatus(articleId: number) {
    return api.get<any, { code: number; message: string; data: boolean }>(`/favorite/article/${articleId}/status`)
  },

  getFavoriteArticles(params: {
    pageNum?: number
    pageSize?: number
  }) {
    return api.get<any, { code: number; message: string; data: FavoriteListResponse }>('/favorite/articles', { params })
  }
}

export default favoriteApi
