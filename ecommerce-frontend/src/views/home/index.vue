<template>
  <div class="home">
    <!-- 主内容区 - 分类导航在左，Banner在右 -->
    <div class="main-content">
      <!-- 左侧分类导航 -->
      <div class="left-sidebar">
        <div class="category-nav">
          <h3>商品分类</h3>
          <ul class="category-list">
            <li v-for="category in categories" :key="category.id" class="category-item">
              <a href="/product" @click.prevent="goToCategory(category.id)">{{ category.name }}</a>
            </li>
          </ul>
        </div>
        
        <!-- 优惠券广告栏 - 小屏幕显示在分类下方 -->
        <div class="coupon-sidebar-mobile">
          <div class="coupon-header">
            <h3>优惠券</h3>
          </div>
          <div class="coupon-list">
            <div v-for="coupon in availableCoupons" :key="coupon.id" class="coupon-item">
              <div class="coupon-info">
                <div class="coupon-value">¥{{ coupon.discountAmount }}</div>
                <div class="coupon-min">满{{ coupon.minAmount }}元可用</div>
                <div class="coupon-name">{{ coupon.name }}</div>
                <div class="coupon-time">有效期：{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</div>
              </div>
              <div class="coupon-action">
                <div v-if="userCouponIds.includes(coupon.id)" class="coupon-status">待使用</div>
                <el-button v-else type="primary" size="small" @click="handleClaimCoupon(coupon.id)">领取</el-button>
              </div>
            </div>
            <div v-if="availableCoupons.length === 0" class="no-coupon">
              <p>暂无优惠券</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧Banner和推荐商品 -->
      <div class="right-content">
        <!-- Banner轮播区 -->
        <div class="banner-carousel" @mouseenter="stopCarousel" @mouseleave="startCarousel">
          <div class="carousel-container">
            <div v-for="(banner, index) in banners" :key="index" class="carousel-item" :class="{ active: currentBanner === index }" @click="handleBannerClick(banner)">
              <img :src="formatBannerUrl(banner.imageUrl)" :alt="banner.title" />
            </div>
            <div class="carousel-indicators">
              <span v-for="(banner, index) in banners" :key="index" :class="{ active: currentBanner === index }" @click.stop="currentBanner = index"></span>
            </div>
          </div>
        </div>

        <!-- 推荐商品区 -->
        <div class="recommended-products">
          <h3>猜你喜欢</h3>
          <div class="product-grid">
            <div class="product-card" v-for="product in recommendedProducts" :key="product.id">
              <a :href="'/product/' + product.id" class="product-link">
                <div class="product-image">
                  <img :src="formatImageUrl(product.pic, product.id, product.categoryId)" :alt="product.name" />
                </div>
                <div class="product-info">
                  <h4 class="product-name">{{ product.name }}</h4>
                  <div class="product-price">¥{{ product.price }}</div>
                  <div class="product-sales">已售 {{ product.sales }} 件</div>
                </div>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 优惠券广告栏 - 大屏幕固定在右侧 -->
    <div class="coupon-sidebar-desktop">
      <div class="coupon-header">
        <h3>优惠券</h3>
      </div>
      <div class="coupon-list">
        <div v-for="coupon in availableCoupons" :key="coupon.id" class="coupon-item">
          <div class="coupon-info">
            <div class="coupon-value">¥{{ coupon.discountAmount }}</div>
            <div class="coupon-min">满{{ coupon.minAmount }}元可用</div>
            <div class="coupon-name">{{ coupon.name }}</div>
            <div class="coupon-time">有效期：{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</div>
          </div>
          <div class="coupon-action">
            <div v-if="userCouponIds.includes(coupon.id)" class="coupon-status">待使用</div>
            <el-button v-else type="primary" size="small" @click="handleClaimCoupon(coupon.id)">领取</el-button>
          </div>
        </div>
        <div v-if="availableCoupons.length === 0" class="no-coupon">
          <p>暂无优惠券</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getProductList } from '../../api/product'
import { getCouponList, claimCoupon } from '../../api/coupon'
import { getUserCoupons } from '../../api/user'
import { ElMessage } from 'element-plus'

export default {
  data() {
    return {
      // 分类数据 - 与数据库中的商品分类一致
      categories: [
        {
          id: 1,
          name: '数码产品',
          children: [
            { id: 11, name: '手机' },
            { id: 12, name: '键盘' },
            { id: 13, name: '鼠标' }
          ]
        },
        {
          id: 2,
          name: '家居用品',
          children: [
            { id: 21, name: '床上用品' },
            { id: 22, name: '枕头' },
            { id: 23, name: '保温杯' },
            { id: 24, name: '玩具' }
          ]
        },
        {
          id: 3,
          name: '服装鞋帽',
          children: [
            { id: 31, name: 'T恤' },
            { id: 32, name: '连衣裙' },
            { id: 33, name: '运动鞋' },
            { id: 34, name: '防晒衣' }
          ]
        },
        {
          id: 4,
          name: '手机配件',
          children: [
            { id: 41, name: '蓝牙耳机' },
            { id: 42, name: '钢化膜' },
            { id: 43, name: '充电宝' },
            { id: 44, name: '数据线' }
          ]
        },
        {
          id: 5,
          name: '厨房用具',
          children: [
            { id: 51, name: '电饭煲' },
            { id: 52, name: '炒锅' },
            { id: 53, name: '电热水壶' }
          ]
        }
      ],
      // Banner轮播数据
      banners: [],
      // 当前Banner索引
      currentBanner: 0,
      // 推荐商品数据
      recommendedProducts: [],
      // 优惠券数据
      coupons: [],
      // 用户已领取的优惠券ID列表
      userCouponIds: [],
      // 定时器ID
      carouselTimer: null
    }
  },
  computed: {
    
    // 只显示可使用的优惠券（状态为1）
    availableCoupons() {
      return this.coupons.filter(coupon => coupon.status === 1)
    },
    // 检查用户是否登录
    isLoggedIn() {
      return localStorage.getItem('token') !== null
    }
  },
  mounted() {
    // 获取轮播图
    this.loadBanners()
    // 获取商品列表
    this.loadProducts()
    // 获取优惠券列表
    this.loadCoupons()
    // 获取用户已领取的优惠券
    this.loadUserCoupons()
    // 开始轮播
    this.startCarousel()
  },
  beforeUnmount() {
    // 清理定时器
    this.stopCarousel()
  },
  methods: {
    // 加载轮播图
    async loadBanners() {
      try {
        const res = await fetch('http://localhost:8083/api/admin/banner/active')
        const data = await res.json()
        if (data.code === 200 && data.data && data.data.length > 0) {
          this.banners = data.data
        } else {
          this.banners = [
            { imageUrl: '/api/Bannerimg/Banner01.png', linkUrl: '' },
            { imageUrl: '/api/Bannerimg/Banner02.png', linkUrl: '' },
            { imageUrl: '/api/Bannerimg/Banner03.png', linkUrl: '' }
          ]
        }
      } catch (error) {
        console.error('获取轮播图失败', error)
        this.banners = [
          { imageUrl: '/api/Bannerimg/Banner01.png', linkUrl: '' },
          { imageUrl: '/api/Bannerimg/Banner02.png', linkUrl: '' },
          { imageUrl: '/api/Bannerimg/Banner03.png', linkUrl: '' }
        ]
      }
    },
    formatBannerUrl(url) {
      if (!url) return ''
      if (url.startsWith('http')) return url
      const baseUrl = 'http://localhost:8083/api'
      if (url.startsWith('/')) return `${baseUrl}${url}`
      return `${baseUrl}/${url}`
    },
    handleBannerClick(banner) {
      console.log('点击轮播图:', banner.id, 'linkUrl:', banner.linkUrl)
      if (!banner.linkUrl || banner.linkUrl.trim() === '') {
        console.log('没有设置跳转链接，不跳转')
        return
      }
      try {
        let url = banner.linkUrl.trim()
        if (url.startsWith('http://') || url.startsWith('https://')) {
          window.open(url, '_blank')
        } else {
          // 确保路径以 / 开头，避免相对路径问题
          if (!url.startsWith('/')) {
            url = '/' + url
          }
          this.$router.push(url)
        }
      } catch (error) {
        console.error('跳转失败:', error)
      }
    },
    // 加载商品列表
    async loadProducts() {
      try {
        const res = await getProductList()
        if (res.code === 200) {
          this.recommendedProducts = (res.data.list || res.data || []).slice(0, 6)
        }
      } catch (error) {
        console.error('获取商品列表失败', error)
      }
    },
    // 加载优惠券列表
    async loadCoupons() {
      try {
        const res = await getCouponList()
        if (res.code === 200) {
          this.coupons = res.data || []
        }
      } catch (error) {
        console.error('获取优惠券列表失败', error)
      }
    },
    // 加载用户已领取的优惠券
    async loadUserCoupons() {
      if (!this.isLoggedIn) return
      try {
        const res = await getUserCoupons()
        if (res.code === 200) {
          this.userCouponIds = (res.data || []).map(item => item.couponId)
        }
      } catch (error) {
        console.error('获取用户优惠券失败', error)
      }
    },
    // 领取优惠券
    async handleClaimCoupon(couponId) {
      console.log('领取优惠券，couponId:', couponId)
      if (!this.isLoggedIn) {
        ElMessage.warning('请先登录后再领取优惠券')
        this.$router.push('/user/login')
        return
      }
      try {
        const res = await claimCoupon(couponId)
        console.log('领取优惠券响应:', res)
        if (res.code === 200) {
          ElMessage.success('优惠券领取成功')
          // 重新加载用户优惠券列表，更新显示状态
          this.loadUserCoupons()
        } else {
          ElMessage.error(res.message || '领取失败')
        }
      } catch (error) {
        console.error('领取优惠券失败', error)
        ElMessage.error('领取优惠券失败，请稍后重试')
      }
    },
    // 格式化图片URL
    formatImageUrl(url, productId, categoryId) {
      if (!url) return ''
      if (url.startsWith('http')) {
        // 处理test.com路径，与管理后台保持一致
        if (url.includes('test.com')) {
          const fileName = url.split('/').pop()
          return `http://localhost:8083/api/images/${fileName}`
        }
        return url
      }
      // 处理相对路径
      return `http://localhost:8083/api/${url}`
    },
    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    },
    // 跳转到分类商品列表
    goToCategory(categoryId) {
      this.$router.push({ path: '/product', query: { categoryId: categoryId } })
    },
    // 开始轮播
    startCarousel() {
      if (this.banners.length === 0) return
      this.carouselTimer = setInterval(() => {
        this.currentBanner = (this.currentBanner + 1) % this.banners.length
      }, 3000)
    },
    // 停止轮播
    stopCarousel() {
      if (this.carouselTimer) {
        clearInterval(this.carouselTimer)
        this.carouselTimer = null
      }
    }
  }
}
</script>

