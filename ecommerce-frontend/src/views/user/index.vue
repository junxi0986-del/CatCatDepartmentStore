<template>
  <div class="user-center">
    <h2>个人中心</h2>
    <div v-if="isLoggedIn" class="user-info">
      <div class="avatar">
        <img :src="getAvatarUrl(userInfo.avatar)" />
      </div>
      <div class="info">
        <h3>{{ userInfo.username }}</h3>
        <p>{{ userInfo.phone }}</p>
        <p v-if="userInfo.email">{{ userInfo.email }}</p>
        <router-link to="/user/edit"><el-button type="primary">编辑个人信息</el-button></router-link>
      </div>
    </div>
    <div v-else class="login-prompt">
      <h3>请先登录</h3>
      <p>登录后可以查看个人信息和管理订单</p>
      <el-button type="primary" @click="goLogin">立即登录</el-button>
      <el-button @click="goRegister">立即注册</el-button>
    </div>
    <div v-if="isLoggedIn" class="user-menu">
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical-demo"
        @select="handleMenuSelect"
      >
        <el-menu-item index="orders">
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="address">
          <span>收货地址</span>
        </el-menu-item>
        <el-menu-item index="coupons">
          <span>我的优惠券</span>
        </el-menu-item>
        <el-menu-item index="password">
          <span>修改密码</span>
        </el-menu-item>
        <el-menu-item index="logout">
          <span>退出登录</span>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="recommend">
      <h3>专属推荐</h3>
      <div class="recommend-products">
        <ProductCard v-for="item in recommendProducts" :key="item.id" :product="item" />
      </div>
    </div>

    <!-- 编辑个人信息模态框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑个人信息"
      width="500px"
    >
      <el-form :model="editForm" :rules="rules" ref="editFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="editForm.avatar" placeholder="请输入头像URL"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveInfo">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 修改密码模态框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="changePassword">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductCard from '../../components/ProductCard.vue'
import { getUserInfo, updateUserInfo, changePassword as changePasswordApi } from '../../api/user'
import { getUserRecommend } from '../../api/recommend'
import { getProductList } from '../../api/product'
import { avatarManager } from '../../utils/avatarManager'

export default {
  components: {
    ProductCard
  },
  setup() {
    const router = useRouter()
    const activeMenu = ref('orders')
    const userInfo = ref({})
    const recommendProducts = ref([])
    const dialogVisible = ref(false)
    const editForm = ref({})
    const editFormRef = ref(null)

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ]
    }

    const passwordDialogVisible = ref(false)
    const passwordForm = ref({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    const passwordFormRef = ref(null)
    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入旧密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.value.newPassword) {
              callback(new Error('两次输入的密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }

    const isLoggedIn = computed(() => {
      return localStorage.getItem('token') !== null
    })

    const getAvatarUrl = (avatar) => {
      if (!avatar || avatar === '') {
        return 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=user%20avatar&image_size=square'
      }
      return avatar
    }

    const loadUserInfo = () => {
      if (isLoggedIn.value) {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          const avatar = avatarManager.getAvatar(user.id)
          user.avatar = avatar || user.avatar || ''
          userInfo.value = user
        } else {
          console.log('本地存储中没有用户信息')
        }
      } else {
        console.log('用户未登录')
      }
    }

    const handleMenuSelect = (key) => {
      activeMenu.value = key
      switch (key) {
        case 'orders':
          router.push('/order')
          break
        case 'address':
          router.push('/address')
          break
        case 'coupons':
          // 跳转到我的优惠券页面
          router.push('/user/coupons')
          break
        case 'password':
          // 打开修改密码模态框
          passwordForm.value = {
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
          }
          passwordDialogVisible.value = true
          break
        case 'logout':
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          router.push('/user/login')
          break
      }
    }

    const editInfo = () => {
      // 打开编辑模态框，并填充表单数据
      editForm.value = { ...userInfo.value }
      dialogVisible.value = true
    }

    const saveInfo = async () => {
      try {
        // 移除phone字段，因为手机号仅管理员才可以修改
        const { phone, ...updateData } = editForm.value
        const res = await updateUserInfo(updateData)
        if (res.code === 200) {
          // 保留原始的phone字段
          res.data.phone = editForm.value.phone
          userInfo.value = res.data
          localStorage.setItem('user', JSON.stringify(userInfo.value))
          ElMessage.success('个人信息更新成功')
          dialogVisible.value = false
        } else {
          ElMessage.error(res.message)
        }
      } catch (error) {
        console.error('更新个人信息失败', error)
        ElMessage.error('更新个人信息失败，请稍后重试')
      }
    }

    const goLogin = () => {
      router.push('/user/login')
    }

    const goRegister = () => {
      router.push('/user/register')
    }

    const changePassword = async () => {
      try {
        await passwordFormRef.value.validate()
        console.log('userInfo:', userInfo.value)
        if (!userInfo.value || !userInfo.value.id) {
          ElMessage.error('用户信息不完整，请重新登录')
          return
        }
        const params = {
          id: userInfo.value.id,
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword
        }
        console.log('修改密码参数:', params)
        const res = await changePasswordApi(params)
        console.log('修改密码响应:', res)
        if (res.code !== 200) {
          throw new Error(res.message || '修改密码失败')
        }
        ElMessage.success('密码修改成功')
        passwordDialogVisible.value = false
      } catch (error) {
        console.error('修改密码失败', error)
        if (error.message) {
          ElMessage.error(error.message)
        } else if (error.response && error.response.data && error.response.data.msg) {
          ElMessage.error(error.response.data.msg)
        } else {
          ElMessage.error('修改密码失败，请稍后重试')
        }
      }
    }

    let unwatch = null

    const loadRecommendProducts = async () => {
      try {
        const userId = userInfo.value.id
        if (!userId) {
          const res = await getProductList({ page: 1, pageSize: 4 })
          if (res.code === 200) {
            recommendProducts.value = (res.data.list || res.data || []).slice(0, 4)
          }
          return
        }
        const res = await getUserRecommend(userId)
        if (res.code === 200) {
          recommendProducts.value = res.data || []
        }
      } catch (error) {
        console.error('加载推荐商品失败', error)
      }
    }

    onMounted(async () => {
      await loadUserInfo()
      unwatch = avatarManager.watch(() => {
        loadUserInfo()
      }, 500)
      loadRecommendProducts()
    })

    onUnmounted(() => {
      if (unwatch) {
        unwatch()
      }
    })

    return {
      activeMenu,
      userInfo,
      recommendProducts,
      isLoggedIn,
      getAvatarUrl,
      handleMenuSelect,
      editInfo,
      saveInfo,
      goLogin,
      goRegister,
      dialogVisible,
      editForm,
      editFormRef,
      rules,
      passwordDialogVisible,
      passwordForm,
      passwordFormRef,
      passwordRules,
      changePassword
    }
  }
}
</script>

<style scoped>
.user-center {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.user-center h2 {
  margin-bottom: 30px;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.avatar {
  margin-right: 30px;
}

.avatar img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
}

.info h3 {
  margin-bottom: 10px;
}

.info p {
  margin-bottom: 20px;
  color: #666;
}

.user-menu {
  margin-bottom: 50px;
}

.recommend {
  margin-top: 50px;
}

.recommend h3 {
  font-size: 20px;
  margin-bottom: 20px;
}

.recommend-products {
  display: flex;
  gap: 20px;
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
</style>
