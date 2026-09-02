<template>
  <div class="dashboard">
    <h2>运营总览</h2>
    
    <!-- 核心指标卡片 -->
    <div class="stats-grid">
      <a-card hoverable>
        <a-statistic title="总用户行为数" :value="dashboardData.totalBehaviorCount" />
      </a-card>
      <a-card hoverable>
        <a-statistic title="今日行为数" :value="dashboardData.todayBehaviorCount" />
      </a-card>
      <a-card hoverable>
        <a-statistic title="总优惠券领取" :value="couponData.totalCouponCount" />
      </a-card>
      <a-card hoverable>
        <a-statistic title="今日优惠券领取" :value="couponData.todayCouponCount" />
      </a-card>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 行为类型占比 -->
      <a-card title="各行为类型占比" hoverable>
        <div class="chart-container">
          <a-pie-chart :data="behaviorTypeData" :width="400" :height="300">
            <a-pie-series
              data-key="count"
              name-key="name"
              :radius="100"
              :label="{ formatter: '{b}: {c} ({d}%)' }"
            />
          </a-pie-chart>
        </div>
      </a-card>

      <!-- 行为趋势 -->
      <a-card title="近7天行为趋势" hoverable>
        <div class="chart-container">
          <a-line-chart :data="behaviorTrendData" :width="400" :height="300">
            <a-cartesian-grid stroke-dasharray="3 3" />
            <a-x-axis data-key="date" />
            <a-y-axis />
            <a-tooltip />
            <a-line data-key="count" stroke="#8884d8" />
          </a-line-chart>
        </div>
      </a-card>
    </div>

    <!-- 热门商品和优惠券 -->
    <div class="tables-grid">
      <!-- 热门商品Top10 -->
      <a-card title="热门商品Top10" hoverable>
        <a-table :columns="hotProductsColumns" :data-source="hotProductsData" row-key="id" pagination={false}>
          <template #pic="{ record }">
            <img :src="record.pic" alt="商品图片" style="width: 50px; height: 50px; object-fit: cover" />
          </template>
        </a-table>
      </a-card>

      <!-- 热门优惠券 -->
      <a-card title="热门优惠券Top10" hoverable>
        <a-table :columns="popularCouponsColumns" :data-source="popularCouponsData" row-key="coupon_name" pagination={false} />
      </a-card>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getDashboardData } from '../api/behavior'
import { getCouponDashboardData } from '../api/userCoupon'

export default {
  setup() {
    const dashboardData = ref({
      totalBehaviorCount: 0,
      todayBehaviorCount: 0,
      behaviorTypeDistribution: [],
      behaviorTrend: [],
      hotProducts: []
    })

    const couponData = ref({
      totalCouponCount: 0,
      todayCouponCount: 0,
      couponStatusDistribution: [],
      popularCoupons: []
    })

    const behaviorTypeData = ref([])
    const behaviorTrendData = ref([])
    const hotProductsData = ref([])
    const popularCouponsData = ref([])

    const hotProductsColumns = [
      {
        title: '排名',
        dataIndex: 'rank',
        key: 'rank',
        width: 60
      },
      {
        title: '商品图片',
        dataIndex: 'pic',
        key: 'pic',
        slots: { customRender: 'pic' }
      },
      {
        title: '商品名称',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: '行为次数',
        dataIndex: 'behavior_count',
        key: 'behavior_count'
      }
    ]

    const popularCouponsColumns = [
      {
        title: '排名',
        dataIndex: 'rank',
        key: 'rank',
        width: 60
      },
      {
        title: '优惠券名称',
        dataIndex: 'coupon_name',
        key: 'coupon_name'
      },
      {
        title: '领取次数',
        dataIndex: 'count',
        key: 'count'
      }
    ]

    const loadDashboardData = async () => {
      try {
        const res = await getDashboardData()
        dashboardData.value = res.data
        
        // 处理行为类型数据
        const typeMap = {
          1: '浏览',
          2: '搜索',
          3: '加购',
          5: '购买'
        }
        behaviorTypeData.value = dashboardData.value.behaviorTypeDistribution.map(item => ({
          name: typeMap[item.behavior_type],
          count: item.count
        }))

        // 处理行为趋势数据
        behaviorTrendData.value = dashboardData.value.behaviorTrend

        // 处理热门商品数据
        hotProductsData.value = dashboardData.value.hotProducts.map((item, index) => ({
          ...item,
          rank: index + 1
        }))
      } catch (error) {
        console.error('获取运营数据失败', error)
        message.error('获取运营数据失败')
      }
    }

    const loadCouponData = async () => {
      try {
        const res = await getCouponDashboardData()
        couponData.value = res.data
        
        // 处理热门优惠券数据
        popularCouponsData.value = couponData.value.popularCoupons.map((item, index) => ({
          ...item,
          rank: index + 1
        }))
      } catch (error) {
        console.error('获取优惠券数据失败', error)
        message.error('获取优惠券数据失败')
      }
    }

    onMounted(() => {
      loadDashboardData()
      loadCouponData()
    })

    return {
      dashboardData,
      couponData,
      behaviorTypeData,
      behaviorTrendData,
      hotProductsData,
      popularCouponsData,
      hotProductsColumns,
      popularCouponsColumns
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.dashboard h2 {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 20px;
}

.chart-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}
</style>