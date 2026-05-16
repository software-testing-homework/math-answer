<template>
  <div class="articles-page">
    <div class="page-header">
      <h1>问题资讯</h1>
      <p class="subtitle">浏览社区中的数学问题和解答</p>
    </div>

    <div class="content-wrapper">
      <div class="main-content">
        <div class="search-bar">
          <input v-model="searchKeyword" type="text" placeholder="搜索问题..." @keyup.enter="handleSearch">
          <button @click="handleSearch">搜索</button>
        </div>

        <div class="category-filter">
          <button :class="['category-btn', { active: selectedCategoryId === null }]" @click="selectCategory(null)">
            全部
          </button>
          <button v-for="category in categories" :key="category.id"
            :class="['category-btn', { active: selectedCategoryId === category.id }]"
            @click="selectCategory(category.id)">
            {{ category.name }}
          </button>
        </div>

        <div v-if="loading" class="loading">
          加载中...
        </div>

        <div v-else-if="articles.length === 0" class="empty">
          暂无数据
        </div>

        <div v-else class="article-list">
          <div v-for="article in articles" :key="article.id" class="article-card" @click="viewArticle(article.id)">
            <div v-if="article.isTop === 1" class="top-badge">置顶</div>
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
                <button :class="['favorite-btn', { active: favoriteStatus.get(article.id) }]"
                  @click="toggleFavorite(article, $event)">
                  <span class="icon">{{ favoriteStatus.get(article.id) ? '⭐' : '☆' }}</span>
                </button>
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

      <div class="sidebar">
        <div class="sidebar-section">
          <h3>热门问题</h3>
          <div class="hot-articles">
            <div v-for="article in hotArticles" :key="article.id" class="hot-article-item"
              @click="viewArticle(article.id)">
              <span class="hot-rank">{{ hotArticles.indexOf(article) + 1 }}</span>
              <span class="hot-title">{{ article.title }}</span>
            </div>
          </div>
        </div>

        <div class="sidebar-section">
          <h3>最新问题</h3>
          <div class="latest-articles">
            <div v-for="article in latestArticles" :key="article.id" class="latest-article-item"
              @click="viewArticle(article.id)">
              <span class="latest-title">{{ article.title }}</span>
              <span class="latest-time">{{ formatTime(article.publishTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi, type Article, type Category } from '../api/article'
import { favoriteApi } from '../api/favorite'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const articles = ref<Article[]>([])
const categories = ref<Category[]>([
  { id: 1, name: '算式与化简', code: 'algebra', parentId: 0, level: 1, sort: 1, description: '', icon: '', status: 1 },
  { id: 2, name: '方程求解', code: 'equation', parentId: 0, level: 1, sort: 2, description: '', icon: '', status: 1 },
  { id: 3, name: '不等式与估计', code: 'inequality', parentId: 0, level: 1, sort: 3, description: '', icon: '', status: 1 },
  { id: 4, name: '函数图像与性质', code: 'function', parentId: 0, level: 1, sort: 4, description: '', icon: '', status: 1 },
  { id: 5, name: '数列与递推', code: 'sequence', parentId: 0, level: 1, sort: 5, description: '', icon: '', status: 1 },
  { id: 6, name: '几何度量与证明', code: 'geometry', parentId: 0, level: 1, sort: 6, description: '', icon: '', status: 1 },
  { id: 7, name: '变换与对称', code: 'transformation', parentId: 0, level: 1, sort: 7, description: '', icon: '', status: 1 },
  { id: 8, name: '统计与概率', code: 'statistics', parentId: 0, level: 1, sort: 8, description: '', icon: '', status: 1 },
  { id: 9, name: '组合与计数', code: 'combination', parentId: 0, level: 1, sort: 9, description: '', icon: '', status: 1 },
  { id: 10, name: '优化与建模', code: 'optimization', parentId: 0, level: 1, sort: 10, description: '', icon: '', status: 1 },
  { id: 11, name: '算法与数值', code: 'algorithm', parentId: 0, level: 1, sort: 11, description: '', icon: '', status: 1 },
  { id: 12, name: '证明与公理化', code: 'proof', parentId: 0, level: 1, sort: 12, description: '', icon: '', status: 1 }
])
const hotArticles = ref<Article[]>([])
const latestArticles = ref<Article[]>([])

const loading = ref(false)
const searchKeyword = ref('')
const selectedCategoryId = ref<number | null>(null)

const pageNum = ref(1)
const pageSize = ref(5)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const favoriteStatus = ref<Map<number, boolean>>(new Map())

const toggleFavorite = async (article: Article, event: Event) => {
  event.stopPropagation()

  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }

  try {
    const isFavorited = favoriteStatus.value.get(article.id) || false
    if (isFavorited) {
      await favoriteApi.removeFavorite(article.id)
      favoriteStatus.value.set(article.id, false)
    } else {
      await favoriteApi.addFavorite(article.id)
      favoriteStatus.value.set(article.id, true)
    }
  } catch (error: any) {
    console.error('收藏操作失败:', error)
    alert(error.response?.data?.message || '操作失败，请稍后重试')
  }
}

const loadArticles = async () => {
  loading.value = true
  try {
    const response = await articleApi.getArticleList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      categoryId: selectedCategoryId.value || undefined,
      keyword: searchKeyword.value || undefined,
      status: 1
    })
    if (response.code === 200) {
      articles.value = response.data.list
      total.value = response.data.total

      if (userStore.isLoggedIn) {
        for (const article of articles.value) {
          try {
            const favResponse = await articleApi.getFavoriteStatus(article.id)
            if (favResponse.code === 200) {
              favoriteStatus.value.set(article.id, favResponse.data)
            }
          } catch (error) {
            favoriteStatus.value.set(article.id, false)
          }
        }
      }
    }
  } catch (error) {
    console.error('加载文章失败:', error)
  } finally {
    loading.value = false
  }
}

