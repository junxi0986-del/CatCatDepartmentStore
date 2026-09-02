import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/home/index.vue')
  },
  {
    path: '/login',
    redirect: '/user/login'
  },
  {
    path: '/register',
    redirect: '/user/register'
  },
  {
    path: '/product',
    name: 'ProductList',
    component: () => import('../views/product/list.vue')
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/product/detail.vue')
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/cart/index.vue')
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('../views/order/index.vue')
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('../views/order/detail.vue'),
    meta: { title: '订单详情' }
  },
  {
    path: '/pay',
    name: 'Pay',
    component: () => import('../views/order/pay.vue'),
    meta: { title: '订单支付' }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('../views/user/index.vue')
  },
  {
    path: '/user/login',
    name: 'Login',
    component: () => import('../views/user/login.vue')
  },
  {
    path: '/user/register',
    name: 'Register',
    component: () => import('../views/user/register.vue')
  },
  {
    path: '/user/order',
    name: 'UserOrder',
    redirect: '/order'
  },
  {
    path: '/user/address',
    name: 'UserAddress',
    redirect: '/address'
  },
  {
    path: '/user/edit',
    name: 'EditUser',
    component: () => import('../views/user/edit.vue')
  },
  {
    path: '/user/coupons',
    name: 'UserCoupons',
    component: () => import('../views/user/coupons.vue')
  },
  {
    path: '/address',
    name: 'Address',
    component: () => import('../views/address/index.vue')
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/chat/index.vue')
  },
  {
    path: '/ai-search',
    name: 'AiSearch',
    component: () => import('../views/ai-search/index.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
