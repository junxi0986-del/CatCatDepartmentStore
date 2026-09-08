<template>
  <div class="pay">
    <h2>订单支付</h2>
    <div class="pay-container">
      <div class="order-info">
        <h3>订单信息</h3>
        <p><strong>订单编号:</strong> {{ order.orderNo }}</p>
        <p><strong>订单金额:</strong> ¥{{ order.totalPrice }}</p>
        <p><strong>收货地址:</strong> 
          <span v-if="order.receiver && order.receiverPhone && order.receiverAddress">{{ order.receiver }} {{ order.receiverPhone }} {{ order.receiverAddress }}</span>
          <span v-else class="no-address">暂无收货地址</span>
          <el-button type="text" @click="goToAddress">修改地址</el-button>
        </p>
      </div>
      <div class="coupon-section">
        <h3>选择优惠券</h3>
        <div class="coupon-list">
          <div v-for="coupon in userCoupons" :key="coupon.id" 
               :class="['coupon-item', { active: selectedCoupon && selectedCoupon.id === coupon.id }]"
               @click="handleCouponSelect(coupon)">
            <div class="coupon-value">¥{{ coupon.discount }}</div>
            <div class="coupon-info">
              <div class="coupon-name">{{ coupon.coupon_name }}</div>
              <div class="coupon-condition">满{{ coupon.minAmount }}元可用</div>
              <div class="coupon-time">有效期：{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</div>
            </div>
            <div class="coupon-select" v-if="selectedCoupon && selectedCoupon.id === coupon.id">✓</div>
          </div>
          <div v-if="userCoupons.length === 0" class="no-coupon">
            <p>暂无可用优惠券</p>
          </div>
          <div class="coupon-item" :class="{ active: !selectedCoupon }" @click="handleCouponSelect(null)">
            <div class="coupon-value">不使用</div>
            <div class="coupon-info">
              <div class="coupon-name">不使用优惠券</div>
            </div>
            <div class="coupon-select" v-if="!selectedCoupon">✓</div>
          </div>
        </div>
      </div>
      <div class="product-list">
        <h3>商品信息</h3>
        <div v-for="(item, index) in order.items" :key="index" class="product-item">
          <div class="product-image">
            <img :src="formatImageUrl(item.productPic)" :alt="item.productName" />
          </div>
          <div class="product-info">
            <div class="product-name">{{ item.productName }}</div>
            <div class="product-spec" v-if="item.specInfo">{{ item.specInfo }}</div>
            <div class="product-price">¥{{ item.productPrice }}</div>
          </div>
          <div class="product-quantity">x{{ item.quantity }}</div>
          <div class="product-total">¥{{ item.totalPrice }}</div>
        </div>
        <div class="price-summary">
          <div class="price-row">
            <span class="price-label">商品总计:</span>
            <span class="price-value">¥{{ order.totalPrice }}</span>
          </div>
          <div class="price-row" v-if="selectedCoupon">
            <span class="price-label">优惠券优惠:</span>
            <span class="price-value coupon-discount">-¥{{ selectedCoupon.discount || selectedCoupon.discountAmount }}</span>
          </div>
          <div class="price-row total">
            <span class="price-label">实付金额:</span>
            <span class="price-value final-price">¥{{ order.payPrice }}</span>
          </div>
        </div>
      </div>
      <div class="pay-method">
        <h3>支付方式</h3>
        <div class="pay-method-options">
          <div 
            v-for="method in payMethods" 
            :key="method.value"
            :class="['pay-method-item', { active: payMethod === method.value }]"
            @click="payMethod = method.value"
          >
            <img class="method-icon" :src="method.icon" :alt="method.name">
            <span class="method-name">{{ method.name }}</span>
          </div>
        </div>
      </div>
      <div class="pay-action">
        <el-button type="primary" @click="showPayDialog">提交订单</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog
      v-model="payDialogVisible"
      title="订单支付"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="handlePayDialogClose"
    >
      <div class="pay-dialog-content">
        <div class="pay-amount">
          <p>订单金额</p>
          <p class="amount">¥{{ order.payPrice }}</p>
        </div>

        <div class="qrcode-section">
          <p class="qrcode-title">请使用{{ selectedPayMethodLabel }}扫码支付</p>
          <div class="qrcode-wrapper">
            <QRCodeVue 
              v-if="qrValue"
              :value="qrValue" 
              :size="200"
              :margin="10"
            />
          </div>
        </div>

        <div class="confirm-section" v-if="showConfirmButton">
          <el-button type="primary" size="large" @click="confirmPay" :loading="paying">
            确认支付
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOrder, payOrder, cancelOrder, getOrderDetail } from '../../api/order'
import { getAddressList } from '../../api/address'
import { getUserCoupons } from '../../api/user'
import { recordBehavior } from '../../api/behavior'
import QRCodeVue from 'qrcode.vue'