const loadHotArticles = async () => {
  try {
    const response = await articleApi.getHotArticles(5)
    if (response.code === 200) {
      hotArticles.value = response.data
    }
  } catch (error) {
    console.error('加载热门文章失败:', error)
  }
}

const loadLatestArticles = async () => {
  try {
    const response = await articleApi.getLatestArticles(5)
    if (response.code === 200) {
      latestArticles.value = response.data
    }
  } catch (error) {
    console.error('加载最新文章失败:', error)
  }
}

const selectCategory = (categoryId: number | null) => {
  selectedCategoryId.value = categoryId
  pageNum.value = 1
  loadArticles()
}

const handleSearch = () => {
  pageNum.value = 1
  loadArticles()
}

const changePage = (page: number) => {
  pageNum.value = page
  loadArticles()
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
  loadArticles()
  loadHotArticles()
  loadLatestArticles()
})
</script>

<style scoped>
.articles-page {
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

.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 2rem;
}

.main-content {
  min-width: 0;
}

.search-bar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-bar input {
  flex: 1;
  padding: 0.85rem 1.25rem;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 1rem;
  outline: none;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.search-bar input:focus {
  border-color: #4CAF50;
  background: white;
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.1);
}

.search-bar button {
  padding: 0.85rem 2rem;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.search-bar button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.search-bar button:active {
  transform: translateY(0);
}

.category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-bottom: 1.5rem;
  padding: 1.25rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.category-btn {
  padding: 0.6rem 1.25rem;
  background: #f8fafc;
  color: #64748b;
  border: 2px solid #e2e8f0;
  border-radius: 25px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-btn:hover {
  background: #f0fdf4;
  border-color: #4CAF50;
  color: #4CAF50;
  transform: translateY(-1px);
}

.category-btn.active {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  border-color: #4CAF50;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.loading,
.empty {
  text-align: center;
  padding: 4rem;
  color: #94a3b8;
  font-size: 1.1rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.article-card:hover {
  border-color: #4CAF50;
  box-shadow: 0 8px 24px rgba(76, 175, 80, 0.15);
  transform: translateY(-4px);
}

.top-badge {
  position: absolute;
  top: 1.25rem;
  right: 1.25rem;
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  color: white;
  padding: 0.4rem 1rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(255, 152, 0, 0.3);
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

.favorite-btn {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.8rem;
  background: #f8fafc;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.85rem;
  font-weight: 500;
  color: #64748b;
}

.favorite-btn:hover {
  background: #fff9e6;
  border-color: #ffd700;
  color: #ffd700;
  transform: translateY(-1px);
}

.favorite-btn.active {
  background: #fff9e6;
  border-color: #ffd700;
  color: #ffd700;
}

.favorite-btn .icon {
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

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sidebar-section {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.sidebar-section:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.sidebar-section h3 {
  font-size: 1.2rem;
  color: #1e293b;
  margin: 0 0 1.25rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 3px solid #4CAF50;
  font-weight: 700;
}

.hot-articles,
.latest-articles {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.hot-article-item,
.latest-article-item {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.85rem;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.hot-article-item:hover,
.latest-article-item:hover {
  background: #f0fdf4;
  border-color: #4CAF50;
  transform: translateX(4px);
}

.hot-rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  border-radius: 50%;
  font-size: 0.9rem;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.hot-article-item:nth-child(2) .hot-rank {
  background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%);
  box-shadow: 0 2px 8px rgba(255, 152, 0, 0.3);
}

.hot-article-item:nth-child(3) .hot-rank {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.3);
}

.hot-title,
.latest-title {
  flex: 1;
  color: #334155;
  font-size: 0.9rem;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.latest-time {
  color: #94a3b8;
  font-size: 0.8rem;
  font-weight: 500;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: -1;
  }

  .sidebar-section {
    display: none;
  }
}

@media (max-width: 640px) {
  .articles-page {
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

  .category-filter {
    gap: 0.4rem;
  }

  .category-btn {
    padding: 0.5rem 1rem;
    font-size: 0.85rem;
  }

  .search-bar {
    flex-direction: column;
  }

  .search-bar button {
    width: 100%;
  }
}
</style>
