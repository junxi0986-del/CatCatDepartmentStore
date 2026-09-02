<template>
  <div class="admin-manage">
    <div class="page-header">
      <h2>管理员管理</h2>
      <a-button type="primary" @click="showAddModal">新增管理员</a-button>
    </div>

    <div class="filter-bar">
      <a-input v-model:value="searchUsername" placeholder="搜索用户名" style="width: 200px" allow-clear @pressEnter="loadAdmins" />
      <a-select v-model:value="searchRoleKey" placeholder="筛选角色" style="width: 160px" allow-clear @change="loadAdmins">
        <a-select-option v-for="role in roles" :key="role.roleKey" :value="role.roleKey">{{ role.roleName }}</a-select-option>
      </a-select>
      <a-button @click="loadAdmins">查询</a-button>
    </div>

    <a-table :dataSource="admins" :columns="columns" :pagination="false" rowKey="id" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'roleKey'">
          <a-tag :color="getRoleColor(record.roleKey)">{{ getRoleName(record.roleKey) }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="showEditModal(record)">编辑</a-button>
            <a-button size="small" @click="showResetPasswordModal(record)">重置密码</a-button>
            <a-button size="small" :type="record.status === 1 ? 'default' : 'primary'" @click="toggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-popconfirm title="确定删除该管理员？" @confirm="handleDelete(record.id)" v-if="record.roleKey !== 'super_admin'">
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="addModalVisible" title="新增管理员" @ok="handleAdd" :confirmLoading="submitting">
      <a-form :labelCol="{ span: 6 }" :wrapperCol="{ span: 16 }">
        <a-form-item label="用户名" required>
          <a-input v-model:value="addForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password v-model:value="addForm.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="addForm.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="角色" required>
          <a-select v-model:value="addForm.roleKey" placeholder="请选择角色">
            <a-select-option v-for="role in roles" :key="role.roleKey" :value="role.roleKey">{{ role.roleName }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="editModalVisible" title="编辑管理员" @ok="handleEdit" :confirmLoading="submitting">
      <a-form :labelCol="{ span: 6 }" :wrapperCol="{ span: 16 }">
        <a-form-item label="用户名">
          <a-input v-model:value="editForm.username" />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="editForm.nickname" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="editForm.roleKey" placeholder="请选择角色">
            <a-select-option v-for="role in roles" :key="role.roleKey" :value="role.roleKey">{{ role.roleName }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="resetPwdModalVisible" title="重置密码" @ok="handleResetPassword" :confirmLoading="submitting">
      <a-form :labelCol="{ span: 6 }" :wrapperCol="{ span: 16 }">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="resetPwdForm.password" placeholder="请输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <div class="section-title" style="margin-top: 30px">
      <h3>角色权限配置</h3>
    </div>

    <div class="role-perm-section">
      <a-card v-for="role in roles" :key="role.roleKey" :title="role.roleName" style="margin-bottom: 16px" :hoverable="true">
        <p style="color: #999; margin-bottom: 12px">{{ role.description }}</p>
        <template v-if="role.roleKey === 'super_admin'">
          <a-tag color="blue" v-for="perm in allPermissions" :key="perm.permissionKey" style="margin-bottom: 4px">{{ perm.permissionName }}</a-tag>
          <p style="color: #999; margin-top: 8px; font-size: 12px">超级管理员拥有所有权限，不可修改</p>
        </template>
        <template v-else>
          <a-checkbox-group v-model:value="rolePermMap[role.roleKey]" @change="(vals) => handlePermChange(role.roleKey, vals)">
            <a-row>
              <a-col :span="8" v-for="perm in allPermissions" :key="perm.permissionKey" style="margin-bottom: 8px">
                <a-checkbox :value="perm.permissionKey">{{ perm.permissionName }}</a-checkbox>
              </a-col>
            </a-row>
          </a-checkbox-group>
          <div style="margin-top: 12px; text-align: right">
            <a-button type="primary" size="small" @click="saveRolePermissions(role.roleKey)">保存权限</a-button>
          </div>
        </template>
      </a-card>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getAdminList, addAdmin, updateAdmin, deleteAdmin,
  updateAdminStatus, resetAdminPassword,
  getRoleList, getPermissionList, getRolePermissions, updateRolePermissions
} from '../../api/admin'

export default {
  setup() {
    const admins = ref([])
    const roles = ref([])
    const allPermissions = ref([])
    const rolePermMap = reactive({})
    const loading = ref(false)
    const submitting = ref(false)
    const searchUsername = ref('')
    const searchRoleKey = ref(undefined)

    const addModalVisible = ref(false)
    const editModalVisible = ref(false)
    const resetPwdModalVisible = ref(false)

    const addForm = reactive({ username: '', password: '', nickname: '', roleKey: 'store_admin' })
    const editForm = reactive({ id: null, username: '', nickname: '', roleKey: '' })
    const resetPwdForm = reactive({ id: null, password: '' })

    const columns = [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
      { title: '用户名', dataIndex: 'username', key: 'username' },
      { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
      { title: '角色', dataIndex: 'roleKey', key: 'roleKey' },
      { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
      { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
      { title: '操作', key: 'action', width: 320 }
    ]

    const getRoleName = (roleKey) => {
      const role = roles.value.find(r => r.roleKey === roleKey)
      return role ? role.roleName : roleKey
    }

    const getRoleColor = (roleKey) => {
      const map = { super_admin: 'red', service_admin: 'blue', store_admin: 'green' }
      return map[roleKey] || 'default'
    }

    const loadAdmins = async () => {
      loading.value = true
      try {
        const res = await getAdminList({ username: searchUsername.value, roleKey: searchRoleKey.value })
        if (res.code === 200) admins.value = res.data
      } finally { loading.value = false }
    }

    const loadRoles = async () => {
      const res = await getRoleList()
      if (res.code === 200) roles.value = res.data
    }

    const loadPermissions = async () => {
      const res = await getPermissionList()
      if (res.code === 200) allPermissions.value = res.data
    }

    const loadRolePermissions = async () => {
      for (const role of roles.value) {
        if (role.roleKey === 'super_admin') continue
        const res = await getRolePermissions(role.roleKey)
        if (res.code === 200) {
          rolePermMap[role.roleKey] = res.data || []
        }
      }
    }

    const showAddModal = () => {
      Object.assign(addForm, { username: '', password: '', nickname: '', roleKey: 'store_admin' })
      addModalVisible.value = true
    }

    const showEditModal = (record) => {
      Object.assign(editForm, { id: record.id, username: record.username, nickname: record.nickname, roleKey: record.roleKey })
      editModalVisible.value = true
    }

    const showResetPasswordModal = (record) => {
      resetPwdForm.id = record.id
      resetPwdForm.password = ''
      resetPwdModalVisible.value = true
    }

    const handleAdd = async () => {
      if (!addForm.username || !addForm.password) { message.warning('用户名和密码不能为空'); return }
      submitting.value = true
      try {
        const res = await addAdmin(addForm)
        if (res.code === 200) { message.success('添加成功'); addModalVisible.value = false; loadAdmins() }
        else message.error(res.message)
      } finally { submitting.value = false }
    }

    const handleEdit = async () => {
      submitting.value = true
      try {
        const res = await updateAdmin(editForm)
        if (res.code === 200) { message.success('编辑成功'); editModalVisible.value = false; loadAdmins() }
        else message.error(res.message)
      } finally { submitting.value = false }
    }

    const handleDelete = async (id) => {
      const res = await deleteAdmin(id)
      if (res.code === 200) { message.success('删除成功'); loadAdmins() }
      else message.error(res.message)
    }

    const toggleStatus = async (record) => {
      const newStatus = record.status === 1 ? 0 : 1
      const res = await updateAdminStatus({ id: record.id, status: newStatus })
      if (res.code === 200) { message.success('操作成功'); loadAdmins() }
      else message.error(res.message)
    }

    const handleResetPassword = async () => {
      if (!resetPwdForm.password) { message.warning('请输入新密码'); return }
      submitting.value = true
      try {
        const res = await resetAdminPassword({ id: resetPwdForm.id, password: resetPwdForm.password })
        if (res.code === 200) { message.success('密码重置成功'); resetPwdModalVisible.value = false }
        else message.error(res.message)
      } finally { submitting.value = false }
    }

    const handlePermChange = (roleKey, vals) => {
      rolePermMap[roleKey] = vals
    }

    const saveRolePermissions = async (roleKey) => {
      const res = await updateRolePermissions({ roleKey, permissionKeys: rolePermMap[roleKey] || [] })
      if (res.code === 200) message.success('权限保存成功')
      else message.error(res.message)
    }

    onMounted(async () => {
      await loadRoles()
      await loadPermissions()
      await loadRolePermissions()
      loadAdmins()
    })

    return {
      admins, roles, allPermissions, rolePermMap, loading, submitting,
      searchUsername, searchRoleKey, columns,
      addModalVisible, editModalVisible, resetPwdModalVisible,
      addForm, editForm, resetPwdForm,
      getRoleName, getRoleColor, loadAdmins,
      showAddModal, showEditModal, showResetPasswordModal,
      handleAdd, handleEdit, handleDelete, toggleStatus, handleResetPassword,
      handlePermChange, saveRolePermissions
    }
  }
}
</script>

<style scoped>
.admin-manage { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.section-title h3 { margin: 0 0 16px; border-bottom: 1px solid #eee; padding-bottom: 8px; }
</style>
