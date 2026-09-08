<template>
  <div class="order-detail">
    <div class="header">
      <el-button link @click="goBack" class="back-button">
        <el-icon><ArrowLeft /></el-icon> 返回订单列表
      </el-button>
      <h2>订单详情</h2>
    </div>
    <div v-if="loading" class="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="orderDetail" class="order-info">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>订单信息</span>
          </div>
        </template>
        <div class="order-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderDetail.orderNo }}</span>
        </div>
        <div class="order-item">
          <span class="label">订单状态：</span>
          <el-tag :type="getStatusType(orderDetail.status)">{{ getStatusText(orderDetail.status) }}</el-tag>
        </div>
        <div v-if="orderDetail.status === 0 && countdown > 0" class="order-item countdown-row">
          <span class="label">支付剩余：</span>
          <span class="countdown-time" :class="{ 'countdown-warning': countdown <= 60 }">
            {{ formatCountdown(countdown) }}
          </span>
        </div>
        <div class="order-item">
          <span class="label">订单金额：</span>
          <span class="value">{{ orderDetail.totalPrice }}</span>
        </div>
        <div class="order-item">
          <span class="label">实付金额：</span>
          <span class="value" style="color: #ff4d4f; font-weight: bold;">¥{{ orderDetail.payPrice }}</span>
        </div>
        <div class="order-item">
          <span class="label">创建时间：</span>
          <span class="value">{{ formatTime(orderDetail.createTime) }}</span>
        </div>
        <div v-if="orderDetail.payTime" class="order-item">
          <span class="label">支付时间：</span>
          <span class="value">{{ formatTime(orderDetail.payTime) }}</span>
        </div>
        <div v-if="orderDetail.deliveryTime" class="order-item">
          <span class="label">发货时间：</span>
          <span class="value">{{ formatTime(orderDetail.deliveryTime) }}</span>
        </div>
        <div v-if="orderDetail.receiveTime" class="order-item">
          <span class="label">收货时间：</span>
          <span class="value">{{ formatTime(orderDetail.receiveTime) }}</span>
        </div>
        <div v-if="orderDetail.refundStatus" class="order-item">
          <span class="label">退款状态：</span>
          <el-tag :type="getRefundStatusType(orderDetail.refundStatus)">{{ getRefundStatusText(orderDetail.refundStatus) }}</el-tag>
        </div>
        <div v-if="orderDetail.refundReason" class="order-item">
          <span class="label">退款原因：</span>
          <span class="value">{{ orderDetail.refundReason }}</span>
        </div>
        <div v-if="orderDetail.refundReply" class="order-item">
          <span class="label">退款回复：</span>
          <span class="value">{{ orderDetail.refundReply }}</span>
        </div>
      </el-card>

      <el-card class="mt-4">
        <template #header>
          <div class="card-header">
            <span>收货信息</span>
            <el-button v-if="orderDetail.status <= 2" link @click="showChangeAddressDialog" class="edit-address-button">
              <el-icon><Edit /></el-icon> 更换地址
            </el-button>
          </div>
        </template>
        <div class="order-item">
          <span class="label">收货人：</span>
          <span class="value">{{ orderDetail.receiver }} {{ orderDetail.receiverPhone }}</span>
        </div>
        <div class="order-item">
          <span class="label">收货地址：</span>
          <span class="value">{{ orderDetail.receiverAddress }}</span>
        </div>
      </el-card>

      <el-card v-if="orderDetail.status >= 3 && orderDetail.expressNo" class="mt-4">
        <template #header>
          <div class="card-header">
            <span>物流信息</span>
            <el-tag type="success">已发货</el-tag>
          </div>
        </template>
        <div class="order-item">
          <span class="label">快递公司：</span>
          <span class="value">{{ orderDetail.expressCompany || '-' }}</span>
        </div>
        <div class="order-item">
          <span class="label">运单号：</span>
          <span class="value">{{ orderDetail.expressNo || '-' }}</span>
        </div>
        <div v-if="orderDetail.logisticsInfo" class="order-item">
          <span class="label">物流状态：</span>
          <span class="value logistics-info">{{ orderDetail.logisticsInfo }}</span>
        </div>
      </el-card>

      <el-card class="mt-4">
        <template #header>
          <div class="card-header">
            <span>商品信息</span>
          </div>
        </template>
        <div v-for="item in orderDetail.items" :key="item.id" class="product-item">
          <img :src="formatImageUrl(item.productPic)" alt="商品图片" class="product-image">
          <div class="product-info">
            <div class="product-name">{{ item.productName }}</div>
            <div class="product-spec" v-if="item.specInfo">{{ item.specInfo }}</div>
            <div class="product-price">价格：{{ item.productPrice }}</div>
            <div class="product-quantity">数量：{{ item.quantity }}</div>
          </div>
        </div>
      </el-card>

      <el-card v-if="orderDetail.status === 0" class="mt-4">
        <template #header>
          <div class="card-header">
            <span>支付方式</span>
          </div>
        </template>
        <div class="pay-method-options">
          <div 
            v-for="method in payMethods" 
            :key="method.value"
            :class="['pay-method-item', { active: payMethod === method.value }]"
            @click="payMethod = method.value"
          >
            <img class="method-icon" :src="method.icon" :alt="method.name" />
            <span class="method-name">{{ method.name }}</span>
          </div>
        </div>
      </el-card>

      <div class="order-actions mt-4">
        <el-button v-if="orderDetail.status === 0" type="primary" @click="showPayDialog">支付</el-button>
        <el-button v-if="(orderDetail.status >= 1 && orderDetail.status <= 3) && (!orderDetail.refundStatus || orderDetail.refundStatus === 3)" type="warning" @click="showRefundDialog">申请退款</el-button>
        <el-button v-if="orderDetail.status === 3" type="success" @click="handleConfirmReceipt">确认收货</el-button>
        <el-button v-if="orderDetail.status === 4 && !hasReviewed" type="primary" @click="showReviewDialog">评价商品</el-button>
        <el-button v-if="orderDetail.status === 0" type="danger" @click="handleCancelOrder">取消订单</el-button>
      </div>
    </div>
    <div v-else class="error">
      <el-empty description="订单不存在" />
      <el-button type="primary" @click="goBack" class="mt-4">返回订单列表</el-button>
    </div>

    <el-dialog v-model="payDialogVisible" title="订单支付" width="500px" :close-on-click-modal="false" :close-on-press-escape="false" :before-close="handlePayDialogClose">
      <div class="pay-dialog-content">
        <div class="pay-amount">
          <p>订单金额</p>
          <p class="amount">¥{{ orderDetail?.payPrice }}</p>
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

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="500px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="收货状态" required>
          <el-radio-group v-model="refundForm.receiptStatus">
            <el-radio label="0">未收到货</el-radio>
            <el-radio label="1">已收到货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-input v-model="refundForm.refundReason" type="textarea" placeholder="请输入退款原因"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRefund">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addressDialogVisible" title="更换收货地址" width="550px">
      <div v-if="addressList.length > 0" class="address-select-section">
        <div class="address-select-label">选择已有地址：</div>
        <div 
          v-for="addr in addressList" 
          :key="addr.id" 
          :class="['address-card', { 'address-card-active': selectedAddressId === addr.id }]"
          @click="selectAddress(addr)"
        >
          <div class="address-card-header">
            <span class="address-card-name">{{ addr.receiver }}</span>
            <span class="address-card-phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault === 1" size="small" type="danger">默认</el-tag>
          </div>
          <div class="address-card-detail">{{ addr.province }}{{ addr.city }}{{ addr.area }}{{ addr.detailAddress }}</div>
        </div>
      </div>
      <div v-else class="no-address-tip">暂无收货地址，请新增收货地址</div>
      <el-button v-if="addressList.length <= 1" type="primary" link @click="showAddAddressForm" class="add-address-btn">
        <el-icon><Plus /></el-icon> 新增收货地址
      </el-button>
      <div v-if="addAddressFormVisible" class="add-address-form">
        <el-divider content-position="left">新增收货地址</el-divider>
        <el-form :model="newAddressForm" label-width="80px" size="small">
          <el-form-item label="收货人" required>
            <el-input v-model="newAddressForm.receiver" placeholder="请输入收货人姓名"></el-input>
          </el-form-item>
          <el-form-item label="手机号" required>
            <el-input v-model="newAddressForm.phone" placeholder="请输入手机号"></el-input>
          </el-form-item>
          <el-form-item label="省份" required>
            <el-input v-model="newAddressForm.province" placeholder="请输入省份"></el-input>
          </el-form-item>
          <el-form-item label="城市" required>
            <el-input v-model="newAddressForm.city" placeholder="请输入城市"></el-input>
          </el-form-item>
          <el-form-item label="区/县" required>
            <el-input v-model="newAddressForm.area" placeholder="请输入区/县"></el-input>
          </el-form-item>
          <el-form-item label="详细地址" required>
            <el-input v-model="newAddressForm.detailAddress" type="textarea" :rows="2" placeholder="请输入详细地址"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleAddAddress" :loading="addingAddress">保存地址</el-button>
            <el-button @click="addAddressFormVisible = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeAddress" :loading="updatingAddress" :disabled="!selectedAddressId">确认更换</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="商品评价" width="600px">
      <div v-for="(item, index) in reviewItems" :key="index" class="review-product-item">
        <div class="product-info-row">
          <img :src="formatImageUrl(item.productPic)" alt="商品图片" class="review-product-image">
          <div class="review-product-name">{{ item.productName }}</div>
        </div>
        <el-form label-width="80px">
          <el-form-item label="评分">
            <el-rate v-model="item.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']"></el-rate>
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="item.content" type="textarea" :rows="3" placeholder="请输入您的评价..."></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReview" :loading="submittingReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, cancelOrder as cancelOrderApi, payOrder, confirmReceipt as confirmReceiptApi, applyRefund } from '../../api/order'
import { getAddressList, addAddress } from '../../api/address'
import { ElMessage, ElMessageBox, ElEmpty, ElDialog } from 'element-plus'
import { Loading, ArrowLeft, Edit, Plus } from '@element-plus/icons-vue'
import QRCodeVue from 'qrcode.vue'

