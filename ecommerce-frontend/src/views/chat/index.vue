<template>
  <div class="chat-container">
    <div class="chat-header">
      <el-button link @click="handleGoBack" class="back-button">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>在线客服</h2>
      <div class="header-actions">
        <el-tag :type="connectionStatus === 'connected' ? 'success' : 'danger'" size="small" class="status-tag">
          {{ connectionStatus === 'connected' ? '已连接' : '连接断开' }}
        </el-tag>
        <el-button type="danger" size="small" @click="handleExitChat" class="exit-button">
          退出客服
        </el-button>
      </div>
    </div>
    <div class="chat-body">
      <div class="message-list" ref="messageList">
        <div 
          v-for="message in messages" 
          :key="message.id" 
          :class="['message-item', message.type === 0 ? 'user-message' : 'admin-message']"
        >
          <div class="message-bubble">
            <div class="message-content">
              <template v-if="message.messageType === 1 && message.imageUrl">
                <img 
                  :src="message.imageUrl" 
                  class="message-image" 
                  @click="previewImage(message.imageUrl)"
                  @error="handleImageError($event, message)"
                  loading="lazy"
                />
              </template>
              <template v-else>
                <span class="message-text" v-html="formatMessageContent(message.content)"></span>
              </template>
            </div>
            <div class="message-time">{{ formatTime(message.createTime) }}</div>
          </div>
        </div>
        <div v-if="reconnecting" class="reconnecting">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在重新连接...</span>
        </div>
        <div v-if="sessionExpired" class="session-expired-tip">
          <span>会话已超时，已为您开启新会话</span>
        </div>
        <div v-if="timeoutWarning" class="timeout-warning-tip">
          <span>您已1分钟未发言，{{ countdownSeconds }}秒后将自动退出客服会话</span>
        </div>
      </div>
      <div class="message-input">
        <el-input 
          v-model="inputMessage" 
          type="textarea" 
          placeholder="请输入消息..." 
          :rows="3"
          @keydown.enter="handleEnterSend"
        ></el-input>
        <div class="button-group">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeImageUpload"
            :http-request="handleImageUpload"
            accept="image/*"
            class="image-upload"
          >
            <el-button type="info" class="image-button">
              <el-icon><Picture /></el-icon>
            </el-button>
          </el-upload>
          <el-button 
            type="primary" 
            @click="sendMessage" 
            class="send-button"
            :loading="!socket || (socket && socket.readyState !== SOCKET_OPEN)"
          >
            发送
          </el-button>
          <el-button 
            v-if="showHumanButton"
            type="warning" 
            @click="switchToHuman" 
            class="human-button"
            :disabled="isHumanMode"
          >
            {{ isHumanMode ? '人工客服中' : '转人工客服' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 图片预览对话框 -->
    <el-dialog 
      v-model="showImagePreview" 
      width="80%" 
      @close="showImagePreview = false"
      class="image-preview-dialog"
    >
      <img :src="previewImageUrl" class="preview-image" />
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import { ArrowLeft, Loading, Picture } from '@element-plus/icons-vue'

export default {
  name: 'Chat',
  components: {
    ArrowLeft,
    Loading,
    Picture
  },
  setup() {
    const router = useRouter()
    const messages = ref([])
    const inputMessage = ref('')
    const messageList = ref(null)
    const socket = ref(null)
    let sessionId = null
    let reconnectTimer = null
    let heartbeatTimer = null
    let inactivityTimer = null
    let warningTimer = null
    const WARNING_TIMEOUT = 1 * 60 * 1000
    const INACTIVITY_TIMEOUT = 2 * 60 * 1000
    const reconnecting = ref(false)
    const connectionStatus = ref('connecting')
    const SOCKET_OPEN = 1
    const isHumanMode = ref(false)
    const showHumanButton = ref(false)
    const showImagePreview = ref(false)
    const previewImageUrl = ref('')
    const sessionExpired = ref(false)
    const timeoutWarning = ref(false)
    const countdownSeconds = ref(60)
    let countdownTimer = null

    const getSessionId = () => {
      let userId = 'anonymous'
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          userId = user.id || 'anonymous'
        } catch (error) {
          console.error('解析用户信息失败', error)
        }
      }
      
      if (userId === 'anonymous') {
        const storedGuestSession = sessionStorage.getItem('guest_chat_session')
        if (storedGuestSession) {
          return storedGuestSession
        }
        const newSessionId = `guest_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        sessionStorage.setItem('guest_chat_session', newSessionId)
        return newSessionId
      }
      
      const storedSessionId = localStorage.getItem(`chat_session_${userId}`)
      if (storedSessionId) {
        return storedSessionId
      }
      const newSessionId = `user_${userId}_${Date.now()}`
      localStorage.setItem(`chat_session_${userId}`, newSessionId)
      return newSessionId
    }
    
    const createNewSession = () => {
      let userId = 'anonymous'
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          userId = user.id || 'anonymous'
        } catch (error) {
          console.error('解析用户信息失败', error)
        }
      }
      let newSessionId
      if (userId === 'anonymous') {
        newSessionId = `guest_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        sessionStorage.setItem('guest_chat_session', newSessionId)
      } else {
        newSessionId = `user_${userId}_${Date.now()}`
        localStorage.setItem(`chat_session_${userId}`, newSessionId)
      }
      sessionId = newSessionId
      messages.value = []
      isHumanMode.value = false
      showHumanButton.value = false
      return newSessionId
    }
    
    const hashCode = (str) => {
      let hash = 0
      for (let i = 0; i < str.length; i++) {
        const char = str.charCodeAt(i)
        hash = ((hash << 5) - hash) + char
        hash = hash & hash
      }
      return hash
    }
    
    const getGuestUserId = () => {
      return -Math.abs(hashCode(sessionId)) % 1000000
    }

    const connectWebSocket = () => {
      let userId = 'anonymous'
      let userName = '游客'
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          userId = user.id || 'anonymous'
          userName = user.username || user.phone || '用户'
        } catch (error) {
          console.error('解析用户信息失败', error)
        }
      }
      sessionId = getSessionId()
      
      const effectiveUserId = userId === 'anonymous' ? getGuestUserId() : userId
      
      // 同源 WebSocket（本地走 Vite 代理，线上走 Pages Function 透传）
      let wsUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/api/ws/chat?userType=user&id=${effectiveUserId}`
      socket.value = new WebSocket(wsUrl)
      
      socket.value.onopen = () => {
        connectionStatus.value = 'connected'
        reconnecting.value = false
        isHumanMode.value = false
        loadChatHistory()
        startHeartbeat()
        resetInactivityTimer()
      }
      
      socket.value.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data)
          if (message.type === 'heartbeat') {
            return
          }
          if (!message.content && !message.imageUrl) {
            return
          }
          // 只接收当前会话的消息，过滤掉其他用户的串流消息
          if (message.sessionId && message.sessionId !== sessionId) {
            return
          }
          // 避免重复显示自己发送的消息（前端已本地添加）
          if (message.type === 0 && message.userId === effectiveUserId) {
            const isDuplicate = messages.value.some(m => 
              m.type === 0 && m.content === message.content && 
              Math.abs(new Date(m.createTime) - new Date(message.createTime)) < 3000
            )
            if (isDuplicate) return
          }
          messages.value.push(message)
          scrollToBottom()
          resetInactivityTimer()
        } catch (error) {
          console.error('解析消息失败', error)
        }
      }
      
      socket.value.onerror = (error) => {
        console.error('WebSocket连接错误', error)
        connectionStatus.value = 'disconnected'
      }
      
      socket.value.onclose = () => {
        connectionStatus.value = 'disconnected'
        startReconnect()
      }
    }

    const loadChatHistory = async () => {
      try {
        const res = await fetch(`/api/chat/message/${sessionId}`)
        const data = await res.json()
        if (data.code === 200) {
          const historyMessages = data.data || []
          if (historyMessages.length === 0) {
            sendWelcomeMessage()
            return
          }
          messages.value = historyMessages
          scrollToBottom()
        } else {
          sendWelcomeMessage()
        }
      } catch (error) {
        console.error('加载聊天历史失败', error)
        sendWelcomeMessage()
      }
    }
    
    const sendWelcomeMessage = () => {
      const welcomeMessage = {
        id: Date.now(),
        userId: null,
        userName: null,
        adminId: 1,
        adminName: 'AI客服',
        content: '您好！这里是猫猫百货商城智能客服 🤖 AI客服已上线，请问有什么可以帮您？',
        type: 1,
        createTime: new Date(),
        sessionId: sessionId
      }
      messages.value.push(welcomeMessage)
      scrollToBottom()
    }

    const handleEnterSend = (e) => {
      e.preventDefault()
      if (!inputMessage.value.trim()) {
        ElMessage.warning('消息不能为空')
        return
      }
      sendMessage()
    }
    
    const sendMessage = () => {
      if (!inputMessage.value.trim()) {
        ElMessage.warning('请输入消息内容')
        return
      }
      if (!socket.value || socket.value.readyState !== SOCKET_OPEN) {
        ElMessage.error('连接已断开，正在重新连接...')
        connectWebSocket()
        return
      }
      
      const messageContent = inputMessage.value.trim()
      
      const humanKeywords = ['人工', '转人工', '人工客服', '转接人工', '联系人工', '人工服务']
      if (humanKeywords.some(keyword => messageContent.includes(keyword))) {
        showHumanButton.value = true
      }
      
      let userId = 'anonymous'
      let userName = '游客'
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          userId = user.id || 'anonymous'
          userName = user.username || user.phone || '用户'
        } catch (error) {
          console.error('解析用户信息失败', error)
        }
      }
      
      const effectiveUserId = userId === 'anonymous' ? getGuestUserId() : parseInt(userId)
      
      const message = {
        userId: effectiveUserId,
        userName: userName,
        adminId: null,
        adminName: null,
        content: messageContent,
        type: 0,
        createTime: new Date(),
        sessionId: sessionId
      }
      socket.value.send(JSON.stringify(message))
      messages.value.push(message)
      scrollToBottom()
      inputMessage.value = ''
      sessionExpired.value = false
      resetInactivityTimer()
    }

    const scrollToBottom = () => {
      setTimeout(() => {
        if (messageList.value) {
          messageList.value.scrollTop = messageList.value.scrollHeight
        }
      }, 100)
    }

    const formatTime = (time) => {
      if (!time) return ''
      const date = new Date(time)
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    }

    const formatMessageContent = (content) => {
      if (!content) return ''
      let text = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      text = text.replace(/\n/g, '<br>')
      text = text.replace(/(\d+[\.\、])\s*/g, '<span class="list-num">$1</span>')
      text = text.replace(/^[•·]\s*/gm, '<span class="list-dot">•</span> ')
      return text
    }

    const handleGoBack = () => {
      router.back()
    }

    const handleExitChat = () => {
      ElMessageBox.confirm(
        '退出后将终止当前聊天并清除聊天记录，确认退出吗？',
        '退出客服',
        {
          confirmButtonText: '确认退出',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      ).then(async () => {
        stopInactivityTimer()
        if (heartbeatTimer) {
          clearInterval(heartbeatTimer)
          heartbeatTimer = null
        }
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }

        if (socket.value) {
          socket.value.close()
          socket.value = null
        }

        try {
          await fetch(`/api/chat/session/${sessionId}`, { method: 'DELETE' })
        } catch (error) {
          console.error('清除会话失败', error)
        }

        messages.value = []
        inputMessage.value = ''
        isHumanMode.value = false
        showHumanButton.value = false
        sessionExpired.value = false
        timeoutWarning.value = false

        ElMessage.success('已退出客服会话')
        router.back()
      }).catch(() => {})
    }

    const startReconnect = () => {
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
      }
      reconnecting.value = true
      reconnectTimer = setTimeout(() => {
        connectWebSocket()
      }, 3000)
    }

    const startHeartbeat = () => {
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer)
      }
      heartbeatTimer = setInterval(() => {
        if (socket.value && socket.value.readyState === SOCKET_OPEN) {
          socket.value.send(JSON.stringify({ type: 'heartbeat' }))
        }
      }, 30000)
    }

    const resetInactivityTimer = () => {
      if (inactivityTimer) {
        clearTimeout(inactivityTimer)
      }
      if (warningTimer) {
        clearTimeout(warningTimer)
      }
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
      sessionExpired.value = false
      timeoutWarning.value = false
      countdownSeconds.value = 60
      warningTimer = setTimeout(() => {
        timeoutWarning.value = true
        countdownSeconds.value = 60
        countdownTimer = setInterval(() => {
          countdownSeconds.value--
          if (countdownSeconds.value <= 0) {
            clearInterval(countdownTimer)
            countdownTimer = null
          }
        }, 1000)
        scrollToBottom()
      }, WARNING_TIMEOUT)
      inactivityTimer = setTimeout(() => {
        handleSessionExpired()
      }, INACTIVITY_TIMEOUT)
    }

    const handleSessionExpired = async () => {
      sessionExpired.value = true
      timeoutWarning.value = false
      isHumanMode.value = false
      showHumanButton.value = false

      if (heartbeatTimer) {
        clearInterval(heartbeatTimer)
        heartbeatTimer = null
      }
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }

      try {
        await fetch(`/api/chat/session/${sessionId}`, { method: 'DELETE' })
      } catch (error) {
        console.error('清除会话失败', error)
      }

      if (socket.value) {
        socket.value.close()
        socket.value = null
      }

      messages.value = []
      inputMessage.value = ''

      ElMessage.warning('会话已超时，已自动退出客服')
      router.back()
    }

    const stopInactivityTimer = () => {
      if (inactivityTimer) {
        clearTimeout(inactivityTimer)
        inactivityTimer = null
      }
      if (warningTimer) {
        clearTimeout(warningTimer)
        warningTimer = null
      }
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }

    const switchToHuman = () => {
      if (!socket.value || socket.value.readyState !== SOCKET_OPEN) {
        ElMessage.error('连接已断开，正在重新连接...')
        connectWebSocket()
        return
      }
      
      let userId = 'anonymous'
      let userName = '游客'
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          userId = user.id || 'anonymous'
          userName = user.username || user.phone || '用户'
        } catch (error) {
          console.error('解析用户信息失败', error)
        }
      }
      
      const effectiveUserId = userId === 'anonymous' ? getGuestUserId() : parseInt(userId)
      
      const switchMessage = {
        type: 'switchToHuman',
        sessionId: sessionId,
        userId: effectiveUserId,
        userName: userName
      }
      
      socket.value.send(JSON.stringify(switchMessage))
      isHumanMode.value = true
      ElMessage.success('正在为您转接人工客服...')
    }

    const beforeImageUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt5M) {
        ElMessage.error('图片大小不能超过 5MB!')
        return false
      }
      return true
    }

    const handleImageUpload = async (options) => {
      const { file } = options
      
      if (!socket.value || socket.value.readyState !== SOCKET_OPEN) {
        ElMessage.error('连接已断开，正在重新连接...')
        connectWebSocket()
        return
      }
      
      const formData = new FormData()
      formData.append('file', file)
      
      try {
        const res = await fetch('/api/chat/upload', {
          method: 'POST',
          body: formData
        })
        const data = await res.json()
        
        if (data.code === 200 && data.data) {
          const imageUrl = data.data.url
          
          let userId = 'anonymous'
          let userName = '游客'
          const userStr = localStorage.getItem('user')
          if (userStr) {
            try {
              const user = JSON.parse(userStr)
              userId = user.id || 'anonymous'
              userName = user.username || user.phone || '用户'
            } catch (error) {
              console.error('解析用户信息失败', error)
            }
          }
          
          const effectiveUserId = userId === 'anonymous' ? getGuestUserId() : parseInt(userId)
          
          const message = {
            userId: effectiveUserId,
            userName: userName,
            adminId: null,
            adminName: null,
            content: '[图片]',
            type: 0,
            createTime: new Date(),
            sessionId: sessionId,
            messageType: 1,
            imageUrl: imageUrl
          }
          
          socket.value.send(JSON.stringify(message))
          messages.value.push(message)
          scrollToBottom()
        } else {
          ElMessage.error('图片上传失败')
        }
      } catch (error) {
        console.error('上传图片失败', error)
        ElMessage.error('图片上传失败')
      }
    }

    const previewImage = (url) => {
      previewImageUrl.value = url
      showImagePreview.value = true
    }

    const handleImageError = (event, message) => {
      console.error('图片加载失败:', message.imageUrl)
      event.target.style.display = 'none'
      const parent = event.target.parentElement
      if (parent) {
        const errorText = document.createElement('span')
        errorText.textContent = '图片加载失败'
        errorText.style.color = '#f56c6c'
        parent.appendChild(errorText)
      }
    }

    watch(messages, () => {
      scrollToBottom()
    }, { deep: true })

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && messages.value.length > 0) {
        resetInactivityTimer()
      }
    }

    onMounted(() => {
      messages.value = []
      isHumanMode.value = false
      showHumanButton.value = false
      sessionExpired.value = false
      connectWebSocket()
      document.addEventListener('visibilitychange', handleVisibilityChange)
    })

    onBeforeUnmount(() => {
      if (socket.value) {
        socket.value.close()
      }
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
      }
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer)
      }
      stopInactivityTimer()
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    })

    return {
      messages,
      inputMessage,
      messageList,
      sendMessage,
      handleEnterSend,
      formatTime,
      handleGoBack,
      handleExitChat,
      reconnecting,
      connectionStatus,
      socket,
      SOCKET_OPEN,
      isHumanMode,
      showHumanButton,
      switchToHuman,
      sessionExpired,
      timeoutWarning,
      countdownSeconds,
      showImagePreview,
      previewImageUrl,
      beforeImageUpload,
      handleImageUpload,
      previewImage,
      formatMessageContent
    }
  }
}
</script>

<style scoped>
.chat-container {
  width: 100%;
  max-width: 800px;
  height: calc(100vh - 20px);
  margin: 10px auto;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

.chat-header {
  background-color: #f5f5f5;
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.back-button {
  margin-right: 15px;
  color: #409EFF;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  flex: 1;
}

.status-tag {
  margin-left: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.exit-button {
  font-size: 13px;
}

.reconnecting {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  color: #666;
  font-size: 14px;
}

.reconnecting .el-icon {
  margin-right: 5px;
}

.session-expired-tip {
  text-align: center;
  padding: 8px 16px;
  background-color: #fdf6ec;
  color: #e6a23c;
  font-size: 13px;
  border-radius: 4px;
  margin: 5px 0;
}

.timeout-warning-tip {
  text-align: center;
  padding: 10px 16px;
  background-color: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  border-radius: 4px;
  margin: 5px 0;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;
  min-height: 0;
}

.message-item {
  margin-bottom: 15px;
  display: flex;
}

.user-message {
  justify-content: flex-end;
}

.admin-message {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 18px;
  position: relative;
}

.user-message .message-bubble {
  background-color: #409EFF;
  color: white;
  border-bottom-right-radius: 10px;
  border-top-right-radius: 10px;
}

.admin-message .message-bubble {
  background-color: white;
  color: #333;
  border-bottom-left-radius: 10px;
  border-top-left-radius: 10px;
  border: 1px solid #e0e0e0;
}

.message-content {
  word-wrap: break-word;
  margin-bottom: 5px;
  line-height: 1.6;
}

.message-text :deep(.list-num) {
  font-weight: 600;
  margin-right: 2px;
}

.message-text :deep(.list-dot) {
  margin-right: 4px;
}

.message-time {
  font-size: 12px;
  opacity: 0.7;
  text-align: right;
}

.message-input {
  padding: 15px;
  border-top: 1px solid #e0e0e0;
  background-color: white;
  flex-shrink: 0;
}

.button-group {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.message-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  display: block;
}

.image-upload {
  display: inline-block;
}

.image-button {
  padding: 8px 12px;
}

.image-preview-dialog .preview-image {
  width: 100%;
  max-height: 80vh;
  object-fit: contain;
}
</style>
