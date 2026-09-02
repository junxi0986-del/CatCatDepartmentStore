import axios from 'axios'
import { useRouter } from 'vue-router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('adminToken')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminInfo')
      window.location.href = '/login'
    }
    if (error.response && error.response.status === 403) {
      const msg = error.response.data?.message || '无操作权限'
      alert(msg)
    }
    return Promise.reject(error)
  }
)

export default request