export default {
  name: 'OrderDetail',
  components: {
    Loading,
    ElEmpty,
    ArrowLeft,
    Edit,
    Plus,
    QRCodeVue
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const orderId = route.params.id
    const orderDetail = ref(null)
    const loading = ref(true)
    const refundDialogVisible = ref(false)
    const refundForm = ref({
      receiptStatus: '0',
      refundReason: ''
    })
    const reviewDialogVisible = ref(false)
    const reviewItems = ref([])
    const submittingReview = ref(false)
    const hasReviewed = ref(false)
    const countdown = ref(0)
    let timer = null
    const orderAutoCancelMinutes = ref(30)
    
    const payDialogVisible = ref(false)
    const payMethod = ref('wechat')
    const showConfirmButton = ref(false)
    const paying = ref(false)
    const payCountdown = ref(5)
    let payTimer = null
    
    const payMethods = [
      { value: 'wechat', name: '微信支付', icon: '/api/couponimg/wexinpay.png' },
      { value: 'alipay', name: '支付宝', icon: '/api/couponimg/alipay.png' }
    ]
    
    const addressDialogVisible = ref(false)
    const addressList = ref([])
    const selectedAddressId = ref(null)
    const selectedAddress = ref(null)
    const updatingAddress = ref(false)
    const addAddressFormVisible = ref(false)
    const addingAddress = ref(false)
    const newAddressForm = ref({
      receiver: '',
      phone: '',
      province: '',
      city: '',
      area: '',
      detailAddress: '',
      isDefault: 0
    })
    
    const selectedPayMethodLabel = computed(() => {
      const method = payMethods.find(m => m.value === payMethod.value)
      return method ? method.name : ''
    })
    
    const qrValue = computed(() => {
      if (!payMethod.value || !orderDetail.value?.orderNo) return ''
      return `pay://${payMethod.value}/${orderDetail.value.orderNo}/${orderDetail.value.payPrice}`
    })

    const getStatusText = (status) => {
      switch (status) {
        case 0:
          return '待支付'
        case 1:
          return '已支付'
        case 2:
          return '待发货'
        case 3:
          return '待收货'
        case 4:
          return '已完成'
        case 5:
          return '已取消'
        case 6:
          return '已退款'
        default:
          return '未知状态'
      }
    }

    const getStatusType = (status) => {
      switch (status) {
        case 0:
          return 'warning'
        case 1:
          return 'info'
        case 2:
          return 'primary'
        case 3:
          return 'success'
        case 4:
          return 'danger'
        case 5:
          return 'danger'
        case 6:
          return 'danger'
        default:
          return ''
      }
    }

    const getRefundStatusText = (status) => {
      switch (status) {
        case 1:
          return '退款申请中'
        case 2:
          return '退款成功'
        case 3:
          return '退款被拒绝'
        default:
          return ''
      }
    }

    const getRefundStatusType = (status) => {
      switch (status) {
        case 1:
          return 'warning'
        case 2:
          return 'success'
        case 3:
          return 'danger'
        default:
          return ''
      }
    }

    const showRefundDialog = () => {
      refundForm.value.receiptStatus = '0'
      refundForm.value.refundReason = ''
      refundDialogVisible.value = true
    }

    const handleRefund = async () => {
      if (!refundForm.value.refundReason) {
        ElMessage.warning('请输入退款原因')
        return
      }
      try {
        const receiptStatusText = refundForm.value.receiptStatus === '0' ? '未收到货' : '已收到货'
        const refundReason = `${receiptStatusText}：${refundForm.value.refundReason}`
        
        const res = await applyRefund({
          orderId: orderDetail.value.id,
          refundReason: refundReason
        })
        if (res.code === 200) {
          ElMessage.success('退款申请已提交')
          refundDialogVisible.value = false
          loadOrderDetail()
        } else {
          ElMessage.error(res.message || '退款申请失败')
        }
      } catch (error) {
        console.error('退款申请失败', error)
        ElMessage.error('退款申请失败，请稍后重试')
      }
    }

    const formatTime = (time) => {
      if (!time) return ''
      const date = new Date(time)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
    }

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

    const initCountdown = () => {
      if (orderDetail.value && orderDetail.value.status === 0 && orderDetail.value.createTime) {
        const now = Date.now()
        const cancelMs = orderAutoCancelMinutes.value * 60 * 1000
        const createTime = new Date(orderDetail.value.createTime).getTime()
        const remaining = createTime + cancelMs - now
        if (remaining > 0) {
          countdown.value = Math.ceil(remaining / 1000)
          startCountdownTimer()
        } else {
          countdown.value = 0
        }
      }
    }

    const startCountdownTimer = () => {
      if (timer) {
        clearInterval(timer)
      }
      timer = setInterval(() => {
        if (countdown.value > 0) {
          countdown.value--
        } else if (countdown.value === 0) {
          clearInterval(timer)
          timer = null
          handleAutoCancelOrder()
        }
      }, 1000)
    }

    const handleAutoCancelOrder = async () => {
      try {
        await cancelOrderApi(orderId)
        ElMessage.info('订单已自动取消')
        loadOrderDetail()
      } catch (error) {
        console.error('自动取消订单失败', error)
      }
    }

    const formatCountdown = (seconds) => {
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }

    const loadOrderDetail = async () => {
      try {
        const configRes = await fetch('/api/admin/system/config')
        const configData = await configRes.json()
        if (configData.code === 200 && configData.data.orderAutoCancel) {
          orderAutoCancelMinutes.value = parseInt(configData.data.orderAutoCancel) || 30
        }
      } catch (e) {
        console.error('获取配置失败', e)
      }
      
      try {
        const res = await getOrderDetail(orderId)
        if (res.code === 200) {
          orderDetail.value = res.data
          initCountdown()
        } else {
          ElMessage.error(res.message || '获取订单详情失败')
        }
      } catch (error) {
        console.error('获取订单详情失败', error)
        ElMessage.error('获取订单详情失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const goBack = () => {
      router.push('/order')
    }

    const showPayDialog = () => {
      payDialogVisible.value = true
      showConfirmButton.value = false
      payCountdown.value = 5
      
      if (payTimer) {
        clearInterval(payTimer)
      }
      
      payTimer = setInterval(() => {
        payCountdown.value--
        if (payCountdown.value <= 0) {
          clearInterval(payTimer)
          showConfirmButton.value = true
        }
      }, 1000)
    }

    const confirmPay = async () => {
      paying.value = true
      try {
        const res = await payOrder({ orderId })
        if (res.code === 200) {
          if (payTimer) {
            clearInterval(payTimer)
          }
          ElMessage.success('支付成功')
          payDialogVisible.value = false
          loadOrderDetail()
        } else {
          ElMessage.error(res.message || '支付失败')
        }
      } catch (error) {
        console.error('支付失败', error)
        ElMessage.error('支付失败，请稍后重试')
      } finally {
        paying.value = false
      }
    }

    const handlePayDialogClose = async (done) => {
      if (payTimer) {
        clearInterval(payTimer)
      }
      
      try {
        await ElMessageBox.confirm('确认取消支付吗？取消后订单将保持待支付状态', '取消支付', {
          confirmButtonText: '确认取消',
          cancelButtonText: '继续支付',
          type: 'warning'
        })
        done()
        ElMessage.info('订单保持待支付状态，您可以在订单详情页继续支付')
      } catch {
        showPayDialog()
      }
    }

    const pay = async () => {
      try {
        const res = await payOrder({ orderId })
        if (res.code === 200) {
          ElMessage.success('支付成功')
          loadOrderDetail()
        } else {
          ElMessage.error(res.message || '支付失败')
        }
      } catch (error) {
        console.error('支付失败', error)
        ElMessage.error('支付失败，请稍后重试')
      }
    }

    const confirmReceipt = async () => {
      try {
        const res = await confirmReceiptApi({ orderId })
        if (res.code === 200) {
          ElMessage.success('确认收货成功')
          loadOrderDetail()
        } else {
          ElMessage.error(res.message || '确认收货失败')
        }
      } catch (error) {
        console.error('确认收货失败', error)
        ElMessage.error('确认收货失败，请稍后重试')
      }
    }

    const cancelOrder = async () => {
      try {
        const res = await cancelOrderApi(orderId)
        if (res.code === 200) {
          ElMessage.success('订单已取消')
          loadOrderDetail()
        } else {
          ElMessage.error(res.message || '取消订单失败')
        }
      } catch (error) {
        console.error('取消订单失败', error)
        ElMessage.error('取消订单失败，请稍后重试')
      }
    }

    const showChangeAddressDialog = async () => {
      selectedAddressId.value = null
      selectedAddress.value = null
      addAddressFormVisible.value = false
      try {
        const res = await getAddressList()
        if (res.code === 200) {
          addressList.value = res.data || []
        }
      } catch (e) {
        addressList.value = []
      }
      addressDialogVisible.value = true
    }

    const selectAddress = (addr) => {
      selectedAddressId.value = addr.id
      selectedAddress.value = addr
    }

    const showAddAddressForm = () => {
      newAddressForm.value = {
        receiver: '',
        phone: '',
        province: '',
        city: '',
        area: '',
        detailAddress: '',
        isDefault: 0
      }
      addAddressFormVisible.value = true
    }

    const handleAddAddress = async () => {
      const form = newAddressForm.value
      if (!form.receiver || !form.phone || !form.province || !form.city || !form.area || !form.detailAddress) {
        ElMessage.warning('请填写完整的地址信息')
        return
      }
      if (!/^1[3-9]\d{9}$/.test(form.phone)) {
        ElMessage.warning('请输入正确的手机号')
        return
      }
      addingAddress.value = true
      try {
        const res = await addAddress(form)
        if (res.code === 200) {
          ElMessage.success('地址添加成功')
          addAddressFormVisible.value = false
          const listRes = await getAddressList()
          if (listRes.code === 200) {
            addressList.value = listRes.data || []
            if (addressList.value.length > 0) {
              const newAddr = addressList.value[addressList.value.length - 1]
              selectedAddressId.value = newAddr.id
              selectedAddress.value = newAddr
            }
          }
        } else {
          ElMessage.error(res.message || '地址添加失败')
        }
      } catch (error) {
        ElMessage.error('地址添加失败，请稍后重试')
      } finally {
        addingAddress.value = false
      }
    }

    const handleChangeAddress = async () => {
      if (!selectedAddress.value) {
        ElMessage.warning('请选择一个收货地址')
        return
      }
      updatingAddress.value = true
      try {
        const addr = selectedAddress.value
        const res = await fetch('/api/order/address/update', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
          },
          body: JSON.stringify({
            orderId: orderId,
            receiver: addr.receiver,
            receiverPhone: addr.phone,
            receiverAddress: `${addr.province}${addr.city}${addr.area}${addr.detailAddress}`
          })
        })
        const data = await res.json()
        if (data.code === 200) {
          ElMessage.success('地址更换成功')
          addressDialogVisible.value = false
          loadOrderDetail()
        } else {
          ElMessage.error(data.message || '地址更换失败')
        }
      } catch (error) {
        ElMessage.error('地址更换失败，请稍后重试')
      } finally {
        updatingAddress.value = false
      }
    }

    const handleConfirmReceipt = async () => {
        try {
            await ElMessageBox.confirm('确定要确认收货吗？确认后订单将完成', '确认收货', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            })
            try {
                const res = await confirmReceiptApi({ orderId })
                if (res.code === 200) {
                    ElMessage.success('确认收货成功')
                    loadOrderDetail()
                } else {
                    ElMessage.error(res.message || '确认收货失败')
                }
            } catch (error) {
                console.error('确认收货失败', error)
                ElMessage.error('确认收货失败，请稍后重试')
            }
        } catch {
            // 用户取消
        }
    }

    const handleCancelOrder = async () => {
        try {
            await ElMessageBox.confirm('确定要取消该订单吗？此操作不可恢复', '取消订单', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            })
            try {
                const res = await cancelOrderApi(orderId)
                if (res.code === 200) {
                    ElMessage.success('订单已取消')
                    loadOrderDetail()
                } else {
                    ElMessage.error(res.message || '取消订单失败')
                }
            } catch (error) {
                console.error('取消订单失败', error)
                ElMessage.error('取消订单失败，请稍后重试')
            }
        } catch {
            // 用户取消
        }
    }

    const showReviewDialog = () => {
      if (orderDetail.value && orderDetail.value.items) {
        reviewItems.value = orderDetail.value.items.map(item => ({
          productId: item.productId,
          productName: item.productName,
          productPic: item.productPic,
          rating: 5,
          content: ''
        }))
        reviewDialogVisible.value = true
      }
    }

    const handleSubmitReview = async () => {
      submittingReview.value = true
      try {
        const reviews = reviewItems.value.map(item => ({
          productId: item.productId,
          rating: item.rating,
          content: item.content
        }))
        
        const response = await fetch('/api/review/add', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
          },
          body: JSON.stringify({
            orderId: orderId,
            reviews: reviews
          })
        })
        
        const result = await response.json()
        
        if (result.code === 200) {
          ElMessage.success('评价提交成功')
          reviewDialogVisible.value = false
          hasReviewed.value = true
          loadOrderDetail()
        } else {
          ElMessage.error(result.message || '评价提交失败')
        }
      } catch (error) {
        console.error('提交评价失败', error)
        ElMessage.error('评价提交失败，请稍后重试')
      } finally {
        submittingReview.value = false
      }
    }

    onMounted(() => {
      loadOrderDetail()
    })

    onUnmounted(() => {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    })

    return {
      orderDetail,
      loading,
      refundDialogVisible,
      refundForm,
      countdown,
      payDialogVisible,
      payMethod,
      payMethods,
      selectedPayMethodLabel,
      qrValue,
      showConfirmButton,
      paying,
      getStatusText,
      getStatusType,
      getRefundStatusText,
      getRefundStatusType,
      formatTime,
      formatImageUrl,
      formatCountdown,
      goBack,
      showPayDialog,
      confirmPay,
      pay,
      handlePayDialogClose,
      showChangeAddressDialog,
      addressDialogVisible,
      addressList,
      selectedAddressId,
      selectedAddress,
      addAddressFormVisible,
      addingAddress,
      newAddressForm,
      updatingAddress,
      selectAddress,
      showAddAddressForm,
      handleAddAddress,
      handleChangeAddress,
      showRefundDialog,
      handleRefund,
      handleConfirmReceipt,
      handleCancelOrder,
      reviewDialogVisible,
      reviewItems,
      submittingReview,
      hasReviewed,
      showReviewDialog,
      handleSubmitReview
    }
  }
}
</script>

