<template>
  <header :class="{ 'header-fixed': isScrolled }">
    <!-- 主导航栏 -->
    <div class="main-nav">
      <div class="container">
        <div class="logo">
          <a href="/">猫猫百货商城</a>
        </div>

        <!-- 搜索框 -->
        <div class="search-box">
          <input type="text" v-model="searchKeyword" :placeholder="currentRecommend" @keydown.enter="search" />
          <button @click="search">搜索</button>
          <button @click="openAISearch" class="ai-btn">AI</button>
        </div>

        <!-- 购物车 -->
        <div class="cart" :class="{ 'hidden-section': isScrolled }">
          <a href="/cart" class="cart-link" @click.prevent="goToCart">
            <span class="cart-icon">🛒</span>
            <span class="cart-text">购物车</span>
          </a>
        </div>

        <!-- 用户区域 -->
        <div class="user" :class="{ 'hidden-section': isScrolled }" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
          <span v-if="!isLogin" class="login-links">
            <a href="/user/login">登录</a>
            <a href="/user/register">注册</a>
          </span>
          <span v-else class="username">
            <img 
              v-if="userInfo.avatar" 
              :src="userInfo.avatar" 
              class="user-avatar"
              @click.stop="goToUserCenter"
              title="点击进入个人中心"
            />
            <div v-else class="user-avatar default-avatar" @click.stop="goToUserCenter" title="点击进入个人中心">
              {{ userInfo.username ? userInfo.username.charAt(0).toUpperCase() : 'U' }}
            </div>
            {{ userInfo.username }}
            <div v-show="showDropdown" class="dropdown-menu">
              <a href="/user" class="dropdown-item" @click.prevent="goToUserCenter">我的</a>
              <a href="/user/order" class="dropdown-item" @click.prevent="goToOrder">我的订单</a>
              <a href="/user/address" class="dropdown-item" @click.prevent="goToAddress">收货地址</a>
              <div class="dropdown-divider"></div>
              <a href="/" class="dropdown-item logout" @click.prevent="logout">退出登录</a>
            </div>
          </span>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
import { ElMessageBox } from 'element-plus'
import { avatarManager } from '../utils/avatarManager'
import { recordBehavior } from '../api/behavior'
import { getProductList } from '../api/product'

export default {
  data() {
    return {
      isLogin: false,
      userInfo: {},
      searchKeyword: '',
      showDropdown: false,
      lastUserUpdate: null,
      checkUserUpdateInterval: null,
      recommendProducts: [],
      currentRecommend: 'AI推荐: 加载中...',
      recommendIndex: 0,
      recommendTimer: null,
      isScrolled: false
    }
  },
  mounted() {
    this.loadUserInfo()
    this.loadRecommendProducts()

    window.addEventListener('scroll', this.handleScroll)

    window.addEventListener('userLoggedIn', (event) => {
      this.isLogin = true
      this.userInfo = event.detail
      const avatar = avatarManager.getAvatar(this.userInfo.id)
      if (avatar) {
        this.userInfo.avatar = avatar
      }
    })

    this.unwatchAvatar = avatarManager.watch(() => {
      this.loadUserInfo()
    }, 500)
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
    window.removeEventListener('userLoggedIn', (event) => {
      this.isLogin = true
      this.userInfo = event.detail
    })
    if (this.unwatchAvatar) {
      this.unwatchAvatar()
    }
    if (this.recommendTimer) {
      clearInterval(this.recommendTimer)
    }
  },
  methods: {
    handleScroll() {
      this.isScrolled = window.scrollY > 150
    },
    async loadRecommendProducts() {
      try {
        const res = await getProductList({})
        const products = res.data.list || res.data || []
        if (products.length > 0) {
          this.recommendProducts = products.slice(0, 10)
          this.updateRecommendPlaceholder()
          this.startRecommendRotation()
        } else {
          this.currentRecommend = '搜索您想要的商品'
        }
      } catch (error) {
        console.error('加载推荐商品失败', error)
        this.currentRecommend = '请输入商品名称'
      }
    },
    updateRecommendPlaceholder() {
      if (this.recommendProducts.length === 0) return
      const product = this.recommendProducts[this.recommendIndex]
      this.currentRecommend = product.name
    },
    startRecommendRotation() {
      if (this.recommendTimer) {
        clearInterval(this.recommendTimer)
      }
      this.recommendTimer = setInterval(() => {
        this.recommendIndex = (this.recommendIndex + 1) % this.recommendProducts.length
        this.updateRecommendPlaceholder()
      }, 20000)
    },
    loadUserInfo() {
      this.isLogin = localStorage.getItem('token') !== null
      if (this.isLogin) {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          const avatar = avatarManager.getAvatar(user.id)
          user.avatar = avatar || user.avatar || ''
          this.userInfo = user
        }
      }
    },
    isValidAvatar(url) {
      if (!url) return false
      if (url.includes('test.com')) return false
      return true
    },
    formatAvatarUrl(avatar) {
      if (!avatar || avatar === '') return ''
      if (avatar.startsWith('http')) {
        return avatar
      }
      if (avatar.startsWith('/')) {
        return avatar
      }
      return `/api/${avatar}`
    },
    search() {
      let keyword = this.searchKeyword
      if (!keyword && this.recommendProducts.length > 0) {
        keyword = this.recommendProducts[this.recommendIndex].name
      }
      if (keyword) {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          recordBehavior({
            userId: user.id,
            productId: null,
            behaviorType: 2,
            stayTime: 0
          }).catch(() => {})
        }
        this.$router.push({ path: '/product', query: { keyword: keyword } })
      }
    },
    openAISearch() {
      this.$router.push('/ai-search')
    },
    goToCart() {
      if (this.isLogin) {
        this.$router.push('/cart')
      } else {
        localStorage.setItem('redirectTo', '/cart')
        this.$router.push('/user/login')
      }
    },
    goToUserCenter() {
      if (this.isLogin) {
        this.$router.push('/user')
      } else {
        localStorage.setItem('redirectTo', '/user')
        this.$router.push('/user/login')
      }
      this.showDropdown = false
    },
    goToOrder() {
      if (this.isLogin) {
        this.$router.push('/user/order')
      } else {
        localStorage.setItem('redirectTo', '/user/order')
        this.$router.push('/user/login')
      }
      this.showDropdown = false
    },
    goToAddress() {
      if (this.isLogin) {
        this.$router.push('/user/address')
      } else {
        localStorage.setItem('redirectTo', '/user/address')
        this.$router.push('/user/login')
      }
      this.showDropdown = false
    },
    logout() {
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        this.isLogin = false
        this.userInfo = {}
        this.showDropdown = false
        this.$router.push('/')
      }).catch(() => {
        this.showDropdown = false
      })
    }
  }
}
</script>

