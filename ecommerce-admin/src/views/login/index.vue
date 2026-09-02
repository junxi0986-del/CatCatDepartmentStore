<template>
  <div class="login">
    <div class="login-container">
      <h2>管理员登录</h2>
      <a-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="loginForm.username" placeholder="请输入用户名" @keydown.enter="handleLogin"></a-input>
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="loginForm.password" placeholder="请输入密码" @keydown.enter="handleLogin"></a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleLogin" style="width: 100%">登录</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '../../api/admin'

export default {
  setup() {
    const router = useRouter()
    const loginForm = reactive({
      username: '',
      password: ''
    })
    const loginFormRef = ref(null)
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }

    const handleLogin = async () => {
      try {
        // 验证表单
        await loginFormRef.value.validate()
        
        const res = await login(loginForm)
        if (res.code === 200) {
          localStorage.setItem('adminToken', res.data.token)
          localStorage.setItem('adminInfo', JSON.stringify(res.data.admin))
          router.push('/')
        } else {
          message.error(res.message)
        }
      } catch (error) {
        if (error.name === 'ValidationError') {
          // 表单验证错误，Ant Design Vue 会自动显示错误信息
          return
        }
        console.error('登录失败', error)
        message.error('登录失败，请稍后重试')
      }
    }

    return {
      loginForm,
      loginFormRef,
      rules,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f5f5;
}

.login-container {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
</style>
