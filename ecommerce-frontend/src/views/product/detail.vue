<template>
  <div class="product-detail">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item><a href="/">首页</a></el-breadcrumb-item>
      <el-breadcrumb-item><a href="/product">商品列表</a></el-breadcrumb-item>
      <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
    </el-breadcrumb>
    
    <div class="product-info">
      <!-- 商品主图轮播区 -->
      <div class="product-image">
        <div class="carousel-container" @mouseenter="stopCarousel" @mouseleave="startCarousel">
          <div 
            v-for="(img, index) in imageList" 
            :key="index" 
            class="carousel-item" 
            :class="{ active: activeIndex === index }"
          >
            <img :src="formatImageUrl(img, product.id, product.categoryId)" :alt="product.name" class="carousel-img" />
          </div>
          <button class="carousel-prev" @click="prevImage">‹</button>
          <button class="carousel-next" @click="nextImage">›</button>
          <div class="carousel-indicators">
            <span 
              v-for="(img, index) in imageList" 
              :key="index" 
              :class="{ active: activeIndex === index }" 
              @click="switchImage(index)"
            ></span>
          </div>
        </div>
        <div class="thumbnail-list">
          <div 
            v-for="(img, index) in imageList" 
            :key="index"
            class="thumbnail-item"
            :class="{ active: activeIndex === index }"
            @click="switchImage(index)"
          >
            <img :src="formatImageUrl(img, product.id, product.categoryId)" :alt="product.name" />
          </div>
        </div>
      </div>
      
      <!-- 商品信息区 -->
      <div class="product-details">
        <h1 class="product-title">{{ product.name }}</h1>
        <div class="price-section">
          <div class="price-main">¥{{ currentPrice }}</div>
          <div class="price-original">¥{{ product.marketPrice }}</div>
          <div class="price-discount">
            <span class="discount-tag">立减¥{{ discountAmount }}</span>
            <span class="discount-tag">满减优惠</span>
          </div>
        </div>
        <div class="sales-info">
          <span>已售 {{ product.sales }} 件</span>
          <span class="divider">|</span>
          <span>好评率 98%</span>
        </div>
        
        <!-- 规格选择区 -->
        <div v-for="(options, specName) in groupedSpecs" :key="specName" class="spec-section">
          <div class="spec-title">{{ specName }}</div>
          <div class="spec-options">
            <div
              v-for="spec in options"
              :key="spec.id"
              class="spec-option"
              :class="{ active: selectedSpecs[specName] === spec.id, disabled: spec.stock <= 0 }"
              @click="selectSpec(specName, spec)"
            >
              <img v-if="spec.image" :src="formatImageUrl(spec.image, product.id, product.categoryId)" :alt="spec.specValue" />
              <span>{{ spec.specValue }}</span>
            </div>
          </div>
        </div>
        
        <div class="stock-info">
          库存：{{ currentStock }} 件
        </div>
        
        <div class="quantity-section">
          <span class="quantity-label">数量</span>
          <el-input-number 
            v-model="quantity" 
            :min="1" 
            :max="currentStock" 
            size="large"
          />
        </div>
        
        <!-- 核心卖点 -->
        <div class="features">
          <div class="feature-item">
            <i class="icon">🛡️</i>
            <span>全国联保</span>
          </div>
          <div class="feature-item">
            <i class="icon">📦</i>
            <span>运费险</span>
          </div>
          <div class="feature-item">
            <i class="icon">🔧</i>
            <span>质保2年</span>
          </div>
          <div class="feature-item">
            <i class="icon">🚚</i>
            <span>次日达</span>
          </div>
        </div>
        
        <!-- 操作按钮 -->
        <div class="actions">
          <el-button type="primary" size="large" class="btn-cart" @click="addToCart">
            加入购物车
          </el-button>
          <el-button type="danger" size="large" class="btn-buy" @click="buyNow">
            立即购买
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 商品详情区 -->
    <div class="product-description">
      <el-tabs>
        <el-tab-pane label="商品详情">
          <div class="detail-content" v-html="product.detail"></div>
        </el-tab-pane>
        <el-tab-pane label="参数信息">
          <div class="params-content">
            <div v-if="loadingParams" class="loading-params">
              <i class="el-icon-loading"></i> AI正在分析商品参数...
            </div>
            <div v-else>
              <div v-for="(param, index) in productParams" :key="index" class="param-item">
                <span class="param-label">{{ param.label }}</span>
                <span class="param-value">{{ param.value }}</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="用户评价">
          <div class="review-content">
            <div v-if="loadingReviews" class="loading-reviews">
              <i class="el-icon-loading"></i> 商品评价加载中...
            </div>
            <div v-else>
              <div v-for="(review, index) in productReviews" :key="index" class="review-item">
                <div class="review-user">{{ review.user }}</div>
                <div class="review-rating">{{ review.rating }}</div>
                <div class="review-content">{{ review.content }}</div>
                <div class="review-time">{{ review.time }}</div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <!-- 推荐商品 -->
    <div class="recommend">
      <h3>看了又看</h3>
      <div class="recommend-products">
        <ProductCard v-for="item in recommendProducts" :key="item.id" :product="item" />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductCard from '../../components/ProductCard.vue'