<style scoped>
header {
  position: relative;
  z-index: 1000;
  background: #c62523;
}

header.header-fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

/* 主导航栏 */
.main-nav {
  background: #c62523;
  color: #fff;
  padding: 15px 0;
}

.container {
  width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo a {
  color: #fff;
  text-decoration: none;
  font-size: 28px;
  font-weight: bold;
}

/* 搜索框 */
.search-box {
  display: flex;
  flex: 1;
  max-width: 500px;
  margin: 0 50px;
}

.search-box input {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
}

.search-box button {
  background: #333;
  color: #fff;
  border: none;
  padding: 0 20px;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
}

.search-box button:hover {
  background: #555;
}

.search-box .ai-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-width: 0px;
  border-style: solid;
  border-color: #000000;
  padding: 0 15px;
  border-radius: 5px;
  cursor: pointer;
  margin-left: 5px;
  font-weight: bold;
  transition: all 0.3s ease;
  line-height: 0px;
  letter-spacing: 0px;
}

.search-box .ai-btn:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  transform: scale(1.05);
}

/* 购物车 */
.cart {
  margin: 0 20px;
  min-width: 100px;
}

.cart.hidden-section {
  visibility: hidden;
  opacity: 0;
}

.cart-link {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  padding: 8px 15px;
  border-radius: 4px;
  transition: background 0.3s;
}

.cart-link:hover {
  background: rgba(255, 255, 255, 0.1);
}

.cart-icon {
  font-size: 16px;
}

/* 用户区域 */
.user {
  display: flex;
  align-items: center;
  gap: 15px;
  position: relative;
  min-width: 150px;
}

.user.hidden-section {
  visibility: hidden;
  opacity: 0;
}

.login-links a {
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  padding: 8px 15px;
  border-radius: 4px;
  transition: background 0.3s;
}

.login-links a:hover {
  background: rgba(255, 255, 255, 0.1);
}

.login-links a + a {
  margin-left: 5px;
}

.username {
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 15px;
  border-radius: 4px;
  transition: background 0.3s;
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.5);
  transition: transform 0.3s, border-color 0.3s;
}

.user-avatar:hover {
  transform: scale(1.1);
  border-color: #fff;
}

.default-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.5);
  transition: transform 0.3s, border-color 0.3s, background 0.3s;
}

.default-avatar:hover {
  transform: scale(1.1);
  border-color: #fff;
  background: rgba(255, 255, 255, 0.3);
}

.username:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 下拉菜单 */
.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
  min-width: 120px;
  z-index: 1000;
  overflow: hidden;
}

.dropdown-item {
  display: block;
  padding: 10px 15px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.3s;
}

.dropdown-item:hover {
  background: #f5f5f5;
  color: #ff4400;
}

.dropdown-divider {
  height: 1px;
  background: #eee;
  margin: 5px 0;
}

.dropdown-item.logout {
  color: #666;
}

.dropdown-item.logout:hover {
  color: #ff4400;
}
</style>
