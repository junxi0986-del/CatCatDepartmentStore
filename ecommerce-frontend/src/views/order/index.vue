<template>
  <div class="order">
    <h2>订单管理</h2>
    <el-tabs v-model="activeTab" @tab-click="(tab) => loadOrderList(tab.props.name)">
      <el-tab-pane label="全部订单" name="all">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <div>
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
                <div v-if="scope.row.status === 0 && countdowns[scope.row.id]" class="countdown">
                  <span class="countdown-label">支付剩余:</span>
                  <span class="countdown-time" :class="{ 'countdown-warning': countdowns[scope.row.id] <= 60 }">
                    {{ formatCountdown(countdowns[scope.row.id]) }}
                  </span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button v-if="scope.row.status === 0" type="primary" size="small" @click="pay(scope.row.id)">支付</el-button>
              <el-button v-if="(scope.row.status >= 1 && scope.row.status <= 3) && (!scope.row.refundStatus || scope.row.refundStatus === 3)" type="warning" size="small" @click="viewDetail(scope.row.id)">申请退款</el-button>
              <el-button v-if="scope.row.status === 3" type="success" size="small" @click="handleConfirmReceipt(scope.row.id)">确认收货</el-button>
              <el-button v-if="scope.row.status === 0" type="danger" size="small" @click="handleCancelOrder(scope.row.id)">取消</el-button>

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
      <el-tab-pane label="待支付" name="unpaid">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <div>
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
                <div v-if="scope.row.status === 0 && countdowns[scope.row.id]" class="countdown">
                  <span class="countdown-label">支付剩余:</span>
                  <span class="countdown-time" :class="{ 'countdown-warning': countdowns[scope.row.id] <= 60 }">
                    {{ formatCountdown(countdowns[scope.row.id]) }}
                  </span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button v-if="scope.row.status === 0" type="primary" size="small" @click="pay(scope.row.id)">支付</el-button>
              <el-button v-if="scope.row.status === 0" type="danger" size="small" @click="handleCancelOrder(scope.row.id)">取消</el-button>

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
      <el-tab-pane label="已支付" name="paid">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
      <el-tab-pane label="待发货" name="unshipped">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button v-if="(!scope.row.refundStatus || scope.row.refundStatus === 3)" type="warning" size="small" @click="viewDetail(scope.row.id)">申请退款</el-button>

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
      <el-tab-pane label="待收货" name="unreceived">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button type="success" size="small" @click="handleConfirmReceipt(scope.row.id)">确认收货</el-button>
              <el-button v-if="(!scope.row.refundStatus || scope.row.refundStatus === 3)" type="warning" size="small" @click="viewDetail(scope.row.id)">申请退款</el-button>

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
      <el-tab-pane label="已完成" name="completed">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>

      <!-- 已取消订单 -->
      <el-tab-pane label="已取消" name="cancelled">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
      <!-- 已退款订单 -->
      <el-tab-pane label="已退款" name="refunded">
        <el-table :data="orderList" style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'createTime', order: 'descending' }" @row-click="handleRowClick" class="clickable-table">
          <el-table-column prop="orderNo" label="订单号" width="150" sortable="custom" />
          <el-table-column label="商品" width="300">
            <template #default="scope">
              <div v-for="item in scope.row.items" :key="item.id" class="order-item">
                <img :src="formatImageUrl(item.productPic)" style="width: 60px; height: 60px" />
                <div class="item-info">
                  <span class="item-name">{{ item.productName }}</span>
                  <span class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" sortable="custom" />
          <el-table-column label="物流信息" width="200">
            <template #default="scope">
              <div v-if="scope.row.expressNo" class="logistics-info">
                <div>{{ scope.row.expressCompany }}</div>
                <div class="express-no">{{ scope.row.expressNo }}</div>
              </div>
              <span v-else class="no-logistics">暂无物流</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="150">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="150" sortable="custom">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">

            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, confirmReceipt as confirmReceiptApi } from '../../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  setup() {
    const router = useRouter()
    const activeTab = ref('all')
    const orderList = ref([])
    const countdowns = ref({})
    let timer = null
    const orderAutoCancelMinutes = ref(30)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)

    const loadOrderList = async (tabName) => {
      try {
        const configRes = await fetch('/api/admin/system/config')
        const configData = await configRes.json()
        if (configData.code === 200 && configData.data.orderAutoCancel) {
          orderAutoCancelMinutes.value = parseInt(configData.data.orderAutoCancel) || 30
        }
      } catch (e) {
        console.error('获取配置失败', e)
      }
      
      try {
        const status = tabName === 'all' ? '' : tabName
        const res = await getOrderList({ 
          status,
          page: currentPage.value,
          size: pageSize.value
        })
        console.log('订单列表响应:', res)
        if (res.code === 200) {
          if (Array.isArray(res.data)) {
            orderList.value = res.data
            total.value = res.data.length
          } else if (res.data && res.data.list) {
            orderList.value = res.data.list
            total.value = res.data.total || res.data.list.length
          } else {
            orderList.value = res.data || []
            total.value = orderList.value.length
          }
          initCountdowns()
        } else {
          ElMessage.error(res.message || '获取订单列表失败')
          orderList.value = []
          total.value = 0
        }
      } catch (error) {
        console.error('获取订单列表失败', error)
        ElMessage.error('获取订单列表失败，请稍后重试')
        orderList.value = []
        total.value = 0
      }
    }

    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
      loadOrderList(activeTab.value)
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      loadOrderList(activeTab.value)
    }

    const initCountdowns = () => {
      const now = Date.now()
      const cancelMs = orderAutoCancelMinutes.value * 60 * 1000
      
      orderList.value.forEach(order => {
        if (order.status === 0 && order.createTime) {
          const createTime = new Date(order.createTime).getTime()
          const remaining = createTime + cancelMs - now
          if (remaining > 0) {
            countdowns.value[order.id] = Math.ceil(remaining / 1000)
          } else {
            countdowns.value[order.id] = 0
          }
        }
      })
    }

    const updateCountdowns = () => {
      let needRefresh = false
      
      Object.keys(countdowns.value).forEach(orderId => {
        if (countdowns.value[orderId] > 0) {
          countdowns.value[orderId]--
        } else if (countdowns.value[orderId] === 0) {
          handleAutoCancelOrder(orderId)
          needRefresh = true
          delete countdowns.value[orderId]
        }
      })
      
      if (needRefresh) {
        loadOrderList(activeTab.value)
      }
    }

    const handleAutoCancelOrder = async (orderId) => {
      try {
        await cancelOrder(parseInt(orderId))
        ElMessage.info('订单已自动取消')
      } catch (error) {
        console.error('自动取消订单失败', error)
      }
    }

    const getStatusType = (status) => {
      switch (status) {
        case 0: return 'warning'
        case 1: return 'info'
        case 2: return 'primary'
        case 3: return 'success'
        case 4: return 'success'
        case 5: return 'danger'
        case 6: return 'danger'
        default: return ''
      }
    }

    const getStatusText = (status) => {
      switch (status) {
        case 0: return '待支付'
        case 1: return '已支付'
        case 2: return '待发货'
        case 3: return '待收货'
        case 4: return '已完成'
        case 5: return '已取消'
        case 6: return '已退款'
        default: return '未知状态'
      }
    }

    const formatCountdown = (seconds) => {
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }

    const pay = (orderId) => {
      router.push(`/order/${orderId}`)
    }

    const handleConfirmReceipt = async (orderId) => {
      try {
        await ElMessageBox.confirm('确定要确认收货吗？确认后订单将完成', '确认收货', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await confirmReceiptApi({ orderId })
        if (res.code === 200) {
          ElMessage.success('确认收货成功')
          loadOrderList(activeTab.value)
        } else {
          ElMessage.error(res.message || '确认收货失败')
        }
      } catch {
      }
    }

    const handleCancelOrder = async (orderId) => {
      try {
        await ElMessageBox.confirm('确定要取消该订单吗？此操作不可恢复', '取消订单', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await cancelOrder(orderId)
        if (res.code === 200) {
          ElMessage.success('订单已取消')
          loadOrderList(activeTab.value)
        } else {
          ElMessage.error(res.message || '取消订单失败')
        }
      } catch {
      }
    }

    const handleSortChange = ({ prop, order }) => {
      if (!order) {
        loadOrderList(activeTab.value)
        return
      }
      
      orderList.value.sort((a, b) => {
        if (order === 'ascending') {
          return a[prop] > b[prop] ? 1 : -1
        } else {
          return a[prop] < b[prop] ? 1 : -1
        }
      })
    }

    const viewDetail = (orderId) => {
      router.push(`/order/${orderId}`)
    }

    const handleRowClick = (row) => {
      router.push(`/order/${row.id}`)
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

    const formatTime = (time) => {
      if (!time) return ''
      const date = new Date(time)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
    }

    onMounted(() => {
      loadOrderList(activeTab.value)
      timer = setInterval(updateCountdowns, 1000)
    })

    onUnmounted(() => {
      if (timer) {
        clearInterval(timer)
      }
    })

    return {
      activeTab,
      orderList,
      countdowns,
      currentPage,
      pageSize,
      total,
      getStatusType,
      getStatusText,
      formatCountdown,
      pay,
      handleConfirmReceipt,
      handleCancelOrder,
      viewDetail,
      handleRowClick,
      loadOrderList,
      handleSortChange,
      handleSizeChange,
      handleCurrentChange,
      formatImageUrl,
      formatTime
    }
  }
}
</script>

<style scoped>
.order {
  width: 1340px;
  margin: 0 auto;
  padding: 20px 0;
}

.order h2 {
  margin-bottom: 30px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.order-item {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
  flex-wrap: wrap;
  width: 100%;
}

.order-item img {
  margin-right: 5px;
}

.item-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.item-name {
  font-weight: 500;
}

.item-spec {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 3px;
  margin: 2px 0;
}

.item-qty {
  color: #999;
  font-size: 13px;
}

.order-item span {
  margin-right: 5px;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 5px;
  font-size: 12px;
}

.countdown-label {
  color: #999;
}

.countdown-time {
  color: #ff4400;
  font-weight: bold;
  font-family: monospace;
}

.countdown-time.countdown-warning {
  color: #f5222d;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.clickable-table .el-table__row {
  cursor: pointer;
}

.clickable-table .el-table__row:hover {
  background-color: #f5f7fa;
}
</style>
