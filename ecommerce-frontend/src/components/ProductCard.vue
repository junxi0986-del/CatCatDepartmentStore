<template>
  <div class="product-card" @click="viewDetail">
    <div class="product-image">
      <img :src="formatImageUrl(product.pic || product.image, product.id, product.categoryId)" :alt="product.name" />
    </div>
    <div class="product-info">
      <h3>{{ product.name }}</h3>
      <p class="product-price">¥{{ product.price }}</p>
      <button class="add-to-cart" @click.stop="addToCart">加入购物车</button>
    </div>
  </div>
</template>

<script>
import { addToCart } from '../api/cart'

export default {
  props: {
    product: {
      type: Object,
      required: true
    }
  },
  methods: {
    formatImageUrl(url, productId, categoryId) {
      if (!url) return ''
      if (url.startsWith('http')) {
        // 处理test.com路径
        if (url.includes('test.com')) {
          const fileName = url.split('/').pop()
          return `/images/${fileName}`
        }
        return url
      }
      // 处理相对路径，通过Vite代理访问
      return `/images/${url.replace('images/', '')}`
    },
    addToCart() {
      addToCart({ productId: this.product.id, quantity: 1 })
        .then(res => {
          alert('加入购物车成功')
        })
    },
    viewDetail() {
      window.location.href = `/product/${this.product.id}`
    }
  }
}
</script>

<style scoped>
.product-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  margin: 10px;
  width: 250px;
  text-align: center;
  transition: transform 0.3s;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.product-image {
  margin-bottom: 15px;
}

.product-image img {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info h3 {
  margin-bottom: 10px;
  font-size: 16px;
}

.product-price {
  color: #ff6b6b;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 15px;
}

.add-to-cart {
  padding: 8px 16px;
  margin: 0 5px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  background: #ff6b6b;
  color: #fff;
}
</style>
