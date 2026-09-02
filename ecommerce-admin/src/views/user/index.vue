<template>
  <div class="user-management">
    <h2>用户管理</h2>
    
    <div class="search-area">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="用户名">
            <a-input placeholder="请输入用户名" v-model:value="searchForm.username" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="手机号">
            <a-input placeholder="请输入手机号" v-model:value="searchForm.phone" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="邮箱">
            <a-input placeholder="请输入邮箱" v-model:value="searchForm.email" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="状态">
            <a-select placeholder="请选择状态" v-model:value="searchForm.status" allowClear style="width: 100%">
              <a-select-option :value="1">正常</a-select-option>
              <a-select-option :value="0">禁用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="24" style="text-align: right;">
          <a-space>
            <a-button type="primary" @click="search">搜索</a-button>
            <a-button @click="resetSearch">重置</a-button>
            <a-button type="primary" @click="showAddModal">添加用户</a-button>
          </a-space>
        </a-col>
      </a-row>
    </div>
    
    <div style="margin-bottom: 20px;">
      <span v-if="selectedRowKeys.length > 0" style="margin-right: 10px;">已选 {{ selectedRowKeys.length }} 项</span>
      <a-button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchEnable" style="margin-right: 10px;">批量启用</a-button>
      <a-button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchDisable">批量禁用</a-button>
    </div>
    
    <a-table :columns="columns" :data-source="users" row-key="id" style="width: 100%" :row-selection="rowSelection">
      <template #avatar="{ record }">
        <a-avatar
          v-if="record.avatar && isValidAvatar(record.avatar)"
          :key="forceUpdateKey"
          :src="formatAvatarUrl(record.avatar)"
          :size="40"
        />
        <a-avatar v-else :key="forceUpdateKey" :size="40" style="background-color: #1890ff">
          {{ record.username ? record.username.charAt(0).toUpperCase() : 'U' }}
        </a-avatar>
      </template>
      <template #status="{ record }">
        <a-select :value="record.status" @change="updateUserStatus(record.id, $event)" style="width: 100px">
          <a-select-option :value="1" style="color: green">正常</a-select-option>
          <a-select-option :value="0" style="color: red">禁用</a-select-option>
        </a-select>
      </template>
      <template #action="{ record }">
        <a-button size="small" @click="viewUser(record.id)">查看</a-button>
        <a-button size="small" type="primary" @click="editUser(record.id)">编辑</a-button>
        <a-button size="small" type="danger" @click="deleteUser(record.id)">删除</a-button>
      </template>
    </a-table>

    <!-- 添加用户模态框 -->
    <a-modal v-model:visible="addModalVisible" title="添加用户" @ok="handleAdd">
      <a-form :model="userForm" :rules="rules" ref="userFormRef">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="userForm.username" placeholder="请输入用户名"></a-input>
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="userForm.password" placeholder="请输入密码"></a-input-password>
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="userForm.phone" placeholder="请输入手机号"></a-input>
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="userForm.email" placeholder="请输入邮箱"></a-input>
        </a-form-item>
        <a-form-item label="头像">
          <div class="avatar-upload">
            <input 
              type="file" 
              id="admin-avatar-input" 
              class="avatar-input"
              accept="image/jpeg,image/png"
              @change="handleAvatarChange"
            />
            <a-button type="primary" @click="triggerAvatarUpload">
              <upload-outlined /> 上传头像
            </a-button>
            <a-avatar v-if="userForm.avatar" :src="userForm.avatar" :size="100" />
            <a-avatar v-else :size="100" style="background-color: #1890ff; margin-top: 10px">
              {{ userForm.username ? userForm.username.charAt(0).toUpperCase() : 'U' }}
            </a-avatar>
            <a-button 
              v-if="userForm.avatar" 
              type="text" 
              danger 
              @click="clearAvatar"
              style="margin-top: 10px"
            >清空头像</a-button>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑用户模态框 -->
    <a-modal v-model:visible="editModalVisible" title="编辑用户" @ok="handleEdit">
      <a-form :model="userForm" :rules="rules" ref="userFormRef">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="userForm.username" placeholder="请输入用户名"></a-input>
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="userForm.password" placeholder="不修改密码请留空"></a-input-password>
          <span style="color: #999; font-size: 12px">不修改密码请留空</span>
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="userForm.phone" placeholder="请输入手机号"></a-input>
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="userForm.email" placeholder="请输入邮箱"></a-input>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看用户详情模态框 -->
    <a-modal v-model:visible="viewModalVisible" title="用户详情">
      <a-descriptions bordered>
        <a-descriptions-item label="用户ID">{{ viewUserData.id }}</a-descriptions-item>
        <a-descriptions-item label="用户名">{{ viewUserData.username }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ viewUserData.phone }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ viewUserData.email }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ viewUserData.status === 1 ? '正常' : '禁用' }}</a-descriptions-item>
        <a-descriptions-item label="注册时间">{{ viewUserData.createTime }}</a-descriptions-item>
        <a-descriptions-item label="头像">
          <a-avatar v-if="viewUserData.avatar && isValidAvatar(viewUserData.avatar)" :key="forceUpdateKey" :src="formatAvatarUrl(viewUserData.avatar)" :size="50" />
          <a-avatar v-else :key="forceUpdateKey" :size="50" style="background-color: #1890ff">
            {{ viewUserData.username ? viewUserData.username.charAt(0).toUpperCase() : 'U' }}
          </a-avatar>
        </a-descriptions-item>
      </a-descriptions>
      <a-divider>收货地址</a-divider>
      <a-list v-if="viewUserData.addresses && viewUserData.addresses.length > 0" :data-source="viewUserData.addresses" bordered>
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                {{ item.receiver }} - {{ item.phone }}
              </template>
              <template #description>
                {{ item.province }} {{ item.city }} {{ item.area }} {{ item.detail_address }}
                <a-tag v-if="item.is_default === 1" color="green" style="margin-left: 10px">默认</a-tag>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else description="暂无收货地址" />
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { getUserList, updateUserStatus as updateUserStatusApi, addUser, editUser as editUserApi, deleteUser as deleteUserApi, getUserById, batchUpdateUserStatus } from '../../api/user'

