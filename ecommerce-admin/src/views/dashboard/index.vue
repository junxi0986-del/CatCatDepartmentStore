<template>
  <div class="dashboard">
    <h2>数据概览</h2>
    <div class="stats">
      <div class="stat-item" v-for="(item, index) in statItems" :key="index">
        <h3>{{ item.label }}</h3>
        <p :class="{ 'animate-number': item.animated }">{{ item.prefix }}{{ item.value }}</p>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-header">
        <h3>销售数据分析</h3>
        <div class="chart-controls">
          <a-select v-model:value="selectedMonth" style="width: 150px;" @change="onMonthChange">
            <a-select-option v-for="month in availableMonths" :key="month" :value="month">
              {{ month }}
            </a-select-option>
          </a-select>
          <a-button type="primary" @click="exportReport" :loading="exporting">
            <template #icon><DownloadOutlined /></template>
            导出报表
          </a-button>
          <a-button @click="analyzeData" :loading="analyzing">
            <template #icon><LineChartOutlined /></template>
            AI分析
          </a-button>
        </div>
      </div>
      
      <div class="bar-chart-container">
        <div class="bar-chart">
          <div 
            v-for="(item, index) in dailyChartData" 
            :key="index" 
            class="bar-item"
            :class="{ 'animate-item': chartAnimated }"
            :style="{ animationDelay: index * 0.05 + 's' }"
          >
            <div class="bar-wrapper">
              <a-tooltip placement="top">
                <template #title>
                  <div class="tooltip-content">
                    <div><strong>{{ item.date }}</strong></div>
                    <div>订单数: {{ item.orderCount }}</div>
                    <div>销售额: ¥{{ formatNumber(item.amount) }}</div>
                    <div v-if="item.avgOrderAmount">客单价: ¥{{ formatNumber(item.avgOrderAmount) }}</div>
                  </div>
                </template>
                <div 
                  class="bar" 
                  :class="{ 'animate-bar': chartAnimated, 'bar-highlight': item.isToday }"
                  :style="{ height: getBarHeight(item.amount) + '%' }"
                  @click="showDayDetail(item)"
                >
                  <span class="bar-value">¥{{ formatNumber(item.amount) }}</span>
                </div>
              </a-tooltip>
            </div>
            <span class="bar-label">{{ item.day }}</span>
          </div>
        </div>
      </div>

      <div class="chart-summary">
        <div class="summary-card">
          <div class="summary-title">本月总销售额</div>
          <div class="summary-value">¥{{ formatNumber(monthSummary.totalAmount) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-title">本月总订单</div>
          <div class="summary-value">{{ monthSummary.totalOrders }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-title">日均销售额</div>
          <div class="summary-value">¥{{ formatNumber(monthSummary.avgAmount) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-title">环比增长</div>
          <div class="summary-value" :class="monthSummary.growth >= 0 ? 'positive' : 'negative'">
            {{ monthSummary.growth >= 0 ? '+' : '' }}{{ monthSummary.growth.toFixed(1) }}%
          </div>
        </div>
      </div>
    </div>

    <a-modal 
      v-model:open="dayDetailVisible" 
      :title="selectedDay + ' 销售明细'"
      width="600px"
      :footer="null"
    >
      <div v-if="dayDetail" class="day-detail">
        <div class="day-stats">
          <div class="day-stat-item">
            <span class="label">销售额</span>
            <span class="value">¥{{ formatNumber(dayDetail.amount) }}</span>
          </div>
          <div class="day-stat-item">
            <span class="label">订单数</span>
            <span class="value">{{ dayDetail.orderCount }}</span>
          </div>
          <div class="day-stat-item">
            <span class="label">客单价</span>
            <span class="value">¥{{ formatNumber(dayDetail.avgOrderAmount) }}</span>
          </div>
        </div>
        <a-table 
          :columns="orderColumns" 
          :data-source="dayOrders" 
          :pagination="{ pageSize: 5 }"
          size="small"
        />
      </div>
    </a-modal>

    <a-modal 
      v-model:open="analysisVisible" 
      title="AI 数据分析报告"
      width="800px"
      :footer="null"
    >
      <div v-if="analyzing" class="analyzing">
        <a-spin size="large" />
        <p>AI正在分析数据...</p>
      </div>
      <div v-else class="analysis-report">
        <div class="report-section" v-html="analysisReport"></div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, LineChartOutlined } from '@ant-design/icons-vue'
import { getStatisticsOverview, getMonthlySales, getDailySales } from '../../api/statistics'

export default {
  components: { DownloadOutlined, LineChartOutlined },
  setup() {
    const stats = ref({ userCount: 0, productCount: 0, orderCount: 0, salesAmount: 0 })
    const displayStats = ref({ userCount: 0, productCount: 0, orderCount: 0, salesAmount: 0 })
    const chartAnimated = ref(false)
    const animationStarted = ref(false)
    
    const selectedMonth = ref('')
    const availableMonths = ref([])
    const dailyChartData = ref([])
    const lastMonthAmount = ref(0)
    const maxAmount = ref(1)
    const exporting = ref(false)
    const analyzing = ref(false)
    const analysisVisible = ref(false)
    const analysisReport = ref('')
    
    const dayDetailVisible = ref(false)
    const selectedDay = ref('')
    const dayDetail = ref(null)
    const dayOrders = ref([])
    
    const orderColumns = [
      { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
      { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
      { title: '时间', dataIndex: 'time', key: 'time' }
    ]
    
    const monthSummary = computed(() => {
      const totalAmount = dailyChartData.value.reduce((sum, item) => sum + (item.amount || 0), 0)
      const totalOrders = dailyChartData.value.reduce((sum, item) => sum + (item.orderCount || 0), 0)
      const days = dailyChartData.value.length || 1
      const avgAmount = totalAmount / days
      let growth = 0
      if (lastMonthAmount.value > 0) {
        growth = ((totalAmount - lastMonthAmount.value) / lastMonthAmount.value) * 100
      }
      return { totalAmount, totalOrders, avgAmount, growth }
    })

    const animateNumber = (key, target, duration = 1500) => {
      const start = 0
      const startTime = performance.now()
      
      const animate = (currentTime) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        const easeOutQuart = 1 - Math.pow(1 - progress, 4)
        const current = start + (target - start) * easeOutQuart
        
        if (key === 'salesAmount') {
          displayStats.value[key] = current.toFixed(2)
        } else {
          displayStats.value[key] = Math.floor(current)
        }
        
        if (progress < 1) {
          requestAnimationFrame(animate)
        } else {
          if (key === 'salesAmount') {
            displayStats.value[key] = target.toFixed(2)
          } else {
            displayStats.value[key] = target
          }
        }
      }
      
      requestAnimationFrame(animate)
    }

    const statItems = computed(() => [
      { label: '总用户数', value: displayStats.value.userCount, prefix: '', animated: animationStarted.value },
      { label: '总商品数', value: displayStats.value.productCount, prefix: '', animated: animationStarted.value },
      { label: '总订单数', value: displayStats.value.orderCount, prefix: '', animated: animationStarted.value },
      { label: '总销售额', value: displayStats.value.salesAmount, prefix: '¥', animated: animationStarted.value }
    ])

    const formatNumber = (num) => {
      if (num === null || num === undefined) return '0.00'
      return parseFloat(num).toFixed(2)
    }

    const getBarHeight = (amount) => {
      if (maxAmount.value === 0) return 0
      return Math.min((amount / maxAmount.value) * 85, 85)
    }

    const loadStats = async () => {
      try {
        const res = await getStatisticsOverview()
        if (res.code === 200) {
          stats.value = res.data
          setTimeout(() => {
            animationStarted.value = true
            animateNumber('userCount', stats.value.userCount)
            animateNumber('productCount', stats.value.productCount)
            animateNumber('orderCount', stats.value.orderCount)
            animateNumber('salesAmount', stats.value.salesAmount)
          }, 600)
        }
      } catch (error) {
        console.error('获取统计数据失败', error)
      }
    }

    const loadAvailableMonths = async () => {
      try {
        const res = await getMonthlySales()
        if (res.code === 200) {
          availableMonths.value = res.data.map(item => item.month)
          if (availableMonths.value.length > 0) {
            selectedMonth.value = availableMonths.value[0]
            await loadDailyData()
          }
        }
      } catch (error) {
        console.error('获取月份列表失败', error)
      }
    }

    const loadDailyData = async () => {
      if (!selectedMonth.value) return
      try {
        const res = await getDailySales(selectedMonth.value)
        if (res.code === 200) {
          const today = new Date().toISOString().split('T')[0]
          dailyChartData.value = res.data.map(item => {
            const amount = parseFloat(item.amount) || 0
            const orderCount = item.orderCount || 0
            return {
              date: item.date,
              day: item.date.split('-')[2],
              amount,
              orderCount,
              avgOrderAmount: orderCount > 0 ? amount / orderCount : 0,
              isToday: item.date === today
            }
          })
          
          if (dailyChartData.value.length > 0) {
            maxAmount.value = Math.max(...dailyChartData.value.map(item => item.amount)) || 1
          }
          
          setTimeout(() => {
            chartAnimated.value = true
          }, 300)
        }
        
        const [year, month] = selectedMonth.value.split('-')
        const lastMonth = month === '01' ? `${parseInt(year) - 1}-12` : `${year}-${String(parseInt(month) - 1).padStart(2, '0')}`
        try {
          const lastRes = await getDailySales(lastMonth)
          if (lastRes.code === 200) {
            lastMonthAmount.value = lastRes.data.reduce((sum, item) => sum + (parseFloat(item.amount) || 0), 0)
          }
        } catch (e) {
          lastMonthAmount.value = 0
        }
      } catch (error) {
        console.error('获取每日数据失败', error)
      }
    }

    const onMonthChange = () => {
      chartAnimated.value = false
      loadDailyData()
    }

    const showDayDetail = async (item) => {
      selectedDay.value = item.date
      dayDetail.value = item
      dayOrders.value = []
      dayDetailVisible.value = true
      
      try {
        const response = await fetch(`/api/admin/statistics/ordersByDate?date=${item.date}`)
        const res = await response.json()
        if (res.code === 200) {
          dayOrders.value = res.data.map(order => ({
            orderNo: order.order_no || order.orderNo || '-',
            amount: `¥${formatNumber(order.pay_price || order.payPrice || 0)}`,
            time: (order.create_time || order.createTime || '').split(' ')[1] || '-'
          }))
        }
      } catch (error) {
        console.error('获取订单明细失败', error)
      }
    }

    const exportReport = async () => {
      exporting.value = true
      try {
        const response = await fetch(`/api/admin/statistics/exportReport?month=${selectedMonth.value}`)
        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `销售报表_${selectedMonth.value}.csv`
        a.click()
        window.URL.revokeObjectURL(url)
        message.success('报表导出成功')
      } catch (error) {
        message.error('导出失败，请稍后重试')
      } finally {
        exporting.value = false
      }
    }

    const analyzeData = async () => {
      analyzing.value = true
      analysisVisible.value = true
      
      try {
        const response = await fetch('/api/ai/analyzeSalesData', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            month: selectedMonth.value,
            dailyData: dailyChartData.value,
            summary: monthSummary.value
          })
        })
        const res = await response.json()
        if (res.code === 200) {
          analysisReport.value = res.data.report
        } else {
          analysisReport.value = generateDefaultAnalysis()
        }
      } catch (error) {
        analysisReport.value = generateDefaultAnalysis()
      } finally {
        analyzing.value = false
      }
    }

    const generateDefaultAnalysis = () => {
      const total = monthSummary.value.totalAmount
      const orders = monthSummary.value.totalOrders
      const avg = monthSummary.value.avgAmount
      return `
        <h4>📊 ${selectedMonth.value} 销售数据分析</h4>
        <p><strong>总销售额：</strong>¥${formatNumber(total)}</p>
        <p><strong>总订单数：</strong>${orders} 单</p>
        <p><strong>日均销售额：</strong>¥${formatNumber(avg)}</p>
        <p><strong>趋势分析：</strong>本月销售${total > 100000 ? '表现良好' : '有待提升'}，建议关注高峰时段的促销活动。</p>
        <p><strong>运营建议：</strong>建议优化商品推荐算法，提升客单价；加强用户粘性，提高复购率。</p>
      `
    }

    onMounted(() => {
      loadStats()
      loadAvailableMonths()
    })

    return {
      stats, statItems, chartAnimated, formatNumber, getBarHeight,
      selectedMonth, availableMonths, dailyChartData, monthSummary,
      exporting, analyzing, analysisVisible, analysisReport,
      dayDetailVisible, selectedDay, dayDetail, dayOrders, orderColumns,
      onMonthChange, showDayDetail, exportReport, analyzeData
    }
  }
}
</script>

<style scoped>
.dashboard { padding: 20px; }
.dashboard h2 { margin-bottom: 30px; color: #333; }

.stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 30px;
}

.stat-item {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  width: 220px;
  opacity: 0;
  transform: translateY(20px);
  animation: slideInUp 0.6s ease forwards;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.stat-item:nth-child(2) { animation-delay: 0.1s; }
.stat-item:nth-child(3) { animation-delay: 0.2s; }
.stat-item:nth-child(4) { animation-delay: 0.3s; }

@keyframes slideInUp {
  to { opacity: 1; transform: translateY(0); }
}

.stat-item h3 { margin-bottom: 12px; color: rgba(255, 255, 255, 0.8); font-size: 16px; font-weight: 500; }
.stat-item p { font-size: 28px; font-weight: bold; color: #fff; }
.stat-item p.animate-number { animation: numberScroll 1.5s ease-out; }

@keyframes numberScroll {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.chart-section {
  margin-top: 20px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 { margin: 0; }

.chart-controls { display: flex; gap: 10px; }

.bar-chart-container { 
  width: 100%; 
  padding: 10px 0; 
  overflow-x: auto;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-evenly;
  height: 280px;
  padding: 20px 10px 10px;
  border-bottom: 2px solid #e8e8e8;
  border-left: 2px solid #e8e8e8;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  min-width: 30px;
  max-width: 50px;
}

.bar-wrapper { 
  height: 220px; 
  display: flex; 
  align-items: flex-end; 
  justify-content: center;
  width: 100%; 
  padding-top: 30px;
}

.bar {
  width: 30px;
  background: linear-gradient(180deg, #1890ff 0%, #096dd9 100%);
  border-radius: 4px 4px 0 0;
  position: relative;
  min-height: 5px;
  height: 0;
  opacity: 0;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.bar:hover { transform: scaleX(1.1); box-shadow: 0 0 10px rgba(24, 144, 255, 0.5); }
.bar-highlight { background: linear-gradient(180deg, #52c41a 0%, #389e0d 100%); }

.bar.animate-bar { animation: barGrow 0.5s ease-out forwards; }

@keyframes barGrow {
  0% { height: 0 !important; opacity: 0; }
  30% { opacity: 0.5; }
  100% { opacity: 1; }
}

.bar-value {
  position: absolute;
  top: -22px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #666;
  white-space: nowrap;
  font-weight: 500;
}
.bar.animate-bar .bar-value { animation: fadeInUp 0.3s ease 0.4s forwards; opacity: 0; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateX(-50%) translateY(5px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.bar-label { 
  margin-top: 8px; 
  font-size: 12px; 
  color: #666; 
  text-align: center;
}
.bar-item.animate-item .bar-label { animation: fadeIn 0.3s ease 0.5s forwards; opacity: 0; }

@keyframes fadeIn { 
  from { opacity: 0; }
  to { opacity: 1; } 
}

.tooltip-content { font-size: 13px; line-height: 1.6; }

.chart-summary {
  display: flex;
  gap: 20px;
  margin-top: 20px;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.summary-card { flex: 1; text-align: center; }
.summary-title { font-size: 14px; color: #666; margin-bottom: 8px; }
.summary-value { font-size: 24px; font-weight: bold; color: #333; }
.summary-value.positive { color: #52c41a; }
.summary-value.negative { color: #f5222d; }

.day-detail { padding: 10px; }
.day-stats { display: flex; gap: 30px; margin-bottom: 20px; }
.day-stat-item { display: flex; flex-direction: column; }
.day-stat-item .label { font-size: 12px; color: #999; }
.day-stat-item .value { font-size: 20px; font-weight: bold; color: #333; }

.analyzing {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.analysis-report { padding: 20px; line-height: 1.8; }
.report-section h4 { color: #1890ff; margin-bottom: 15px; }
.report-section p { margin-bottom: 10px; }
</style>
