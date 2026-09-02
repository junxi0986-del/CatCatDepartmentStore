import { createRouter, createWebHistory } from 'vue-router'

const allMenuRoutes = [
  { path: 'dashboard', name: 'Dashboard', permission: 'dashboard' },
  { path: 'user', name: 'User', permission: 'user' },
  { path: 'product', name: 'Product', permission: 'product' },
  { path: 'order', name: 'Order', permission: 'order' },
  { path: 'service', name: 'Service', permission: 'service' },
  { path: 'service/knowledge', name: 'Knowledge', permission: 'knowledge' },
  { path: 'coupon', name: 'Coupon', permission: 'coupon' },
  { path: 'behavior', name: 'Behavior', permission: 'behavior' },
  { path: 'behavior/analysis', name: 'BehaviorAnalysis', permission: 'behavior' },
  { path: 'userCoupon', name: 'UserCoupon', permission: 'userCoupon' },
  { path: 'system', name: 'System', permission: 'system' },
  { path: 'admin', name: 'AdminManage', permission: 'admin' }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { permission: 'dashboard' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/user/index.vue'),
        meta: { permission: 'user' }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('../views/product/index.vue'),
        meta: { permission: 'product' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('../views/order/index.vue'),
        meta: { permission: 'order' }
      },
      {
        path: 'coupon',
        name: 'Coupon',
        component: () => import('../views/coupon/index.vue'),
        meta: { permission: 'coupon' }
      },
      {
        path: 'behavior',
        name: 'Behavior',
        component: () => import('../views/behavior/index.vue'),
        meta: { permission: 'behavior' }
      },
      {
        path: 'behavior/analysis',
        name: 'BehaviorAnalysis',
        component: () => import('../views/behavior/analysis.vue'),
        meta: { permission: 'behavior' }
      },
      {
        path: 'userCoupon',
        name: 'UserCoupon',
        component: () => import('../views/userCoupon/index.vue'),
        meta: { permission: 'userCoupon' }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('../views/system/index.vue'),
        meta: { permission: 'system' }
      },
      {
        path: 'service',
        name: 'Service',
        component: () => import('../views/service/index.vue'),
        meta: { permission: 'service' }
      },
      {
        path: 'service/knowledge',
        name: 'Knowledge',
        component: () => import('../views/service/knowledge.vue'),
        meta: { permission: 'knowledge' }
      },
      {
        path: 'admin',
        name: 'AdminManage',
        component: () => import('../views/admin/index.vue'),
        meta: { permission: 'admin' }
      }
    ]
  }
]

function getFirstAllowedPath(permissions, roleKey) {
  if (roleKey === 'super_admin') return '/dashboard'
  for (const route of allMenuRoutes) {
    if (permissions.includes(route.permission)) {
      return '/' + route.path
    }
  }
  return '/dashboard'
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
    return
  }

  const adminToken = localStorage.getItem('adminToken')
  if (!adminToken) {
    next('/login')
    return
  }

  const adminInfoStr = localStorage.getItem('adminInfo')
  let adminInfo = null
  try {
    adminInfo = adminInfoStr ? JSON.parse(adminInfoStr) : null
  } catch (e) {
    // ignore
  }

  if (!adminInfo || !adminInfo.permissions) {
    next()
    return
  }

  const permissions = adminInfo.permissions || []
  const roleKey = adminInfo.roleKey || ''

  if (roleKey === 'super_admin') {
    next()
    return
  }

  if (to.path === '/') {
    const firstPath = getFirstAllowedPath(permissions, roleKey)
    next(firstPath)
    return
  }

  if (to.meta.permission && !permissions.includes(to.meta.permission)) {
    const firstPath = getFirstAllowedPath(permissions, roleKey)
    next(firstPath)
    return
  }

  next()
})

export default router
