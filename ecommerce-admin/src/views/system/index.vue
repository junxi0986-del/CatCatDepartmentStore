<template>
  <div class="system-management">
    <h2>系统配置</h2>
    <a-form :model="systemConfig" layout="vertical">
      <a-form-item label="商城名称">
        <a-input v-model:value="systemConfig.mallName" placeholder="请输入商城名称" />
      </a-form-item>
      <a-form-item label="DeepSeek API Key">
        <a-input v-model:value="systemConfig.deepseekApiKey" placeholder="请输入DeepSeek API Key" />
      </a-form-item>
      <a-form-item label="订单自动取消时间">
        <a-input-number v-model:value="systemConfig.orderAutoCancel" placeholder="请输入订单自动取消时间（分钟）" />
      </a-form-item>
      <a-form-item label="商城描述">
        <a-textarea v-model:value="systemConfig.mallDescription" placeholder="请输入商城描述" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="saveConfig">保存配置</a-button>
      </a-form-item>
    </a-form>

    <a-divider />

    <h2>首页轮播图管理</h2>
    <div class="banner-management">
      <a-button type="primary" @click="addBanner" style="margin-bottom: 16px;">
        + 添加轮播图
      </a-button>
      
      <a-table :columns="bannerColumns" :data-source="banners" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'image'">
            <img :src="formatImageUrl(record.imageUrl)" style="width: 120px; height: 60px; object-fit: cover; border-radius: 4px;" />
          </template>
          <template v-else-if="column.key === 'linkUrl'">
            <a v-if="record.linkUrl" :href="record.linkUrl" target="_blank" style="color: #1890ff;">{{ record.linkUrl }}</a>
            <span v-else style="color: #999;">未设置</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="editBanner(record)">编辑</a-button>
            <a-button type="link" danger @click="confirmDeleteBanner(record)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="isBannerModalVisible"
      :title="bannerModalTitle"
      @ok="handleBannerOk"
      width="600px"
    >
      <a-form :model="bannerForm" layout="vertical">
        <a-form-item label="轮播图图片">
          <div class="upload-area">
            <img v-if="bannerForm.imageUrl" :src="formatImageUrl(bannerForm.imageUrl)" class="preview-image" />
            <div class="upload-actions">
              <a-input v-model:value="bannerForm.imageUrl" placeholder="请输入图片URL或上传" style="margin-bottom: 8px;" />
              <input type="file" ref="bannerFileInput" @change="handleBannerUpload" accept="image/*" style="display: none;" />
              <a-button @click="triggerBannerUpload">上传图片</a-button>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="跳转链接">
          <a-input v-model:value="bannerForm.linkUrl" placeholder="如: /product/1 或 https://..." />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="bannerForm.sortOrder" :min="0" style="width: 100%;" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="bannerForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getSystemConfig, updateSystemConfig } from '../../api/system'
import { message, Modal } from 'ant-design-vue'
import axios from 'axios'

