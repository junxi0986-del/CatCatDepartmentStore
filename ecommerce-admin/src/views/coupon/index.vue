<template>
  <div class="coupon-management">
    <h2>优惠券管理</h2>
    <a-button type="primary" @click="addCoupon">添加优惠券</a-button>
    <a-input placeholder="请输入优惠券名称" v-model:value="searchKeyword" style="width: 300px; margin-left: 10px" />
    <a-button type="primary" @click="search">搜索</a-button>
    <a-table :columns="columns" :data-source="coupons" row-key="id" style="width: 100%; margin-top: 20px">
      <template #status="{ record }">
        <a-select :value="Number(record.status)" @change="updateCouponStatus(record.id, $event)" style="width: 100px">
          <a-select-option :value="1" style="color: green">启用</a-select-option>
          <a-select-option :value="0" style="color: red">禁用</a-select-option>
        </a-select>
      </template>
      <template #type="{ record }">
        <span v-if="record.type === 1">满减券</span>
        <span v-else-if="record.type === 2">折扣券</span>
        <span v-else>未知</span>
      </template>
      <template #action="{ record }">
        <a-button size="small" @click="viewCoupon(record.id)">查看</a-button>
        <a-button size="small" type="primary" @click="editCoupon(record)">编辑</a-button>
        <a-button size="small" type="danger" style="margin-left: 5px" @click="deleteCouponById(record.id)">删除</a-button>
      </template>
    </a-table>

    <a-modal
      v-model:open="isModalVisible"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="优惠券名称">
          <a-input v-model:value="formState.name" placeholder="请输入优惠券名称" />
        </a-form-item>
        <a-form-item label="优惠券类型">
          <a-select v-model:value="formState.type">
            <a-select-option :value="1">满减券</a-select-option>
            <a-select-option :value="2">折扣券</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="使用门槛（最小订单金额）">
          <a-input-number v-model:value="formState.minAmount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="优惠金额">
          <a-input-number v-model:value="formState.discountAmount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="发放总数量">
          <a-input-number v-model:value="formState.totalCount" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="已使用数量">
          <a-input-number v-model:value="formState.usedCount" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="生效时间">
          <a-date-picker v-model:value="formState.startTime" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="失效时间">
          <a-date-picker v-model:value="formState.endTime" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="isViewModalVisible"
      title="优惠券详情"
      @cancel="handleViewCancel"
    >
      <div v-if="viewCouponData">
        <a-descriptions bordered column="1">
          <a-descriptions-item label="优惠券ID">{{ viewCouponData.id }}</a-descriptions-item>
          <a-descriptions-item label="优惠券名称">{{ viewCouponData.name }}</a-descriptions-item>
          <a-descriptions-item label="优惠券类型">
            <span v-if="viewCouponData.type === 1">满减券</span>
            <span v-else-if="viewCouponData.type === 2">折扣券</span>
            <span v-else>未知</span>
          </a-descriptions-item>
          <a-descriptions-item label="使用门槛">¥{{ viewCouponData.minAmount }}</a-descriptions-item>
          <a-descriptions-item label="优惠金额">¥{{ viewCouponData.discountAmount }}</a-descriptions-item>
          <a-descriptions-item label="发放总数量">{{ viewCouponData.totalCount }}</a-descriptions-item>
          <a-descriptions-item label="已使用数量">{{ viewCouponData.usedCount }}</a-descriptions-item>
          <a-descriptions-item label="剩余数量">{{ viewCouponData.totalCount - viewCouponData.usedCount }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="viewCouponData.status === 1 ? 'green' : 'red'">
              {{ viewCouponData.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="生效时间">{{ viewCouponData.startTime }}</a-descriptions-item>
          <a-descriptions-item label="失效时间">{{ viewCouponData.endTime }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ viewCouponData.createTime }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { getCouponList, addCoupon as addCouponApi, updateCoupon, deleteCoupon, updateCouponStatus as updateCouponStatusApi, getCouponById } from '../../api/coupon'
import dayjs from 'dayjs'

export default {
  setup() {
    const coupons = ref([])
    const searchKeyword = ref('')
    const isModalVisible = ref(false)
    const modalTitle = ref('')
    const isViewModalVisible = ref(false)
    const viewCouponData = ref(null)

    const formState = ref({
      id: null,
      name: '',
      type: 1,
      minAmount: 0,
      discountAmount: 0,
      totalCount: 100,
      usedCount: 0,
      status: 1,
      startTime: null,
      endTime: null
    })

    const columns = [
      {
        title: '优惠券ID',
        dataIndex: 'id',
        key: 'id',
        width: 100
      },
      {
        title: '优惠券名称',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        width: 100,
        slots: { customRender: 'type' }
      },
      {
        title: '使用门槛',
        dataIndex: 'minAmount',
        key: 'minAmount',
        width: 120
      },
      {
        title: '优惠金额',
        dataIndex: 'discountAmount',
        key: 'discountAmount',
        width: 120
      },
      {
        title: '发放数量',
        dataIndex: 'totalCount',
        key: 'totalCount',
        width: 100
      },
      {
        title: '已使用',
        dataIndex: 'usedCount',
        key: 'usedCount',
        width: 100
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        slots: { customRender: 'status' }
      },
      {
        title: '生效时间',
        dataIndex: 'startTime',
        key: 'startTime',
        width: 180
      },
      {
        title: '失效时间',
        dataIndex: 'endTime',
        key: 'endTime',
        width: 180
      },
      {
        title: '操作',
        key: 'action',
        width: 200,
        fixed: 'right',
        slots: { customRender: 'action' }
      }
    ]

    const loadCoupons = async () => {
      try {
        const res = await getCouponList({ keyword: searchKeyword.value })
        coupons.value = res.data
      } catch (error) {
        console.error('获取优惠券列表失败', error)
        message.error('获取优惠券列表失败')
      }
    }

    const search = () => {
      loadCoupons()
    }

    const addCoupon = () => {
      modalTitle.value = '添加优惠券'
      formState.value = {
        id: null,
        name: '',
        type: 1,
        minAmount: 0,
        discountAmount: 0,
        totalCount: 100,
        usedCount: 0,
        status: 1,
        startTime: dayjs().startOf('day'),
        endTime: dayjs().add(30, 'day').endOf('day')
      }
      isModalVisible.value = true
    }

    const viewCoupon = async (couponId) => {
      try {
        const res = await getCouponById(couponId)
        viewCouponData.value = res.data
        isViewModalVisible.value = true
      } catch (error) {
        console.error('获取优惠券详情失败', error)
        message.error('获取优惠券详情失败')
      }
    }

    const editCoupon = (record) => {
      modalTitle.value = '编辑优惠券'
      formState.value = {
        id: record.id,
        name: record.name,
        type: record.type,
        minAmount: record.minAmount,
        discountAmount: record.discountAmount,
        totalCount: record.totalCount,
        usedCount: record.usedCount,
        status: record.status,
        startTime: record.startTime ? dayjs(record.startTime) : null,
        endTime: record.endTime ? dayjs(record.endTime) : null
      }
      isModalVisible.value = true
    }

    const deleteCouponById = async (couponId) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这个优惠券吗？此操作不可撤销。',
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            await deleteCoupon(couponId)
            message.success('删除优惠券成功')
            loadCoupons()
          } catch (error) {
            console.error('删除优惠券失败', error)
            message.error('删除优惠券失败')
          }
        }
      })
    }

    const handleOk = async () => {
      try {
        const data = {
          ...formState.value,
          startTime: formState.value.startTime ? formState.value.startTime.format('YYYY-MM-DD HH:mm:ss') : null,
          endTime: formState.value.endTime ? formState.value.endTime.format('YYYY-MM-DD HH:mm:ss') : null
        }
        if (formState.value.id) {
          await updateCoupon(data)
          message.success('编辑优惠券成功')
        } else {
          await addCouponApi(data)
          message.success('添加优惠券成功')
        }
        isModalVisible.value = false
        loadCoupons()
      } catch (error) {
        console.error('保存优惠券失败', error)
        message.error('保存优惠券失败，请稍后重试')
      }
    }

    const handleCancel = () => {
      isModalVisible.value = false
    }

    const handleViewCancel = () => {
      isViewModalVisible.value = false
    }

    const updateCouponStatus = async (couponId, status) => {
      try {
        await updateCouponStatusApi({ id: couponId, status: status })
        message.success(status === 1 ? '优惠券已启用' : '优惠券已禁用')
        loadCoupons()
      } catch (error) {
        console.error('更新优惠券状态失败', error)
        loadCoupons()
      }
    }

    onMounted(() => {
      loadCoupons()
    })

    return {
      coupons,
      searchKeyword,
      columns,
      loadCoupons,
      search,
      addCoupon,
      viewCoupon,
      editCoupon,
      deleteCouponById,
      updateCouponStatus,
      isModalVisible,
      modalTitle,
      formState,
      handleOk,
      handleCancel,
      handleViewCancel,
      isViewModalVisible,
      viewCouponData
    }
  }
}
</script>

<style scoped>
.coupon-management {
  padding: 20px;
}

.coupon-management h2 {
  margin-bottom: 20px;
}

.coupon-management .ant-table-cell button {
  margin-left: 3px;
  margin-right: 3px;
}
</style>