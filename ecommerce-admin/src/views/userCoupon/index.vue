<template>
  <div class="user-coupon-management">
    <h2>用户优惠券管理</h2>
    
    <!-- 筛选条件 -->
    <a-card hoverable class="filter-card">
      <a-form :model="filterForm" layout="inline">
        <a-form-item label="用户ID">
          <a-input v-model:value="filterForm.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item label="优惠券ID">
          <a-input v-model:value="filterForm.couponId" placeholder="请输入优惠券ID" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="filterForm.status" placeholder="请选择状态" allowClear :style="{ width: '150px' }">
            <a-select-option value="0">未使用</a-select-option>
            <a-select-option value="1">已使用</a-select-option>
            <a-select-option value="2">已过期</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search" class="search-btn">查询</a-button>
          <a-button @click="reset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <a-button type="primary" @click="openAddCouponModal">添加优惠券</a-button>
    </div>

    <!-- 优惠券列表 -->
    <a-table :columns="columns" :data-source="coupons" row-key="id">
      <template #status="{ record }">
        <span v-if="record.status === 0">未使用</span>
        <span v-else-if="record.status === 1">已使用</span>
        <span v-else-if="record.status === 2">已过期</span>
        <span v-else>未知</span>
      </template>
      <template #action="{ record }">
        <a-button size="small" @click="viewUserDetail(record.user_id)">查看用户</a-button>
        <a-button size="small" v-if="record.status === 0" @click="handleUseCoupon(record.id)">使用</a-button>
        <a-button size="small" type="danger" @click="handleDeleteCoupon(record.id)">删除</a-button>
      </template>
    </a-table>

    <!-- 添加/编辑优惠券模态框 -->
    <a-modal
      v-model:open="isModalVisible"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="formState">
        <a-form-item label="用户ID">
          <a-input v-model:value="formState.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item label="优惠券ID">
          <a-input v-model:value="formState.couponId" placeholder="请输入优惠券ID" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status" placeholder="请选择状态">
            <a-select-option value="0">未使用</a-select-option>
            <a-select-option value="1">已使用</a-select-option>
            <a-select-option value="2">已过期</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { getCouponList, addCoupon, useCoupon, deleteCoupon } from '../../api/userCoupon'

export default {
  setup() {
    const coupons = ref([])
    const filterForm = ref({
      userId: '',
      couponId: '',
      status: ''
    })

    const isModalVisible = ref(false)
    const modalTitle = ref('添加优惠券')
    const formState = ref({
      id: null,
      userId: '',
      couponId: '',
      status: 0
    })

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
        title: '优惠券ID',
        dataIndex: 'coupon_id',
        key: 'coupon_id',
        width: 120
      },
      {
        title: '优惠券名称',
        dataIndex: 'coupon_name',
        key: 'coupon_name'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        slots: { customRender: 'status' }
      },
      {
        title: '领取时间',
        dataIndex: 'create_time',
        key: 'create_time',
        width: 180,
        customRender: ({ record }) => formatDateTime(record.create_time)
      },
      {
        title: '使用时间',
        dataIndex: 'use_time',
        key: 'use_time',
        width: 180,
        customRender: ({ record }) => formatDateTime(record.use_time)
      },
      {
        title: '操作',
        key: 'action',
        width: 150,
        slots: { customRender: 'action' }
      }
    ]

    const loadCoupons = async () => {
      try {
        const params = {
          userId: filterForm.value.userId ? parseInt(filterForm.value.userId) : undefined,
          couponId: filterForm.value.couponId ? parseInt(filterForm.value.couponId) : undefined,
          status: filterForm.value.status ? parseInt(filterForm.value.status) : undefined
        }
        const res = await getCouponList(params)
        coupons.value = res.data
      } catch (error) {
        console.error('获取优惠券列表失败', error)
        message.error('获取优惠券列表失败')
      }
    }

    const search = () => {
      loadCoupons()
    }

    const reset = () => {
      filterForm.value = {
        userId: '',
        couponId: '',
        status: ''
      }
      loadCoupons()
    }

    const openAddCouponModal = () => {
      modalTitle.value = '添加优惠券'
      formState.value = {
        id: null,
        userId: '',
        couponId: '',
        status: 0
      }
      isModalVisible.value = true
    }

    const handleOk = async () => {
      try {
        await addCoupon(formState.value)
        message.success('添加成功')
        isModalVisible.value = false
        loadCoupons()
      } catch (error) {
        console.error('添加失败', error)
        message.error('添加失败')
      }
    }

    const handleCancel = () => {
      isModalVisible.value = false
    }

    const viewUserDetail = (userId) => {
      // 这里可以实现查看用户详情的逻辑
      message.info(`查看用户ID: ${userId}`)
    }

    const handleUseCoupon = (id) => {
      Modal.confirm({
        title: '确认使用',
        content: '确定要使用这张优惠券吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          try {
            await useCoupon({ id, orderId: 1 }) // 这里应该传入真实的订单ID
            message.success('使用成功')
            loadCoupons()
          } catch (error) {
            console.error('使用失败', error)
            message.error('使用失败')
          }
        }
      })
    }

    const handleDeleteCoupon = (id) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这个优惠券记录吗？此操作不可撤销。',
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            await deleteCoupon(id)
            message.success('删除成功')
            loadCoupons()
          } catch (error) {
            console.error('删除失败', error)
            message.error('删除失败')
          }
        }
      })
    }

    const formatDateTime = (time) => {
      if (!time) return '-'
      let date
      if (Array.isArray(time)) {
        const [year, month, day, hour, minute, second] = time
        date = new Date(year, month - 1, day, hour, minute, second)
      } else {
        date = new Date(time)
      }
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const h = String(date.getHours()).padStart(2, '0')
      const i = String(date.getMinutes()).padStart(2, '0')
      const s = String(date.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${d} ${h}:${i}:${s}`
    }

    onMounted(() => {
      loadCoupons()
    })

    return {
      coupons,
      filterForm,
      columns,
      isModalVisible,
      modalTitle,
      formState,
      loadCoupons,
      search,
      reset,
      openAddCouponModal,
      handleOk,
      handleCancel,
      viewUserDetail,
      handleUseCoupon,
      handleDeleteCoupon,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.user-coupon-management {
  padding: 20px;
}

.user-coupon-management h2 {
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

.user-coupon-management .ant-table-cell button {
  margin-left: 3px;
  margin-right: 3px;
}

.search-btn {
  margin-left: 15px !important;
  margin-right: 15px !important;
}
</style>