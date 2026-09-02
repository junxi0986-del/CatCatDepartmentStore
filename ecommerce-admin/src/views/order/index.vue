<template>
  <div class="order-management">
    <h2>订单管理</h2>
    
    <div class="search-area">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="订单号">
            <a-input placeholder="请输入订单号" v-model:value="searchForm.orderNo" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="收货人">
            <a-input placeholder="请输入收货人姓名" v-model:value="searchForm.receiver" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="手机号">
            <a-input placeholder="请输入手机号" v-model:value="searchForm.receiverPhone" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="商品名称">
            <a-input placeholder="请输入商品名称" v-model:value="searchForm.productName" allowClear />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="最低价格">
            <a-input-number placeholder="最低价格" v-model:value="searchForm.minPrice" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="最高价格">
            <a-input-number placeholder="最高价格" v-model:value="searchForm.maxPrice" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="订单状态">
            <a-select placeholder="请选择状态" v-model:value="searchForm.status" allowClear style="width: 100%">
              <a-select-option :value="0">待支付</a-select-option>
              <a-select-option :value="1">已支付</a-select-option>
              <a-select-option :value="2">待发货</a-select-option>
              <a-select-option :value="3">已发货</a-select-option>
              <a-select-option :value="4">已完成</a-select-option>
              <a-select-option :value="5">已取消</a-select-option>
              <a-select-option :value="6">已退款</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="search">搜索</a-button>
              <a-button @click="resetSearch">重置</a-button>
            </a-space>
          </a-form-item>
        </a-col>
      </a-row>
    </div>
    
    <div style="margin-bottom: 20px;">
      <a-button type="primary" @click="showAddModal">添加订单</a-button>
      <a-badge :count="refundApplyCount" :offset="[10, 0]">
        <a-button type="primary" style="margin-left: 20px; background-color: rgb(59, 130, 246); color: #ffffff; border-color: rgb(59, 130, 246);" @click="showRefundModal">退款申请</a-button>
      </a-badge>
      <a-divider type="vertical" style="margin: 0 20px;" />
      <span v-if="selectedRowKeys.length > 0" style="margin-right: 10px;">已选 {{ selectedRowKeys.length }} 项</span>
      <a-button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchConfirmPayment" style="margin-right: 10px;">批量确认支付</a-button>
      <a-button type="primary" :disabled="selectedRowKeys.length === 0" @click="showBatchShipModal" style="margin-right: 10px;">批量发货</a-button>
      <a-button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchCancel">批量取消</a-button>
    </div>
    <a-table 
      :columns="columns" 
      :data-source="orders" 
      row-key="id" 
      style="width: 100%"
      :row-selection="rowSelection"
    >
      <template #status="{ record }">
        <span v-if="record.status === 0" style="color: #ff4d4f">待支付</span>
        <span v-else-if="record.status === 1" style="color: #faad14">已支付</span>
        <span v-else-if="record.status === 2" style="color: #1890ff">待发货</span>
        <span v-else-if="record.status === 3" style="color: #52c41a">已发货</span>
        <span v-else-if="record.status === 4" style="color: #8c8c8c">已完成</span>
        <span v-else-if="record.status === 5" style="color: #8c8c8c">已取消</span>
        <span v-else-if="record.status === 6" style="color: #722ed1">已退款</span>
      </template>
      <template #action="{ record }">
        <a-button size="small" @click="viewOrder(record.id)">查看</a-button>
        <a-button size="small" style="margin-left: 5px" @click="showEditModal(record)">编辑</a-button>
        <a-button size="small" type="danger" style="margin-left: 5px" @click="deleteOrderById(record.id)">删除</a-button>
      </template>
    </a-table>

    <!-- 添加订单模态框 -->
    <a-modal
      v-model:open="isAddModalVisible"
      title="添加订单"
      @ok="handleAddOk"
      @cancel="handleAddCancel"
    >
      <a-form :model="addForm" layout="vertical">
        <a-form-item label="订单编号">
          <a-input v-model:value="addForm.orderNo" />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input v-model:value="addForm.userId" type="number" />
        </a-form-item>
        <a-form-item label="订单总金额">
          <a-input v-model:value="addForm.totalPrice" type="number" />
        </a-form-item>
        <a-form-item label="实付金额">
          <a-input v-model:value="addForm.payPrice" type="number" />
        </a-form-item>
        <a-form-item label="收货人">
          <a-input v-model:value="addForm.receiver" />
        </a-form-item>
        <a-form-item label="收货电话">
          <a-input v-model:value="addForm.receiverPhone" />
        </a-form-item>
        <a-form-item label="收货地址">
          <a-input v-model:value="addForm.receiverAddress" />
        </a-form-item>
        <a-form-item label="订单状态">
          <a-select v-model:value="addForm.status">
            <a-select-option :value="0">待支付</a-select-option>
            <a-select-option :value="1">已支付</a-select-option>
            <a-select-option :value="2">待发货</a-select-option>
            <a-select-option :value="3">已发货</a-select-option>
            <a-select-option :value="4">已完成</a-select-option>
            <a-select-option :value="5">已取消</a-select-option>
            <a-select-option :value="6">已退款</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑订单模态框 -->
    <a-modal
      v-model:open="isEditModalVisible"
      title="编辑订单"
      width="900px"
      @ok="handleEditOk"
      @cancel="handleEditCancel"
    >
      <a-form :model="editForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="订单编号">
              <a-input v-model:value="editForm.orderNo" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="用户ID">
              <a-input v-model:value="editForm.userId" type="number" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="订单总金额">
              <a-input v-model:value="editForm.totalPrice" type="number" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实付金额">
              <a-input v-model:value="editForm.payPrice" type="number" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="收货人">
              <a-input v-model:value="editForm.receiver" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收货电话">
              <a-input v-model:value="editForm.receiverPhone" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="收货地址">
          <a-input v-model:value="editForm.receiverAddress" />
        </a-form-item>
        <a-form-item label="订单状态">
          <a-select v-model:value="editForm.status">
            <a-select-option :value="0">待支付</a-select-option>
            <a-select-option :value="1">已支付</a-select-option>
            <a-select-option :value="2">待发货</a-select-option>
            <a-select-option :value="3">已发货</a-select-option>
            <a-select-option :value="4">已完成</a-select-option>
            <a-select-option :value="5">已取消</a-select-option>
            <a-select-option :value="6">已退款</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-divider>订单商品</a-divider>
        <div v-for="(item, index) in editOrderItems" :key="item.id" class="edit-order-item">
          <a-card size="small">
            <template #title>
              <span>商品 {{ index + 1 }}: {{ item.productName }}</span>
              <a-tag v-if="item.specInfo" color="blue" style="margin-left: 8px;">{{ item.specInfo }}</a-tag>
            </template>
            <a-row :gutter="16">
              <a-col :span="8">
                <div class="item-image">
                  <img v-if="item.productPic" :src="formatImageUrl(item.productPic)" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px;" />
                  <span v-else>无图片</span>
                </div>
              </a-col>
              <a-col :span="16">
                <div v-if="item.specsLoading" class="specs-loading">
                  <a-spin size="small" /> 加载规格中...
                </div>
                <div v-else-if="item.groupedSpecs && Object.keys(item.groupedSpecs).length > 0" class="spec-select-area">
                  <div v-for="(options, specName) in item.groupedSpecs" :key="specName" class="spec-row">
                    <div class="spec-label">{{ specName }}:</div>
                    <div class="spec-options">
                      <a-tag
                        v-for="spec in options"
                        :key="spec.id"
                        :color="item.selectedSpecs && item.selectedSpecs[specName] === spec.id ? 'blue' : 'default'"
                        :class="['spec-tag', { 'spec-tag-active': item.selectedSpecs && item.selectedSpecs[specName] === spec.id }]"
                        @click="selectItemSpec(item, specName, spec)"
                      >
                        {{ spec.specValue }}
                      </a-tag>
                    </div>
                  </div>
                </div>
                <div v-else class="no-specs">
                  <a-input v-model:value="item.specInfo" placeholder="该商品无规格，可自定义填写" style="width: 100%;" />
                </div>
                <a-row :gutter="16" style="margin-top: 12px;">
                  <a-col :span="12">
                    <a-form-item label="商品单价">
                      <a-input-number v-model:value="item.productPrice" :min="0" :precision="2" style="width: 100%;" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="购买数量">
                      <a-input-number v-model:value="item.quantity" :min="1" style="width: 100%;" />
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
          </a-card>
        </div>
      </a-form>
    </a-modal>

    <!-- 查看订单详情模态框 -->
    <a-modal
      v-model:open="isViewModalVisible"
      title="订单详情"
      width="888px"
      @cancel="handleViewCancel"
    >
      <div v-if="viewOrderData">
        <p><strong>订单ID:</strong> {{ viewOrderData.id }}</p>
        <p><strong>订单编号:</strong> {{ viewOrderData.orderNo }}</p>
        <p><strong>用户ID:</strong> {{ viewOrderData.userId }}</p>
        <p><strong>订单总金额:</strong> ¥{{ viewOrderData.totalPrice }}</p>
        <p><strong>实付金额:</strong> ¥{{ viewOrderData.payPrice }}</p>
        <p><strong>收货人:</strong> {{ viewOrderData.receiver }}</p>
        <p><strong>收货电话:</strong> {{ viewOrderData.receiverPhone }}</p>
        <p><strong>收货地址:</strong> {{ viewOrderData.receiverAddress }}</p>
        <p><strong>订单状态:</strong>
          <span v-if="viewOrderData.status === 0" style="color: #ff4d4f">待支付</span>
          <span v-else-if="viewOrderData.status === 1" style="color: #faad14">已支付</span>
          <span v-else-if="viewOrderData.status === 2" style="color: #1890ff">待发货</span>
          <span v-else-if="viewOrderData.status === 3" style="color: #52c41a">已发货</span>
          <span v-else-if="viewOrderData.status === 4" style="color: #8c8c8c">已完成</span>
          <span v-else-if="viewOrderData.status === 5" style="color: #8c8c8c">已取消</span>
          <span v-else-if="viewOrderData.status === 6" style="color: #722ed1">已退款</span>
        </p>
        <p v-if="viewOrderData.status >= 2">
          <strong>快递公司:</strong> {{ viewOrderData.expressCompany || '-' }}
        </p>
        <p v-if="viewOrderData.status >= 2">
          <strong>快递单号:</strong> {{ viewOrderData.expressNo || '-' }}
        </p>
        <p><strong>创建时间:</strong> {{ formatDateTime(viewOrderData.createTime) }}</p>
        <h3 style="margin-top: 20px">订单项</h3>
        <a-table :columns="orderItemColumns" :data-source="orderItems" row-key="id" style="width: 100%">
          <template #productPic="{ record }">
            <img v-if="record.productPic" :src="formatImageUrl(record.productPic)" style="width: 50px; height: 50px; object-fit: cover" />
            <span v-else>无图片</span>
          </template>
        </a-table>
        <div style="margin-top: 20px; display: flex; justify-content: flex-end; gap: 10px">
          <a-button v-if="viewOrderData.status === 0 || viewOrderData.status === 1" type="primary" @click="confirmPayment">确认支付</a-button>
          <a-button v-if="viewOrderData.status === 0 || viewOrderData.status === 1 || viewOrderData.status === 2 || viewOrderData.status === 3" type="default" @click="editLogisticsInfo">
            {{ viewOrderData.status === 3 ? '编辑物流信息' : '发货' }}
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 编辑物流信息模态框 -->
    <a-modal
      v-model:open="isLogisticsModalVisible"
      title="编辑物流信息"
      @ok="handleLogisticsOk"
      @cancel="handleLogisticsCancel"
    >
      <a-form :model="logisticsForm" layout="vertical">
        <a-form-item label="快递单号">
          <a-input v-model:value="logisticsForm.expressNo" placeholder="请输入快递单号" :maxlength="15" />
        </a-form-item>
        <a-form-item label="快递公司">
          <a-select v-model:value="logisticsForm.expressCompany" placeholder="请选择快递公司">
            <a-select-option value="顺丰速运">顺丰速运</a-select-option>
            <a-select-option value="中通快递">中通快递</a-select-option>
            <a-select-option value="圆通速递">圆通速递</a-select-option>
            <a-select-option value="申通快递">申通快递</a-select-option>
            <a-select-option value="韵达快递">韵达快递</a-select-option>
            <a-select-option value="EMS">EMS</a-select-option>
            <a-select-option value="中国邮政">中国邮政</a-select-option>
            <a-select-option value="天天快递">天天快递</a-select-option>
            <a-select-option value="京东物流">京东物流</a-select-option>
            <a-select-option value="苏宁物流">苏宁物流</a-select-option>
            <a-select-option value="德邦快递">德邦快递</a-select-option>
            <a-select-option value="宅急送">宅急送</a-select-option>
            <a-select-option value="优速快递">优速快递</a-select-option>
            <a-select-option value="全峰快递">全峰快递</a-select-option>
            <a-select-option value="快捷快递">快捷快递</a-select-option>
            <a-select-option value="国通快递">国通快递</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 退款申请模态框 -->
    <a-modal
      v-model:open="isRefundModalVisible"
      title="退款申请列表"
      width="900px"
      :footer="null"
    >
      <a-table :columns="refundColumns" :data-source="refundList" row-key="id" style="width: 100%">
        <template #action="{ record }">
          <a-button type="primary" size="small" @click="showRefundDetail(record)">处理</a-button>
        </template>
      </a-table>
    </a-modal>

    <!-- 退款详情和处理模态框 -->
    <a-modal
      v-model:open="isRefundDetailModalVisible"
      title="退款申请详情"
      @ok="handleRefundOk"
      @cancel="handleRefundCancel"
    >
      <div v-if="refundDetailData">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="订单编号">{{ refundDetailData.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="用户名">{{ refundDetailData.username }}</a-descriptions-item>
          <a-descriptions-item label="订单金额">¥{{ refundDetailData.totalPrice }}</a-descriptions-item>
          <a-descriptions-item label="实付金额">¥{{ refundDetailData.payPrice }}</a-descriptions-item>
          <a-descriptions-item label="收货人">{{ refundDetailData.receiver }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{ refundDetailData.receiverPhone }}</a-descriptions-item>
          <a-descriptions-item label="收货地址" :span="2">{{ refundDetailData.receiverAddress }}</a-descriptions-item>
          <a-descriptions-item label="退款原因" :span="2">{{ refundDetailData.refundReason }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ refundDetailData.refundApplyTime }}</a-descriptions-item>
          <a-descriptions-item label="超时时间">{{ getTimeoutTime(refundDetailData.refundApplyTime) }}</a-descriptions-item>
        </a-descriptions>
        
        <h4 style="margin-top: 20px">订单商品</h4>
        <a-table :columns="refundItemColumns" :data-source="refundDetailData.items || []" row-key="id" style="width: 100%" :pagination="false" size="small">
          <template #productPic="{ record }">
            <img v-if="record.productPic" :src="formatImageUrl(record.productPic)" style="width: 50px; height: 50px; object-fit: cover" />
            <span v-else>无图片</span>
          </template>
        </a-table>
        
        <a-form :model="refundHandleForm" style="margin-top: 20px">
          <a-form-item label="处理意见">
            <a-input v-model:value="refundHandleForm.reply" type="textarea" placeholder="请输入处理意见" />
          </a-form-item>
        </a-form>
        
        <div style="color: #999; font-size: 12px; margin-top: 10px;">
          提示：超过30分钟未处理，系统将自动退款
        </div>
      </div>
    </a-modal>

    <!-- 批量发货弹窗 -->
    <a-modal
      v-model:open="isBatchShipModalVisible"
      title="批量发货"
      @ok="handleBatchShip"
      @cancel="handleBatchShipCancel"
    >
      <p style="margin-bottom: 16px;">将为选中的 <strong>{{ selectedRowKeys.length }}</strong> 个订单设置物流信息并发货</p>
      <a-form :model="batchShipForm" layout="vertical">
        <a-form-item label="快递公司" required>
          <a-select v-model:value="batchShipForm.expressCompany" placeholder="请选择快递公司">
            <a-select-option value="顺丰速运">顺丰速运</a-select-option>
            <a-select-option value="中通快递">中通快递</a-select-option>
            <a-select-option value="圆通速递">圆通速递</a-select-option>
            <a-select-option value="申通快递">申通快递</a-select-option>
            <a-select-option value="韵达快递">韵达快递</a-select-option>
            <a-select-option value="EMS">EMS</a-select-option>
            <a-select-option value="中国邮政">中国邮政</a-select-option>
            <a-select-option value="天天快递">天天快递</a-select-option>
            <a-select-option value="京东物流">京东物流</a-select-option>
            <a-select-option value="苏宁物流">苏宁物流</a-select-option>
            <a-select-option value="德邦快递">德邦快递</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="快递单号" required>
          <a-input v-model:value="batchShipForm.expressNo" placeholder="请输入快递单号" :maxlength="15" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { getOrderList, addOrder, updateOrder, deleteOrder, getOrderDetail, getOrderItemsByOrderId, confirmPayment as confirmPaymentApi, updateLogisticsInfo as updateLogisticsInfoApi, getRefundList, handleRefund as handleRefundApi, updateOrderItem, batchConfirmPayment, batchCancelOrder, batchShipOrder } from '../../api/order'
import { getProductSpecs } from '../../api/product'

export default {
  setup() {
    const orders = ref([])
    const orderItems = ref([])
    const searchKeyword = ref('')
    const searchForm = ref({
      orderNo: '',
      receiver: '',
      receiverPhone: '',
      productName: '',
      minPrice: null,
      maxPrice: null,
      status: null
    })
    const isAddModalVisible = ref(false)
    const isEditModalVisible = ref(false)
    const isViewModalVisible = ref(false)
    const viewOrderData = ref(null)
    const isLogisticsModalVisible = ref(false)
    const logisticsForm = ref({
      orderId: '',
      expressNo: '',
      expressCompany: ''
    })
    const isRefundModalVisible = ref(false)
    const refundList = ref([])
    const refundApplyCount = ref(0)
    const isRefundDetailModalVisible = ref(false)
    const refundDetailData = ref(null)
    const refundHandleForm = ref({
      orderId: '',
      action: 1,
      reply: ''
    })
    
    const addForm = ref({
      orderNo: '',
      userId: '',
      totalPrice: '',
      payPrice: '',
      receiver: '',
      receiverPhone: '',
      receiverAddress: '',
      status: 0
    })
    
    const editForm = ref({
      id: '',
      orderNo: '',
      userId: '',
      totalPrice: '',
      payPrice: '',
      receiver: '',
      receiverPhone: '',
      receiverAddress: '',
      status: 0
    })
    const editOrderItems = ref([])
    
    const selectedRowKeys = ref([])
    const rowSelection = computed(() => ({
      selectedRowKeys: selectedRowKeys.value,
      onChange: (keys) => {
        selectedRowKeys.value = keys
      }
    }))
    
    const isBatchShipModalVisible = ref(false)
    const batchShipForm = ref({
      expressCompany: '',
      expressNo: ''
    })

    const columns = [
      {
        title: '订单ID',
        dataIndex: 'id',
        key: 'id',
        width: 100
      },
      {
        title: '订单编号',
        dataIndex: 'orderNo',
        key: 'orderNo'
      },
      {
        title: '用户ID',
        dataIndex: 'userId',
        key: 'userId',
        width: 100
      },
      {
        title: '订单总金额',
        dataIndex: 'totalPrice',
        key: 'totalPrice',
        width: 100
      },
      {
        title: '收货人',
        dataIndex: 'receiver',
        key: 'receiver',
        width: 100
      },
      {
        title: '收货电话',
        dataIndex: 'receiverPhone',
        key: 'receiverPhone',
        width: 120
      },
      {
        title: '订单状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        slots: { customRender: 'status' }
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        customRender: ({ record }) => formatDateTime(record.createTime)
      },
      {
        title: '操作',
        key: 'action',
        width: 200,
        slots: { customRender: 'action' }
      }
    ]
    
    const orderItemColumns = [
      {
        title: '订单项ID',
        dataIndex: 'id',
        key: 'id',
        width: 100
      },
      {
        title: '商品名称',
        dataIndex: 'productName',
        key: 'productName',
        width: 150
      },
      {
        title: '商品主图',
        dataIndex: 'productPic',
        key: 'productPic',
        width: 100,
        slots: { customRender: 'productPic' }
      },
      {
        title: '商品规格',
        dataIndex: 'specInfo',
        key: 'specInfo',
        width: 190
      },
      {
        title: '商品单价',
        dataIndex: 'productPrice',
        key: 'productPrice',
        width: 100
      },
      {
        title: '购买数量',
        dataIndex: 'quantity',
        key: 'quantity',
        width: 100
      },
      {
        title: '小计金额',
        dataIndex: 'totalPrice',
        key: 'totalPrice',
        width: 100
      }
    ]

    const refundColumns = [
      {
        title: '订单编号',
        dataIndex: 'orderNo',
        key: 'orderNo'
      },
      {
        title: '用户名',
        dataIndex: 'username',
        key: 'username'
      },
      {
        title: '订单金额',
        dataIndex: 'payPrice',
        key: 'payPrice'
      },
      {
        title: '退款原因',
        dataIndex: 'refundReason',
        key: 'refundReason'
      },
      {
        title: '申请时间',
        dataIndex: 'refundApplyTime',
        key: 'refundApplyTime',
        customRender: ({ record }) => formatDateTime(record.refundApplyTime)
      },
      {
        title: '操作',
        key: 'action',
        width: 100,
        slots: { customRender: 'action' }
      }
    ]

    const refundItemColumns = [
      {
        title: '商品图片',
        dataIndex: 'productPic',
        key: 'productPic',
        width: 80,
        slots: { customRender: 'productPic' }
      },
      {
        title: '商品名称',
        dataIndex: 'productName',
        key: 'productName'
      },
      {
        title: '商品规格',
        dataIndex: 'specInfo',
        key: 'specInfo',
        width: 150
      },
      {
        title: '商品单价',
        dataIndex: 'productPrice',
        key: 'productPrice',
        width: 100
      },
      {
        title: '购买数量',
        dataIndex: 'quantity',
        key: 'quantity',
        width: 100
      },
      {
        title: '小计金额',
        dataIndex: 'totalPrice',
        key: 'totalPrice',
        width: 100
      }
    ]

    const loadOrders = async () => {
      try {
        const params = {}
        if (searchForm.value.orderNo) params.orderNo = searchForm.value.orderNo
        if (searchForm.value.receiver) params.receiver = searchForm.value.receiver
        if (searchForm.value.receiverPhone) params.receiverPhone = searchForm.value.receiverPhone
        if (searchForm.value.productName) params.productName = searchForm.value.productName
        if (searchForm.value.minPrice !== null) params.minPrice = searchForm.value.minPrice
        if (searchForm.value.maxPrice !== null) params.maxPrice = searchForm.value.maxPrice
        if (searchForm.value.status !== null) params.status = searchForm.value.status
        
        const res = await getOrderList(params)
        orders.value = res.data
      } catch (error) {
        console.error('获取订单列表失败', error)
      }
    }

    const search = () => {
      loadOrders()
    }
    
    const resetSearch = () => {
      searchForm.value = {
        orderNo: '',
        receiver: '',
        receiverPhone: '',
        productName: '',
        minPrice: null,
        maxPrice: null,
        status: null
      }
      loadOrders()
    }

    const showAddModal = () => {
      addForm.value = {
        orderNo: '',
        userId: '',
        totalPrice: '',
        payPrice: '',
        receiver: '',
        receiverPhone: '',
        receiverAddress: '',
        status: 0
      }
      isAddModalVisible.value = true
    }

    const handleAddOk = async () => {
      try {
        await addOrder(addForm.value)
        isAddModalVisible.value = false
        loadOrders()
      } catch (error) {
        console.error('添加订单失败', error)
      }
    }

    const handleAddCancel = () => {
      isAddModalVisible.value = false
    }

    const showEditModal = async (record) => {
      editForm.value = {
        id: record.id,
        orderNo: record.orderNo,
        userId: record.userId,
        totalPrice: record.totalPrice,
        payPrice: record.payPrice,
        receiver: record.receiver,
        receiverPhone: record.receiverPhone,
        receiverAddress: record.receiverAddress,
        status: record.status
      }
      try {
        const itemsRes = await getOrderItemsByOrderId(record.id)
        editOrderItems.value = (itemsRes.data || []).map(item => ({
          ...item,
          specsLoading: true,
          groupedSpecs: {},
          selectedSpecs: {}
        }))
        for (const item of editOrderItems.value) {
          loadItemSpecs(item)
        }
      } catch (error) {
        console.error('获取订单项失败', error)
        editOrderItems.value = []
      }
      isEditModalVisible.value = true
    }

    const loadItemSpecs = async (item) => {
      try {
        const res = await getProductSpecs(item.productId)
        const specs = res.data || []
        const groups = {}
        specs.forEach(spec => {
          const name = spec.specName || '规格'
          if (!groups[name]) {
            groups[name] = []
          }
          groups[name].push(spec)
        })
        item.groupedSpecs = groups
        if (Object.keys(groups).length > 0) {
          item.selectedSpecs = {}
          if (item.specInfo) {
            const specParts = item.specInfo.split(', ').map(p => p.trim())
            for (const [specName, options] of Object.entries(groups)) {
              for (const part of specParts) {
                const match = part.match(new RegExp(`${specName}:\\s*(.+)`))
                if (match) {
                  const value = match[1].trim()
                  const foundSpec = options.find(o => o.specValue === value)
                  if (foundSpec) {
                    item.selectedSpecs[specName] = foundSpec.id
                  }
                }
              }
            }
          }
          for (const [specName, options] of Object.entries(groups)) {
            if (!item.selectedSpecs[specName] && options.length > 0) {
              item.selectedSpecs[specName] = options[0].id
            }
          }
          updateItemSpecInfo(item)
        }
      } catch (error) {
        console.error('获取商品规格失败', error)
        item.groupedSpecs = {}
      } finally {
        item.specsLoading = false
      }
    }

    const selectItemSpec = (item, specName, spec) => {
      if (!item.selectedSpecs) {
        item.selectedSpecs = {}
      }
      item.selectedSpecs[specName] = spec.id
      updateItemSpecInfo(item)
    }

    const updateItemSpecInfo = (item) => {
      if (!item.selectedSpecs || !item.groupedSpecs) return
      const specParts = []
      for (const [specName, specId] of Object.entries(item.selectedSpecs)) {
        const options = item.groupedSpecs[specName] || []
        const spec = options.find(o => o.id === specId)
        if (spec) {
          specParts.push(`${specName}: ${spec.specValue}`)
        }
      }
      item.specInfo = specParts.join(', ')
    }

    const handleEditOk = async () => {
      try {
        await updateOrder(editForm.value)
        for (const item of editOrderItems.value) {
          await updateOrderItem({
            id: item.id,
            specInfo: item.specInfo,
            productPrice: item.productPrice,
            quantity: item.quantity,
            totalPrice: item.productPrice * item.quantity
          })
        }
        message.success('订单更新成功')
        isEditModalVisible.value = false
        loadOrders()
      } catch (error) {
        console.error('更新订单失败', error)
        message.error('更新订单失败')
      }
    }

    const handleEditCancel = () => {
      isEditModalVisible.value = false
    }

    const deleteOrderById = async (orderId) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这个订单吗？此操作不可撤销。',
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            await deleteOrder(orderId)
            message.success('删除订单成功')
            loadOrders()
          } catch (error) {
            console.error('删除订单失败', error)
            message.error('删除订单失败')
          }
        }
      })
    }

    const viewOrder = async (orderId) => {
      try {
        const res = await getOrderDetail(orderId)
        viewOrderData.value = res.data
        
        // 加载订单项
        const itemsRes = await getOrderItemsByOrderId(orderId)
        orderItems.value = itemsRes.data
        
        isViewModalVisible.value = true
      } catch (error) {
        console.error('获取订单详情失败', error)
      }
    }

    const handleViewCancel = () => {
      isViewModalVisible.value = false
    }

    const confirmPayment = async () => {
      try {
        const res = await confirmPaymentApi(viewOrderData.value.id)
        message.success(res.message || '确认支付成功')
        // 重新加载订单详情，获取最新状态
        const orderRes = await getOrderDetail(viewOrderData.value.id)
        viewOrderData.value = orderRes.data
      } catch (error) {
        console.error('确认支付失败', error)
        message.error('确认支付失败，请稍后重试')
      }
    }

    const editLogisticsInfo = () => {
      logisticsForm.value = {
        orderId: viewOrderData.value.id,
        expressNo: viewOrderData.value.expressNo || '',
        expressCompany: viewOrderData.value.expressCompany || ''
      }
      isLogisticsModalVisible.value = true
    }

    const handleLogisticsOk = async () => {
      try {
        const res = await updateLogisticsInfoApi(logisticsForm.value)
        message.success(res.message || '物流信息更新成功')
        isLogisticsModalVisible.value = false
        // 重新加载订单详情
        const orderRes = await getOrderDetail(logisticsForm.value.orderId)
        viewOrderData.value = orderRes.data
      } catch (error) {
        console.error('更新物流信息失败', error)
        message.error('更新物流信息失败，请稍后重试')
      }
    }

    const handleLogisticsCancel = () => {
      isLogisticsModalVisible.value = false
    }

    const showRefundModal = async () => {
      try {
        const res = await getRefundList()
        if (res.code === 200) {
          refundList.value = res.data || []
          refundApplyCount.value = refundList.value.length
        }
      } catch (error) {
        console.error('获取退款列表失败', error)
      }
      isRefundModalVisible.value = true
    }

    const showRefundDetail = (record) => {
      refundDetailData.value = record
      refundHandleForm.value.orderId = record.id
      refundHandleForm.value.action = 1
      refundHandleForm.value.reply = ''
      isRefundDetailModalVisible.value = true
    }

    const getTimeoutTime = (applyTime) => {
      if (!applyTime) return ''
      let date
      if (Array.isArray(applyTime)) {
        const [year, month, day, hour, minute, second] = applyTime
        date = new Date(year, month - 1, day, hour, minute, second)
      } else {
        date = new Date(applyTime)
      }
      date.setMinutes(date.getMinutes() + 30)
      return formatDateTime(date)
    }

    const handleRefundOk = async () => {
      try {
        const res = await handleRefundApi({
          orderId: refundHandleForm.value.orderId,
          action: refundHandleForm.value.action,
          reply: refundHandleForm.value.reply
        })
        if (res.code === 200) {
          message.success(res.data || '处理成功')
          isRefundDetailModalVisible.value = false
          showRefundModal()
        } else {
          message.error(res.message || '处理失败')
        }
      } catch (error) {
        console.error('处理退款失败', error)
        message.error('处理失败，请稍后重试')
      }
    }

    const handleRefundCancel = () => {
      isRefundDetailModalVisible.value = false
    }
    
    const handleBatchConfirmPayment = () => {
      Modal.confirm({
        title: '批量确认支付',
        content: `确定要批量确认支付选中的 ${selectedRowKeys.value.length} 个订单吗？`,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          try {
            const res = await batchConfirmPayment(selectedRowKeys.value)
            message.success(res.data || '批量确认支付成功')
            selectedRowKeys.value = []
            loadOrders()
          } catch (error) {
            console.error('批量确认支付失败', error)
            message.error('批量确认支付失败')
          }
        }
      })
    }
    
    const handleBatchShip = async () => {
      if (!batchShipForm.value.expressCompany) {
        message.error('请选择快递公司')
        return
      }
      if (!batchShipForm.value.expressNo) {
        message.error('请输入快递单号')
        return
      }
      
      try {
        const res = await batchShipOrder({
          ids: selectedRowKeys.value,
          expressCompany: batchShipForm.value.expressCompany,
          expressNo: batchShipForm.value.expressNo
        })
        message.success(res.data || '批量发货成功')
        isBatchShipModalVisible.value = false
        batchShipForm.value = { expressCompany: '', expressNo: '' }
        selectedRowKeys.value = []
        loadOrders()
      } catch (error) {
        console.error('批量发货失败', error)
        message.error('批量发货失败')
      }
    }
    
    const showBatchShipModal = () => {
      batchShipForm.value = { expressCompany: '', expressNo: '' }
      isBatchShipModalVisible.value = true
    }
    
    const handleBatchShipCancel = () => {
      isBatchShipModalVisible.value = false
      batchShipForm.value = { expressCompany: '', expressNo: '' }
    }
    
    const handleBatchCancel = () => {
      Modal.confirm({
        title: '批量取消订单',
        content: `确定要批量取消选中的 ${selectedRowKeys.value.length} 个订单吗？此操作将恢复商品库存。注意：已发货、已完成、已退款的订单无法取消。`,
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            const res = await batchCancelOrder(selectedRowKeys.value)
            message.success(res.data || '批量取消成功')
            selectedRowKeys.value = []
            loadOrders()
          } catch (error) {
            console.error('批量取消失败', error)
            message.error('批量取消失败')
          }
        }
      })
    }

    const formatImageUrl = (pic) => {
      if (!pic) return ''
      // 处理 test.com 路径
      if (pic.includes('test.com')) {
        const fileName = pic.split('/').pop()
        return `/api/images/${fileName}`
      }
      // 处理完整 URL
      if (pic.startsWith('http')) {
        return pic
      }
      // 处理相对路径
      return `/${pic}`
    }

    const formatDateTime = (time) => {
      if (!time) return ''
      let date
      // 处理数组格式的时间
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
      loadOrders()
    })

    return {
      orders,
      orderItems,
      searchKeyword,
      searchForm,
      columns,
      orderItemColumns,
      search,
      resetSearch,
      showAddModal,
      handleAddOk,
      handleAddCancel,
      showEditModal,
      handleEditOk,
      handleEditCancel,
      deleteOrderById,
      viewOrder,
      isAddModalVisible,
      isEditModalVisible,
      isViewModalVisible,
      viewOrderData,
      addForm,
      editForm,
      editOrderItems,
      selectItemSpec,
      handleViewCancel,
      confirmPayment,
      editLogisticsInfo,
      isLogisticsModalVisible,
      logisticsForm,
      handleLogisticsOk,
      handleLogisticsCancel,
      formatImageUrl,
      formatDateTime,
      showRefundModal,
      showRefundDetail,
      getTimeoutTime,
      handleRefundOk,
      handleRefundCancel,
      isRefundModalVisible,
      refundList,
      refundApplyCount,
      isRefundDetailModalVisible,
      refundDetailData,
      refundHandleForm,
      refundColumns,
      refundItemColumns,
      selectedRowKeys,
      rowSelection,
      handleBatchConfirmPayment,
      handleBatchShip,
      showBatchShipModal,
      handleBatchShipCancel,
      isBatchShipModalVisible,
      batchShipForm,
      handleBatchCancel
    }
  }
}
</script>

