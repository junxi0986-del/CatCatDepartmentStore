<template>
  <div class="ai-recommend">
    <h2>AI猜你喜欢</h2>
    <div class="product-list">
      <ProductCard 
        v-for="product in recommendList" 
        :key="product.id" 
        :product="product" 
      />
    </div>
  </div>
</template>

<script>
import { getRecommend } from '../api/recommend'
import ProductCard from './ProductCard.vue'

export default {
  components: {
    ProductCard
  },
  data() {
    return {
      recommendList: []
    }
  },
  mounted() {
    this.fetchRecommend()
  },
  methods: {
    fetchRecommend() {
      getRecommend()
        .then(res => {
          this.recommendList = res.data
        })
        .catch(err => {
          console.error('获取推荐商品失败', err)
        })
    }
  }
}
</script>

<style scoped>
.ai-recommend {
  margin: 50px 0;
}

.ai-recommend h2 {
  text-align: center;
  margin-bottom: 30px;
  font-size: 24px;
  color: #333;
}

.product-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}
</style>
