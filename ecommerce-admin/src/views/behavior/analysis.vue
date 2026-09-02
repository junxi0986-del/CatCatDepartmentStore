<template>
  <div class="behavior-analysis">
    <h2>用户行为分析</h2>
    
    <!-- 用户ID输入 -->
    <div class="user-input">
      <a-input v-model:value="userId" placeholder="请输入用户ID" style="width: 300px; margin-right: 10px" />
      <a-button type="primary" @click="analyzeUser">分析用户</a-button>
    </div>

    <!-- 分析结果 -->
    <div v-if="userAnalysisData" class="analysis-result">
      <!-- 兴趣标签 -->
      <a-card title="用户兴趣标签" hoverable class="analysis-card">
        <div class="tags">
          <a-tag v-for="tag in userAnalysisData.interestTags" :key="tag" color="blue">
            {{ tag }}
          </a-tag>
        </div>
      </a-card>

      <!-- 行为类型统计 -->
      <a-card title="行为类型统计" hoverable class="analysis-card">
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

      <!-- 行为时间线 -->
      <a-card title="行为时间线" hoverable class="analysis-card">
        <a-timeline>
          <a-timeline-item
            v-for="behavior in userAnalysisData.behaviors"
            :key="behavior.id"
            :color="getBehaviorColor(behavior.behavior_type)"
            :dot="getBehaviorIcon(behavior.behavior_type)"
          >
            <div class="timeline-content">
              <h4>{{ getBehaviorName(behavior.behavior_type) }}</h4>
              <p>商品ID: {{ behavior.product_id }}</p>
              <p v-if="behavior.stay_time">停留时长: {{ behavior.stay_time }}秒</p>
              <p>时间: {{ behavior.create_time }}</p>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-card>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { analyzeUserBehavior } from '../../api/behavior'

export default {
  setup() {
    const userId = ref('')
    const userAnalysisData = ref(null)
    const behaviorTypeData = ref([])

    const getBehaviorName = (type) => {
      const typeMap = {
        1: '浏览商品',
        2: '搜索商品',
        3: '加入购物车',
        5: '购买商品'
      }
      return typeMap[type] || '未知行为'
    }

    const getBehaviorColor = (type) => {
      const colorMap = {
        1: 'blue',
        2: 'purple',
        3: 'orange',
        5: 'red'
      }
      return colorMap[type] || 'default'
    }

    const getBehaviorIcon = (type) => {
      const iconMap = {
        1: 'eye',
        2: 'search',
        3: 'shopping-cart',
        5: 'check-circle'
      }
      return iconMap[type] || 'question-circle'
    }

    const analyzeUser = async () => {
      if (!userId.value) {
        message.error('请输入用户ID')
        return
      }

      try {
        const res = await analyzeUserBehavior(parseInt(userId.value))
        userAnalysisData.value = res.data
        
        // 处理行为类型数据
        const typeMap = {
          1: '浏览',
          2: '搜索',
          3: '加购',
          5: '购买'
        }
        behaviorTypeData.value = Object.entries(userAnalysisData.value.behaviorTypeCount).map(([type, count]) => ({
          name: typeMap[type],
          count: count
        }))
      } catch (error) {
        console.error('分析用户行为失败', error)
        message.error('分析用户行为失败')
      }
    }

    return {
      userId,
      userAnalysisData,
      behaviorTypeData,
      getBehaviorName,
      getBehaviorColor,
      getBehaviorIcon,
      analyzeUser
    }
  }
}
</script>

<style scoped>
.behavior-analysis {
  padding: 20px;
}

.behavior-analysis h2 {
  margin-bottom: 20px;
}

.user-input {
  margin-bottom: 20px;
}

.analysis-result {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.analysis-card {
  margin-bottom: 20px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chart-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.timeline-content {
  padding: 10px 0;
}

.timeline-content h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: 600;
}

.timeline-content p {
  margin: 5px 0;
  font-size: 14px;
  color: #666;
}
</style>