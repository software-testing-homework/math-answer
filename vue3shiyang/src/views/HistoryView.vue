<template>
  <div class="history-page">
    <div class="page-header">
      <h1>问题管理</h1>
      <p class="subtitle">创建和管理您的数学问题</p>
    </div>

    <div class="tab-container">
      <button class="tab-btn" :class="{ active: currentTab === 'create' }" @click="currentTab = 'create'">
        创建问题
      </button>
      <button class="tab-btn" :class="{ active: currentTab === 'my' }" @click="currentTab = 'my'">
        我的问题
      </button>
    </div>

    <div class="form-container" v-if="currentTab === 'create'">
      <form @submit.prevent="handleSubmit" class="article-form">
        <h2 class="form-title">{{ isEditMode ? '编辑问题' : '创建问题' }}</h2>
        <div class="form-group">
          <label for="title">
            <span class="required">*</span>问题标题
          </label>
          <input id="title" v-model="formData.title" type="text" placeholder="请输入问题标题（3-100字）" maxlength="100" required>
          <span class="char-count">{{ formData.title.length }}/100</span>
        </div>

        <div class="form-group">
          <label for="category">
            <span class="required">*</span>问题分类
          </label>
          <select id="category" v-model="formData.categoryId" required>
            <option value="">请选择分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label for="summary">
            <span class="required">*</span>问题摘要
          </label>
          <textarea id="summary" v-model="formData.summary" placeholder="请简要描述您的问题（5-200字）" maxlength="200" rows="3"
            required></textarea>
          <span class="char-count">{{ formData.summary.length }}/200</span>
        </div>

        <div class="form-group">
          <label for="content">
            <span class="required">*</span>问题详情
          </label>
          <textarea id="content" v-model="formData.content" placeholder="请详细描述您的问题，可以包含数学公式、具体场景等...（至少5个字符）" rows="12"
            required></textarea>
          <span class="char-count">{{ formData.content.length }} 字</span>
        </div>

        <div class="form-group">
          <label for="tags">标签</label>
          <input id="tags" v-model="formData.tags" type="text" placeholder="请输入标签，多个标签用逗号分隔（如：代数,几何,微积分）">
          <span class="hint">多个标签用逗号分隔</span>
        </div>

        <div class="form-group">
          <label>提交问题图片</label>
          <div class="upload-area">
            <input ref="fileInput" type="file" accept="image/*" @change="handleFileChange"
              :disabled="submitting || uploading" style="display: none">
            <div v-if="!formData.coverImage" class="upload-placeholder" @click="triggerFileUpload"
              :style="{ pointerEvents: submitting || uploading ? 'none' : 'auto', opacity: submitting || uploading ? 0.5 : 1 }">
              <span class="upload-icon">📷</span>
              <span class="upload-text">点击上传问题图片</span>
              <span class="upload-hint">支持jpg、png、gif等格式，最大5MB</span>
            </div>
            <div v-else class="upload-preview">
              <img :src="formData.coverImage" alt="问题图片预览">
              <button type="button" class="remove-btn" @click="removeCover">
                ✕
              </button>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn-secondary" @click="handleCancel">
            取消
          </button>
          <button type="submit" class="btn-primary" :disabled="submitting || uploading">
            {{ submitting ? (isEditMode ? '保存中...' : '发布中...') : uploading ? '上传中...' : (isEditMode ? '保存修改' : '发布问题')
            }}
          </button>
        </div>
      </form>

      <div class="tips-section">
        <h3>发布提示</h3>
        <ul>
          <li>📝 请详细描述您的问题，包括已知条件、求解目标等</li>
          <li>🔢 如果涉及数学公式，请使用清晰的数学符号表示</li>
          <li>📸 可以添加相关图片或截图帮助说明问题</li>
          <li>🏷️ 添加合适的标签可以让更多人看到您的问题</li>
          <li>⚠️ 请勿发布与数学无关的内容</li>
        </ul>
      </div>
    </div>

    <div class="my-articles-container" v-else>
      <div class="my-articles-list">
        <div v-if="loading" class="loading">
          加载中...
        </div>
        <div v-else-if="myArticles.length === 0" class="empty">
          <p>您还没有创建任何问题</p>
          <button class="btn-primary" @click="currentTab = 'create'">创建第一个问题</button>
        </div>
        <div v-else class="article-items">
          <div v-for="article in myArticles" :key="article.id" class="article-item">
            <div class="article-info">
              <h3 class="article-title">{{ article.title }}</h3>
              <p class="article-summary">{{ article.summary }}</p>
              <div class="article-meta">
                <span class="category">{{ article.categoryName }}</span>
                <span class="time">{{ formatDate(article.publishTime) }}</span>
                <span class="views">👁 {{ article.viewCount }}</span>
                <span class="likes">👍 {{ article.likeCount }}</span>
              </div>
            </div>
            <div class="article-actions">
              <button class="btn-view" @click="viewArticle(article.id)">查看</button>
              <button class="btn-edit" @click="editArticle(article)">编辑</button>
              <button class="btn-delete" @click="deleteArticle(article.id)">删除</button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi, type ArticleCreateRequest, type ArticleUpdateRequest, type Category, type Article } from '../api/article'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const currentTab = ref<'create' | 'my'>('create')
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const formData = ref<ArticleCreateRequest>({
  title: '',
  summary: '',
  content: '',
  categoryId: 0,
  tags: '',
  coverImage: '',
  contentType: 1
})