export default {
  components: {
    QRCodeVue
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const payMethod = ref('wechat')
    const selectedCoupon = ref(null)
    const userCoupons = ref([])
    const order = ref({
      orderNo: '',
      totalPrice: 0,
      payPrice: 0,
      receiver: '',
      receiverPhone: '',
      receiverAddress: '',
      items: [],
      id: null
    })

    const payDialogVisible = ref(false)
    const showConfirmButton = ref(false)
    const paying = ref(false)
    const isPaid = ref(false)
    let countdownTimer = null

    const payMethods = [
      { value: 'wechat', name: '微信支付', icon: '/api/couponimg/wexinpay.png' },
      { value: 'alipay', name: '支付宝', icon: '/api/couponimg/alipay.png' }
    ]

    const selectedPayMethodLabel = computed(() => {
      const method = payMethods.find(m => m.value === payMethod.value)
      return method ? method.name : ''
    })

    const qrValue = computed(() => {
      if (!payMethod.value || !order.value.orderNo) return ''
      return `pay://${payMethod.value}/${order.value.orderNo}/${order.value.payPrice}`
    })

    onMounted(async () => {
      const urlOrderId = route.query.orderId
      
      if (urlOrderId) {
        try {
          const res = await getOrderDetail(urlOrderId)
          if (res.code === 200 && res.data) {
            const orderData = res.data
            order.value = {
              id: orderData.id,
              orderNo: orderData.orderNo,
              totalPrice: orderData.totalPrice,
              payPrice: orderData.payPrice,
              receiver: orderData.receiver,
              receiverPhone: orderData.receiverPhone,
              receiverAddress: orderData.receiverAddress,
              items: orderData.items || []
            }
          } else {
            ElMessage.error('订单不存在或已被删除')
            router.push('/order')
            return
          }
        } catch (error) {
          console.error('获取订单详情失败', error)
          ElMessage.error('获取订单详情失败')
          router.push('/order')
          return
        }
      } else {
        const orderInfo = localStorage.getItem('orderInfo')
        if (orderInfo) {
          order.value = JSON.parse(orderInfo)
          order.value.orderNo = 'ORDER' + Math.random().toString(36).substring(2, 10).toUpperCase()
          order.value.payPrice = order.value.totalPrice
          
          if (!order.value.receiver || !order.value.receiverPhone || !order.value.receiverAddress) {
            try {
              const res = await getAddressList()
              if (res.code === 200 && res.data && res.data.length > 0) {
                const defaultAddress = res.data.find(item => item.isDefault === 1) || res.data[0]
                order.value.receiver = defaultAddress.receiver
                order.value.receiverPhone = defaultAddress.phone
                order.value.receiverAddress = defaultAddress.province + defaultAddress.city + defaultAddress.area + defaultAddress.detailAddress
              }
            } catch (error) {
              console.error('获取地址列表失败', error)
            }
          }
        } else {
          ElMessage.error('订单信息不存在')
          router.push('/cart')
          return
        }
      }
      
      try {
        const res = await getUserCoupons()
        if (res.code === 200) {
          const allCoupons = res.data || []
          const now = Date.now()
          userCoupons.value = allCoupons.filter(coupon => {
            const isUnused = coupon.status === 0
            const endTime = coupon.endTime ? new Date(coupon.endTime).getTime() : now + 1
            const isNotExpired = endTime > now
            return isUnused && isNotExpired
          })
        }
      } catch (error) {
        console.error('获取用户优惠券失败', error)
      }
    })

    onUnmounted(() => {
      if (countdownTimer) {
        clearInterval(countdownTimer)
      }
    })

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

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    }

    const handleCouponSelect = (coupon) => {
      selectedCoupon.value = coupon
      if (coupon) {
        const discount = coupon.discount || coupon.discountAmount || 0
        const minAmount = coupon.minAmount || coupon.min_price || 0
        if (order.value.totalPrice >= minAmount && discount > 0) {
          order.value.payPrice = order.value.totalPrice - discount
        } else {
          order.value.payPrice = order.value.totalPrice
          selectedCoupon.value = null
          ElMessage.warning('订单金额不满足优惠券使用条件')
        }
      } else {
        order.value.payPrice = order.value.totalPrice
      }
    }

    const showPayDialog = async () => {
      try {
        if (!order.value.id) {
          const createRes = await createOrder({
            totalPrice: order.value.totalPrice,
            items: order.value.items,
            receiver: order.value.receiver,
            receiverPhone: order.value.receiverPhone,
            receiverAddress: order.value.receiverAddress,
            couponId: selectedCoupon.value ? selectedCoupon.value.id : null
          })
          
          if (createRes.code !== 200) {
            ElMessage.error(createRes.message || '订单创建失败')
            return
          }
          order.value.id = createRes.data.orderId
        }
        
        payDialogVisible.value = true
        showConfirmButton.value = false
        isPaid.value = false
        
        if (countdownTimer) {
          clearInterval(countdownTimer)
        }
        
        countdownTimer = setInterval(() => {
          if (!showConfirmButton.value) {
            clearInterval(countdownTimer)
            showConfirmButton.value = true
          }
        }, 5000)
      } catch (error) {
        console.error('创建订单失败', error)
        ElMessage.error('创建订单失败，请稍后重试')
      }
    }

    const handlePayDialogClose = async (done) => {
      if (countdownTimer) {
        clearInterval(countdownTimer)
      }
      
      try {
        await ElMessageBox.confirm('确认取消支付吗？取消后订单将保持待支付状态', '取消支付', {
          confirmButtonText: '确认取消',
          cancelButtonText: '继续支付',
          type: 'warning'
        })
        done()
        ElMessage.info('订单保持待支付状态，您可以在订单详情页继续支付')
        router.push(`/order/${order.value.id}`)
      } catch {
        showPayDialog()
      }
    }

    const confirmPay = async () => {
      paying.value = true
      try {
        if (!order.value.id) {
          const createRes = await createOrder({
            totalPrice: order.value.totalPrice,
            items: order.value.items,
            receiver: order.value.receiver,
            receiverPhone: order.value.receiverPhone,
            receiverAddress: order.value.receiverAddress,
            couponId: selectedCoupon.value ? selectedCoupon.value.id : null
          })
          
          if (createRes.code !== 200) {
            ElMessage.error(createRes.message || '订单提交失败')
            paying.value = false
            return
          }
          order.value.id = createRes.data.orderId
        }
        
        const payRes = await payOrder({
          orderId: order.value.id
        })
        
        if (payRes.code === 200) {
          isPaid.value = true
          if (countdownTimer) {
            clearInterval(countdownTimer)
          }

          const userStr = localStorage.getItem('user')
          if (userStr && order.value.items) {
            const user = JSON.parse(userStr)
            order.value.items.forEach(item => {
              recordBehavior({
                userId: user.id,
                productId: item.productId,
                behaviorType: 5,
                stayTime: 0
              }).catch(() => {})
            })
          }

          ElMessage.success('支付成功')
          payDialogVisible.value = false
          localStorage.removeItem('orderInfo')
          router.push(`/order/${order.value.id}`)
        } else {
          ElMessage.error(payRes.message || '支付失败')
        }
      } catch (error) {
        console.error('支付失败', error)
        ElMessage.error('支付失败，请稍后重试')
      } finally {
        paying.value = false
      }
    }

    const cancel = () => {
      router.push('/cart')
    }

    const goToAddress = () => {
      router.push('/address')
    }

    return {
      payMethod,
      payMethods,
      order,
      userCoupons,
      selectedCoupon,
      payDialogVisible,
      selectedPayMethodLabel,
      qrValue,
      showConfirmButton,
      paying,
      showPayDialog,
      confirmPay,
      cancel,
      formatImageUrl,
      goToAddress,
      handleCouponSelect,
      formatDate
    }
  }
}
</script>

