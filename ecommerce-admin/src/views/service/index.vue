<template>
  <div class="service-container">
    <div class="service-header">
      <h2>客服中心</h2>
      <a-tag :color="connectionStatus === 'connected' ? 'green' : 'red'" class="status-tag">
        {{ connectionStatus === 'connected' ? '已连接' : '连接断开' }}
      </a-tag>
    </div>
    <div class="service-body">
      <div class="session-list">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <a-input 
            v-model:value="searchKeyword" 
            placeholder="搜索用户名或消息..." 
            allow-clear
            class="search-input"
          >
            <template #prefix>
              <a-icon type="search" />
            </template>
          </a-input>
        </div>
        
        <!-- 全选栏 -->
        <div class="select-all-bar" v-if="filteredSessions().length > 0">
          <a-checkbox 
            :checked="selectAll" 
            @change="handleSelectAll"
          >全选</a-checkbox>
          <template v-if="selectedSessions.length > 0">
            <a-button 
              type="danger" 
              size="small" 
              @click="batchDelete"
              class="batch-delete-btn"
            >批量删除</a-button>
            <span class="selected-count">
              已选 {{ selectedSessions.length }} 项
            </span>
          </template>
        </div>
        
        <div 
          v-for="session in filteredSessions()"
          :key="session.sessionId"
          :class="['session-item', currentSessionId === session.sessionId ? 'active' : '']"
          @click="selectSession(session.sessionId)"
        >
          <a-checkbox 
            :checked="selectedSessions.includes(session.sessionId)"
            @click.stop="toggleSelect(session.sessionId)"
            class="session-checkbox"
          ></a-checkbox>
          <div class="session-info">
            <div class="session-title">
              用户 {{ session.userName }}
              <a-tag :color="session.isHumanMode ? 'orange' : 'blue'" size="small" class="mode-tag">
                {{ session.isHumanMode ? '人工' : 'AI' }}
              </a-tag>
            </div>
            <div class="session-last-message">{{ session.lastMessage }}</div>
          </div>
          <div class="session-actions">
            <a-badge v-if="session.unreadCount > 0" :count="session.unreadCount" :offset="[10, 0]" />
            <a-button type="text" danger size="small" @click.stop="confirmDelete(session.sessionId)" class="delete-button">
              删除
            </a-button>
          </div>
        </div>
        <div v-if="reconnecting" class="reconnecting">
          <a-spin size="small" />
          <span>正在重新连接...</span>
        </div>
      </div>
      <div class="chat-area" v-if="currentSessionId">
        <div class="chat-header">
          <h3>与 {{ currentSession?.userName }} 的对话</h3>
        </div>
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
                  />
                </template>
                <template v-else>
                  {{ message.content }}
                </template>
              </div>
              <div class="message-time">{{ formatTime(message.createTime) }}</div>
            </div>
          </div>
        </div>
        <div class="message-input">
          <a-textarea 
            v-model:value="inputMessage" 
            placeholder="请输入消息..." 
            :rows="3"
            @keydown.enter="handleEnterSend"
          ></a-textarea>
          <div class="button-group">
            <a-upload
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :custom-request="handleImageUpload"
              accept="image/*"
            >
              <a-button type="default">
                <template #icon><PictureOutlined /></template>
              </a-button>
            </a-upload>
            <a-button 
              type="primary" 
              @click="sendMessage" 
              class="send-button"
              :loading="!socket || (socket && socket.readyState !== SOCKET_OPEN)"
            >
              发送
            </a-button>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <a-empty description="请选择一个会话开始聊天" />
      </div>
    </div>

    <!-- 图片预览对话框 -->
    <a-modal 
      v-model:open="showImagePreview" 
      :footer="null"
      width="80%"
      class="image-preview-modal"
    >
      <img :src="previewImageUrl" class="preview-image" />
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { message, Modal, Spin, Tag, Empty } from 'ant-design-vue'
import { PictureOutlined } from '@ant-design/icons-vue'

