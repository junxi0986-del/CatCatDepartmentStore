<template>
  <div class="cart-container">
    <h2>购物车</h2>
    
    <div v-if="cartList.length === 0" class="empty-cart">
      <p>购物车是空的，快去选购商品吧！</p>
      <router-link to="/" class="go-shopping">去购物</router-link>
    </div>
    
    <div v-else class="cart-content">
      <div class="cart-header">
        <div class="select-all">
          <el-checkbox v-model="selectAll" @change="handleSelectAll"></el-checkbox>
          <span>全选</span>
        </div>
        <div class="product-info">商品信息</div>
        <div class="product-price">单价</div>
        <div class="product-quantity">数量</div>
        <div class="product-total">小计</div>
        <div class="product-action">操作</div>
      </div>
      
      <div class="cart-list">
        <div v-for="item in cartList" :key="item.id" class="cart-item">
          <div class="select-item">
            <el-checkbox :model-value="item.selected === 1" @change="(value) => handleItemSelect(item, value)"></el-checkbox>
          </div>
          <div class="product-info">
            <div class="product-image">
              <img :src="formatImageUrl(item.productPic)" :alt="item.productName" />
            </div>
            <div class="product-name">{{ item.productName }}</div>
          </div>
          <div class="product-price">¥{{ item.productPrice }}</div>
          <div class="product-quantity">
            <el-input-number
              v-model="item.quantity"
              :min="1"
              @change="handleQuantityChange(item)"
              size="small"
            />
          </div>
          <div class="product-total">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</div>
          <div class="product-action">
            <el-button type="text" @click="handleDelete(item.id)" size="small">删除</el-button>
          </div>
        </div>
      </div>
      
      <div class="cart-footer">
        <div class="total-info">
          <span>合计：</span>
          <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
          <span>（共{{ totalQuantity }}件商品）</span>
        </div>
        <div class="action-buttons">
          <el-button @click="handleClear">清空购物车</el-button>
          <el-button type="primary" @click="handleCheckout">去结算</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCartList, updateCartQuantity, updateCartSelected, deleteCart, clearSelectedCart } from '../../api/cart'