import { getProductDetail, getProductSpecs } from '../../api/product'
import { addToCart as addToCartApi } from '../../api/cart'
import { recordBehavior, updateStayTime } from '../../api/behavior'

export default {
  components: {
    ProductCard
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const product = ref({})
    const specs = ref([])
    const quantity = ref(1)
    const recommendProducts = ref([])
    const activeIndex = ref(0)
    const selectedSpecs = ref({})
    const productParams = ref([])
    const loadingParams = ref(false)
    const productReviews = ref([])
    const loadingReviews = ref(false)
    const carouselTimer = ref(null)
    const pageEnterTime = ref(null)
    const behaviorRecorded = ref(false)

    const getUserId = () => {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        return user.id
      }
      return null
    }

    // 计算当前价格和库存
    const currentPrice = computed(() => {
      let basePrice = product.value.price || 0
      let extraPrice = 0
      for (const specId of Object.values(selectedSpecs.value)) {
        const spec = specs.value.find(s => s.id === specId)
        if (spec && spec.price) {
          extraPrice += spec.price
        }
      }
      return basePrice + extraPrice
    })

    const currentStock = computed(() => {
      if (Object.keys(selectedSpecs.value).length === 0) {
        return Math.max(product.value.stock || 0, 1)
      }
      for (const specId of Object.values(selectedSpecs.value)) {
        const spec = specs.value.find(s => s.id === specId)
        if (spec && spec.stock !== undefined && spec.stock <= 0) {
          return 1
        }
      }
      return Math.max(product.value.stock || 0, 1)
    })

    // 按规格名称分组
    const groupedSpecs = computed(() => {
      const groups = {}
      specs.value.forEach(spec => {
        const name = spec.specName || '规格'
        if (!groups[name]) {
          groups[name] = []
        }
        groups[name].push(spec)
      })
      return groups
    })

    // 计算优惠金额
    const discountAmount = computed(() => {
      if (product.value.marketPrice && product.value.price) {
        return (product.value.marketPrice - product.value.price).toFixed(2)
      }
      return 0
    })

    // 图片列表
    const imageList = computed(() => {
      const list = []
      if (product.value.pic) {
        list.push(product.value.pic)
      }
      if (product.value.images) {
        list.push(...product.value.images.split(','))
      }
      return list
    })

    // 格式化图片URL
    const formatImageUrl = (url, productId, categoryId) => {
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
    }

    // 加载商品详情
    const loadProductDetail = async () => {
      try {
        const res = await getProductDetail(route.params.id)
        product.value = res.data
      } catch (error) {
        console.error('获取商品详情失败', error)
      }
    }

    // 加载商品规格
    const loadProductSpecs = async () => {
      try {
        const res = await getProductSpecs(route.params.id)
        specs.value = res.data || []
        // 初始化选中第一个规格
        if (specs.value.length > 0) {
          specs.value.forEach(spec => {
            const name = spec.specName || '规格'
            if (!selectedSpecs.value[name]) {
              selectedSpecs.value[name] = spec.id
            }
          })
        }
      } catch (error) {
        console.error('获取商品规格失败', error)
      }
    }

    // 加载推荐商品
    const loadRecommendProducts = async () => {
      try {
        const response = await fetch('http://localhost:8083/api/recommend')
        const result = await response.json()
        if (result.code === 200 && result.data) {
          recommendProducts.value = result.data
        } else {
          // 使用默认推荐商品
          recommendProducts.value = [
            { id: 1, name: '推荐商品1', price: 100, pic: 'images/p1.jpg' },
            { id: 2, name: '推荐商品2', price: 200, pic: 'images/p2.jpg' },
            { id: 3, name: '推荐商品3', price: 300, pic: 'images/p3.jpg' },
            { id: 4, name: '推荐商品4', price: 400, pic: 'images/p4.jpg' }
          ]
        }
      } catch (error) {
        console.error('加载推荐商品失败', error)
        recommendProducts.value = [
          { id: 1, name: '推荐商品1', price: 100, pic: 'images/p1.jpg' },
          { id: 2, name: '推荐商品2', price: 200, pic: 'images/p2.jpg' },
          { id: 3, name: '推荐商品3', price: 300, pic: 'images/p3.jpg' },
          { id: 4, name: '推荐商品4', price: 400, pic: 'images/p4.jpg' }
        ]
      }
    }

    // AI分析商品参数
    const analyzeProductParams = async () => {
      loadingParams.value = true
      try {
        const response = await fetch('http://localhost:8083/api/ai/analyzeProduct', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            name: product.value.name || '',
            detail: product.value.detail || ''
          })
        })
        
        const result = await response.json()
        
        if (result.code === 200 && result.data && result.data.params) {
          productParams.value = result.data.params
        } else {
          productParams.value = generateDefaultParams()
        }
      } catch (error) {
        console.error('AI分析商品参数失败', error)
        productParams.value = generateDefaultParams()
      } finally {
        loadingParams.value = false
      }
    }

    const generateDefaultParams = () => {
      const name = product.value.name || ''
      const categoryId = product.value.categoryId
      const detail = product.value.detail || ''
      
      // 提取品牌（取商品名第一个词）
      const brand = name.split(' ')[0] || name.substring(0, 4) || '未知品牌'
      
      // 根据商品名关键词判断类型
      const isPhone = /手机|phone|iphone|小米|华为|三星|oppo|vivo|荣耀/i.test(name)
      const isClothes = /衣|裤|鞋|裙|衫|外套|夹克|t恤|shirt/i.test(name)
      const isComputer = /电脑|笔记本|macbook|thinkpad|surface/i.test(name)
      const isAppliance = /冰箱|洗衣机|空调|电视|微波炉|电饭煲/i.test(name)
      const isHeadphone = /耳机|headphone|airpods|蓝牙耳机/i.test(name)
      
      if (categoryId === 1 || isPhone) {
        return [
          { label: '品牌', value: brand },
          { label: '型号', value: name },
          { label: '屏幕', value: '6.7英寸 OLED' },
          { label: '处理器', value: '高性能处理器' },
          { label: '内存', value: '8GB/12GB' },
          { label: '存储', value: '128GB/256GB/512GB' },
          { label: '电池', value: '5000mAh' }
        ]
      } else if (categoryId === 3 || isClothes) {
        return [
          { label: '品牌', value: brand },
          { label: '材质', value: '优质面料' },
          { label: '尺码', value: 'S/M/L/XL/XXL' },
          { label: '颜色', value: '多色可选' },
          { label: '风格', value: '时尚简约' },
          { label: '洗涤', value: '可机洗' }
        ]
      } else if (categoryId === 2 || isComputer) {
        return [
          { label: '品牌', value: brand },
          { label: '型号', value: name },
          { label: '屏幕', value: '15.6英寸' },
          { label: '处理器', value: 'Intel Core i5/i7' },
          { label: '内存', value: '16GB DDR4' },
          { label: '硬盘', value: '512GB SSD' },
          { label: '显卡', value: '独立显卡' }
        ]
      } else if (isAppliance) {
        return [
          { label: '品牌', value: brand },
          { label: '型号', value: name },
          { label: '功率', value: '节能环保' },
          { label: '尺寸', value: '标准尺寸' },
          { label: '产地', value: '中国' }
        ]
      } else if (isHeadphone) {
        return [
          { label: '品牌', value: brand },
          { label: '型号', value: name },
          { label: '连接方式', value: '蓝牙5.0' },
          { label: '续航', value: '30小时' },
          { label: '降噪', value: '主动降噪' }
        ]
      } else {
        // 通用参数，根据商品名生成
        return [
          { label: '品牌', value: brand },
          { label: '型号', value: name },
          { label: '产地', value: '中国' },
          { label: '材质', value: '优质材料' },
          { label: '保修', value: '一年质保' }
        ]
      }
    }

    // 加载商品评价（真实评价 + AI评价合并显示）
    const generateProductReviews = async () => {
      loadingReviews.value = true
      try {
        // 先加载真实用户评价
        const response = await fetch(`http://localhost:8083/api/review/list/${product.value.id}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        const result = await response.json()
        let allReviews = []
        
        // 处理真实评价
        if (result.code === 200 && result.data && result.data.length > 0) {
          const now = new Date()
          const realReviews = result.data.map((review, index) => {
            const date = review.createTime ? new Date(review.createTime) : new Date(now.getTime() - index * 24 * 60 * 60 * 1000)
            return {
              user: `用户***${review.userId % 100}`,
              rating: '⭐'.repeat(review.rating || 5),
              content: review.content || '用户未填写评价内容',
              time: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`,
              isReal: true
            }
          })
          allReviews = realReviews
        }
        
        // 如果真实评价少于5条，用AI评价补充
        if (allReviews.length < 5) {
          try {
            const aiResponse = await fetch('http://localhost:8083/api/ai/generateReviews', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                name: product.value.name || '',
                categoryId: product.value.categoryId
              })
            })
            
            const aiResult = await aiResponse.json()
            
            if (aiResult.code === 200 && aiResult.data && aiResult.data.reviews) {
              // 只添加需要的AI评价数量
              const neededCount = 5 - allReviews.length
              const aiReviews = aiResult.data.reviews.slice(0, neededCount).map(r => ({
                ...r,
                isReal: false
              }))
              allReviews = [...allReviews, ...aiReviews]
            }
          } catch (aiError) {
            console.error('AI生成评价失败', aiError)
          }
        }
        
        // 如果还是没有评价，使用默认评价
        if (allReviews.length === 0) {
          allReviews = generateDefaultReviews()
        }
        
        productReviews.value = allReviews
      } catch (error) {
        console.error('加载评价失败', error)
        productReviews.value = generateDefaultReviews()
      } finally {
        loadingReviews.value = false
      }
    }

    const generateDefaultReviews = () => {
      const name = product.value.name || '商品'
      const now = new Date()
      const reviews = []
      
      const reviewTemplates = [
        { rating: 5, content: `${name}质量很好，包装精美，物流也很快，非常满意这次购物体验！` },
        { rating: 5, content: `收到货了，${name}和描述一致，做工精细，性价比很高，推荐购买。` },
        { rating: 4, content: `${name}整体不错，就是发货稍微慢了点，不过商品质量没问题。` },
        { rating: 5, content: `第二次购买了，${name}一如既往的好，客服态度也很好，好评！` },
        { rating: 5, content: `非常满意！${name}超出预期，物超所值，会推荐给朋友。` }
      ]
      
      const userNames = ['用户***8', '购物达人', '老顾客', '新用户***6', '会员用户']
      
      for (let i = 0; i < Math.min(5, reviewTemplates.length); i++) {
        const date = new Date(now.getTime() - i * 24 * 60 * 60 * 1000)
        const rating = reviewTemplates[i].rating
        reviews.push({
          user: userNames[i],
          rating: '⭐'.repeat(rating),
          content: reviewTemplates[i].content,
          time: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        })
      }
      
      return reviews
    }

    // 选择规格
    const selectSpec = (specName, spec) => {
      selectedSpecs.value[specName] = spec.id
    }

    // 切换到指定图片
    const switchImage = (index) => {
      activeIndex.value = index
    }

    // 下一张
    const nextImage = () => {
      activeIndex.value = (activeIndex.value + 1) % imageList.value.length
    }

    // 上一张
    const prevImage = () => {
      activeIndex.value = (activeIndex.value - 1 + imageList.value.length) % imageList.value.length
    }

    // 开始自动轮播
    const startCarousel = () => {
      if (carouselTimer.value) return
      carouselTimer.value = setInterval(() => {
        nextImage()
      }, 3000)
    }

    // 停止自动轮播
    const stopCarousel = () => {
      if (carouselTimer.value) {
        clearInterval(carouselTimer.value)
        carouselTimer.value = null
      }
    }

    // 加入购物车
    const addToCart = async () => {
      // 检查登录状态
      const token = localStorage.getItem('token')
      if (!token) {
        localStorage.setItem('redirectTo', `/product/${product.value.id}`)
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      // 获取用户ID
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        localStorage.setItem('redirectTo', `/product/${product.value.id}`)
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      const user = JSON.parse(userStr)
      
      try {
        await addToCartApi({
          productId: product.value.id,
          quantity: quantity.value
        })
        ElMessage.success('加入购物车成功')

        const userId = getUserId()
        if (userId) {
          recordBehavior({
            userId,
            productId: product.value.id,
            behaviorType: 3,
            stayTime: 0
          }).catch(() => {})
        }
      } catch (error) {
        console.error('加入购物车失败', error)
        ElMessage.error('加入购物车失败')
      }
    }

    // 立即购买
    const buyNow = async () => {
      // 检查登录状态
      const token = localStorage.getItem('token')
      if (!token) {
        localStorage.setItem('redirectTo', `/product/${product.value.id}`)
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      // 获取用户ID
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        localStorage.setItem('redirectTo', `/product/${product.value.id}`)
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      const user = JSON.parse(userStr)
      
      // 生成规格信息字符串
      let specInfoStr = ''
      if (specs.value && specs.value.length > 0 && Object.keys(selectedSpecs.value).length > 0) {
        const specParts = []
        for (const [specName, specId] of Object.entries(selectedSpecs.value)) {
          const selectedSpec = specs.value.find(s => s.id === specId)
          if (selectedSpec) {
            specParts.push(`${specName}: ${selectedSpec.specValue}`)
          }
        }
        if (specParts.length > 0) {
          specInfoStr = specParts.join(', ')
        }
      }
      
      // 构建订单信息
      const orderInfo = {
        totalPrice: currentPrice.value * quantity.value,
        receiver: user.address || '',
        receiverPhone: user.phone || '',
        receiverAddress: user.addressDetail || '',
        items: [{
          productId: product.value.id,
          productName: product.value.name,
          productPic: product.value.pic,
          specInfo: specInfoStr,
          productPrice: currentPrice.value,
          quantity: quantity.value,
          totalPrice: currentPrice.value * quantity.value
        }]
      }
      
      // 存储订单信息到localStorage
      localStorage.setItem('orderInfo', JSON.stringify(orderInfo))
      
      // 跳转到订单支付页面
      router.push('/pay')
    }

    const sendBehaviorWithStayTime = () => {
      const userId = getUserId()
      if (!userId || behaviorRecorded.value) return
      
      const stayTime = pageEnterTime.value 
        ? Math.floor((Date.now() - pageEnterTime.value) / 1000) 
        : 0
      
      // 只有停留时长超过10秒才上报
      if (stayTime < 10) return
      
      recordBehavior({
        userId,
        productId: route.params.id,
        behaviorType: 1,
        stayTime: stayTime
      }).catch(() => {})
      
      behaviorRecorded.value = true
    }

    onMounted(async () => {
      await loadProductDetail()
      await loadProductSpecs()
      await analyzeProductParams()
      await generateProductReviews()
      await loadRecommendProducts()
      startCarousel()

      pageEnterTime.value = Date.now()
      
      window.addEventListener('beforeunload', sendBehaviorWithStayTime)
    })

    onUnmounted(() => {
      stopCarousel()
      sendBehaviorWithStayTime()
      window.removeEventListener('beforeunload', sendBehaviorWithStayTime)
    })

    return {
      product,
      specs,
      quantity,
      recommendProducts,
      activeIndex,
      selectedSpecs,
      currentPrice,
      currentStock,
      discountAmount,
      imageList,
      formatImageUrl,
      selectSpec,
      addToCart,
      buyNow,
      router,
      productParams,
      loadingParams,
      productReviews,
      loadingReviews,
      switchImage,
      prevImage,
      nextImage,
      startCarousel,
      stopCarousel,
      groupedSpecs
    }
  }
}
</script>

