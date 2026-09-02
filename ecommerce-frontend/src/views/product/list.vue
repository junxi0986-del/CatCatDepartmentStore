<template>
  <div class="product-list">
    <div class="filter">
      <el-input placeholder="请输入商品名称" v-model="searchKeyword" style="width: 300px; margin-right: 10px" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-select v-model="categoryId" placeholder="选择分类" style="margin-left: 20px" @change="search">
        <el-option label="全部" value="0" />
        <el-option label="数码产品" value="1" />
        <el-option label="家居用品" value="2" />
        <el-option label="服装鞋帽" value="3" />
        <el-option label="手机配件" value="4" />
        <el-option label="厨房用具" value="5" />
      </el-select>
    </div>
    <div class="product-grid">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>
    <div class="pagination">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import ProductCard from '../../components/ProductCard.vue'
import { getProductList } from '../../api/product'

export default {
  components: {
    ProductCard
  },
  setup() {
    const route = useRoute()
    const products = ref([])
    const total = ref(0)
    const pageSize = ref(12)
    const currentPage = ref(1)
    const searchKeyword = ref('')
    const categoryId = ref('0')

    const loadProducts = async () => {
      try {
        const res = await getProductList({
          page: currentPage.value,
          pageSize: pageSize.value,
          keyword: searchKeyword.value,
          categoryId: categoryId.value
        })
        if (res.code === 200) {
          products.value = res.data.list || []
          total.value = res.data.total || 0
        }
      } catch (error) {
        console.error('获取商品列表失败', error)
      }
    }

    const search = () => {
      currentPage.value = 1
      loadProducts()
    }

    const handleCurrentChange = (page) => {
      currentPage.value = page
      loadProducts()
    }

    onMounted(() => {
      // 从URL参数中获取categoryId
      const queryCategoryId = route.query.categoryId
      if (queryCategoryId) {
        categoryId.value = queryCategoryId
      }
      // 从URL参数中获取keyword
      const queryKeyword = route.query.keyword
      if (queryKeyword) {
        searchKeyword.value = queryKeyword
      }
      loadProducts()
    })

    // 监听URL参数变化
    watch(() => route.query.categoryId, (newVal) => {
      if (newVal) {
        categoryId.value = newVal
        currentPage.value = 1
        loadProducts()
      }
    })

    return {
      products,
      total,
      pageSize,
      currentPage,
      searchKeyword,
      categoryId,
      search,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.product-list {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.filter {
  margin-bottom: 30px;
  display: flex;
  align-items: center;
}

.product-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}
</style>
