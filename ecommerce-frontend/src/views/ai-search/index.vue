<template>
  <div class="ai-shopping-page">
    <div class="ai-header">
      <h1><span class="ai-icon">🤖</span> AI智能购物顾问</h1>
      <p class="subtitle">您的专属购物助手，可以帮您推荐商品、比较价格、完成下单</p>
    </div>

    <div class="chat-container">
      <div class="message-list" ref="messageList">
        <div v-if="messages.length === 0" class="welcome-tips">
          <div class="tips-content">
            <h3><span class="icon">👋</span> 您好，我是AI购物顾问</h3>
            <p>我可以帮您：</p>
            <ul>
              <li>根据您的需求推荐合适的商品</li>
              <li>比较不同商品的价格和特点</li>
              <li>帮您下单并追踪订单</li>
              <li>解答购物相关的各种问题</li>
            </ul>
            <p class="example">试试这样问我：</p>
            <div class="example-queries">
              <el-tag @click="sendExample('我想买一个2000元左右的手机，用于打游戏')" class="example-tag">
                我想买一个2000元左右的手机，用于打游戏
              </el-tag>
              <el-tag @click="sendExample('帮我推荐一款拍照好看的相机')" class="example-tag">
                帮我推荐一款拍照好看的相机
              </el-tag>
              <el-tag @click="sendExample('有什么适合送父母的礼物推荐吗')" class="example-tag">
                有什么适合送父母的礼物推荐吗
              </el-tag>
            </div>
          </div>
        </div>

        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message-item', message.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="avatar">
            {{ message.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div v-if="message.type === 'text'" class="text-content">
                <p v-for="(paragraph, idx) in formatMessage(message.content)" :key="idx">{{ paragraph }}</p>
              </div>
              <div v-else-if="message.type === 'products'" class="products-content">
                <p class="intro">{{ message.intro }}</p>
                <div class="product-list">
                  <div v-for="product in message.products" :key="product.id" class="product-item" @click="goToProduct(product.id)">
                    <img :src="formatImageUrl(product.pic)" :alt="product.name" class="product-img" />
                    <div class="product-info">
                      <div class="product-name">{{ product.name }}</div>
                      <div class="product-price">¥{{ product.price }}</div>
                      <div class="product-actions">
                        <el-button type="primary" size="small" @click.stop="addToCart(product)">加入购物车</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else-if="message.type === 'cart'" class="cart-content">
                <span class="cart-icon">🛒</span>
                <div class="cart-info">
                  <p>{{ message.productName }} 已加入购物车</p>
                  <div class="cart-actions">
                    <el-button type="primary" size="small" @click="goToCart">查看购物车</el-button>
                    <el-button size="small" @click="continueShopping">继续购物</el-button>
                  </div>
                </div>
              </div>
            </div>
            <div class="message-time">{{ message.time }}</div>
          </div>
        </div>

        <div v-if="loading" class="message-item ai-message loading-message">
          <div class="avatar">🤖</div>
          <div class="message-content">
            <div class="message-bubble">
              <div class="loading-indicator">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>AI正在思考中...</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          placeholder="请输入您的问题或需求..."
          :rows="2"
          @keydown.enter.exact="handleSend"
          resize="none"
        ></el-input>
        <el-button type="primary" @click="handleSend" class="send-button" :disabled="!inputMessage.trim() || loading">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import axios from 'axios'
import { addToCart as addToCartApi } from '../../api/cart'

export default {
  name: 'AiShopping',
  components: {
    Loading
  },
  setup() {
    const router = useRouter()
    const messages = ref([])
    const chatHistory = ref([]) // 保存对话历史，用于AI记忆
    const inputMessage = ref('')
    const messageList = ref(null)
    const loading = ref(false)
    let messageId = 0

    const scrollToBottom = () => {
      nextTick(() => {
        if (messageList.value) {
          messageList.value.scrollTop = messageList.value.scrollHeight
        }
      })
    }

    const formatMessage = (content) => {
      if (!content) return []
      return content.split('\n').filter(p => p.trim())
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

    const formatTime = () => {
      const now = new Date()
      const hours = String(now.getHours()).padStart(2, '0')
      const minutes = String(now.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    }

    const addMessage = (role, content, type = 'text', extra = {}) => {
      const message = {
        id: ++messageId,
        role,
        content,
        type,
        time: formatTime(),
        ...extra
      }
      messages.value.push(message)
      scrollToBottom()
      return message
    }

    const sendExample = (query) => {
      inputMessage.value = query
      handleSend()
    }

    const handleSend = async () => {
      const content = inputMessage.value.trim()
      if (!content || loading.value) return

      loading.value = true
      inputMessage.value = ''

      // 添加用户消息到聊天历史
      chatHistory.value.push({
        role: 'user',
        content: content
      })

      addMessage('user', content)

      try {
        const res = await axios.post('/api/ai/chat', {
          message: content,
          history: chatHistory.value, // 发送对话历史给后端
          context: true
        })

        if (res.data.code === 200) {
          const data = res.data.data
          let aiReply = ''
          let aiType = 'text'
          let aiExtra = {}

          if (data.type === 'products') {
            aiReply = data.reply
            aiType = 'products'
            aiExtra = {
              products: data.products,
              intro: data.intro || '为您找到以下商品：'
            }
          } else if (data.type === 'order') {
            aiReply = ''
            aiType = 'order'
            aiExtra = {
              orderId: data.orderId,
              orderNo: data.orderNo,
              payPrice: data.payPrice
            }
          } else if (data.type === 'cart') {
            aiReply = ''
            aiType = 'cart'
            aiExtra = {
              productName: data.productName
            }
          } else {
            aiReply = data.reply || data
            aiType = 'text'
          }

          // 添加AI回复到聊天历史
          chatHistory.value.push({
            role: 'assistant',
            content: aiReply
          })

          addMessage('ai', aiReply, aiType, aiExtra)
        } else {
          addMessage('ai', '抱歉，我遇到了一些问题，请稍后再试。')
        }
      } catch (error) {
        console.error('AI响应失败', error)
        addMessage('ai', '抱歉，AI服务暂时不可用，请稍后再试。')
      } finally {
        loading.value = false
      }
    }

    const addToCart = async (product) => {
      try {
        await addToCartApi({ productId: product.id, quantity: 1 })
        ElMessage.success(`${product.name} 已加入购物车`)
      } catch (error) {
        console.error('加入购物车失败', error)
        ElMessage.error('加入购物车失败')
      }
    }

    const goToProduct = (id) => {
      router.push(`/product/${id}`)
    }

    const goToCart = () => {
      router.push('/cart')
    }

    const continueShopping = () => {
      addMessage('ai', '好的，请问还有什么需要帮您推荐的吗？')
    }

    onMounted(() => {
      scrollToBottom()
    })

    return {
      messages,
      inputMessage,
      messageList,
      loading,
      handleSend,
      formatMessage,
      formatImageUrl,
      sendExample,
      addToCart,
      goToProduct,
      goToCart,
      continueShopping
    }
  }
}
</script>

<style scoped>
.ai-shopping-page {
  width: 100%;
  max-width: 1000px;
  margin: 57px auto;
  padding: 30px 20px;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.ai-header {
  text-align: center;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.ai-header h1 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.ai-icon {
  font-size: 32px;
  margin-right: 8px;
}

.subtitle {
  font-size: 14px;
  color: #666;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: 0;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

.welcome-tips {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.tips-content {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  max-width: 500px;
  text-align: left;
}

.tips-content h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.tips-content .icon {
  margin-right: 8px;
}

.tips-content p {
  color: #666;
  margin-bottom: 10px;
}

.tips-content ul {
  list-style: none;
  padding: 0;
  margin: 0 0 20px 0;
}

.tips-content li {
  padding: 6px 0;
  color: #666;
  font-size: 14px;
}

.tips-content li::before {
  content: "✓ ";
  color: #67c23a;
  margin-right: 8px;
}

.example {
  font-weight: bold;
  color: #333 !important;
  margin-top: 20px;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.example-tag {
  cursor: pointer;
  padding: 8px 12px;
  font-size: 13px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.user-message {
  flex-direction: row-reverse;
}

.ai-message {
  flex-direction: row;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.user-message .avatar {
  background: #409eff;
}

.ai-message .avatar {
  background: #f0f0f0;
}

.message-content {
  max-width: 75%;
  margin: 0 12px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
}

.user-message .message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.text-content p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.user-message .message-time {
  text-align: right;
}

.loading-message .loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.loading-message .el-icon {
  font-size: 18px;
}

.products-content .intro {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 14px;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-item {
  display: flex;
  background: #f9f9f9;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.product-item:hover {
  background: #f0f0f0;
}

.product-img {
  width: 80px;
  height: 80px;
  object-fit: contain;
  border-radius: 4px;
  margin-right: 12px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 16px;
  color: #ff4400;
  font-weight: bold;
  margin-bottom: 8px;
}

.product-actions {
  display: flex;
  gap: 8px;
}

.cart-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-icon {
  font-size: 32px;
}

.cart-info p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.cart-actions {
  display: flex;
  gap: 8px;
}

.input-area {
  padding: 15px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  flex-shrink: 0;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
}

.send-button {
  height: 52px;
  padding: 0 24px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
}

.send-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

.send-button:disabled {
  background: #ccc;
  border-color: #ccc;
}
</style>