<style scoped>
.home {
  width: 1200px;
  margin: 0 auto;
  padding: 50px 0;
  position: relative;
}

/* 主内容区布局 */
.main-content {
  display: flex;
  gap: 20px;
}

/* 左侧分类导航 */
.left-sidebar {
  width: 200px;
  flex-shrink: 0;
}

.category-nav {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
}

.category-nav h3 {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #333;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-item {
  margin-bottom: 5px;
}

.category-item a {
  display: block;
  padding: 10px 15px;
  color: #333;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.category-item a:hover {
  background: #ff4400;
  color: #fff;
}

/* 右侧内容区 */
.right-content {
  flex: 1;
}

/* Banner轮播区 */
.banner-carousel {
  position: relative;
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;
}

.carousel-container {
  position: relative;
  height: 400px;
}

.carousel-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}

.carousel-item.active {
  opacity: 1;
  cursor: pointer;
  pointer-events: auto;
}

.carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-indicators {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 10;
}

.carousel-indicators span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.carousel-indicators span.active {
  background: #fff;
  width: 30px;
  border-radius: 6px;
}

/* 推荐商品区 */
.recommended-products {
  margin-top: 20px;
}

.recommended-products h3 {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.product-link {
  display: block;
  text-decoration: none;
  color: #333;
}

.product-image {
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.product-card:hover .product-image img {
  transform: scale(1.05);
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff4400;
  margin-bottom: 5px;
}

.product-sales {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

/* 优惠券广告栏 - 大屏幕固定在右侧 */
.coupon-sidebar-desktop {
  position: fixed;
  right: 20px;
  top: 100px;
  width: 200px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

/* 优惠券广告栏 - 小屏幕显示在分类下方 */
.coupon-sidebar-mobile {
  display: none;
  margin-top: 20px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #eee;
}

/* 响应式：当页面宽度小于1700px时显示移动端优惠券栏，隐藏桌面端 */
@media (max-width: 1699px) {
  .coupon-sidebar-desktop {
    display: none;
  }
  
  .coupon-sidebar-mobile {
    display: block;
  }
  
  .coupon-sidebar-mobile .coupon-list {
    max-height: 300px;
  }
}

.coupon-header {
  margin-bottom: 15px;
}

.coupon-header h3 {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.coupon-list {
  max-height: 400px;
  overflow-y: auto;
}

.coupon-item {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.coupon-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.15);
}

.coupon-info {
  margin-bottom: 10px;
}

.coupon-value {
  font-size: 24px;
  font-weight: bold;
  color: #ff4400;
  margin-bottom: 5px;
}

.coupon-min {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.coupon-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-time {
  font-size: 12px;
  color: #999;
}

.coupon-action {
  text-align: center;
}

.coupon-status {
  display: inline-block;
  padding: 6px 12px;
  background: #f0f9eb;
  color: #67c23a;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.no-coupon {
  text-align: center;
  padding: 20px 0;
  color: #999;
}
</style>
