<template>
  <div class="knowledge-container">
    <!-- 页面头部：标题和RAG服务状态 -->
    <div class="knowledge-header">
      <h2>知识库管理</h2>
      <div>
        <!-- RAG服务连接状态标签 -->
        <a-tag :color="serviceStatus.available ? 'green' : 'red'" class="status-tag">
          {{ serviceStatus.available ? 'RAG服务已连接' : 'RAG服务未连接' }}
        </a-tag>
        <!-- 同步到向量库按钮 -->
        <a-button type="primary" size="small" @click="syncToRag" :loading="syncing" style="margin-left: 10px;">
          同步到向量库
        </a-button>
      </div>
    </div>

    <!-- 知识检索测试卡片 -->
    <a-card class="search-card">
      <template #title>
        <span>知识检索测试</span>
      </template>
      <div class="search-section">
        <!-- 搜索输入框 -->
        <a-input-search
          v-model:value="searchQuery"
          placeholder="输入问题测试知识库检索..."
          enter-button="搜索"
          @search="handleSearch"
          :loading="searching"
        />
        <!-- 搜索结果展示 -->
        <div v-if="searchResults.length > 0" class="search-results">
          <h4>检索结果（{{ searchResults.length }}条）<a-tag v-if="searchSource" :color="getSourceColor(searchSource)" style="margin-left: 8px;">{{ getSourceText(searchSource) }}</a-tag></h4>
          <div v-for="(result, index) in searchResults" :key="index" class="result-item">
            <div class="result-content">{{ result.content }}</div>
            <a-tag v-if="result.source" size="small" style="margin-top: 5px;">{{ result.source === 'database' ? '数据库' : '向量库' }}</a-tag>
          </div>
        </div>
        <!-- 无结果提示 -->
        <div v-else-if="searched && searchResults.length === 0" class="search-results">
          <a-empty description="未找到相关知识">
            <template #extra>
              <a-button type="primary" size="small" @click="syncToRag" v-if="vectorStoreCount === 0">同步知识到向量库</a-button>
            </template>
          </a-empty>
          <div v-if="vectorStoreCount !== null" style="margin-top: 10px; color: #999;">
            向量库文档数: {{ vectorStoreCount }}
          </div>
        </div>
      </div>
    </a-card>

    <!-- 添加知识卡片 -->
    <a-card class="add-card">
      <template #title>
        <span>添加知识</span>
      </template>
      <a-form :model="addForm" layout="vertical">
        <!-- 知识内容输入 -->
        <a-form-item label="知识内容">
          <a-textarea
            v-model:value="addForm.content"
            placeholder="输入要添加到知识库的内容..."
            :rows="4"
          />
        </a-form-item>
        <!-- 分类选择 -->
        <a-form-item label="分类">
          <a-select v-model:value="addForm.category" style="width: 200px;">
            <a-select-option value="general">通用</a-select-option>
            <a-select-option value="product">商品咨询</a-select-option>
            <a-select-option value="logistics">物流问题</a-select-option>
            <a-select-option value="after_sale">售后问题</a-select-option>
            <a-select-option value="coupon">优惠券活动</a-select-option>
          </a-select>
        </a-form-item>
        <!-- AI识别关键词区域 -->
        <a-form-item label="关键词">
          <div class="keywords-section">
            <a-input
              v-model:value="addForm.keywords"
              placeholder="点击「AI识别关键词」自动提取，或手动输入关键词（逗号分隔）"
            />
            <a-button
              type="default"
              @click="handleExtractKeywords"
              :loading="extractingKeywords"
              style="margin-top: 8px;"
            >
              AI识别关键词
            </a-button>
          </div>
        </a-form-item>
        <!-- 确认提交区域 -->
        <a-form-item v-if="keywordsConfirmed" class="keywords-confirmed-tip">
          <a-tag color="green">✓ 关键词已确认</a-tag>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleAddKnowledge" :loading="adding" :disabled="!keywordsConfirmed">
            添加到知识库
          </a-button>
          <span v-if="!keywordsConfirmed && addForm.keywords" style="margin-left: 12px; color: #faad14; font-size: 13px;">
            请确认关键词无误后再添加
          </span>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 知识库列表卡片 -->
    <a-card class="list-card">
      <template #title>
        <span>知识库列表（共 {{ knowledgeList.length }} 条）</span>
      </template>
      <!-- 知识列表表格 -->
      <a-table :columns="columns" :data-source="knowledgeList" row-key="id" :pagination="{ pageSize: 10 }">
        <template #bodyCell="{ column, record }">
          <!-- 内容列：点击查看详情 -->
          <template v-if="column.key === 'content'">
            <div class="content-cell" @click="showDetail(record)" style="cursor: pointer; color: #1890ff;">{{ record.content }}</div>
          </template>
          <!-- 分类列：显示带颜色标签 -->
          <template v-if="column.key === 'category'">
            <a-tag :color="getCategoryColor(record.category)">{{ getCategoryName(record.category) }}</a-tag>
          </template>
          <!-- 时间列：格式化显示 -->
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <!-- 操作列：编辑和删除 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="showEdit(record)">编辑</a-button>
              <a-popconfirm
                title="确定要删除这条知识吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗：显示知识的完整信息 -->
    <a-modal v-model:open="detailVisible" title="知识详情" :footer="null" width="700px">
      <a-descriptions :column="1" bordered v-if="currentKnowledge">
        <a-descriptions-item label="ID">{{ currentKnowledge.id }}</a-descriptions-item>
        <a-descriptions-item label="分类">
          <a-tag :color="getCategoryColor(currentKnowledge.category)">{{ getCategoryName(currentKnowledge.category) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="关键词">{{ currentKnowledge.keywords || '无' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatDate(currentKnowledge.createdAt) }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ formatDate(currentKnowledge.updatedAt) }}</a-descriptions-item>
        <a-descriptions-item label="内容">
          <div style="white-space: pre-wrap;">{{ currentKnowledge.content }}</div>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 编辑弹窗：修改知识内容和分类 -->
    <a-modal v-model:open="editVisible" title="编辑知识" @ok="handleEdit" :confirm-loading="editLoading" width="700px">
      <a-form :model="editForm" layout="vertical" v-if="editForm">
        <a-form-item label="分类" required>
          <a-select v-model:value="editForm.category" style="width: 200px;">
            <a-select-option value="general">通用</a-select-option>
            <a-select-option value="product">商品咨询</a-select-option>
            <a-select-option value="logistics">物流问题</a-select-option>
            <a-select-option value="after_sale">售后问题</a-select-option>
            <a-select-option value="coupon">优惠券活动</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="知识内容" required>
          <a-textarea v-model:value="editForm.content" :rows="8" placeholder="请输入知识内容" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 关键词确认弹窗 -->
    <a-modal
      v-model:open="keywordsConfirmVisible"
      title="确认AI识别的关键词"
      @ok="confirmKeywords"
      ok-text="确认无误"
      cancel-text="重新编辑"
      width="500px"
    >
      <div style="margin-bottom: 16px;">
        <p style="color: #666; margin-bottom: 12px;">AI已从知识内容中提取以下关键词，请确认是否正确：</p>
        <div style="margin-bottom: 12px;">
          <span style="font-weight: 500;">AI提取的关键词：</span>
          <div style="margin-top: 8px;">
            <a-tag v-for="(kw, idx) in aiKeywordsList" :key="idx" color="blue" style="margin: 2px 4px;">{{ kw }}</a-tag>
          </div>
        </div>
        <a-divider style="margin: 12px 0;" />
        <div>
          <span style="font-weight: 500;">您可以修改关键词：</span>
          <a-input
            v-model:value="addForm.keywords"
            placeholder="用逗号分隔关键词"
            style="margin-top: 8px;"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
/**
 * 知识库管理页面
 * 
 * 功能说明：
 * 1. 显示RAG服务连接状态
 * 2. 知识检索测试：输入问题测试向量检索效果
 * 3. 添加知识：将新知识添加到数据库和向量库
 * 4. 知识列表：展示所有知识，支持查看详情、编辑、删除
 * 5. 同步向量库：将数据库知识同步到向量库
 * 
 * @author ecommerce-team
 * @since 1.0.0
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { message } from 'ant-design-vue'

export default {
  name: 'Knowledge',
  setup() {
    // ==================== 状态定义 ====================
    
    /** RAG服务状态 */
    const serviceStatus = ref({ available: false, service: '' })
    
    /** 搜索相关状态 */
    const searchQuery = ref('')           // 搜索查询文本
    const searchResults = ref([])         // 搜索结果列表
    const searching = ref(false)          // 搜索中状态
    const searched = ref(false)           // 是否已搜索
    const searchSource = ref('')          // 搜索结果来源
    const vectorStoreCount = ref(null)    // 向量库文档数量
    
    /** 添加知识相关状态 */
    const adding = ref(false)             // 添加中状态
    const syncing = ref(false)            // 同步中状态
    const extractingKeywords = ref(false) // AI识别关键词中状态
    const keywordsConfirmed = ref(false)  // 关键词已确认
    const keywordsConfirmVisible = ref(false) // 关键词确认弹窗
    const aiKeywordsList = ref([])        // AI提取的关键词列表
    const addForm = ref({
      content: '',
      category: 'general',
      keywords: ''
    })
    
    /** 知识列表 */
    const knowledgeList = ref([])
    
    /** 详情弹窗相关状态 */
    const detailVisible = ref(false)      // 详情弹窗可见性
    const currentKnowledge = ref(null)    // 当前查看的知识
    
    /** 编辑弹窗相关状态 */
    const editVisible = ref(false)        // 编辑弹窗可见性
    const editLoading = ref(false)        // 编辑保存中状态
    const editForm = ref({
      id: null,
      content: '',
      category: ''
    })
    
    /** 表格列定义 */
    const columns = [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
      { title: '内容', dataIndex: 'content', key: 'content' },
      { title: '关键词', dataIndex: 'keywords', key: 'keywords', width: 180 },
      { title: '分类', dataIndex: 'category', key: 'category', width: 100 },
      { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
      { title: '操作', key: 'action', width: 80 }
    ]

    // ==================== API调用方法 ====================

    /**
     * 获取带认证的fetch选项
     */
    const getAuthHeaders = () => {
      const token = localStorage.getItem('adminToken')
      return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': token } : {})
      }
    }

    /**
     * 检查RAG服务状态
     */
    const checkStatus = async () => {
      try {
        const res = await fetch('/api/knowledge/status', { headers: getAuthHeaders() })
        const data = await res.json()
        if (data.code === 200) {
          serviceStatus.value = data.data
        }
      } catch (error) {
        console.error('检查服务状态失败', error)
        serviceStatus.value = { available: false, service: 'Unavailable' }
      }
    }

    /**
     * 加载知识库列表
     */
    const loadKnowledgeList = async () => {
      try {
        const res = await fetch('/api/knowledge/list', { headers: getAuthHeaders() })
        const data = await res.json()
        if (data.code === 200) {
          knowledgeList.value = data.data || []
        }
      } catch (error) {
        console.error('加载知识库列表失败', error)
      }
    }

    /**
     * 处理知识检索
     * 调用后端API进行向量检索
     */
    const handleSearch = async () => {
      if (!searchQuery.value.trim()) {
        message.warning('请输入搜索内容')
        return
      }

      searching.value = true
      searched.value = false
      try {
        const res = await fetch('/api/knowledge/search', {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({
            query: searchQuery.value,
            topK: 3,
            minScore: 0.3
          })
        })
        const data = await res.json()
        if (data.code === 200 && data.data.success) {
          searchResults.value = data.data.results || []
          searchSource.value = data.data.source || ''
          vectorStoreCount.value = data.data.vectorStoreCount ?? null
          searched.value = true
        } else {
          message.error('搜索失败：' + (data.data?.error || '未知错误'))
        }
      } catch (error) {
        console.error('搜索失败', error)
        message.error('搜索失败')
      } finally {
        searching.value = false
      }
    }

    /**
     * AI识别关键词
     * 调用后端API使用DeepSeek提取关键词
     */
    const handleExtractKeywords = async () => {
      if (!addForm.value.content.trim()) {
        message.warning('请先输入知识内容')
        return
      }

      extractingKeywords.value = true
      keywordsConfirmed.value = false
      try {
        const res = await fetch('/api/knowledge/extractKeywords', {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({
            content: addForm.value.content,
            category: addForm.value.category
          })
        })
        const data = await res.json()
        if (data.code === 200) {
          const aiKw = data.data.aiKeywords || ''
          addForm.value.keywords = aiKw
          aiKeywordsList.value = aiKw ? aiKw.split(',').filter(k => k.trim()) : []
          if (aiKeywordsList.value.length > 0) {
            keywordsConfirmVisible.value = true
          } else {
            message.warning('AI未能提取关键词，请手动输入')
            keywordsConfirmed.value = true
          }
        } else {
          message.error('AI识别关键词失败：' + (data.message || '未知错误'))
          keywordsConfirmed.value = true
        }
      } catch (error) {
        console.error('AI识别关键词失败', error)
        message.error('AI识别关键词失败，请手动输入')
        keywordsConfirmed.value = true
      } finally {
        extractingKeywords.value = false
      }
    }

    /**
     * 确认关键词
     * 管理员确认AI提取的关键词无误
     */
    const confirmKeywords = () => {
      if (!addForm.value.keywords || !addForm.value.keywords.trim()) {
        message.warning('关键词不能为空')
        return
      }
      keywordsConfirmed.value = true
      keywordsConfirmVisible.value = false
      message.success('关键词已确认')
    }

    /**
     * 处理添加知识
     * 将知识保存到数据库并同步到向量库
     */
    const handleAddKnowledge = async () => {
      if (!addForm.value.content.trim()) {
        message.warning('请输入知识内容')
        return
      }

      if (!keywordsConfirmed.value) {
        message.warning('请先确认关键词')
        return
      }

      adding.value = true
      try {
        const res = await fetch('/api/knowledge/add', {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({
            content: addForm.value.content,
            keywords: addForm.value.keywords,
            metadata: { category: addForm.value.category }
          })
        })
        const data = await res.json()
        if (data.code === 200) {
          message.success(data.data.message || '添加成功')
          addForm.value.content = ''
          addForm.value.keywords = ''
          keywordsConfirmed.value = false
          aiKeywordsList.value = []
          loadKnowledgeList()
        } else {
          message.error(data.message || '添加失败')
        }
      } catch (error) {
        console.error('添加知识失败', error)
        message.error('添加失败')
      } finally {
        adding.value = false
      }
    }

    /**
     * 处理删除知识
     * @param {number} id 知识ID
     */
    const handleDelete = async (id) => {
      try {
        const res = await fetch(`/api/knowledge/${id}`, {
          method: 'DELETE',
          headers: getAuthHeaders()
        })
        const data = await res.json()
        if (data.code === 200) {
          message.success('删除成功')
          loadKnowledgeList()
        } else {
          message.error(data.message || '删除失败')
        }
      } catch (error) {
        console.error('删除失败', error)
        message.error('删除失败')
      }
    }

    /**
     * 同步知识库到向量库
     * 清空向量库并重新添加所有知识
     */
    const syncToRag = async () => {
      syncing.value = true
      try {
        const res = await fetch('/api/knowledge/syncToRag', {
          method: 'POST',
          headers: getAuthHeaders()
        })
        const data = await res.json()
        if (data.code === 200) {
          message.success(data.data.message || '同步完成')
        } else {
          message.error('同步失败')
        }
      } catch (error) {
        console.error('同步失败', error)
        message.error('同步失败')
      } finally {
        syncing.value = false
      }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取分类显示名称
     * @param {string} category 分类代码
     * @returns {string} 分类名称
     */
    const getCategoryName = (category) => {
      const map = {
        'general': '通用',
        'product': '商品咨询',
        'logistics': '物流问题',
        'after_sale': '售后问题',
        'coupon': '优惠券活动'
      }
      return map[category] || category
    }

    /**
     * 获取分类标签颜色
     * @param {string} category 分类代码
     * @returns {string} 颜色值
     */
    const getCategoryColor = (category) => {
      const map = {
        'general': 'default',
        'product': 'blue',
        'logistics': 'green',
        'after_sale': 'orange',
        'coupon': 'purple'
      }
      return map[category] || 'default'
    }

    /**
     * 获取检索来源显示文本
     * @param {string} source 来源代码
     * @returns {string} 来源名称
     */
    const getSourceText = (source) => {
      const map = {
        'vector_store': '向量库检索',
        'database_fallback': '数据库回退',
        'database_only': '数据库检索',
        'none': '无结果'
      }
      return map[source] || source
    }

    /**
     * 获取检索来源标签颜色
     * @param {string} source 来源代码
     * @returns {string} 颜色值
     */
    const getSourceColor = (source) => {
      const map = {
        'vector_store': 'green',
        'database_fallback': 'orange',
        'database_only': 'blue',
        'none': 'default'
      }
      return map[source] || 'default'
    }

    /**
     * 格式化日期
     * @param {string} date 日期字符串
     * @returns {string} 格式化后的日期
     */
    const formatDate = (date) => {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN')
    }

    /**
     * 显示知识详情弹窗
     * @param {object} record 知识记录
     */
    const showDetail = (record) => {
      currentKnowledge.value = record
      detailVisible.value = true
    }

    /**
     * 显示编辑弹窗
     * @param {object} record 知识记录
     */
    const showEdit = (record) => {
      editForm.value = {
        id: record.id,
        content: record.content,
        category: record.category
      }
      editVisible.value = true
    }

    /**
     * 处理编辑保存
     */
    const handleEdit = async () => {
      if (!editForm.value.content.trim()) {
        message.warning('请输入知识内容')
        return
      }
      
      editLoading.value = true
      try {
        const res = await fetch(`/api/knowledge/${editForm.value.id}`, {
          method: 'PUT',
          headers: getAuthHeaders(),
          body: JSON.stringify({
            content: editForm.value.content,
            category: editForm.value.category
          })
        })
        const data = await res.json()
        if (data.code === 200) {
          message.success('更新成功')
          editVisible.value = false
          loadKnowledgeList()
        } else {
          message.error(data.message || '更新失败')
        }
      } catch (error) {
        console.error('更新失败', error)
        message.error('更新失败')
      } finally {
        editLoading.value = false
      }
    }

    // ==================== 生命周期 ====================

    /**
     * 定时器引用
     */
    let statusTimer = null

    /**
     * 组件挂载时初始化，并启动定时状态检查
     */
    onMounted(() => {
      checkStatus()
      loadKnowledgeList()
      // 每30秒自动检查RAG服务状态，实现自动重连检测
      statusTimer = setInterval(() => {
        checkStatus()
      }, 30000)
    })

    /**
     * 组件卸载时清除定时器
     */
    onUnmounted(() => {
      if (statusTimer) {
        clearInterval(statusTimer)
        statusTimer = null
      }
    })

    watch(() => addForm.value.content, () => {
      keywordsConfirmed.value = false
    })

    watch(() => addForm.value.category, () => {
      keywordsConfirmed.value = false
    })

    // ==================== 导出 ====================

    return {
      serviceStatus,
      searchQuery,
      searchResults,
      searching,
      searched,
      searchSource,
      vectorStoreCount,
      adding,
      syncing,
      extractingKeywords,
      keywordsConfirmed,
      keywordsConfirmVisible,
      aiKeywordsList,
      addForm,
      knowledgeList,
      columns,
      handleSearch,
      handleExtractKeywords,
      confirmKeywords,
      handleAddKnowledge,
      handleDelete,
      syncToRag,
      getCategoryName,
      getCategoryColor,
      getSourceText,
      getSourceColor,
      formatDate,
      showDetail,
      detailVisible,
      currentKnowledge,
      showEdit,
      editVisible,
      editLoading,
      editForm,
      handleEdit
    }
  }
}
</script>

<style scoped>
/* 容器样式 */
.knowledge-container {
  padding: 20px;
}

/* 头部样式 */
.knowledge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.knowledge-header h2 {
  margin: 0;
}

.status-tag {
  margin-left: 10px;
}

/* 卡片样式 */
.search-card,
.add-card,
.list-card {
  margin-bottom: 20px;
}

/* 搜索区域样式 */
.search-section {
  max-width: 800px;
}

.search-results {
  margin-top: 20px;
}

/* 搜索结果项样式 */
.result-item {
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 10px;
}

.result-content {
  white-space: pre-wrap;
}

/* 表格内容单元格样式 */
.content-cell {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 关键词区域样式 */
.keywords-section {
  display: flex;
  flex-direction: column;
}

.keywords-confirmed-tip {
  margin-bottom: 0;
}
</style>