export default {
  name: 'Service',
  components: {
    Spin,
    Tag,
    Empty,
    PictureOutlined
  },
  setup() {
    const sessions = ref([])
    const messages = ref([])
    const inputMessage = ref('')
    const currentSessionId = ref('')
    const currentSession = ref(null)
    const messageList = ref(null)
    const socket = ref(null)
    let reconnectTimer = null
    let heartbeatTimer = null
    const reconnecting = ref(false)
    const connectionStatus = ref('connecting')
    // 把 WebSocket.OPEN 存成常量，避免模板直接访问
    const SOCKET_OPEN = 1
    
    // 批量删除相关
    const selectedSessions = ref([])
    const selectAll = ref(false)
    
    // 模糊搜索相关
    const searchKeyword = ref('')
    
    // 图片预览相关
    const showImagePreview = ref(false)
    const previewImageUrl = ref('')

    // 切换单个会话选择
    const toggleSelect = (sessionId) => {
      const index = selectedSessions.value.indexOf(sessionId)
      if (index > -1) {
        selectedSessions.value.splice(index, 1)
      } else {
        selectedSessions.value.push(sessionId)
      }
      // 更新全选状态
      selectAll.value = sessions.value.length > 0 && sessions.value.every(s => selectedSessions.value.includes(s.sessionId))
    }
    
    // 全选/取消全选
    const handleSelectAll = () => {
      selectAll.value = !selectAll.value
      if (selectAll.value) {
        selectedSessions.value = sessions.value.map(s => s.sessionId)
      } else {
        selectedSessions.value = []
      }
    }
    
    // 获取过滤后的会话列表
    const filteredSessions = () => {
      if (!searchKeyword.value.trim()) {
        return sessions.value
      }
      const keyword = searchKeyword.value.toLowerCase()
      return sessions.value.filter(session => {
        const userName = session.userName ? session.userName.toLowerCase() : ''
        const lastMessage = session.lastMessage ? session.lastMessage.toLowerCase() : ''
        return userName.includes(keyword) || lastMessage.includes(keyword)
      })
    }
    
    // 批量删除会话
    const batchDelete = () => {
      if (selectedSessions.value.length === 0) {
        message.warning('请先选择要删除的会话')
        return
      }
      
      Modal.confirm({
        title: '确认批量删除',
        content: `确定要删除选中的 ${selectedSessions.value.length} 个会话吗？删除后将无法恢复。`,
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          try {
            for (const sessionId of selectedSessions.value) {
              const res = await fetch(`/api/chat/session/${sessionId}`, {
                method: 'DELETE'
              })
              const data = await res.json()
              if (data.code !== 200) {
                console.error(`删除会话 ${sessionId} 失败`)
              }
            }
            message.success('批量删除成功')
            // 清空选中状态
            selectedSessions.value = []
            selectAll.value = false
            // 重新加载会话列表
            loadSessions()
          } catch (error) {
            console.error('批量删除失败', error)
            message.error('批量删除失败')
          }
        }
      })
    }
    
    // 删除会话
    const deleteSession = async (sessionId) => {
      try {
        const res = await fetch(`/api/chat/session/${sessionId}`, {
          method: 'DELETE'
        })
        const data = await res.json()
        if (data.code === 200) {
          message.success('删除成功')
          // 如果删除的是当前会话，清空消息列表
          if (currentSessionId.value === sessionId) {
            currentSessionId.value = ''
            currentSession.value = null
            messages.value = []
          }
          // 重新加载会话列表
          loadSessions()
        } else {
          message.error(data.message || '删除失败')
        }
      } catch (error) {
        console.error('删除会话失败', error)
        message.error('删除会话失败')
      }
    }

    // 确认删除会话
    const confirmDelete = (sessionId) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这个会话吗？删除后将无法恢复。',
        okText: '确定',
        cancelText: '取消',
        onOk: () => {
          deleteSession(sessionId)
        }
      })
    }

    // 加载会话列表
    const loadSessions = async () => {
      try {
        // 假设管理员ID为1
        const adminId = 1
        const res = await fetch(`/api/chat/sessions?adminId=${adminId}`)
        const data = await res.json()
        
        console.log('会话列表响应:', data)
        
        if (data.code === 200) {
          const sessionListData = data.data || []
          console.log('会话列表:', sessionListData)
          
          if (sessionListData.length === 0) {
            sessions.value = []
            return
          }
          
          const sessionList = []
          
          for (const sessionInfo of sessionListData) {
            try {
              // 获取未读消息数
              const unreadCountRes = await fetch(`/api/chat/unreadCount?sessionId=${sessionInfo.sessionId}`)
              const unreadCountData = await unreadCountRes.json()
              
              console.log(`会话 ${sessionInfo.sessionId} 的未读数:`, unreadCountData)
              
              // 获取消息列表来判断是否是人工模式
              const messagesRes = await fetch(`/api/chat/message/${sessionInfo.sessionId}`)
              const messagesData = await messagesRes.json()
              let isHumanMode = false
              if (messagesData.code === 200 && messagesData.data) {
                // 检查是否有转人工的消息
                isHumanMode = messagesData.data.some(msg => 
                  msg.content && (
                    msg.content.includes('转接人工客服') || 
                    msg.content.includes('人工客服中') ||
                    msg.content.includes('当前客服繁忙')
                  )
                )
              }
              
              sessionList.push({
                sessionId: sessionInfo.sessionId,
                userId: sessionInfo.userId || '',
                userName: sessionInfo.userName || '用户',
                lastMessage: sessionInfo.lastMessage || '',
                unreadCount: unreadCountData.data || 0,
                isHumanMode: isHumanMode
              })
            } catch (innerError) {
              console.error(`加载会话 ${sessionInfo.sessionId} 详情失败:`, innerError)
              // 即使获取未读消息失败，也添加会话到列表
              sessionList.push({
                sessionId: sessionInfo.sessionId,
                userId: sessionInfo.userId || '',
                userName: sessionInfo.userName || '用户',
                lastMessage: sessionInfo.lastMessage || '',
                unreadCount: 0,
                isHumanMode: false
              })
            }
          }
          
          sessions.value = sessionList
          console.log('最终会话列表:', sessionList)
        }
      } catch (error) {
        console.error('加载会话列表失败', error)
        message.error('加载会话列表失败: ' + error.message)
      }
    }

    // 选择会话
    const selectSession = async (sessionId) => {
      currentSessionId.value = sessionId
      
      // 查找当前会话
      const session = sessions.value.find(s => s.sessionId === sessionId)
      if (session) {
        currentSession.value = session
      }
      
      // 加载聊天历史记录
      try {
        const res = await fetch(`/api/chat/message/${sessionId}`)
        const data = await res.json()
        if (data.code === 200) {
          messages.value = data.data || []
          
          // 标记消息为已读
          await fetch(`/api/chat/markAsRead?sessionId=${sessionId}`, {
            method: 'POST'
          })
          
          // 重新加载会话列表，更新未读计数
          loadSessions()
          
          // 滚动到底部
          scrollToBottom()
        }
      } catch (error) {
        console.error('加载聊天历史失败', error)
        message.error('加载聊天历史失败')
      }
    }

    // 建立WebSocket连接
    const connectWebSocket = () => {
      // 假设管理员ID为1
      const adminId = 1
      const adminName = '客服'
      
      // 建立WebSocket连接（同源，本地走 Vite 代理，线上走 Pages Function 透传）
      socket.value = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/api/ws/chat?userType=admin&id=${adminId}`)
      
      socket.value.onopen = () => {
        console.log('WebSocket连接已建立')
        connectionStatus.value = 'connected'
        reconnecting.value = false
        startHeartbeat()
      }
      
      socket.value.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          console.log('收到消息:', msg)
          
          if (msg.type === 'heartbeat') {
            return
          }
          
          if (!msg.content && !msg.imageUrl) {
            console.warn('收到空消息:', msg)
            return
          }
          
          if (msg.sessionId === currentSessionId.value) {
            messages.value.push(msg)
            scrollToBottom()
          } else {
            loadSessions()
          }
        } catch (error) {
          console.error('解析消息失败', error)
        }
      }
      
      socket.value.onerror = (error) => {
        console.error('WebSocket连接错误', error)
        connectionStatus.value = 'disconnected'
      }
      
      socket.value.onclose = () => {
        console.log('WebSocket连接已关闭')
        connectionStatus.value = 'disconnected'
        startReconnect()
      }
    }

    // 回车键发送
    const handleEnterSend = (e) => {
      // 阻止默认行为（避免换行）
      e.preventDefault()
      
      if (!inputMessage.value.trim()) {
        message.warning('消息不能为空')
        return
      }
      
      sendMessage()
    }
    
    // 发送消息
    const sendMessage = () => {
      if (!inputMessage.value.trim()) {
        message.warning('请输入消息内容')
        return
      }
      
      if (!socket.value || socket.value.readyState !== SOCKET_OPEN) {
        message.error('连接已断开，正在重新连接...')
        connectWebSocket()
        return
      }
      
      if (!currentSessionId.value) {
        message.warning('请先选择一个会话')
        return
      }
      
      // 假设管理员ID为1
      const adminId = 1
      const adminName = '客服'
      
      // 从当前会话中获取用户ID
      const userId = currentSession.value?.userId || ''
      
      const msg = {
        userId: userId,
        userName: currentSession.value?.userName || '用户',
        adminId: adminId,
        adminName: adminName,
        content: inputMessage.value.trim(),
        type: 1,
        createTime: new Date(),
        sessionId: currentSessionId.value
      }
      
      // 发送消息
      socket.value.send(JSON.stringify(msg))
      
      // 直接添加到消息列表，避免等待服务器响应
      messages.value.push(msg)
      scrollToBottom()
      
      // 清空输入框
      inputMessage.value = ''
    }

    // 滚动到底部
    const scrollToBottom = () => {
      setTimeout(() => {
        if (messageList.value) {
          messageList.value.scrollTop = messageList.value.scrollHeight
        }
      }, 100)
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      const date = new Date(time)
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    }

    // 开始重连
    const startReconnect = () => {
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
      }
      reconnecting.value = true
      reconnectTimer = setTimeout(() => {
        console.log('尝试重新连接...')
        connectWebSocket()
      }, 3000)
    }

    // 开始心跳
    const startHeartbeat = () => {
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer)
      }
      heartbeatTimer = setInterval(() => {
        if (socket.value && socket.value.readyState === SOCKET_OPEN) {
          // 发送心跳消息
          socket.value.send(JSON.stringify({ type: 'heartbeat' }))
        }
      }, 30000)
    }

    // 图片上传前验证
    const beforeImageUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      
      if (!isImage) {
        message.error('只能上传图片文件!')
        return false
      }
      if (!isLt5M) {
        message.error('图片大小不能超过 5MB!')
        return false
      }
      return true
    }

    // 上传图片
    const handleImageUpload = async (options) => {
      const { file } = options
      
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
          
          const adminId = 1
          const adminName = '客服'
          const userId = currentSession.value?.userId || ''
          
          const msg = {
            userId: userId,
            userName: currentSession.value?.userName || '用户',
            adminId: adminId,
            adminName: adminName,
            content: '[图片]',
            type: 1,
            createTime: new Date(),
            sessionId: currentSessionId.value,
            messageType: 1,
            imageUrl: imageUrl
          }
          
          socket.value.send(JSON.stringify(msg))
          messages.value.push(msg)
          scrollToBottom()
        } else {
          message.error('图片上传失败')
        }
      } catch (error) {
        console.error('上传图片失败', error)
        message.error('图片上传失败')
      }
    }

    // 预览图片
    const previewImage = (url) => {
      previewImageUrl.value = url
      showImagePreview.value = true
    }

    // 监听消息变化，自动滚动到底部
    watch(messages, () => {
      scrollToBottom()
    }, { deep: true })

    // 组件挂载时加载会话列表并建立连接
    onMounted(() => {
      loadSessions()
      connectWebSocket()
    })

    // 组件卸载前清理
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
    })

    return {
      sessions,
      messages,
      inputMessage,
      currentSessionId,
      currentSession,
      messageList,
      selectSession,
      deleteSession,
      confirmDelete,
      sendMessage,
      handleEnterSend,
      formatTime,
      reconnecting,
      connectionStatus,
      socket,
      SOCKET_OPEN,
      // 批量删除相关
      selectedSessions,
      selectAll,
      toggleSelect,
      handleSelectAll,
      batchDelete,
      // 模糊搜索相关
      searchKeyword,
      filteredSessions,
      // 图片相关
      showImagePreview,
      previewImageUrl,
      beforeImageUpload,
      handleImageUpload,
      previewImage
    }
  }
}
</script>

<style scoped>
.service-container {
  padding: 20px;
}

.service-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.service-header h2 {
  margin: 0;
}

.status-tag {
  margin-left: 10px;
}

.reconnecting {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15px;
  color: #666;
  font-size: 14px;
  border-top: 1px solid #e0e0e0;
}

.reconnecting a-spin {
  margin-right: 8px;
}

.service-body {
  display: flex;
  height: 70vh;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.session-list {
  width: 300px;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.search-bar {
  padding: 10px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fff;
}

.search-input {
  width: 100%;
}

.select-all-bar {
  padding: 10px 15px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #fff;
}

.batch-delete-btn {
  margin-left: auto;
}

.selected-count {
  font-size: 12px;
  color: #666;
}

.session-item {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.session-checkbox {
  margin-right: 10px;
  flex-shrink: 0;
}

.session-item:hover {
  background-color: #f0f0f0;
}

.session-item.active {
  background-color: #e6f7ff;
}

.session-title {
  font-weight: bold;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-tag {
  font-size: 10px;
  line-height: 1;
  padding: 0 4px;
}

.session-last-message {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 150px;
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 5px;
}

.delete-button {
  font-size: 12px;
  padding: 2px 5px;
  height: auto;
}

.unread-count {
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 12px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  background-color: #f5f5f5;
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
}

.chat-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.message-item {
  margin-bottom: 15px;
  display: flex;
}

.user-message {
  justify-content: flex-start;
}

.admin-message {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 18px;
  position: relative;
}

.user-message .message-bubble {
  background-color: white;
  color: #333;
  border-bottom-left-radius: 4px;
  border: 1px solid #e0e0e0;
}

.admin-message .message-bubble {
  background-color: #409EFF;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-content {
  word-wrap: break-word;
  margin-bottom: 5px;
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
}

.button-group {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.send-button {
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f9f9f9;
}

.message-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  display: block;
}

.image-preview-modal .preview-image {
  width: 100%;
  max-height: 80vh;
  object-fit: contain;
}
</style>