export default {
  setup() {
    const systemConfig = ref({
      mallName: '',
      deepseekApiKey: '',
      orderAutoCancel: '30',
      mallDescription: ''
    })

    const banners = ref([])
    const isBannerModalVisible = ref(false)
    const bannerModalTitle = ref('添加轮播图')
    const bannerForm = ref({
      id: null,
      imageUrl: '',
      linkUrl: '',
      sortOrder: 0,
      status: 1
    })
    const bannerFileInput = ref(null)

    const bannerColumns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 60
      },
      {
        title: '图片',
        dataIndex: 'imageUrl',
        key: 'image',
        width: 150
      },
      {
        title: '跳转链接',
        dataIndex: 'linkUrl',
        key: 'linkUrl'
      },
      {
        title: '排序',
        dataIndex: 'sortOrder',
        key: 'sortOrder',
        width: 80
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 80
      },
      {
        title: '操作',
        key: 'action',
        width: 150
      }
    ]

    const formatImageUrl = (url) => {
      if (!url) return ''
      if (url.startsWith('http')) return url
      const baseUrl = '/api'
      if (url.startsWith('/')) return `${baseUrl}${url}`
      return `${baseUrl}/${url}`
    }

    const loadSystemConfig = async () => {
      try {
        const res = await getSystemConfig()
        systemConfig.value = res.data
      } catch (error) {
        console.error('获取系统配置失败', error)
        message.error('获取系统配置失败')
      }
    }

    const saveConfig = async () => {
      try {
        await updateSystemConfig(systemConfig.value)
        message.success('保存成功')
      } catch (error) {
        console.error('保存系统配置失败', error)
        message.error('保存失败，请稍后重试')
      }
    }

    const loadBanners = async () => {
      try {
        const res = await axios.get('/api/admin/banner/list')
        console.log('轮播图响应:', res.data)
        if (res.data.code === 200) {
          banners.value = res.data.data || []
        } else {
          banners.value = []
        }
      } catch (error) {
        console.error('获取轮播图列表失败', error)
        banners.value = []
      }
    }

    const addBanner = () => {
      bannerModalTitle.value = '添加轮播图'
      bannerForm.value = {
        id: null,
        imageUrl: '',
        linkUrl: '',
        sortOrder: 0,
        status: 1
      }
      isBannerModalVisible.value = true
    }

    const editBanner = (record) => {
      bannerModalTitle.value = '编辑轮播图'
      bannerForm.value = { ...record }
      isBannerModalVisible.value = true
    }

    const confirmDeleteBanner = (record) => {
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除这个轮播图吗？`,
        okText: '确定',
        cancelText: '取消',
        okType: 'danger',
        onOk: () => deleteBanner(record.id)
      })
    }

    const deleteBanner = async (id) => {
      try {
        const res = await axios.delete(`/api/admin/banner/delete?id=${id}`)
        if (res.data.code === 200) {
          message.success('删除成功')
          loadBanners()
        } else {
          message.error(res.data.msg || '删除失败')
        }
      } catch (error) {
        console.error('删除轮播图失败', error)
        message.error('删除失败')
      }
    }

    const handleBannerOk = async () => {
      try {
        let res
        if (bannerForm.value.id) {
          res = await axios.post('/api/admin/banner/update', bannerForm.value)
        } else {
          res = await axios.post('/api/admin/banner/add', bannerForm.value)
        }
        if (res.data.code === 200) {
          message.success('保存成功')
          isBannerModalVisible.value = false
          loadBanners()
        } else {
          message.error(res.data.msg || '保存失败')
        }
      } catch (error) {
        console.error('保存轮播图失败', error)
        message.error('保存失败')
      }
    }

    const triggerBannerUpload = () => {
      bannerFileInput.value?.click()
    }

    const handleBannerUpload = async (event) => {
      const file = event.target.files[0]
      if (!file) return
      
      const isValidImage = await new Promise((resolve) => {
        const img = new Image()
        const url = URL.createObjectURL(file)
        img.onload = () => {
          URL.revokeObjectURL(url)
          if (img.width < 1200 || img.height < 500) {
            message.error(`图片尺寸不足：当前 ${img.width}x${img.height}px，要求至少 1200x500px`)
            resolve(false)
          } else {
            resolve(true)
          }
        }
        img.onerror = () => {
          URL.revokeObjectURL(url)
          message.error('无法读取图片文件')
          resolve(false)
        }
        img.src = url
      })
      
      if (!isValidImage) return
      
      const formData = new FormData()
      formData.append('file', file)
      
      try {
        const res = await axios.post('/api/admin/banner/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.data.code === 200) {
          bannerForm.value.imageUrl = res.data.data
          message.success('上传成功')
        } else {
          message.error(res.data.msg || '上传失败')
        }
      } catch (error) {
        console.error('上传失败', error)
        message.error('上传失败')
      }
    }

    onMounted(() => {
      loadSystemConfig()
      loadBanners()
    })

    return {
      systemConfig,
      saveConfig,
      banners,
      bannerColumns,
      isBannerModalVisible,
      bannerModalTitle,
      bannerForm,
      bannerFileInput,
      formatImageUrl,
      addBanner,
      editBanner,
      confirmDeleteBanner,
      deleteBanner,
      handleBannerOk,
      triggerBannerUpload,
      handleBannerUpload
    }
  }
}
</script>

<style scoped>
.system-management {
  padding: 20px;
}

.system-management h2 {
  margin-bottom: 20px;
}

.a-form {
  max-width: 600px;
}

.banner-management {
  margin-top: 16px;
}

.upload-area {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.preview-image {
  width: 200px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