export default {
  components: {
    UploadOutlined
  },
  setup() {
    const users = ref([])
    const searchKeyword = ref('')
    const searchForm = ref({
      username: '',
      phone: '',
      email: '',
      status: null
    })
    const addModalVisible = ref(false)
    const editModalVisible = ref(false)
    const viewModalVisible = ref(false)
    const viewUserData = ref({
      id: '',
      username: '',
      password: '',
      phone: '',
      email: '',
      avatar: '',
      status: 1,
      addresses: []
    })
    const forceUpdateKey = ref(0)
    const userForm = ref({
      id: '',
      username: '',
      password: '',
      phone: '',
      email: '',
      avatar: '',
      status: 1
    })
    const userFormRef = ref(null)

    const selectedRowKeys = ref([])
    const rowSelection = computed(() => ({
      selectedRowKeys: selectedRowKeys.value,
      onChange: (keys) => {
        selectedRowKeys.value = keys
      }
    }))

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: false, message: '请输入密码', trigger: 'blur' }
      ],
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ]
    }

    const isValidAvatar = (url) => {
      if (!url) return false
      if (url.includes('test.com')) return false
      return true
    }

    const formatAvatarUrl = (avatar) => {
      if (!avatar) return ''
      if (avatar.startsWith('data:')) {
        return avatar
      }
      if (avatar.startsWith('http')) {
        return avatar
      }
      return `/api/${avatar}`
    }

    const columns = [
      {
        title: '头像',
        dataIndex: 'avatar',
        key: 'avatar',
        width: 80,
        slots: { customRender: 'avatar' }
      },
      {
        title: '用户ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
      },
      {
        title: '用户名',
        dataIndex: 'username',
        key: 'username'
      },
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        slots: { customRender: 'status' }
      },
      {
        title: '注册时间',
        dataIndex: 'createTime',
        key: 'createTime'
      },
      {
        title: '操作',
        key: 'action',
        width: 250,
        slots: { customRender: 'action' }
      }
    ]

    const loadUsers = async () => {
      try {
        const params = {}
        if (searchForm.value.username) params.username = searchForm.value.username
        if (searchForm.value.phone) params.phone = searchForm.value.phone
        if (searchForm.value.email) params.email = searchForm.value.email
        if (searchForm.value.status !== null) params.status = searchForm.value.status
        
        const res = await getUserList(params)
        const userList = res.data
        
        users.value = userList
        forceUpdateKey.value++
        
        console.log('用户列表加载完成，共', userList.length, '个用户')
        if (userList.length > 0 && userList[0].avatar) {
          console.log('第一个用户头像:', userList[0].avatar?.substring(0, 50) + '...')
        }
      } catch (error) {
        console.error('获取用户列表失败', error)
        message.error('获取用户列表失败')
      }
    }

    const search = () => {
      loadUsers()
    }
    
    const resetSearch = () => {
      searchForm.value = {
        username: '',
        phone: '',
        email: '',
        status: null
      }
      loadUsers()
    }

    const updateUserStatus = async (userId, status) => {
      try {
        await updateUserStatusApi({ userId, status: status ? 1 : 0 })
        message.success('更新用户状态成功')
        loadUsers()
      } catch (error) {
        console.error('更新用户状态失败', error)
        message.error('更新用户状态失败')
        loadUsers()
      }
    }

    const showAddModal = () => {
      userForm.value = {
        id: '',
        username: '',
        password: '',
        phone: '',
        email: '',
        avatar: '',
        status: 1
      }
      addModalVisible.value = true
    }

    const handleAdd = async () => {
      try {
        await addUser(userForm.value)
        if (userForm.value.avatar) {
          avatarManager.setAvatar(userForm.value.id, userForm.value.avatar)
        }
        message.success('添加用户成功')
        addModalVisible.value = false
        loadUsers()
      } catch (error) {
        console.error('添加用户失败', error)
        message.error('添加用户失败')
      }
    }

    const triggerAvatarUpload = () => {
      document.getElementById('admin-avatar-input')?.click()
    }

    const handleAvatarChange = (event) => {
      const file = event.target.files[0]
      if (!file) return

      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJpgOrPng) {
        message.error('只能上传JPG或PNG图片！')
        return
      }
      if (!isLt2M) {
        message.error('图片大小不能超过2MB！')
        return
      }

      const reader = new FileReader()
      reader.onload = (e) => {
        userForm.value.avatar = e.target.result
      }
      reader.readAsDataURL(file)
    }

    const clearAvatar = () => {
      userForm.value.avatar = ''
    }

    const viewUser = async (userId) => {
      try {
        const res = await getUserById(userId)
        const userData = res.data.user || res.data
        
        viewUserData.value = {
          ...userData,
          addresses: res.data.addresses || []
        }
        forceUpdateKey.value++
        viewModalVisible.value = true
        
        console.log('查看用户详情，用户头像:', userData.avatar?.substring(0, 50) + '...')
      } catch (error) {
        console.error('获取用户详情失败', error)
        message.error('获取用户详情失败')
      }
    }

    const editUser = async (userId) => {
      try {
        const res = await getUserById(userId)
        const userData = res.data.user || res.data
        
        userForm.value = {
          ...userData,
          password: ''
        }
        editModalVisible.value = true
      } catch (error) {
        console.error('获取用户详情失败', error)
        message.error('获取用户详情失败')
      }
    }

    const handleEdit = async () => {
      try {
        await editUserApi(userForm.value)
        message.success('编辑用户成功')
        editModalVisible.value = false
        forceUpdateKey.value++
        loadUsers()
      } catch (error) {
        console.error('编辑用户失败', error)
        message.error('编辑用户失败')
      }
    }

    const deleteUser = async (userId) => {
      if (confirm('确定要删除这个用户吗？')) {
        try {
          await deleteUserApi(userId)
          message.success('删除用户成功')
          loadUsers()
        } catch (error) {
          console.error('删除用户失败', error)
          message.error('删除用户失败')
        }
      }
    }

    const handleBatchEnable = () => {
      Modal.confirm({
        title: '批量启用',
        content: `确定要批量启用选中的 ${selectedRowKeys.value.length} 个用户吗？`,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          try {
            const res = await batchUpdateUserStatus({ ids: selectedRowKeys.value, status: 1 })
            message.success(res.data || '批量启用成功')
            selectedRowKeys.value = []
            loadUsers()
          } catch (error) {
            console.error('批量启用失败', error)
            message.error('批量启用失败')
          }
        }
      })
    }

    const handleBatchDisable = () => {
      Modal.confirm({
        title: '批量禁用',
        content: `确定要批量禁用选中的 ${selectedRowKeys.value.length} 个用户吗？`,
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            const res = await batchUpdateUserStatus({ ids: selectedRowKeys.value, status: 0 })
            message.success(res.data || '批量禁用成功')
            selectedRowKeys.value = []
            loadUsers()
          } catch (error) {
            console.error('批量禁用失败', error)
            message.error('批量禁用失败')
          }
        }
      })
    }

    onMounted(() => {
      loadUsers()
    })

    return {
      users,
      searchKeyword,
      searchForm,
      resetSearch,
      addModalVisible,
      editModalVisible,
      viewModalVisible,
      viewUserData,
      userForm,
      userFormRef,
      rules,
      columns,
      search,
      updateUserStatus,
      showAddModal,
      handleAdd,
      viewUser,
      editUser,
      handleEdit,
      deleteUser,
      isValidAvatar,
      formatAvatarUrl,
      triggerAvatarUpload,
      handleAvatarChange,
      clearAvatar,
      forceUpdateKey,
      selectedRowKeys,
      rowSelection,
      handleBatchEnable,
      handleBatchDisable
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.user-management h2 {
  margin-bottom: 20px;
}

.search-area {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-area .ant-form-item {
  margin-bottom: 8px;
}

.search-area .ant-form-item-label {
  padding-bottom: 4px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.avatar-input {
  display: none;
}

:deep(.user-management > .ant-btn:nth-child(2)) {
  margin-top: 10px !important;
  margin-bottom: 10px !important;
}

:deep(.user-management > .ant-btn:nth-child(4)) {
  margin-left: 3px !important;
  margin-right: 3px !important;
}

:deep(.ant-table-cell .ant-btn) {
  margin-left: 3px !important;
  margin-right: 3px !important;
}

:deep(.ant-btn-danger) {
  background: #ff6b6b !important;
  border-color: #ff6b6b !important;
  color: #fff !important;
}

:deep(.ant-btn-danger:hover) {
  background: #ff5252 !important;
  border-color: #ff5252 !important;
  color: #fff !important;
}
</style>
