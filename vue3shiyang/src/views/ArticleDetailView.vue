<template>
  <div class="article-detail-page">
    <div v-if="loading" class="loading">
      加载中...
    </div>

    <div v-else-if="article" class="main-container">
      <div class="left-sidebar">
        <div v-if="topComments.length > 0" class="top-comments-section">
          <h3>🏆 热门回答</h3>
          <div class="top-comments-list">
            <div v-for="comment in topComments" :key="comment.id" class="top-comment-item">
              <div class="comment-header">
                <img v-if="comment.userAvatar" :src="comment.userAvatar" alt="avatar" class="comment-avatar">
                <div class="comment-user-info">
                  <span class="comment-username">{{ comment.userName }}</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-actions">
                <button :class="['like-btn', { active: comment.isLiked }]" @click="toggleCommentLike(comment)">
                  <span>{{ comment.isLiked ? '❤️' : '🤍' }}</span>
                  <span>{{ comment.likeCount }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="article-content-wrapper">
        <div class="article-container">
          <div class="article-header">
            <div class="back-btn" @click="goBack">
              ← 返回
            </div>
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-meta">
              <div class="author-info">
                <img v-if="article.authorAvatar" :src="article.authorAvatar" alt="avatar" class="author-avatar">
                <div class="author-details">
                  <span class="author-name">{{ article.authorName }}</span>
                  <span class="article-time">{{ formatTime(article.publishTime) }}</span>
                </div>
              </div>
              <div class="article-stats">
                <span class="stat-item">
                  <span class="icon">👁️</span>
                  {{ article.viewCount }}
                </span>
                <span class="stat-item">
                  <span class="icon">👍</span>
                  {{ article.likeCount }}
                </span>
                <span class="stat-item">
                  <span class="icon">💬</span>
                  {{ article.commentCount }}
                </span>
              </div>
            </div>
            <div class="article-category">
              <span class="category-badge">{{ article.categoryName }}</span>
              <span v-if="article.tags" class="tags">
                <span v-for="(tag, index) in parseTags(article.tags)" :key="index" class="tag">
                  {{ tag }}
                </span>
              </span>
            </div>
          </div>

          <div class="article-content">
            <div v-if="article.coverImage" class="cover-image" @click="openImagePreview">
              <img :src="article.coverImage" alt="问题图片">
              <div class="image-overlay">
                <span class="zoom-icon">🔍</span>
              </div>
            </div>
            <div class="content-text" v-html="formatContent(article.content)"></div>
          </div>

          <div class="article-actions">
            <button :class="['action-btn', { active: isLiked }]" @click="toggleLike">
              <span class="icon">{{ isLiked ? '❤️' : '🤍' }}</span>
              <span>{{ isLiked ? '已点赞' : '点赞' }}</span>
              <span class="count">({{ article.likeCount }})</span>
            </button>
            <button :class="['action-btn', { active: isFavorited }]" @click="toggleFavorite">
              <span class="icon">{{ isFavorited ? '⭐' : '☆' }}</span>
              <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
            </button>
            <button class="action-btn" @click="scrollToComments">
              <span class="icon">💬</span>
              <span>评论</span>
              <span class="count">({{ article.commentCount }})</span>
            </button>
          </div>

          <div class="comments-section" id="comments">
            <h3>评论 ({{ article.commentCount }})</h3>
            <div class="comment-form">
              <textarea v-model="commentText" placeholder="写下你的评论..." rows="3"></textarea>
              <button class="submit-comment-btn" @click="submitComment" :disabled="!commentText.trim()">
                发表评论
              </button>
            </div>
            <div v-if="commentsLoading" class="loading">
              加载评论中...
            </div>
            <div v-else-if="comments.length === 0" class="empty-comments">
              暂无评论，快来抢沙发吧！
            </div>
            <div v-else class="comments-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-header">
                  <img v-if="comment.userAvatar" :src="comment.userAvatar" alt="avatar" class="comment-avatar">
                  <div class="comment-user-info">
                    <span class="comment-username">{{ comment.userName }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <button v-if="canDeleteComment(comment)" class="delete-comment-btn"
                    @click="deleteComment(comment.id)">
                    删除
                  </button>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
                <div class="comment-actions">
                  <button :class="['like-btn', { active: comment.isLiked }]" @click="toggleCommentLike(comment)">
                    <span>{{ comment.isLiked ? '❤️' : '🤍' }}</span>
                    <span>{{ comment.likeCount }}</span>
                  </button>
                </div>
              </div>
            </div>

            <div v-if="totalPages > 1" class="pagination">
              <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
                上一页
              </button>
              <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
                下一页
              </button>
            </div>
          </div>
        </div>

        <div v-if="showImagePreview && article?.coverImage" class="image-preview-modal"
          @click="showImagePreview = false">
          <div class="modal-content" @click.stop>
            <img :src="article.coverImage" alt="问题图片大图">
            <button class="close-btn" @click="showImagePreview = false">✕</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, type Article } from '../api/article'
import { commentApi, type Comment } from '../api/comment'
import { favoriteApi } from '../api/favorite'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref<Article | null>(null)
const loading = ref(true)
const isLiked = ref(false)
const isFavorited = ref(false)
const commentText = ref('')
const showImagePreview = ref(false)

const comments = ref<Comment[]>([])
const topComments = ref<Comment[]>([])
const commentsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)

