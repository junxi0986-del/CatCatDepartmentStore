<template>
  <div class="layout">
    <div class="sidebar">
      <div class="logo">
        <h2>管理后台</h2>
      </div>
      <nav>
        <ul>
          <li v-for="menu in visibleMenus" :key="menu.path">
            <router-link :to="menu.path" :class="{ active: $route.path === menu.path }">{{ menu.name }}</router-link>
          </li>
        </ul>
      </nav>
    </div>
    <div class="main">
      <div class="header">
        <div class="user-info">
          <a-tag v-if="roleName" :color="roleColor">{{ roleName }}</a-tag>
          <span>欢迎，{{ adminNickname }}</span>
          <button class="logout" @click="handleLogout">退出</button>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import { onMounted, ref, computed } from 'vue'
import { logout } from '../api/admin'

const allMenus = [
  { path: '/dashboard', name: '数据概览', permission: 'dashboard' },
  { path: '/user', name: '用户管理', permission: 'user' },
  { path: '/product', name: '商品管理', permission: 'product' },
  { path: '/order', name: '订单管理', permission: 'order' },
  { path: '/service', name: '客服中心', permission: 'service' },
  { path: '/service/knowledge', name: '知识库管理', permission: 'knowledge' },
  { path: '/coupon', name: '促销管理', permission: 'coupon' },
  { path: '/behavior', name: '用户行为', permission: 'behavior' },
  { path: '/userCoupon', name: '用户优惠券', permission: 'userCoupon' },
  { path: '/system', name: '系统配置', permission: 'system' },
  { path: '/admin', name: '管理员管理', permission: 'admin' }
]

const roleNameMap = {
  super_admin: { name: '超级管理员', color: 'red' },
  service_admin: { name: '客服管理员', color: 'blue' },
  store_admin: { name: '商城管理员', color: 'green' }
}

export default {
  setup() {
    const router = useRouter()
    const adminNickname = ref('管理员')
    const adminRoleKey = ref('')
    const adminPermissions = ref([])

    const roleName = computed(() => {
      const info = roleNameMap[adminRoleKey.value]
      return info ? info.name : ''
    })

    const roleColor = computed(() => {
      const info = roleNameMap[adminRoleKey.value]
      return info ? info.color : 'default'
    })

    const visibleMenus = computed(() => {
      if (adminRoleKey.value === 'super_admin') return allMenus
      return allMenus.filter(menu => adminPermissions.value.includes(menu.permission))
    })

    onMounted(() => {
      const adminInfoStr = localStorage.getItem('adminInfo')
      if (adminInfoStr) {
        try {
          const adminInfo = JSON.parse(adminInfoStr)
          if (adminInfo.nickname) adminNickname.value = adminInfo.nickname
          if (adminInfo.roleKey) adminRoleKey.value = adminInfo.roleKey
          if (adminInfo.permissions) adminPermissions.value = adminInfo.permissions
        } catch (error) {
          // ignore
        }
      }
    })

    const handleLogout = async () => {
      try { await logout() } catch (e) { /* ignore */ }
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminInfo')
      router.push('/login')
    }

    return {
      handleLogout,
      adminNickname,
      roleName,
      roleColor,
      visibleMenus
    }
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 200px;
  background: #333;
  color: #fff;
  padding: 20px 0;
  overflow-y: auto;
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.logo h2 {
  font-size: 18px;
}

nav ul {
  list-style: none;
}

nav ul li {
  margin: 10px 0;
}

nav ul li a {
  color: #fff;
  text-decoration: none;
  display: block;
  padding: 10px 20px;
  transition: background 0.3s;
}

nav ul li a:hover {
  background: #555;
}

nav ul li a.active {
  background: #c62523;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  background: #f0f0f0;
  padding: 10px 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logout {
  margin-left: 12px;
  padding: 5px 10px;
  border: none;
  background: #ff6b6b;
  color: #fff;
  border-radius: 4px;
  cursor: pointer;
}

.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>