<style scoped>
.pay {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.pay h2 {
  margin-bottom: 30px;
}

.pay-container {
  background-color: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.order-info {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.order-info h3 {
  margin-bottom: 15px;
  color: #333;
}

.pay-method {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.pay-method h3 {
  margin-bottom: 15px;
  color: #333;
}

.pay-method-options {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.pay-method-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 30px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  width: 120px;
}

.pay-method-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.pay-method-item.active {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.method-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 10px;
}

.method-name {
  font-size: 14px;
  color: #333;
}

.product-list {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.product-list h3 {
  margin-bottom: 15px;
  color: #333;
}

.product-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.product-item:last-child {
  border-bottom: none;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 15px;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.product-spec {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 5px;
  display: inline-block;
}

.product-price {
  font-size: 14px;
  color: #666;
}

.product-quantity {
  width: 80px;
  text-align: center;
}

.product-total {
  width: 100px;
  text-align: right;
  font-weight: bold;
  color: #ff6b6b;
}

.price-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px dashed #eee;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
}

.price-row.total {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid #ff6b6b;
  font-size: 18px;
  font-weight: bold;
}

.price-label {
  color: #666;
}

.price-row.total .price-label {
  color: #333;
  font-size: 16px;
}

.price-value {
  color: #333;
  font-weight: 500;
}

.coupon-discount {
  color: #ff6b6b;
  font-weight: bold;
}

.final-price {
  color: #ff6b6b;
  font-size: 22px;
}

.no-address {
  color: #ff4d4f;
  margin-right: 10px;
}

.coupon-section {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.coupon-section h3 {
  margin-bottom: 15px;
  color: #333;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coupon-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.coupon-item:hover {
  border-color: #ff6b6b;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.coupon-item.active {
  border-color: #ff6b6b;
  background-color: #fff5f5;
}

.coupon-value {
  width: 100px;
  font-size: 20px;
  font-weight: bold;
  color: #ff6b6b;
  text-align: center;
}

.coupon-info {
  flex: 1;
  margin-left: 20px;
}

.coupon-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.coupon-condition {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.coupon-time {
  font-size: 12px;
  color: #999;
}

.coupon-select {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background-color: #ff6b6b;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.no-coupon {
  text-align: center;
  padding: 20px;
  color: #999;
  border: 1px dashed #e0e0e0;
  border-radius: 8px;
}

.pay-action {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pay-dialog-content {
  text-align: center;
}

.pay-amount {
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.pay-amount p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.pay-amount .amount {
  font-size: 32px;
  font-weight: bold;
  color: #ff6b6b;
  margin-top: 10px;
}

.qrcode-section {
  padding: 15px 0 10px 0;
}

.qrcode-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
}

.qrcode-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
}

.confirm-section {
  padding: 10px 0;
}

.confirm-section .el-button {
  width: 100%;
}
</style>
