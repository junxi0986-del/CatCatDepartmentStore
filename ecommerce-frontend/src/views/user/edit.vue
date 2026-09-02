<template>
  <div class="edit-user">
    <h2>编辑个人信息</h2>
    <div class="edit-form">
      <el-form :model="userForm" :rules="rules" ref="userFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-wrapper">
            <input 
              type="file" 
              id="avatar-input" 
              class="avatar-input"
              accept="image/jpeg,image/png"
              @change="handleAvatarChange"
            />
            <label for="avatar-input" class="avatar-uploader">
              <img 
                v-if="userForm.avatar" 
                :src="userForm.avatar" 
                class="avatar"
              />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </label>
            <button 
              v-if="userForm.avatar" 
              class="clear-avatar-btn"
              @click="clearAvatar"
            >清空头像</button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveUserInfo">保存</el-button>
          <router-link to="/user"><el-button>取消</el-button></router-link>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { avatarManager } from '../../utils/avatarManager'
import { updateUserInfo } from '../../api/user'

export default {
  components: {
    Plus
  },
  setup() {
    const router = useRouter()
    const userForm = ref({
      id: '',
      username: '',
      email: '',
      avatar: '',
      phone: ''
    })
    const userFormRef = ref(null)

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ]
    }

    const loadUserInfo = () => {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        userForm.value.id = user.id || ''
        userForm.value.username = user.username || ''
        userForm.value.email = user.email || ''
        userForm.value.phone = user.phone || ''
        const avatar = avatarManager.getAvatar(user.id)
        userForm.value.avatar = avatar || user.avatar || ''
      } else {
        ElMessage.error('请先登录')
        router.push('/user/login')
      }
    }

    const handleAvatarChange = (event) => {
      const file = event.target.files[0]
      if (!file) return

      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJpgOrPng) {
        ElMessage.error('只能上传JPG或PNG图片！')
        return
      }
      if (!isLt2M) {
        ElMessage.error('图片大小不能超过2MB！')
        return
      }

      const reader = new FileReader()
      reader.onload = (e) => {
        userForm.value.avatar = e.target.result
      }
      reader.readAsDataURL(file)
    }

    const clearAvatar = () => {
      userForm.value.avatar = ''
      if (userForm.value.id) {
        avatarManager.removeAvatar(userForm.value.id)
      }
    }

    const saveUserInfo = async () => {
      try {
        const userData = {
          id: userForm.value.id,
          username: userForm.value.username,
          email: userForm.value.email,
          phone: userForm.value.phone,
          avatar: userForm.value.avatar
        }
        
        console.log('准备发送的数据:', {
          ...userData,
          avatar: userData.avatar ? userData.avatar.substring(0, 100) + '...' : null
        })
        
        const res = await updateUserInfo(userData)
        if (res.code === 200) {
          if (res.data) {
            localStorage.setItem('user', JSON.stringify(res.data))
            if (res.data.avatar) {
              userForm.value.avatar = res.data.avatar
            }
          } else {
            localStorage.setItem('user', JSON.stringify(userData))
          }
          
          if (userForm.value.id && userForm.value.avatar) {
            avatarManager.setAvatar(userForm.value.id, userForm.value.avatar)
          }
          
          ElMessage.success('个人信息更新成功')
          router.push('/user')
        } else {
          ElMessage.error(res.message || '更新失败')
        }
      } catch (error) {
        console.error('更新用户信息失败', error)
        console.error('错误响应:', error.response)
        console.error('错误配置:', error.config)
        
        if (error.response) {
          console.error('服务器响应状态:', error.response.status)
          console.error('服务器响应数据:', error.response.data)
          ElMessage.error(`服务器错误: ${error.response.status} - ${error.response.data?.message || '未知错误'}`)
        } else if (error.request) {
          console.error('请求已发出但没有收到响应')
          ElMessage.error('服务器无响应，请稍后重试')
        } else {
          ElMessage.error('更新失败，请稍后重试')
        }
      }
    }

    onMounted(() => {
      loadUserInfo()
    })

    return {
      userForm,
      userFormRef,
      rules,
      handleAvatarChange,
      clearAvatar,
      saveUserInfo
    }
  }
}
</script>

<style scoped>
.edit-user {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.edit-user h2 {
  margin-bottom: 30px;
}

.edit-form {
  width: 600px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.avatar-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.avatar-input {
  display: none;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
  width: 178px;
  height: 178px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.clear-avatar-btn {
  width: 178px;
  padding: 8px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  transition: all 0.3s;
}

.clear-avatar-btn:hover {
  background: #f5f5f5;
  color: #ff4400;
  border-color: #ff4400;
}
</style>