export default {
  setup() {
    const router = useRouter()
    const cartList = ref([])
    const selectAll = ref(false)
    
    // 获取用户ID
    const getUserId = () => {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        return user.id
      }
      return null
    }
    const userId = ref(getUserId())
    
    // 加载购物车列表
    const loadCartList = async () => {
      try {
        const id = getUserId()
        if (!id) {
          ElMessage.warning('请先登录')
          router.push('/login')
          return
        }
        userId.value = id
        const res = await getCartList()
        if (res.code === 200) {
          cartList.value = res.data || []
          // 检查是否全选
          checkSelectAll()
        }
      } catch (error) {
        console.error('获取购物车列表失败', error)
      }
    }
    
    // 检查是否全选
    const checkSelectAll = () => {
      if (cartList.value.length === 0) {
        selectAll.value = false
        return
      }
      selectAll.value = cartList.value.every(item => item.selected === 1 || item.selected === true)
    }
    
    // 全选/取消全选
    const handleSelectAll = (value) => {
      selectAll.value = value
      const selected = value ? 1 : 0
      cartList.value.forEach(item => {
        item.selected = selected
      })
      // 批量更新后端
      const promises = cartList.value.map(item => updateCartSelected(item.id, selected))
      Promise.all(promises).catch(error => {
        console.error('更新选中状态失败', error)
        loadCartList()
      })
    }
    
    // 单个商品选中状态变更
    const handleItemSelect = (item, value) => {
      const selected = value ? 1 : 0
      updateCartSelected(item.id, selected).then(() => {
        item.selected = selected
        checkSelectAll()
      }).catch(error => {
        console.error('更新选中状态失败', error)
        checkSelectAll()
      })
    }
    
    // 数量变更
    const handleQuantityChange = (item) => {
      updateCartQuantity(item.id, item.quantity)
    }
    
    // 删除商品
    const handleDelete = (id) => {
      deleteCart(id).then(res => {
        if (res.code === 200) {
          cartList.value = cartList.value.filter(item => item.id !== id)
          checkSelectAll()
          ElMessage.success('删除成功')
        } else {
          ElMessage.error(res.message || '删除失败')
        }
      }).catch(error => {
        console.error('删除购物车失败', error)
        ElMessage.error('删除失败，请稍后重试')
      })
    }
    
    // 清空购物车
    const handleClear = () => {
      clearSelectedCart().then(res => {
        if (res.code === 200) {
          cartList.value = []
          selectAll.value = false
        }
      })
    }
    
    // 去结算
    const handleCheckout = () => {
      // 获取选中的商品
      const selectedItems = cartList.value.filter(item => item.selected === 1)
      if (selectedItems.length === 0) {
        ElMessage.warning('请选择要结算的商品')
        return
      }
      
      // 构建订单信息
      const orderInfo = {
        totalPrice: totalPrice.value,
        items: selectedItems.map(item => ({
          productId: item.productId,
          productName: item.productName,
          productPic: item.productPic,
          productPrice: item.productPrice,
          quantity: item.quantity,
          totalPrice: item.productPrice * item.quantity
        }))
      }
      
      // 存储订单信息到localStorage
      localStorage.setItem('orderInfo', JSON.stringify(orderInfo))
      
      // 跳转到订单支付页面
      router.push('/pay')
    }
    
    // 计算总价
    const totalPrice = computed(() => {
      return cartList.value
        .filter(item => item.selected === 1)
        .reduce((sum, item) => sum + item.productPrice * item.quantity, 0)
    })
    
    // 计算总数量
    const totalQuantity = computed(() => {
      return cartList.value
        .filter(item => item.selected === 1)
        .reduce((sum, item) => sum + item.quantity, 0)
    })
    
    // 格式化图片URL
    const formatImageUrl = (url) => {
      if (!url) return ''
      if (url.startsWith('http')) {
        if (url.includes('test.com')) {
          const fileName = url.split('/').pop()
          return `/api/images/${fileName}`
        }
        return url
      }
      return `/api/${url}`
    }
    
    onMounted(() => {
      // 检查登录状态
      const token = localStorage.getItem('token')
      if (!token) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      loadCartList()
    })
    
    return {
      cartList,
      selectAll,
      totalPrice,
      totalQuantity,
      handleSelectAll,
      handleItemSelect,
      handleQuantityChange,
      handleDelete,
      handleClear,
      handleCheckout,
      formatImageUrl
    }
  }
}
</script>

<style scoped>
.cart-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.cart-container h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.empty-cart {
  text-align: center;
  padding: 100px 0;
  background: #f9f9f9;
  border-radius: 8px;
}

.empty-cart p {
  font-size: 18px;
  color: #999;
  margin-bottom: 20px;
}

.go-shopping {
  display: inline-block;
  padding: 10px 20px;
  background: #ff6b6b;
  color: #fff;
  text-decoration: none;
  border-radius: 4px;
  transition: background 0.3s;
}

.go-shopping:hover {
  background: #ff5252;
}

.cart-content {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.cart-header {
  display: flex;
  background: #f5f5f5;
  padding: 15px 20px;
  font-weight: bold;
  border-bottom: 1px solid #e0e0e0;
}

.select-all {
  width: 100px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-info {
  flex: 1;
}

.product-price,
.product-quantity,
.product-total,
.product-action {
  width: 120px;
  text-align: center;
}

.cart-list {
  max-height: 600px;
  overflow-y: auto;
}

.cart-item {
  display: flex;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  align-items: center;
  transition: background 0.3s;
}

.cart-item:hover {
  background: #f9f9f9;
}

.select-item {
  width: 100px;
  text-align: center;
}

.product-info {
  flex: 1;
  display: flex;
  gap: 20px;
  align-items: center;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
}

.product-price,
.product-quantity,
.product-total,
.product-action {
  width: 120px;
  text-align: center;
}

.product-price {
  color: #666;
}

.product-total {
  font-weight: bold;
  color: #ff6b6b;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f9f9f9;
  border-top: 1px solid #e0e0e0;
}

.total-info {
  font-size: 16px;
}

.total-price {
  font-size: 20px;
  font-weight: bold;
  color: #ff6b6b;
  margin: 0 10px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}
</style>
