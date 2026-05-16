<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <h2>个人信息</h2>
        <p class="subtitle">管理您的个人资料</p>
      </div>

      <form @submit.prevent="handleSubmit" class="profile-form">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar" />
            <div v-else class="avatar-placeholder">
              {{ userStore.user?.nickname?.[0] || userStore.user?.username?.[0] || 'U' }}
            </div>
            <label class="avatar-upload">
              <input type="file" accept="image/*" @change="handleAvatarChange" />
              <span>更换头像</span>
            </label>
          </div>
        </div>

        <div class="form-group">
          <label>用户名</label>
          <input type="text" :value="userStore.user?.username" disabled class="disabled-input" />
          <span class="hint">用户名不可修改</span>
        </div>

        <div class="form-group">
          <label>昵称</label>
          <input type="text" v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
        </div>

        <div class="form-group">
          <label>邮箱</label>
          <input type="email" v-model="form.email" placeholder="请输入邮箱" />
        </div>

        <div class="form-group">
          <label>手机号</label>
          <input type="tel" v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
        </div>

        <div class="form-group">
          <label>个人简介</label>
          <textarea v-model="form.bio" placeholder="介绍一下自己..." maxlength="500" rows="4"></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="save-btn" :disabled="saving">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
          <span v-if="message" :class="['message', messageType]">{{ message }}</span>
        </div>
      </form>

      <div class="password-section">
        <h3>修改密码</h3>
        <form @submit.prevent="handlePasswordSubmit" class="password-form">
          <div class="form-group">
            <label>原密码</label>
            <input type="password" v-model="passwordForm.oldPassword" placeholder="请输入原密码" />
          </div>

          <div class="form-group">
            <label>新密码</label>
            <input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码（6-20位）" />
          </div>

          <div class="form-group">
            <label>确认新密码</label>
            <input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
          </div>

          <div class="form-actions">
            <button type="submit" class="save-btn" :disabled="savingPassword">
              {{ savingPassword ? '修改中...' : '修改密码' }}
            </button>
            <span v-if="passwordMessage" :class="['message', passwordMessageType]">{{ passwordMessage }}</span>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'

const userStore = useUserStore()
const saving = ref(false)
const savingPassword = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const passwordMessage = ref('')
const passwordMessageType = ref<'success' | 'error'>('success')

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarUrl = computed(() => {
  if (!userStore.user?.avatar) return ''
  if (userStore.user.avatar.startsWith('http')) {
    return userStore.user.avatar
  }
  return 'http://localhost:8081' + userStore.user.avatar
})

onMounted(async () => {
  try {
    const res = await userApi.getProfile()
    if (res.code === 200 && res.data) {
      form.nickname = res.data.nickname || ''
      form.email = res.data.email || ''
      form.phone = res.data.phone || ''
      form.bio = res.data.bio || ''
    }
  } catch (error) {
    console.error('获取个人信息失败:', error)
  }
})

async function handleSubmit() {
  saving.value = true
  message.value = ''

  try {
    const res = await userApi.updateProfile(form)
    if (res.code === 200) {
      message.value = '保存成功'
      messageType.value = 'success'
      userStore.updateUser(form)
    } else {
      message.value = res.message || '保存失败'
      messageType.value = 'error'
    }
  } catch (error: any) {
    message.value = error.response?.data?.message || '保存失败'
    messageType.value = 'error'
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    message.value = '图片大小不能超过2MB'
    messageType.value = 'error'
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await fetch('http://localhost:8081/api/user/avatar', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: formData
    }).then(r => r.json())

    if (res.code === 200) {
      const avatarUrl = 'http://localhost:8081' + res.data.url
      const updateRes = await userApi.updateProfile({ avatar: avatarUrl })
      if (updateRes.code === 200) {
        userStore.updateUser({ avatar: avatarUrl })
        message.value = '头像上传成功'
        messageType.value = 'success'
      }
    } else {
      message.value = res.message || '头像上传失败'
      messageType.value = 'error'
    }
  } catch (error) {
    message.value = '头像上传失败'
    messageType.value = 'error'
  }

  input.value = ''
}

async function handlePasswordSubmit() {
  if (!passwordForm.oldPassword) {
    passwordMessage.value = '请输入原密码'
    passwordMessageType.value = 'error'
    return
  }
  if (!passwordForm.newPassword) {
    passwordMessage.value = '请输入新密码'
    passwordMessageType.value = 'error'
    return
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
    passwordMessage.value = '新密码长度应为6-20位'
    passwordMessageType.value = 'error'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordMessage.value = '两次输入的新密码不一致'
    passwordMessageType.value = 'error'
    return
  }

  savingPassword.value = true
  passwordMessage.value = ''

  try {
    const res = await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code === 200) {
      passwordMessage.value = '密码修改成功，请重新登录'
      passwordMessageType.value = 'success'
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      setTimeout(() => {
        userStore.logout()
        window.location.href = '/login'
      }, 2000)
    } else {
      passwordMessage.value = res.message || '密码修改失败'
      passwordMessageType.value = 'error'
    }
  } catch (error: any) {
    passwordMessage.value = error.response?.data?.message || '密码修改失败'
    passwordMessageType.value = 'error'
  } finally {
    savingPassword.value = false
  }
}
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  padding: 40px 20px;
}

.profile-card {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  border: 2px solid #e5e7eb;
}

.profile-header {
  padding: 32px 40px;
  background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
  color: #fff;
}

.profile-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.profile-header .subtitle {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.profile-form {
  padding: 32px 40px;
}

.avatar-section {
  text-align: center;
  margin-bottom: 32px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #f0f0f0;
  overflow: hidden;
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 600;
}

.avatar-upload {
  display: block;
  margin-top: 12px;
  color: #4CAF50;
  font-size: 14px;
  cursor: pointer;
}

.avatar-upload input {
  display: none;
}

.avatar-upload:hover {
  text-decoration: underline;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #1e293b;
  font-size: 14px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.3s ease;
  outline: none;
  box-sizing: border-box;
  background: #f9fafb;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #4CAF50;
  background: white;
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.1);
}

.form-group input:disabled,
.form-group textarea:disabled {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.disabled-input {
  background: #f5f5f5;
  color: #666;
}

.hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
}

.save-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message {
  font-size: 14px;
}

.message.success {
  color: #4CAF50;
}

.message.error {
  color: #f5222d;
}

.password-section {
  padding: 32px 40px;
  border-top: 2px solid #f1f5f9;
}

.password-section h3 {
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.password-form {
  max-width: 400px;
}

@media (max-width: 768px) {
  .profile-header {
    padding: 24px;
  }

  .profile-form {
    padding: 24px;
  }
}
</style>
