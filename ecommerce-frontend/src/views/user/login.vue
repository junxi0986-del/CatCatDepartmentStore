<template>
  <div class="login" :style="{ backgroundImage: `url(${backgroundImage})` }">
    <div class="login-container">
      <h2>用户登录</h2>
      <div class="login-tabs">
        <el-tabs v-model="loginType" @tab-click="handleTabClick">
          <el-tab-pane label="账号登录" name="username">
            <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" maxlength="10" @keydown.enter="handleLogin"></el-input>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input type="password" v-model="loginForm.password" placeholder="请输入密码" maxlength="10" @input="onPasswordInput" @keydown.enter="handleLogin"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="手机号登录" name="phone">
            <el-form :model="loginForm" :rules="phoneRules" ref="loginFormRef">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="loginForm.phone" placeholder="请输入手机号" maxlength="11" @input="onPhoneInput" @keydown.enter="handleLogin"></el-input>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input type="password" v-model="loginForm.password" placeholder="请输入密码" maxlength="10" @input="onPasswordInput" @keydown.enter="handleLogin"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div class="register-link">
        还没有账号？<router-link to="/user/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../../api/user'

export default {
  setup() {
    const router = useRouter()
    const loginType = ref('username')
    const loginForm = reactive({
      username: '',
      phone: '',
      password: ''
    })
    const loginFormRef = ref(null)
    const backgroundImage = ref('/api/avatars/cat00.png')
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { max: 10, message: '用户名最多10个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { max: 10, message: '密码最多10个字符', trigger: 'blur' }
      ]
    }
    const phoneRules = {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { min: 10, max: 11, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { max: 10, message: '密码最多10个字符', trigger: 'blur' }
      ]
    }

    const handleTabClick = () => {
      // 切换登录方式时清空表单
      if (loginType.value === 'username') {
        loginForm.username = ''
      } else {
        loginForm.phone = ''
      }
      loginForm.password = ''
      backgroundImage.value = '/api/avatars/cat00.png'
    }

    const onPasswordInput = () => {
      if (loginForm.password) {
        backgroundImage.value = '/api/avatars/catxx.png'
      } else {
        backgroundImage.value = '/api/avatars/cat00.png'
      }
    }

    const onPhoneInput = () => {
      loginForm.phone = loginForm.phone.replace(/[^\d]/g, '')
    }

    const handleLogin = async () => {
      try {
        let loginParams
        if (loginType.value === 'username') {
          loginParams = {
            username: loginForm.username,
            password: loginForm.password
          }
        } else {
          loginParams = {
            phone: loginForm.phone,
            password: loginForm.password
          }
        }
        const res = await login(loginParams)
        if (res.code === 200) {
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('user', JSON.stringify(res.data.user))
          window.dispatchEvent(new CustomEvent('userLoggedIn', { detail: res.data.user }))
          const redirectTo = localStorage.getItem('redirectTo')
          if (redirectTo) {
            localStorage.removeItem('redirectTo')
            router.push(redirectTo)
          } else {
            router.push('/')
          }
        } else {
          ElMessage.error(res.message)
        }
      } catch (error) {
        console.error('登录失败', error)
        ElMessage.error('登录失败，请稍后重试')
      }
    }

    return {
      loginType,
      loginForm,
      loginFormRef,
      rules,
      phoneRules,
      handleTabClick,
      handleLogin,
      backgroundImage,
      onPasswordInput,
      onPhoneInput
    }
  }
}
</script>

<style scoped>
.login {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  padding-right: 10%;
}

.login-container {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.register-link a {
  color: #409EFF;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