<style scoped>
.product-detail {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.product-info {
  display: flex;
  margin: 30px 0;
  gap: 50px;
}

.product-image {
  flex: 1;
  max-width: 500px;
}

.carousel-container {
  position: relative;
  height: 450px;
  border-radius: 8px;
  overflow: hidden;
}

.carousel-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.carousel-item.active {
  opacity: 1;
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.carousel-prev,
.carousel-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
}

.carousel-prev:hover,
.carousel-next:hover {
  background: rgba(0, 0, 0, 0.7);
}

.carousel-prev {
  left: 10px;
}

.carousel-next {
  right: 10px;
}

.carousel-indicators {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
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

.thumbnail-list {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  overflow-x: auto;
  height: 85px;
}

.thumbnail-item {
  width: 80px;
  height: 80px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s ease;
}

.thumbnail-item.active {
  border-color: #ff4400;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-details {
  flex: 1;
  min-width: 400px;
}

.product-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.product-subtitle {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
}

.price-section {
  background-color: #f5f5f5;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 4px;
}

.price-main {
  font-size: 32px;
  font-weight: bold;
  color: #ff4400;
  margin-bottom: 5px;
}

.price-original {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
  margin-bottom: 10px;
}

.price-discount {
  display: flex;
  gap: 10px;
}

.discount-tag {
  background-color: #ff4400;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.sales-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  color: #666;
  font-size: 14px;
}

.divider {
  color: #ddd;
}

.spec-section {
  margin-bottom: 20px;
}

.spec-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.spec-option:hover {
  border-color: #ff4400;
}

.spec-option.active {
  border-color: #ff4400;
  background-color: rgba(255, 68, 0, 0.1);
}

.spec-option img {
  width: 30px;
  height: 30px;
  object-fit: cover;
}

.spec-option.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.stock-info {
  margin-bottom: 20px;
  color: #666;
  font-size: 14px;
}

.quantity-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.quantity-label {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.features {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 30px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #666;
}

.feature-item .icon {
  font-size: 16px;
}

.actions {
  display: flex;
  gap: 20px;
}

.btn-cart {
  flex: 1;
  height: 48px;
  font-size: 16px;
}

.btn-buy {
  flex: 1;
  height: 48px;
  font-size: 16px;
}

.product-description {
  margin: 50px 0;
  padding-top: 30px;
  border-top: 1px solid #eee;
}

.detail-content {
  line-height: 1.8;
  color: #333;
}

.params-content {
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.param-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.param-label {
  width: 100px;
  font-weight: bold;
  color: #666;
}

.param-value {
  flex: 1;
  color: #333;
}

.loading-params {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #666;
  font-size: 14px;
}

.loading-params i {
  margin-right: 8px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.review-content {
  padding: 20px;
}

.review-item {
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.review-user {
  font-weight: bold;
  margin-bottom: 5px;
}

.review-rating {
  color: #ff9500;
  margin-bottom: 10px;
}

.review-time {
  color: #999;
  font-size: 12px;
  margin-top: 10px;
}

.loading-reviews {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #666;
  font-size: 14px;
}

.loading-reviews i {
  margin-right: 8px;
  animation: rotate 1s linear infinite;
}

.recommend {
  margin-top: 50px;
}

.recommend h3 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
}

.recommend-products {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .product-detail {
    width: 95%;
  }
  
  .product-info {
    flex-direction: column;
    align-items: center;
  }
  
  .product-image {
    max-width: 100%;
  }
  
  .product-details {
    width: 100%;
    min-width: auto;
  }
}
</style>
