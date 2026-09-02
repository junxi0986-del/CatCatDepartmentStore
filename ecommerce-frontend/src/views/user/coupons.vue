<template>
  <div class="user-coupons">
    <h2>我的优惠券</h2>
    <div v-if="isLoggedIn" class="coupon-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="可使用" name="0"></el-tab-pane>
        <el-tab-pane label="已使用" name="1"></el-tab-pane>
        <el-tab-pane label="已过期" name="3"></el-tab-pane>
      </el-tabs>
      <div class="coupon-list">
        <div v-for="coupon in filteredCoupons" :key="coupon.id" class="coupon-item">
          <div class="coupon-info">
            <div class="coupon-value">¥{{ coupon.discount }}</div>
            <div class="coupon-min">满{{ coupon.minAmount }}元可用</div>
            <div class="coupon-name">{{ coupon.coupon_name }}</div>
            <div class="coupon-time">有效期：{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</div>
          </div>
          <div class="coupon-status">
            <span v-if="coupon.status === 1" class="status-used">已使用</span>
            <span v-else-if="isCouponNotYetActive(coupon)" class="status-not-active">未生效</span>
            <span v-else-if="isCouponExpired(coupon)" class="status-expired">已过期</span>
            <span v-else class="status-active">可使用</span>
          </div>
        </div>
        <div v-if="filteredCoupons.length === 0" class="no-coupon">
          <p>暂无优惠券</p>
        </div>
      </div>
    </div>
    <div v-else class="login-prompt">
      <h3>请先登录</h3>
      <p>登录后可以查看您的优惠券</p>
      <el-button type="primary" @click="goLogin">立即登录</el-button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserCoupons } from '../../api/user'

export default {
  setup() {
    const router = useRouter()
    const activeTab = ref('0')
    const userCoupons = ref([])

    const isLoggedIn = computed(() => {
      return localStorage.getItem('token') !== null
    })

    const loadCoupons = async () => {
      try {
        const userCouponRes = await getUserCoupons()
        console.log('用户优惠券数据:', userCouponRes)
        if (userCouponRes.code === 200) {
          userCoupons.value = userCouponRes.data || []
          console.log('解析后的优惠券:', userCoupons.value)
        }
      } catch (error) {
        console.error('获取优惠券失败', error)
        ElMessage.error('获取优惠券失败，请稍后重试')
      }
    }

    const filteredCoupons = computed(() => {
      if (activeTab.value === '0') {
        return userCoupons.value.filter(coupon => coupon.status === 0)
      } else if (activeTab.value === '1') {
        return userCoupons.value.filter(coupon => coupon.status === 1)
      } else {
        return userCoupons.value.filter(coupon => coupon.status === 3)
      }
    })

    const handleTabClick = (tab) => {
      activeTab.value = tab.props.name
    }

    const goLogin = () => {
      router.push('/user/login')
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    }

    const isCouponNotYetActive = (coupon) => {
      if (!coupon.startTime) return false
      const now = new Date().getTime()
      const startTime = new Date(coupon.startTime).getTime()
      return startTime > now
    }

    const isCouponExpired = (coupon) => {
      if (!coupon.endTime) return false
      const now = new Date().getTime()
      const endTime = new Date(coupon.endTime).getTime()
      return endTime < now
    }

    onMounted(() => {
      if (isLoggedIn.value) {
        loadCoupons()
      }
    })

    return {
      activeTab,
      isLoggedIn,
      filteredCoupons,
      handleTabClick,
      goLogin,
      formatDate,
      isCouponNotYetActive,
      isCouponExpired
    }
  }
}
</script>

<style scoped>
.user-coupons {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.user-coupons h2 {
  margin-bottom: 30px;
}

.coupon-tabs {
  margin-bottom: 30px;
}

.coupon-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 20px;
}

.coupon-item {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  width: calc(33.333% - 14px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  border: 1px solid #e8e8e8;
}

.coupon-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.coupon-info {
  margin-bottom: 15px;
}

.coupon-value {
  font-size: 28px;
  font-weight: bold;
  color: #ff4400;
  margin-bottom: 8px;
}

.coupon-min {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.coupon-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
  height: 24px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-time {
  font-size: 12px;
  color: #999;
}

.coupon-status {
  text-align: right;
}

.status-active {
  display: inline-block;
  background: #52c41a;
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.status-used {
  display: inline-block;
  background: #999;
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.status-expired {
  display: inline-block;
  background: #ff4400;
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.status-not-active {
  display: inline-block;
  background: #1890ff;
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.no-coupon {
  text-align: center;
  padding: 50px 0;
  color: #999;
  width: 100%;
  background: #f5f5f5;
  border-radius: 8px;
}

.login-prompt {
  text-align: center;
  padding: 50px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 30px;
}

.login-prompt h3 {
  margin-bottom: 20px;
  color: #333;
}

.login-prompt p {
  margin-bottom: 30px;
  color: #666;
}

.login-prompt .el-button {
  margin: 0 10px;
}

@media (max-width: 1200px) {
  .user-coupons {
    width: 95%;
  }

  .coupon-item {
    width: calc(50% - 10px);
  }
}

@media (max-width: 768px) {
  .coupon-item {
    width: 100%;
  }
}
</style>