<style scoped>
.order-detail {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.back-button {
  margin-right: 20px;
  color: #409EFF;
}

.order-detail h2 {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading .el-icon {
  font-size: 24px;
  margin-right: 10px;
}

.order-info {
  margin-top: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.edit-address-button {
  font-size: 14px;
  color: #409EFF;
}

.order-item {
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
}

.order-item .label {
  width: 100px;
  font-weight: bold;
  flex-shrink: 0;
}

.order-item .value {
  flex: 1;
  word-break: break-word;
}

.mt-4 {
  margin-top: 20px;
}

.product-item {
  display: flex;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.product-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.product-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  margin-right: 15px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.product-spec {
  color: #666;
  font-size: 13px;
  margin-bottom: 5px;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  display: inline-block;
}

.product-price {
  color: #ff4d4f;
  margin-bottom: 5px;
}

.product-quantity {
  color: #999;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.error {
  padding: 40px 0;
  text-align: center;
}

.mt-4 {
  margin-top: 16px;
}

.countdown-row {
  display: flex;
  align-items: center;
}

.countdown-time {
  color: #ff4400;
  font-weight: bold;
  font-size: 16px;
}

.countdown-time.countdown-warning {
  color: #f5222d;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.pay-method-options {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 15px 0;
}

.pay-method-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px 30px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
}

.pay-method-item:hover {
  border-color: #ff6b6b;
}

.pay-method-item.active {
  border-color: #ff6b6b;
  background-color: #fff5f5;
}

.method-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
  object-fit: contain;
}

.method-name {
  font-size: 14px;
  color: #333;
}

.pay-dialog-content {
  text-align: center;
  padding-bottom: 20px;
}

.pay-amount {
  padding: 20px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 0;
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
  padding: 25px 0;
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

.review-product-item {
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-product-item:last-child {
  border-bottom: none;
}

.product-info-row {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.review-product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 15px;
}

.review-product-name {
  font-weight: bold;
  color: #333;
}

.address-select-section {
  margin-bottom: 15px;
}

.address-select-label {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.address-card {
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px 15px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.address-card:hover {
  border-color: #ff6b6b;
  background: #fff8f8;
}

.address-card-active {
  border-color: #ff4d4f;
  background: #fff1f0;
}

.address-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.address-card-name {
  font-weight: bold;
  font-size: 14px;
  color: #333;
}

.address-card-phone {
  color: #666;
  font-size: 13px;
}

.address-card-detail {
  color: #999;
  font-size: 13px;
  line-height: 1.5;
}

.no-address-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 14px;
}

.add-address-btn {
  margin-bottom: 10px;
  font-size: 14px;
}

.add-address-form {
  margin-top: 10px;
}
</style>