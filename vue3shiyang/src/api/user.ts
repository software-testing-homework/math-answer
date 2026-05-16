import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      const isLoginRequest = error.config?.url?.includes('/user/login')
      if (!isLoginRequest) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
}

export interface UpdateProfileRequest {
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  bio?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface UserResponse {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  bio: string
  role: number
  token: string
}

export const userApi = {
  login(data: LoginRequest) {
    return api.post<any, { code: number; message: string; data: UserResponse }>('/user/login', data)
  },

  register(data: RegisterRequest) {
    return api.post<any, { code: number; message: string; data: UserResponse }>('/user/register', data)
  },

  getProfile() {
    return api.get<any, { code: number; message: string; data: any }>('/user/profile')
  },

  updateProfile(data: UpdateProfileRequest) {
    return api.put<any, { code: number; message: string; data: any }>('/user/profile', data)
  },

  changePassword(data: ChangePasswordRequest) {
    return api.put<any, { code: number; message: string; data: any }>('/user/password', data)
  }
}

export default api
