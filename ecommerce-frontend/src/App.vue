<template>
  <div class="app-container">
    <div class="background-layer" ref="backgroundLayer"></div>
    <Header />
    <main class="main-content">
      <router-view />
    </main>
    <Footer />
    
    <div class="customer-service-float" @click="goToChat" title="联系客服">
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <div class="float-button">
          <el-icon :size="24"><Service /></el-icon>
        </div>
      </el-badge>
    </div>
  </div>
</template>

<script>
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import { Service } from '@element-plus/icons-vue'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'App',
  components: {
    Header,
    Footer,
    Service
  },
  setup() {
    const router = useRouter()
    const unreadCount = ref(0)
    const backgroundLayer = ref(null)
    let resizeObserver = null
    
    const goToChat = () => {
      router.push('/chat')
    }

    const updateBackgroundTop = () => {
      const header = document.querySelector('header')
      if (header && backgroundLayer.value) {
        backgroundLayer.value.style.top = header.offsetHeight + 'px'
      }
    }
    
    onMounted(() => {
      updateBackgroundTop()
      const header = document.querySelector('header')
      if (header) {
        resizeObserver = new ResizeObserver(updateBackgroundTop)
        resizeObserver.observe(header)
      }
    })
    
    onBeforeUnmount(() => {
      if (resizeObserver) {
        resizeObserver.disconnect()
      }
    })
    
    return {
      unreadCount,
      goToChat,
      backgroundLayer
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
}

.app-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.background-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('/api/indbackground.png');
  background-size: 100% auto;
  background-position: center top;
  background-repeat: no-repeat;
  z-index: -1;
  pointer-events: none;
}

.main-content {
  position: relative;
  z-index: 1;
  flex: 1;
}

.customer-service-float {
  position: fixed;
  right: 30px;
  bottom: 100px;
  z-index: 1000;
  cursor: pointer;
}

.float-button {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transition: all 0.3s ease;
  color: white;
}

.float-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.6);
}
</style>
