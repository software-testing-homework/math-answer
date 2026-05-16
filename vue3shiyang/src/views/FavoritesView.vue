<template>
  <div class="favorites-page">
    <div class="page-header">
      <h1>问题收藏</h1>
      <p class="subtitle">管理您收藏的数学问题和解答</p>
    </div>

    <div v-if="loading" class="loading">
      加载中...
    </div>

    <div v-else-if="articles.length === 0" class="empty">
      <div class="empty-icon">⭐</div>
      <h2>暂无收藏</h2>
      <p>您还没有收藏任何问题，快去资讯页面看看吧！</p>
    </div>

    <div v-else class="article-list">
      <div v-for="article in articles" :key="article.id" class="article-card" @click="viewArticle(article.id)">
        <div class="article-header">
          <h3 class="article-title">{{ article.title }}</h3>
          <span class="article-category">{{ article.categoryName }}</span>
        </div>
        <p class="article-summary">{{ article.summary }}</p>
        <div class="article-meta">
          <div class="author-info">
            <img v-if="article.authorAvatar" :src="article.authorAvatar" alt="avatar" class="author-avatar">
            <span class="author-name">{{ article.authorName }}</span>
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
          <div class="article-time">
            {{ formatTime(article.publishTime) }}
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <button :disabled="pageNum === 1" @click="changePage(pageNum - 1)">
        上一页
      </button>
      <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum === totalPages" @click="changePage(pageNum + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi, type Article } from '../api/favorite.ts'

const router = useRouter()

const articles = ref<Article[]>([])
const loading = ref(false)

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const loadFavoriteArticles = async () => {
  loading.value = true
  try {
    const response = await favoriteApi.getFavoriteArticles({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (response.code === 200) {
      articles.value = response.data.list
      total.value = response.data.total
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  pageNum.value = page
  loadFavoriteArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const viewArticle = (id: number) => {
  router.push(`/article/${id}`)
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes + '分钟前'
    }
    return hours + '小时前'
  } else if (days < 7) {
    return days + '天前'
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

onMounted(() => {
  loadFavoriteArticles()
})
</script>

<style scoped>
.favorites-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;
  padding: 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.page-header h1 {
  font-size: 2.5rem;
  color: #1e293b;
  margin-bottom: 0.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #64748b;
  font-size: 1.1rem;
  font-weight: 400;
}

.loading,
.empty {
  text-align: center;
  padding: 4rem;
  color: #94a3b8;
  font-size: 1.1rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
}

.empty h2 {
  font-size: 1.5rem;
  color: #1e293b;
  margin-bottom: 0.5rem;
  font-weight: 700;
}

.empty p {
  color: #64748b;
  font-size: 1rem;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.article-card {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.75rem;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.article-card:hover {
  border-color: #4CAF50;
  box-shadow: 0 8px 24px rgba(76, 175, 80, 0.15);
  transform: translateY(-4px);
}

.article-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.article-title {
  font-size: 1.35rem;
  color: #1e293b;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
}

.article-category {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #2e7d32;
  padding: 0.35rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.article-summary {
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.7;
  margin: 0 0 1.25rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 1.25rem;
  border-top: 2px solid #f1f5f9;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #4CAF50;
}

.author-name {
  color: #475569;
  font-size: 0.95rem;
  font-weight: 600;
}

.article-stats {
  display: flex;
  gap: 1.25rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #94a3b8;
  font-size: 0.85rem;
  font-weight: 500;
}

.stat-item .icon {
  font-size: 1.1rem;
}

.article-time {
  color: #94a3b8;
  font-size: 0.85rem;
  font-weight: 500;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.pagination button {
  padding: 0.6rem 1.75rem;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 600;
  color: #64748b;
}

.pagination button:hover:not(:disabled) {
  background: #4CAF50;
  border-color: #4CAF50;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  color: #64748b;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 0.6rem 1.5rem;
  background: #f8fafc;
  border-radius: 8px;
}

@media (max-width: 640px) {
  .favorites-page {
    padding: 1rem;
  }

  .page-header h1 {
    font-size: 2rem;
  }

  .article-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .pagination {
    flex-direction: column;
    gap: 0.75rem;
  }

  .pagination button {
    width: 100%;
  }
}
</style>