const loadArticle = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const response = await articleApi.getArticleById(id)
    if (response.code === 200) {
      article.value = response.data

      if (userStore.isLoggedIn) {
        try {
          const favResponse = await articleApi.getFavoriteStatus(id)
          if (favResponse.code === 200) {
            isFavorited.value = favResponse.data
          }
        } catch (error) {
          isFavorited.value = false
        }
      }

      await loadTopComments()
      await loadComments()
    } else {
      article.value = null
    }
  } catch (error) {
    console.error('加载文章失败:', error)
    article.value = null
  } finally {
    loading.value = false
  }
}

const loadTopComments = async () => {
  if (!article.value) return

  try {
    const response = await commentApi.getTopComments({
      articleId: article.value.id,
      limit: 2
    })
    if (response.code === 200) {
      topComments.value = response.data
    }
  } catch (error) {
    console.error('加载热门评论失败:', error)
  }
}

const loadComments = async () => {
  if (!article.value) return

  commentsLoading.value = true
  try {
    const response = await commentApi.getCommentList({
      articleId: article.value.id,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    if (response.code === 200) {
      comments.value = response.data.list
      totalPages.value = response.data.pages
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

const toggleLike = async () => {
  if (!article.value) return

  try {
    if (isLiked.value) {
      await articleApi.unlikeArticle(article.value.id)
      article.value.likeCount--
    } else {
      await articleApi.likeArticle(article.value.id)
      article.value.likeCount++
    }
    isLiked.value = !isLiked.value
  } catch (error) {
    console.error('点赞失败:', error)
    alert('操作失败，请稍后重试')
  }
}

const toggleFavorite = async () => {
  if (!article.value) return

  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }

  try {
    if (isFavorited.value) {
      await favoriteApi.removeFavorite(article.value.id)
      isFavorited.value = false
    } else {
      await favoriteApi.addFavorite(article.value.id)
      isFavorited.value = true
    }
  } catch (error: any) {
    console.error('收藏操作失败:', error)
    alert(error.response?.data?.message || '操作失败，请稍后重试')
  }
}

const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }

  if (!article.value) return

  try {
    const response = await commentApi.createComment({
      articleId: article.value.id,
      content: commentText.value
    })
    if (response.code === 200) {
      commentText.value = ''
      article.value.commentCount++
      await loadComments()
      await loadTopComments()
    } else {
      alert(response.message || '评论失败')
    }
  } catch (error: any) {
    console.error('评论失败:', error)
    alert(error.response?.data?.message || '评论失败，请稍后重试')
  }
}

const deleteComment = async (commentId: number) => {
  if (!confirm('确定要删除这条评论吗？')) {
    return
  }

  try {
    const response = await commentApi.deleteComment(commentId)
    if (response.code === 200) {
      if (article.value) {
        article.value.commentCount--
      }
      await loadComments()
      await loadTopComments()
    } else {
      alert(response.message || '删除失败')
    }
  } catch (error: any) {
    console.error('删除评论失败:', error)
    alert(error.response?.data?.message || '删除失败，请稍后重试')
  }
}

const toggleCommentLike = async (comment: Comment) => {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }

  try {
    if (comment.isLiked) {
      await commentApi.unlikeComment(comment.id)
      comment.likeCount--
    } else {
      await commentApi.likeComment(comment.id)
      comment.likeCount++
    }
    comment.isLiked = !comment.isLiked
  } catch (error: any) {
    console.error('点赞失败:', error)
    alert(error.response?.data?.message || '操作失败，请稍后重试')
  }
}

const canDeleteComment = (comment: Comment) => {
  return userStore.isLoggedIn && userStore.user?.id === comment.userId
}

const changePage = (page: number) => {
  currentPage.value = page
  loadComments()
}

const scrollToComments = () => {
  document.getElementById('comments')?.scrollIntoView({ behavior: 'smooth' })
}

const openImagePreview = () => {
  showImagePreview.value = true
}

const goBack = () => {
  router.back()
}

const parseTags = (tags: string) => {
  return tags.split(',').map(tag => tag.trim()).filter(tag => tag)
}

const formatContent = (content: string) => {
  return content.replace(/\n/g, '<br>')
}

const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.article-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

.loading,
.error {
  text-align: center;
  padding: 4rem 2rem;
  color: #94a3b8;
  font-size: 1.1rem;
}

.main-container {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 2rem;
  align-items: start;
}

.left-sidebar {
  position: sticky;
  top: 2rem;
}

.top-comments-section {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.top-comments-section h3 {
  font-size: 1.2rem;
  color: #1e293b;
  margin: 0 0 1.25rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 3px solid #FFD700;
  font-weight: 700;
}

.top-comments-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.top-comment-item {
  padding: 1rem;
  background: #fff9e6;
  border-radius: 10px;
  border: 2px solid #ffd700;
}

.article-content-wrapper {
  flex: 1;
}

.article-container {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.article-header {
  padding: 2rem;
  border-bottom: 2px solid #f1f5f9;
}

.back-btn {
  display: inline-block;
  color: #4CAF50;
  cursor: pointer;
  font-size: 0.95rem;
  margin-bottom: 1rem;
  transition: all 0.3s ease;
  font-weight: 600;
}

.back-btn:hover {
  color: #45a049;
  transform: translateX(-2px);
}

.article-title {
  font-size: 2rem;
  color: #1e293b;
  margin: 0 0 1.5rem 0;
  line-height: 1.4;
  font-weight: 700;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #4CAF50;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.author-name {
  color: #1e293b;
  font-size: 0.95rem;
  font-weight: 600;
}

.article-time {
  color: #94a3b8;
  font-size: 0.85rem;
  font-weight: 500;
}

.article-stats {
  display: flex;
  gap: 1.5rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 500;
}

.stat-item .icon {
  font-size: 1.2rem;
}

.article-category {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.category-badge {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #2e7d32;
  padding: 0.35rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
}

.tags {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.tag {
  background: #f8fafc;
  color: #64748b;
  padding: 0.35rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
  border: 1px solid #e5e7eb;
}

.article-content {
  padding: 2rem;
  color: #1e293b;
  font-size: 1.1rem;
  line-height: 1.8;
}

.cover-image {
  margin-bottom: 2rem;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  max-width: 400px;
  position: relative;
  border: 2px solid #e5e7eb;
}

.cover-image img {
  width: 100%;
  height: auto;
  display: block;
  max-height: 250px;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.cover-image:hover .image-overlay {
  opacity: 1;
}

.zoom-icon {
  font-size: 2.5rem;
  color: white;
}

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 2rem;
}

.modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.modal-content img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.close-btn {
  position: absolute;
  top: -50px;
  right: -50px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: white;
  color: #1e293b;
  border: none;
  font-size: 1.8rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.close-btn:hover {
  background: #f1f5f9;
  transform: scale(1.1);
}

.article-content {
  color: #333;
  font-size: 1.05rem;
  line-height: 1.8;
}

.article-actions {
  display: flex;
  gap: 1.25rem;
  padding: 1.5rem 2rem;
  border-top: 2px solid #f1f5f9;
  border-bottom: 2px solid #f1f5f9;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.5rem;
  background: #f8fafc;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 0.95rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.action-btn:hover {
  background: #f0fdf4;
  border-color: #4CAF50;
  color: #4CAF50;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
}

.action-btn.active {
  background: #f0fdf4;
  border-color: #4CAF50;
  color: #4CAF50;
}

.action-btn.active.favorite {
  background: #fff9e6;
  border-color: #ffd700;
  color: #ffd700;
}

.action-btn .icon {
  font-size: 1.2rem;
}

.action-btn .count {
  color: #94a3b8;
  font-size: 0.85rem;
  font-weight: 600;
}

.comments-section {
  padding: 2rem;
}

.comments-section h3 {
  font-size: 1.4rem;
  color: #1e293b;
  margin: 0 0 1.5rem 0;
  font-weight: 700;
}

.comment-form {
  margin-bottom: 2rem;
}

.comment-form textarea {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
  min-height: 120px;
  margin-bottom: 1rem;
  outline: none;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.comment-form textarea:focus {
  border-color: #4CAF50;
  background: white;
  box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.1);
}

.submit-comment-btn {
  padding: 0.875rem 2rem;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.submit-comment-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #45a049 0%, #3d8b40 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.submit-comment-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comments-list {
  min-height: 120px;
}

.empty-comments {
  text-align: center;
  padding: 4rem;
  color: #94a3b8;
  font-size: 1rem;
  font-weight: 500;
}

.comment-item {
  padding: 1.5rem 0;
  border-bottom: 2px solid #f1f5f9;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.comment-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e5e7eb;
}

.comment-user-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.comment-username {
  color: #1e293b;
  font-size: 0.95rem;
  font-weight: 600;
}

.comment-time {
  color: #94a3b8;
  font-size: 0.85rem;
  font-weight: 500;
}

.delete-comment-btn {
  padding: 0.5rem 1rem;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-comment-btn:hover {
  background: #d32f2f;
  transform: translateY(-1px);
}

.comment-content {
  color: #1e293b;
  font-size: 1rem;
  line-height: 1.7;
  margin-bottom: 0.75rem;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 1.25rem;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 1.25rem;
  background: #f8fafc;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 0.95rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.like-btn:hover {
  background: #fce4ec;
  border-color: #f44336;
  color: #f44336;
  transform: translateY(-1px);
}

.like-btn.active {
  background: #fce4ec;
  border-color: #f44336;
  color: #f44336;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.25rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #f1f5f9;
}

.page-btn {
  padding: 0.6rem 1.75rem;
  border: 2px solid #e5e7eb;
  background: white;
  color: #64748b;
  border-radius: 12px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.page-btn:hover:not(:disabled) {
  border-color: #4CAF50;
  color: #4CAF50;
  background: #f0fdf4;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #64748b;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 0.75rem 1.5rem;
  background: #f8fafc;
  border-radius: 10px;
  border: 2px solid #e5e7eb;
}

@media (max-width: 1024px) {
  .main-container {
    grid-template-columns: 1fr;
  }

  .left-sidebar {
    position: static;
  }

  .top-comments-section {
    margin-bottom: 2rem;
  }
}

@media (max-width: 640px) {
  .article-detail-page {
    padding: 1rem;
  }

  .article-header {
    padding: 1.5rem;
  }

  .article-title {
    font-size: 1.5rem;
  }

  .article-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .article-content {
    padding: 1.5rem;
  }

  .article-actions {
    flex-wrap: wrap;
    padding: 1rem 1.5rem;
  }

  .action-btn {
    flex: 1;
    justify-content: center;
    min-width: 140px;
  }

  .comments-section {
    padding: 1.5rem;
  }

  .comment-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .delete-comment-btn {
    width: 100%;
    margin-top: 0.5rem;
  }

  .cover-image {
    max-width: 100%;
  }
}
</style>
