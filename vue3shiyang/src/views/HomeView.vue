<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { articleApi, type Article, type Category } from '@/api/article'

const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)

const categories = ref<Category[]>([
  { id: 0, name: '全部', code: 'all', parentId: 0, level: 1, sort: 0, description: '', icon: '', status: 1 },
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

const selectedCategoryId = ref<number>(0)
const hotArticles = ref<Article[]>([])
const latestArticles = ref<Article[]>([])
const loading = ref(false)

const loadArticles = async () => {
  loading.value = true
  try {
    if (selectedCategoryId.value === 0) {
      try {
        console.log('开始调用热门文章API...')
        const hotResponse = await articleApi.getHotArticles(10)
        console.log('热门文章完整响应:', hotResponse)
        console.log('热门文章code:', hotResponse?.code)

        if (hotResponse && hotResponse.code === 200) {
          hotArticles.value = hotResponse.data || []
          console.log('热门文章数据:', hotArticles.value)
        } else {
          console.error('热门文章加载失败，code:', hotResponse?.code, 'message:', hotResponse?.message)
        }
      } catch (error) {
        console.error('热门文章API调用异常:', error)
        console.error('错误类型:', typeof error)
        console.error('错误消息:', (error as any)?.message)
        console.error('错误响应:', (error as any)?.response)
        hotArticles.value = []
      }

      try {
        console.log('开始调用最新文章API...')
        const latestResponse = await articleApi.getLatestArticles(10)
        console.log('最新文章完整响应:', latestResponse)
        console.log('最新文章code:', latestResponse?.code)

        if (latestResponse && latestResponse.code === 200) {
          latestArticles.value = latestResponse.data || []
          console.log('最新文章数据:', latestArticles.value)
        } else {
          console.error('最新文章加载失败，code:', latestResponse?.code, 'message:', latestResponse?.message)
        }
      } catch (error) {
        console.error('最新文章API调用异常:', error)
        console.error('错误类型:', typeof error)
        console.error('错误消息:', (error as any)?.message)
        console.error('错误响应:', (error as any)?.response)
        latestArticles.value = []
      }
    } else {
      try {
        console.log('开始调用分类热门文章API...')
        const hotResponse = await articleApi.getHotArticlesByCategory(selectedCategoryId.value, 10)
        console.log('分类热门文章完整响应:', hotResponse)
        console.log('分类热门文章code:', hotResponse?.code)

        if (hotResponse && hotResponse.code === 200) {
          hotArticles.value = hotResponse.data || []
          console.log('分类热门文章数据:', hotArticles.value)
        } else {
          console.error('分类热门文章加载失败，code:', hotResponse?.code, 'message:', hotResponse?.message)
        }
      } catch (error) {
        console.error('分类热门文章API调用异常:', error)
        console.error('错误类型:', typeof error)
        console.error('错误消息:', (error as any)?.message)
        console.error('错误响应:', (error as any)?.response)
        hotArticles.value = []
      }

      try {
        console.log('开始调用分类最新文章API...')
        const latestResponse = await articleApi.getLatestArticlesByCategory(selectedCategoryId.value, 10)
        console.log('分类最新文章完整响应:', latestResponse)
        console.log('分类最新文章code:', latestResponse?.code)

        if (latestResponse && latestResponse.code === 200) {
          latestArticles.value = latestResponse.data || []
          console.log('分类最新文章数据:', latestArticles.value)
        } else {
          console.error('分类最新文章加载失败，code:', latestResponse?.code, 'message:', latestResponse?.message)
        }
      } catch (error) {
        console.error('分类最新文章API调用异常:', error)
        console.error('错误类型:', typeof error)
        console.error('错误消息:', (error as any)?.message)
        console.error('错误响应:', (error as any)?.response)
        latestArticles.value = []
      }
    }
  } catch (error) {
    console.error('加载文章异常:', error)
  } finally {
    loading.value = false
  }
}

watch(selectedCategoryId, () => {
  loadArticles()
})

const selectCategory = (categoryId: number) => {
  selectedCategoryId.value = categoryId
}

const goToArticle = (id: number) => {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  router.push(`/article/${id}`)
}

const goToCategory = (categoryId: number) => {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  router.push(`/articles?categoryId=${categoryId}`)
}

const goToArticles = () => {
  router.push('/articles')
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div class="home-container">
    <header class="home-header">
      <h1>数学问答系统</h1>
      <p class="subtitle">探索数学问题，分享知识，共同成长</p>
      <div v-if="!isLoggedIn" class="auth-buttons">
        <button class="login-btn" @click="router.push('/login')">登录</button>
        <button class="register-btn" @click="router.push('/register')">注册</button>
      </div>
    </header>

    <div class="category-tabs">
      <button v-for="category in categories" :key="category.id"
        :class="['category-tab', { active: selectedCategoryId === category.id }]" @click="selectCategory(category.id)">
        {{ category.name }}
      </button>
    </div>

    <div v-if="loading" class="loading">
      加载中...
    </div>

    <main v-else class="home-main">
      <div class="articles-container">
        <div class="articles-section">
          <div class="section-header">
            <h2 class="section-title">🔥 热门问题</h2>
            <button class="view-more-btn" @click="goToCategory(selectedCategoryId)">
              查看更多 →
            </button>
          </div>
          <div v-if="hotArticles.length === 0" class="empty-state">
            暂无热门问题
          </div>
          <div v-else class="article-list">
            <div v-for="article in hotArticles" :key="article.id" class="article-card" @click="goToArticle(article.id)">
              <div v-if="article.coverImage" class="article-image">
                <img :src="article.coverImage" alt="问题图片">
              </div>
              <div class="article-info">
                <h4 class="article-title">{{ article.title }}</h4>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <span class="author">{{ article.authorName }}</span>
                  <span class="views">👁 {{ article.viewCount }}</span>
                  <span class="likes">👍 {{ article.likeCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="articles-section">
          <div class="section-header">
            <h2 class="section-title">📰 最新问题</h2>
            <button class="view-more-btn" @click="goToCategory(selectedCategoryId)">
              查看更多 →
            </button>
          </div>
          <div v-if="latestArticles.length === 0" class="empty-state">
            暂无最新问题
          </div>
          <div v-else class="article-list">
            <div v-for="article in latestArticles" :key="article.id" class="article-card"
              @click="goToArticle(article.id)">
              <div v-if="article.coverImage" class="article-image">
                <img :src="article.coverImage" alt="问题图片">
              </div>
              <div class="article-info">
                <h4 class="article-title">{{ article.title }}</h4>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <span class="author">{{ article.authorName }}</span>
                  <span class="time">{{ formatTime(article.publishTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <footer class="home-footer">
      <p>&copy; 2026 数学问答系统. All rights reserved.</p>
    </footer>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  display: flex;
  flex-direction: column;
}

.home-header {
  text-align: center;
  padding: 3rem 2rem 2rem;
  background: white;
  border-bottom: 1px solid #e0e0e0;
}

.home-header h1 {
  font-size: 2.5rem;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  font-size: 1.1rem;
  color: #64748b;
  margin: 0;
}

.auth-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 1.5rem;
}

.login-btn,
.register-btn {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.login-btn {
  background: #4CAF50;
  color: white;
}

.login-btn:hover {
  background: #45a049;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.register-btn {
  background: white;
  color: #4CAF50;
  border: 2px solid #4CAF50;
}

.register-btn:hover {
  background: #f0fdf4;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 1.5rem 2rem;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  justify-content: center;
}

.category-tab {
  padding: 0.6rem 1.2rem;
  background: #f5f5f5;
  color: #64748b;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.category-tab:hover {
  background: #e8f5e9;
  border-color: #4CAF50;
  color: #4CAF50;
}

.category-tab.active {
  background: #4CAF50;
  color: white;
  border-color: #4CAF50;
  font-weight: 500;
}

.home-main {
  flex: 1;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem;
}

.loading {
  text-align: center;
  padding: 4rem;
  font-size: 1.2rem;
  color: #64748b;
}

.articles-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

.articles-section {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 300px);
  overflow: hidden;
}

.articles-section .article-list {
  overflow-y: auto;
  padding-right: 0.5rem;
}

.articles-section .article-list::-webkit-scrollbar {
  width: 6px;
}

.articles-section .article-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.articles-section .article-list::-webkit-scrollbar-thumb {
  background: #4CAF50;
  border-radius: 3px;
}

.articles-section .article-list::-webkit-scrollbar-thumb:hover {
  background: #45a049;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #4CAF50;
}

.section-title {
  font-size: 1.5rem;
  color: #1e293b;
  margin: 0;
}

.view-more-btn {
  padding: 0.6rem 1.2rem;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.view-more-btn:hover {
  background: #45a049;
  transform: translateX(3px);
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #999;
  font-size: 0.95rem;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.article-card {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #f9fafb;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.article-card:hover {
  background: #f0fdf4;
  border-color: #4CAF50;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.2);
}

.article-image {
  flex-shrink: 0;
  width: 100px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  background: #e5e7eb;
}

.article-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.article-title {
  font-size: 0.95rem;
  color: #1e293b;
  margin: 0 0 0.4rem 0;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  font-size: 0.85rem;
  color: #64748b;
  margin: 0 0 0.6rem 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  margin-top: auto;
  font-size: 0.75rem;
  color: #94a3b8;
}

.article-meta .author {
  color: #4CAF50;
  font-weight: 500;
}

.article-meta .views,
.article-meta .likes,
.article-meta .time {
  display: flex;
  align-items: center;
  gap: 0.2rem;
}

.home-footer {
  text-align: center;
  padding: 2rem;
  background: white;
  color: #64748b;
  margin-top: auto;
}

@media (max-width: 1024px) {
  .articles-container {
    grid-template-columns: 1fr;
  }

  .articles-section {
    max-height: none;
    overflow: visible;
  }

  .articles-section .article-list {
    overflow-y: visible;
  }
}

@media (max-width: 768px) {
  .home-header h1 {
    font-size: 2rem;
  }

  .category-tabs {
    padding: 1rem;
    gap: 0.3rem;
  }

  .category-tab {
    padding: 0.5rem 0.8rem;
    font-size: 0.8rem;
  }

  .section-title {
    font-size: 1.2rem;
  }

  .article-card {
    flex-direction: column;
  }

  .article-image {
    width: 100%;
    height: 160px;
  }
}
</style>