const editingArticleId = ref<number | null>(null)
const isEditMode = ref(false)

const myArticles = ref<Article[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)

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
const submitting = ref(false)

const validateForm = (): boolean => {
  if (formData.value.title.trim().length < 3) {
    alert('标题至少需要3个字符')
    return false
  }
  if (formData.value.title.trim().length > 100) {
    alert('标题不能超过100个字符')
    return false
  }
  if (formData.value.summary.trim().length < 5) {
    alert('摘要至少需要5个字符')
    return false
  }
  if (formData.value.summary.trim().length > 200) {
    alert('摘要不能超过200个字符')
    return false
  }
  if (formData.value.content.trim().length < 5) {
    alert('内容至少需要5个字符')
    return false
  }
  if (!formData.value.categoryId) {
    alert('请选择分类')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  if (!userStore.isLoggedIn) {
    alert('请先登录')
    router.push('/login')
    return
  }

  submitting.value = true
  try {
    if (isEditMode.value && editingArticleId.value) {
      const updateData: ArticleUpdateRequest = {
        id: editingArticleId.value,
        title: formData.value.title,
        summary: formData.value.summary,
        content: formData.value.content,
        categoryId: formData.value.categoryId,
        tags: formData.value.tags,
        coverImage: formData.value.coverImage,
        contentType: formData.value.contentType
      }
      const response = await articleApi.updateArticle(updateData)
      if (response.code === 200) {
        alert('修改成功！')
        resetForm()
        currentTab.value = 'my'
        loadMyArticles()
      } else {
        alert(response.message || '修改失败')
      }
    } else {
      const response = await articleApi.createArticle(formData.value)
      if (response.code === 200) {
        alert('发布成功！')
        resetForm()
        currentTab.value = 'my'
        loadMyArticles()
      } else {
        alert(response.message || '发布失败')
      }
    }
  } catch (error: any) {
    console.error('操作失败:', error)
    alert(error.response?.data?.message || '操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  if (!file.type.startsWith('image/')) {
    alert('只能上传图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超过5MB')
    return
  }

  uploading.value = true
  try {
    const response = await articleApi.uploadCover(file)
    console.log('上传响应:', response)
    if (response.code === 200) {
      const imageUrl = response.data.url
      console.log('图片URL:', imageUrl)
      formData.value.coverImage = imageUrl.startsWith('http') ? imageUrl : `http://localhost:8081${imageUrl}`
      console.log('设置的封面图片:', formData.value.coverImage)
    } else {
      alert(response.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传失败:', error)
    alert(error.response?.data?.message || '上传失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

const removeCover = () => {
  formData.value.coverImage = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const handleCancel = () => {
  if (formData.value.title || formData.value.summary || formData.value.content) {
    if (confirm('确定要取消吗？未保存的内容将丢失。')) {
      resetForm()
      currentTab.value = 'my'
    }
  } else {
    resetForm()
    currentTab.value = 'my'
  }
}

const resetForm = () => {
  formData.value = {
    title: '',
    summary: '',
    content: '',
    categoryId: 0,
    tags: '',
    coverImage: '',
    contentType: 1
  }
  editingArticleId.value = null
  isEditMode.value = false
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const loadMyArticles = async () => {
  if (!userStore.isLoggedIn) {
    return
  }

  loading.value = true
  try {
    const response = await articleApi.getMyArticles({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    if (response.code === 200) {
      myArticles.value = response.data.list
      totalPages.value = response.data.pages
    } else {
      alert(response.message || '获取我的问题失败')
    }
  } catch (error: any) {
    console.error('获取我的问题失败:', error)
    alert(error.response?.data?.message || '获取我的问题失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const viewArticle = (id: number) => {
  router.push(`/article/${id}`)
}

const editArticle = (article: Article) => {
  editingArticleId.value = article.id
  isEditMode.value = true
  formData.value = {
    title: article.title,
    summary: article.summary,
    content: article.content,
    categoryId: article.categoryId,
    tags: article.tags,
    coverImage: article.coverImage,
    contentType: article.contentType
  }
  currentTab.value = 'create'
}

const deleteArticle = async (id: number) => {
  if (!confirm('确定要删除这个问题吗？此操作不可恢复。')) {
    return
  }

  try {
    const response = await articleApi.deleteArticle(id)
    if (response.code === 200) {
      alert('删除成功！')
      loadMyArticles()
    } else {
      alert(response.message || '删除失败')
    }
  } catch (error: any) {
    console.error('删除失败:', error)
    alert(error.response?.data?.message || '删除失败，请稍后重试')
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadMyArticles()
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
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
  loadMyArticles()
})
</script>

<style scoped>
.history-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
  padding: 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.page-header h1 {
  font-size: 2rem;
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
  font-size: 1rem;
  font-weight: 400;
}

.tab-container {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2rem;
  padding: 1rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tab-btn {
  padding: 0.75rem 2rem;
  border: 2px solid #e5e7eb;
  background: #f8fafc;
  color: #64748b;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  border-color: #4CAF50;
  color: #4CAF50;
  background: #f0fdf4;
  transform: translateY(-1px);
}

.tab-btn.active {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  border-color: #4CAF50;
  color: white;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.form-container {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 2rem;
}

.article-form {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.form-title {
  font-size: 1.5rem;
  color: #1e293b;
  margin: 0 0 1.5rem 0;
  padding-bottom: 1rem;
  border-bottom: 3px solid #4CAF50;
  font-weight: 700;
}

.form-group {
  margin-bottom: 1.5rem;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #1e293b;
  font-weight: 500;
  font-size: 0.95rem;
}

.required {
  color: #ef4444;
  margin-right: 0.25rem;
}

.form-group input[type="text"],
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 1rem;
  outline: none;
  transition: all 0.3s ease;
  font-family: inherit;
  background: #f9fafb;
}

.form-group input[type="text"]:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #4CAF50;
  background: white;
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
}

.char-count {
  position: absolute;
  right: 0;
  bottom: -1.5rem;
  font-size: 0.85rem;
  color: #94a3b8;
}

.hint {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #94a3b8;
}

.upload-area {
  position: relative;
}

.upload-placeholder {
  border: 2px dashed #e5e7eb;
  border-radius: 10px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.upload-placeholder:hover {
  border-color: #4CAF50;
  background: #f0fdf4;
}

.upload-icon {
  display: block;
  font-size: 3rem;
  margin-bottom: 0.5rem;
}

.upload-text {
  display: block;
  color: #1e293b;
  font-size: 1rem;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.upload-hint {
  display: block;
  color: #94a3b8;
  font-size: 0.85rem;
}

.upload-preview {
  position: relative;
  width: 100%;
  max-width: 200px;
}

.upload-preview img {
  width: 100%;
  height: auto;
  max-height: 150px;
  border-radius: 8px;
  display: block;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f44336;
  color: white;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.remove-btn:hover {
  background: #d32f2f;
  transform: scale(1.1);
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #f1f5f9;
}

.btn-primary,
.btn-secondary {
  flex: 1;
  padding: 0.875rem 2rem;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #45a049 0%, #3d8b40 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f8fafc;
  color: #64748b;
  border: 2px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #f1f5f9;
  border-color: #4CAF50;
  color: #4CAF50;
}

.tips-section {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  height: fit-content;
  position: sticky;
  top: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tips-section h3 {
  font-size: 1.1rem;
  color: #1e293b;
  margin: 0 0 1rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 3px solid #4CAF50;
  font-weight: 700;
}

.tips-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-section li {
  padding: 0.75rem 0;
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.6;
  border-bottom: 1px solid #f1f5f9;
}

.tips-section li:last-child {
  border-bottom: none;
}

.my-articles-container {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.my-articles-list {
  min-height: 400px;
}

.loading {
  text-align: center;
  padding: 4rem 0;
  color: #94a3b8;
  font-size: 1.1rem;
}

.empty {
  text-align: center;
  padding: 4rem 0;
  color: #94a3b8;
}

.empty p {
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
}

.article-items {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.article-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.article-item:hover {
  border-color: #4CAF50;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.15);
  transform: translateY(-2px);
  background: white;
}

.article-info {
  flex: 1;
  min-width: 0;
}

.article-title {
  font-size: 1.1rem;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
  font-weight: 600;
}

.article-summary {
  color: #64748b;
  font-size: 0.9rem;
  margin: 0 0 0.75rem 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-meta {
  display: flex;
  gap: 1rem;
  align-items: center;
  font-size: 0.85rem;
  color: #94a3b8;
}

.article-meta .category {
  color: #4CAF50;
  font-weight: 500;
}

.article-actions {
  display: flex;
  gap: 0.5rem;
  margin-left: 1rem;
}

.article-actions button {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-view {
  background: #2196F3;
  color: white;
}

.btn-view:hover {
  background: #1976D2;
  transform: translateY(-1px);
}

.btn-edit {
  background: #FF9800;
  color: white;
}

.btn-edit:hover {
  background: #F57C00;
  transform: translateY(-1px);
}

.btn-delete {
  background: #f44336;
  color: white;
}

.btn-delete:hover {
  background: #d32f2f;
  transform: translateY(-1px);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #f1f5f9;
}

.page-btn {
  padding: 0.5rem 1.5rem;
  border: 2px solid #e5e7eb;
  background: white;
  color: #64748b;
  border-radius: 10px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.page-btn:hover:not(:disabled) {
  border-color: #4CAF50;
  color: #4CAF50;
  background: #f0fdf4;
  transform: translateY(-1px);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 600;
  padding: 0.6rem 1.5rem;
  background: #f8fafc;
  border-radius: 8px;
}

@media (max-width: 1024px) {
  .form-container {
    grid-template-columns: 1fr;
  }

  .tips-section {
    position: static;
  }

  .my-articles-container {
    padding: 1.5rem;
  }
}

@media (max-width: 640px) {
  .history-page {
    padding: 1rem;
  }

  .article-form {
    padding: 1.5rem;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
  }

  .tab-container {
    flex-direction: column;
  }

  .tab-btn {
    width: 100%;
  }

  .article-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .article-actions {
    width: 100%;
    margin-left: 0;
    margin-top: 1rem;
    justify-content: flex-end;
  }

  .article-info {
    width: 100%;
  }

  .article-summary {
    white-space: normal;
    overflow: visible;
  }

  .article-meta {
    flex-wrap: wrap;
  }
}
</style>