<style scoped>
.order-management {
  padding: 20px;
}

.order-management h2 {
  margin-bottom: 20px;
}

.search-area {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-area .ant-form-item {
  margin-bottom: 8px;
}

.search-area .ant-form-item-label {
  padding-bottom: 4px;
}

/* 操作按钮样式 */
.order-management .ant-table-cell button {
  margin-left: 3px;
  margin-right: 3px;
}

.edit-order-item {
  margin-bottom: 16px;
}

.edit-order-item:last-child {
  margin-bottom: 0;
}

.item-image {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  background: #f5f5f5;
  border-radius: 4px;
}

.specs-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  color: #999;
}

.spec-select-area {
  padding: 8px 0;
}

.spec-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
}

.spec-row:last-child {
  margin-bottom: 0;
}

.spec-label {
  width: 70px;
  font-weight: 500;
  color: #333;
  padding-top: 4px;
  flex-shrink: 0;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.spec-tag {
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: all 0.2s;
  user-select: none;
}

.spec-tag:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.spec-tag-active {
  background-color: #1890ff !important;
  border-color: #1890ff !important;
  color: #fff !important;
}

.spec-tag-active:hover {
  background-color: #40a9ff !important;
  border-color: #40a9ff !important;
  color: #fff !important;
}

.no-specs {
  padding: 4px 0;
}
</style>