<template>
  <div class="register" :style="{ backgroundImage: `url(${backgroundImage})` }">
    <div class="register-container">
      <h2>用户注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" maxlength="10" @keydown.enter="handleRegister"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="registerForm.password" placeholder="请输入密码" maxlength="15" @input="onPasswordInput" @keydown.enter="handleRegister"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input type="password" v-model="registerForm.confirmPassword" placeholder="请确认密码" maxlength="15" @input="onPasswordInput" @keydown.enter="handleRegister"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" maxlength="11" @input="onPhoneInput" @keydown.enter="handleRegister"></el-input>
        </el-form-item>
        <el-form-item label="人机验证" prop="captchaAnswer">
          <div class="captcha-container">
            <el-input v-model="registerForm.captchaAnswer" placeholder="请输入计算结果" maxlength="3" style="width: 100px" @keydown.enter="handleRegister"></el-input>
            <img v-if="captchaImage" :src="captchaImage" class="captcha-image" @click="refreshCaptcha" title="点击刷新" />
            <el-button type="text" @click="refreshCaptcha" :loading="captchaLoading">刷新</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%">注册</el-button>
        </el-form-item>
        <div class="login-link">
          已有账号？<router-link to="/user/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, getCaptcha } from '../../api/user'

export default {
  setup() {
    const router = useRouter()
    const registerForm = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      phone: '',
      captchaId: '',
      captchaAnswer: ''
    })
    const registerFormRef = ref(null)
    const backgroundImage = ref('/api/avatars/cat00.png')
    const captchaImage = ref('')
    const captchaLoading = ref(false)
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { max: 10, message: '用户名最多10个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码最少6个字符', trigger: 'blur' },
        { max: 15, message: '密码最多15个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { min: 6, message: '密码最少6个字符', trigger: 'blur' },
        { max: 15, message: '密码最多15个字符', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== registerForm.password) {
              callback(new Error('两次输入的密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ],
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { min: 8, message: '手机号最少8位', trigger: 'blur' },
        { max: 11, message: '手机号最多11位', trigger: 'blur' }
      ],
      captchaAnswer: [
        { required: true, message: '请完成人机验证', trigger: 'blur' }
      ]
    }

    const refreshCaptcha = async () => {
      captchaLoading.value = true
      try {
        const res = await getCaptcha()
        if (res.code === 200) {
          captchaImage.value = res.data.image
          registerForm.captchaId = res.data.captchaId
          registerForm.captchaAnswer = ''
        }
      } catch (error) {
        console.error('获取验证码失败', error)
      } finally {
        captchaLoading.value = false
      }
    }

    onMounted(() => {
      refreshCaptcha()
    })

    const onPasswordInput = () => {
      if (registerForm.password || registerForm.confirmPassword) {
        backgroundImage.value = '/api/avatars/catxx.png'
      } else {
        backgroundImage.value = '/api/avatars/cat00.png'
      }
    }

    const onPhoneInput = () => {
      registerForm.phone = registerForm.phone.replace(/[^\d]/g, '')
    }

    const handleRegister = async () => {
      try {
        const res = await register({
          username: registerForm.username,
          password: registerForm.password,
          phone: registerForm.phone,
          captchaId: registerForm.captchaId,
          captchaAnswer: registerForm.captchaAnswer
        })
        if (res.code === 200) {
          ElMessage.success('注册成功')
          router.push('/user/login')
        } else {
          refreshCaptcha()
          ElMessage.error(res.message)
        }
      } catch (error) {
        console.error('注册失败', error)
        refreshCaptcha()
        ElMessage.error('注册失败，请稍后重试')
      }
    }

    return {
      registerForm,
      registerFormRef,
      rules,
      handleRegister,
      backgroundImage,
      onPasswordInput,
      onPhoneInput,
      captchaImage,
      captchaLoading,
      refreshCaptcha
    }
  }
}
</script>

<style scoped>
.register {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  padding-right: 10%;
}

.register-container {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.register-container h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.login-link a {
  color: #409EFF;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}

.captcha-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-image {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.captcha-image:hover {
  border-color: #409EFF;
}
</style>
