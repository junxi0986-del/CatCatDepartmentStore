<template>
  <div class="behavior-management">
    <h2>用户行为明细</h2>
    
    <!-- 筛选条件 -->
    <a-card hoverable class="filter-card">
      <a-form :model="filterForm" layout="inline">
        <a-form-item label="用户ID">
          <a-input v-model:value="filterForm.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item label="商品ID">
          <a-input v-model:value="filterForm.productId" placeholder="请输入商品ID" />
        </a-form-item>
        <a-form-item label="行为类型">
          <a-select v-model:value="filterForm.behaviorType" placeholder="请选择行为类型" :style="{ width: '150px' }">
            <a-select-option value="1">浏览</a-select-option>
            <a-select-option value="2">搜索</a-select-option>
            <a-select-option value="3">加购</a-select-option>
            <a-select-option value="5">购买</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker v-model:value="filterForm.timeRange" format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search" class="search-btn">查询</a-button>
          <a-button @click="reset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <a-button type="primary" @click="exportExcel">导出Excel</a-button>
    </div>

    <!-- 行为列表 -->
    <a-table :columns="columns" :data-source="behaviors" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'behavior_type'">
          <span v-if="record.behavior_type === 1">浏览</span>
          <span v-else-if="record.behavior_type === 2">搜索</span>
          <span v-else-if="record.behavior_type === 3">加购</span>
          <span v-else-if="record.behavior_type === 5">购买</span>
          <span v-else>未知</span>
        </template>
        <template v-else-if="column.key === 'stay_time'">
          <span>{{ record.stay_time }}秒</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button size="small" @click="viewUserDetail(record)">查看用户</a-button>
          <a-button size="small" @click="viewProductDetail(record)">查看商品</a-button>
          <a-button size="small" type="danger" @click="handleDeleteBehavior(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>

    <!-- 用户详情模态框 -->
    <a-modal v-model:open="userModalVisible" title="用户详情" :footer="null">
      <a-descriptions bordered column="1" v-if="selectedUser">
        <a-descriptions-item label="用户ID">{{ selectedUser.user_id }}</a-descriptions-item>
        <a-descriptions-item label="用户名">{{ selectedUser.username || '未知' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 商品详情模态框 -->
    <a-modal v-model:open="productModalVisible" title="商品详情" :footer="null">
      <a-descriptions bordered column="1" v-if="selectedProduct">
        <a-descriptions-item label="商品ID">{{ selectedProduct.product_id }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ selectedProduct.product_name || '未知' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { getBehaviorList, deleteBehavior } from '../../api/behavior'
import dayjs from 'dayjs'

export default {
  setup() {
    const behaviors = ref([])
    const filterForm = ref({
      userId: '',
      productId: '',
      behaviorType: '',
      timeRange: null
    })
    const userModalVisible = ref(false)
    const productModalVisible = ref(false)
    const selectedUser = ref(null)
    const selectedProduct = ref(null)

    const columns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 100
      },
      {
        title: '用户ID',
        dataIndex: 'user_id',
        key: 'user_id',
        width: 120
      },
      {
        title: '用户名',
        dataIndex: 'username',
        key: 'username'
      },
      {
        title: '商品ID',
        dataIndex: 'product_id',
        key: 'product_id',
        width: 120
      },
      {
        title: '商品名称',
        dataIndex: 'product_name',
        key: 'product_name'
      },
      {
        title: '行为类型',
        dataIndex: 'behavior_type',
        key: 'behavior_type',
        width: 100
      },
      {
        title: '停留时长',
        dataIndex: 'stay_time',
        key: 'stay_time',
        width: 100
      },
      {
        title: '发生时间',
        dataIndex: 'create_time',
        key: 'create_time',
        width: 180
      },
      {
        title: '操作',
        key: 'action',
        width: 200
      }
    ]

    const loadBehaviors = async () => {
      try {
        const params = {
          userId: filterForm.value.userId ? parseInt(filterForm.value.userId) : undefined,
          productId: filterForm.value.productId ? parseInt(filterForm.value.productId) : undefined,
          behaviorType: filterForm.value.behaviorType ? parseInt(filterForm.value.behaviorType) : undefined,
          startTime: filterForm.value.timeRange ? filterForm.value.timeRange[0].format('YYYY-MM-DD 00:00:00') : undefined,
          endTime: filterForm.value.timeRange ? filterForm.value.timeRange[1].format('YYYY-MM-DD 23:59:59') : undefined
        }
        const res = await getBehaviorList(params)
        behaviors.value = res.data
      } catch (error) {
        console.error('获取行为列表失败', error)
        message.error('获取行为列表失败')
      }
    }

    const search = () => {
      loadBehaviors()
    }

    const reset = () => {
      filterForm.value = {
        userId: '',
        productId: '',
        behaviorType: '',
        timeRange: null
      }
      loadBehaviors()
    }

    const exportExcel = () => {
      if (behaviors.value.length === 0) {
        message.warning('暂无数据可导出')
        return
      }
      
      const behaviorTypeMap = { 1: '浏览', 2: '搜索', 3: '加购', 5: '购买' }
      
      const headers = ['ID', '用户ID', '用户名', '商品ID', '商品名称', '行为类型', '停留时长(秒)', '发生时间']
      const rows = behaviors.value.map(item => [
        item.id,
        item.user_id,
        item.username || '-',
        item.product_id || '-',
        item.product_name || '-',
        behaviorTypeMap[item.behavior_type] || '未知',
        item.stay_time || 0,
        item.create_time || '-'
      ])
      
      const csvContent = [headers, ...rows].map(row => row.join(',')).join('\n')
      const BOM = '\uFEFF'
      const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `用户行为报表_${new Date().toISOString().split('T')[0]}.csv`
      a.click()
      window.URL.revokeObjectURL(url)
      message.success('导出成功')
    }

    const viewUserDetail = (record) => {
      selectedUser.value = record
      userModalVisible.value = true
    }

    const viewProductDetail = (record) => {
      selectedProduct.value = record
      productModalVisible.value = true
    }

    const handleDeleteBehavior = (id) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这个行为记录吗？此操作不可撤销。',
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            await deleteBehavior(id)
            message.success('删除成功')
            loadBehaviors()
          } catch (error) {
            console.error('删除失败', error)
            message.error('删除失败')
          }
        }
      })
    }

    onMounted(() => {
      loadBehaviors()
    })

    return {
      behaviors,
      filterForm,
      columns,
      loadBehaviors,
      search,
      reset,
      exportExcel,
      viewUserDetail,
      viewProductDetail,
      handleDeleteBehavior,
      userModalVisible,
      productModalVisible,
      selectedUser,
      selectedProduct
    }
  }
}
</script>

<style scoped>
.behavior-management {
  padding: 20px;
}

.behavior-management h2 {
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.action-buttons {
  margin-bottom: 20px;
}

.action-buttons button {
  margin-right: 10px;
}

.behavior-management .ant-table-cell button {
  margin-left: 3px;
  margin-right: 3px;
}

.search-btn {
  margin-left: 15px !important;
  margin-right: 15px !important;
}
</style>