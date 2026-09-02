import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        ws: true
      },
      '/images': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => '/api' + path
      },
      '/avatars': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => '/api' + path
      },
      '/chat_images': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => '/api' + path
      },
      '/api/couponimg': {
        target: 'http://localhost:8083',
        changeOrigin: true
      }
    }
  }
})
