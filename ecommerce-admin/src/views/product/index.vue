<template>
  <div class="product-management">
    <h2>商品管理</h2>
    
    <div class="search-area">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="商品名称">
            <a-input placeholder="请输入商品名称" v-model:value="searchForm.name" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="分类">
            <a-select placeholder="请选择分类" v-model:value="searchForm.categoryId" allowClear style="width: 100%">
              <a-select-option :value="1">数码产品</a-select-option>
              <a-select-option :value="2">家居用品</a-select-option>
              <a-select-option :value="3">服装鞋帽</a-select-option>
              <a-select-option :value="4">手机配件</a-select-option>
              <a-select-option :value="5">厨房用具</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="最低价格">
            <a-input-number placeholder="最低价格" v-model:value="searchForm.minPrice" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="最高价格">
            <a-input-number placeholder="最高价格" v-model:value="searchForm.maxPrice" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="最低库存">
            <a-input-number placeholder="最低库存" v-model:value="searchForm.minStock" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="最高库存">
            <a-input-number placeholder="最高库存" v-model:value="searchForm.maxStock" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="状态">
            <a-select placeholder="请选择状态" v-model:value="searchForm.status" allowClear style="width: 100%">
              <a-select-option :value="1">上架</a-select-option>
              <a-select-option :value="0">下架</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="search">搜索</a-button>
              <a-button @click="resetSearch">重置</a-button>
              <a-button type="primary" @click="addProduct">添加商品</a-button>
            </a-space>
          </a-form-item>
        </a-col>
      </a-row>
    </div>
    
    <div style="margin-bottom: 20px;">
      <span v-if="selectedRowKeys.length > 0" style="margin-right: 10px;">已选 {{ selectedRowKeys.length }} 项</span>
      <a-button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchShelve" style="margin-right: 10px;">批量上架</a-button>
      <a-button :disabled="selectedRowKeys.length === 0" @click="handleBatchUnshelve">批量下架</a-button>
    </div>
    
    <a-table :columns="columns" :data-source="products" row-key="id" style="width: 100%" :row-selection="rowSelection">
      <template #status="{ record }">
        <a-select :value="Number(record.status)" @change="updateProductStatus(record.id, $event)" style="width: 80px">
          <a-select-option :value="1">上架</a-select-option>
          <a-select-option :value="0">下架</a-select-option>
        </a-select>
      </template>
      <template #category="{ record }">
        {{ categoryMap[record.categoryId] || record.categoryId }}
      </template>
      <template #pic="{ record }">
        <img v-if="record.pic" :src="formatImageUrl(record.pic)" style="width: 50px; height: 50px; object-fit: cover" />
        <span v-else>无图片</span>
      </template>
      <template #action="{ record }">
        <a-button size="small" @click="viewProduct(record.id)">查看</a-button>
        <a-button size="small" type="primary" @click="editProduct(record)">编辑</a-button>
        <a-button size="small" type="danger" @click="deleteProduct(record.id)">删除</a-button>
      </template>
    </a-table>

    <!-- 添加/编辑商品模态框 -->
    <a-modal
      v-model:open="isModalVisible"
      :title="modalTitle"
      width="600px"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="商品名称">
          <div style="display: flex; gap: 10px; align-items: center;">
            <a-input v-model:value="formState.name" placeholder="请输入商品名称" style="flex: 1;" />
            <a-button type="primary" @click="handleGenerateDetail" :loading="generatingDetail" :disabled="!formState.name">
              <template v-if="generatingDetail">AI生成中...</template>
              <template v-else>AI生成详情</template>
            </a-button>
          </div>
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="formState.categoryId">
            <a-select-option v-for="category in categories" :key="category.value" :value="category.value">
              {{ category.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="价格">
          <a-input v-model:value="formState.price" type="number" placeholder="请输入价格" />
        </a-form-item>
        <a-form-item label="市场价">
          <a-input v-model:value="formState.marketPrice" type="number" placeholder="请输入市场价" />
        </a-form-item>
        <a-form-item label="库存">
          <a-input v-model:value="formState.stock" type="number" placeholder="请输入库存" />
        </a-form-item>
        <a-form-item label="销量">
          <a-input v-model:value="formState.sales" type="number" placeholder="请输入销量" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status">
            <a-select-option value="1">上架</a-select-option>
            <a-select-option value="0">下架</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="图片">
          <div class="upload-section">
            <div class="upload-item">
              <span class="upload-label">商品主图：</span>
              <a-upload
                action="/api/admin/product/upload"
                :on-success="handleMainPicSuccess"
                :on-error="handleUploadError"
                :show-upload-list="false"
              >
                <a-button type="primary">
                  <upload-outlined /> 上传主图
                </a-button>
              </a-upload>
              <div v-if="formState.pic" class="preview-wrapper">
                <img :src="formatImageUrl(formState.pic)" class="preview-img" />
                <span class="delete-btn" @click="removeMainPic">×</span>
              </div>
            </div>
            <div class="upload-item">
              <span class="upload-label">详情图集：</span>
              <a-upload
                action="/api/admin/product/uploadSingle"
                :on-success="handleImagesSuccess"
                :on-error="handleUploadError"
                :show-upload-list="false"
                multiple
                name="file"
                :disabled="detailImagesCount >= 6"
              >
                <a-button type="primary" :disabled="detailImagesCount >= 6">
                  <upload-outlined /> 上传详情图 {{ detailImagesCount >= 6 ? '(已达上限)' : `(${detailImagesCount}/6)` }}
                </a-button>
              </a-upload>
              <div class="images-preview" v-if="formState.images">
                <div v-for="(img, idx) in formState.images.split(',')" :key="idx" class="preview-item">
                  <img :src="formatImageUrl(img)" class="preview-img-small" />
                  <span class="delete-btn-small" @click="removeDetailImage(idx)">×</span>
                </div>
              </div>
              <div v-if="detailImagesCount >= 6" style="color: #ff4d4f; font-size: 12px; margin-top: 8px;">
                已达到最大上传数量（6张）
              </div>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="商品详情">
          <div style="width: 100%; min-height: 300px; border: 1px solid #d9d9d9; border-radius: 4px; padding: 10px;">
            <textarea v-model="formState.detail" placeholder="请输入商品详情（支持HTML富文本）" style="width: 100%; height: 300px; border: none; resize: none; outline: none;"></textarea>
          </div>
        </a-form-item>
        <a-form-item label="商品规格">
          <div class="spec-section">
            <div class="spec-header">
              <a-button type="primary" size="small" @click="handleAnalyzeSpecs" :loading="analyzingSpecs" :disabled="!formState.name">
                <template v-if="analyzingSpecs">AI分析中...</template>
                <template v-else>AI智能分析规格</template>
              </a-button>
              <a-button type="default" size="small" @click="addSpecGroup" style="margin-left: 10px">
                添加规格组
              </a-button>
            </div>
            <div v-for="(group, gIndex) in specGroups" :key="'group-' + gIndex" class="spec-group">
              <div class="spec-group-header">
                <a-input v-model:value="group.specName" placeholder="规格名称（如：颜色、型号）" style="width: 150px" />
                <a-button type="text" danger size="small" @click="removeSpecGroup(gIndex)">删除</a-button>
              </div>
              <div class="spec-options">
                <div v-for="(option, oIndex) in group.options" :key="'option-' + gIndex + '-' + oIndex" class="spec-option-row">
                  <div class="spec-field">
                    <span class="spec-field-label">规格值</span>
                    <a-input v-model:value="option.value" placeholder="如：红色" style="width: 100px" />
                  </div>
                  <div class="spec-field">
                    <span class="spec-field-label">加价</span>
                    <a-input-number v-model:value="option.price" placeholder="0" style="width: 80px" :min="0" />
                  </div>
                  <div class="spec-field">
                    <span class="spec-field-label">库存</span>
                    <a-input-number v-model:value="option.stock" placeholder="0" style="width: 80px" :min="0" />
                  </div>
                  <a-button type="text" danger size="small" @click="removeSpecOption(gIndex, oIndex)">删除</a-button>
                </div>
                <a-button type="dashed" size="small" @click="addSpecOption(gIndex)" style="margin-top: 5px">
                  + 添加选项
                </a-button>
              </div>
            </div>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看商品详情模态框 -->
    <a-modal
      v-model:open="isViewModalVisible"
      title="商品详情"
      @cancel="handleViewCancel"
    >
      <div v-if="viewProductData">
        <p><strong>商品ID:</strong> {{ viewProductData.id }}</p>
        <p><strong>商品名称:</strong> {{ viewProductData.name }}</p>
        <p><strong>分类:</strong> {{ categoryMap[viewProductData.categoryId] || viewProductData.categoryId }}</p>
        <p><strong>价格:</strong> {{ viewProductData.price }}</p>
        <p><strong>市场价:</strong> {{ viewProductData.marketPrice }}</p>
        <p><strong>库存:</strong> {{ viewProductData.stock }}</p>
        <p><strong>销量:</strong> {{ viewProductData.sales }}</p>
        <p><strong>状态:</strong> {{ viewProductData.status === 1 ? '上架' : '下架' }}</p>
        <p><strong>商品主图:</strong></p>
        <img v-if="viewProductData.pic" :src="formatImageUrl(viewProductData.pic)" style="width: 200px; height: 200px; object-fit: cover" />
        <span v-else>无图片</span>
        <p v-if="viewProductData.images"><strong>详情图集:</strong></p>
        <div v-if="viewProductData.images" style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px;">
          <img v-for="(img, idx) in viewProductData.images.split(',')" :key="idx" :src="formatImageUrl(img)" style="width: 80px; height: 80px; object-fit: cover; border: 1px solid #d9d9d9; border-radius: 4px" />
        </div>
        <p v-if="viewProductData.detail"><strong>商品详情:</strong></p>
        <div v-if="viewProductData.detail" style="margin-top: 10px; padding: 10px; border: 1px solid #d9d9d9; border-radius: 4px; min-height: 100px; white-space: pre-wrap;">
          {{ viewProductData.detail }}
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, computed, nextTick } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { getProductList, updateProduct, updateProductStatus as updateProductStatusApi, deleteProduct as deleteProductApi, addProduct as addProductApi, batchUpdateProductStatus } from '../../api/product'
import { getSpecList, addSpec, updateSpec, deleteSpec, deleteAllSpecs, analyzeProductSpecs, generateProductDetail } from '../../api/spec'

export default {
  components: {
    UploadOutlined
  },
  setup() {
    const products = ref([])
    const searchKeyword = ref('')
    const searchForm = ref({
      name: '',
      categoryId: null,
      minPrice: null,
      maxPrice: null,
      minStock: null,
      maxStock: null,
      status: null
    })
    const isModalVisible = ref(false)
    const modalTitle = ref('')
    const categoryMap = {
      1: '数码产品',
      2: '家居用品',
      3: '服装鞋帽',
      4: '手机配件',
      5: '厨房用具'
    }

    const categories = [
      { value: 1, label: '数码产品' },
      { value: 2, label: '家居用品' },
      { value: 3, label: '服装鞋帽' },
      { value: 4, label: '手机配件' },
      { value: 5, label: '厨房用具' }
    ]

    const formState = ref({
      id: null,
      name: '',
      categoryId: 1,
      price: 0,
      marketPrice: 0,
      stock: 0,
      sales: 0,
      status: 1,
      pic: '',
      images: '',
      detail: ''
    })

    const specGroups = ref([])
    const analyzingSpecs = ref(false)
    const generatingDetail = ref(false)

    const isViewModalVisible = ref(false)
    const viewProductData = ref(null)

    const selectedRowKeys = ref([])
    const rowSelection = computed(() => ({
      selectedRowKeys: selectedRowKeys.value,
      onChange: (keys) => {
        selectedRowKeys.value = keys
      }
    }))

    const detailImagesCount = computed(() => {
      if (!formState.value.images || formState.value.images.trim() === '') {
        return 0
      }
      return formState.value.images.split(',').filter(img => img.trim() !== '').length
    })

    const columns = [
      {
        title: '商品ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
      },
      {
        title: '商品名称',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: '分类',
        dataIndex: 'categoryId',
        key: 'categoryId',
        slots: { customRender: 'category' }
      },
      {
        title: '价格',
        dataIndex: 'price',
        key: 'price'
      },
      {
        title: '库存',
        dataIndex: 'stock',
        key: 'stock'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        slots: { customRender: 'status' }
      },
      {
        title: '图片',
        dataIndex: 'pic',
        key: 'pic',
        width: 100,
        slots: { customRender: 'pic' }
      },
      {
        title: '操作',
        key: 'action',
        width: 250,
        slots: { customRender: 'action' }
      }
    ]

    const loadProducts = async () => {
      try {
        const params = {}
        if (searchForm.value.name) params.name = searchForm.value.name
        if (searchForm.value.categoryId !== null) params.categoryId = searchForm.value.categoryId
        if (searchForm.value.minPrice !== null) params.minPrice = searchForm.value.minPrice
        if (searchForm.value.maxPrice !== null) params.maxPrice = searchForm.value.maxPrice
        if (searchForm.value.minStock !== null) params.minStock = searchForm.value.minStock
        if (searchForm.value.maxStock !== null) params.maxStock = searchForm.value.maxStock
        if (searchForm.value.status !== null) params.status = searchForm.value.status
        
        const res = await getProductList(params)
        products.value = res.data.list || res.data || []
      } catch (error) {
        console.error('获取商品列表失败', error)
      }
    }

    const search = () => {
      loadProducts()
    }
    
    const resetSearch = () => {
      searchForm.value = {
        name: '',
        categoryId: null,
        minPrice: null,
        maxPrice: null,
        minStock: null,
        maxStock: null,
        status: null
      }
      loadProducts()
    }

    const updateProductStatus = async (productId, status) => {
      try {
        await updateProductStatusApi({ id: productId, status: status })
        message.success(status === 1 ? '商品已上架' : '商品已下架')
        loadProducts()
      } catch (error) {
        console.error('更新商品状态失败', error)
        loadProducts() // 失败后重新加载
      }
    }

    const addProduct = () => {
      modalTitle.value = '添加商品'
      formState.value = {
        id: null,
        name: '',
        categoryId: '',
        price: '',
        marketPrice: '',
        stock: '',
        sales: '',
        status: 1,
        pic: '',
        images: '',
        detail: ''
      }
      specGroups.value = []
      isModalVisible.value = true
    }

    const viewProduct = (productId) => {
      // 查看商品详情
      const product = products.value.find(p => p.id === productId)
      if (product) {
        viewProductData.value = product
        isViewModalVisible.value = true
      }
    }

    const editProduct = async (record) => {
      modalTitle.value = '编辑商品'
      formState.value = { 
        ...record,
        pic: record.pic || '',
        images: record.images || ''
      }
      await loadProductSpecs(record.id)
      isModalVisible.value = true
    }

    const deleteProduct = async (productId) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除该商品吗？此操作不可撤销。',
        okText: '确认',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
          try {
            await deleteProductApi(productId)
            message.success('商品删除成功')
            loadProducts()
          } catch (error) {
            console.error('删除商品失败', error)
          }
        }
      })
    }

    const handleBatchShelve = () => {
      Modal.confirm({
        title: '批量上架',
        content: `确定要批量上架选中的 ${selectedRowKeys.value.length} 个商品吗？`,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          try {
            const res = await batchUpdateProductStatus({ ids: selectedRowKeys.value, status: 1 })
            message.success(res.data || '批量上架成功')
            selectedRowKeys.value = []
            loadProducts()
          } catch (error) {
            console.error('批量上架失败', error)
            message.error('批量上架失败')
          }
        }
      })
    }

    const handleBatchUnshelve = () => {
      Modal.confirm({
        title: '批量下架',
        content: `确定要批量下架选中的 ${selectedRowKeys.value.length} 个商品吗？`,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          try {
            const res = await batchUpdateProductStatus({ ids: selectedRowKeys.value, status: 0 })
            message.success(res.data || '批量下架成功')
            selectedRowKeys.value = []
            loadProducts()
          } catch (error) {
            console.error('批量下架失败', error)
            message.error('批量下架失败')
          }
        }
      })
    }

    const handleOk = async () => {
      try {
        if (formState.value.id) {
          await updateProduct(formState.value)
          await saveProductSpecs(formState.value.id)
          message.success('商品编辑成功')
        } else {
          const res = await addProductApi(formState.value)
          if (res.code === 200) {
            await saveProductSpecs(res.data)
          }
          message.success('商品添加成功')
        }
        isModalVisible.value = false
        loadProducts()
      } catch (error) {
        console.error('保存商品失败', error)
        message.error('保存商品失败，请稍后重试')
      }
    }

    const handleCancel = () => {
      isModalVisible.value = false
    }

    const handleViewCancel = () => {
      isViewModalVisible.value = false
    }

    const handleMainPicSuccess = (response, file, fileList) => {
      console.log('主图上传响应:', response)
      if (response && (response.code === 200 || response.code === '200')) {
        formState.value.pic = response.data
        message.success('主图上传成功')
      } else {
        console.error('主图上传失败:', response)
        message.error(response?.message || '主图上传失败')
      }
    }

    const handleImagesSuccess = (response, file, fileList) => {
      console.log('详情图上传响应:', response)
      if (response && (response.code === 200 || response.code === '200')) {
        if (formState.value.images) {
          formState.value.images += ',' + response.data
        } else {
          formState.value.images = response.data
        }
        message.success('详情图上传成功')
      } else {
        console.error('详情图上传失败:', response)
        message.error(response?.message || '详情图上传失败')
      }
    }

    const handleUploadError = (error) => {
      console.error('上传错误:', error)
      message.error('上传失败：' + (error?.message || '网络错误'))
    }

    const removeMainPic = () => {
      formState.value.pic = ''
      message.success('主图已删除')
    }

    const removeDetailImage = (index) => {
      if (!formState.value.images) return
      const imagesArray = formState.value.images.split(',')
      imagesArray.splice(index, 1)
      formState.value.images = imagesArray.join(',')
      message.success('图片已删除')
    }

    const addSpecGroup = () => {
      specGroups.value.push({
        specName: '',
        options: [{ value: '', price: 0, stock: 100 }]
      })
    }

    const removeSpecGroup = (gIndex) => {
      specGroups.value.splice(gIndex, 1)
    }

    const addSpecOption = (gIndex) => {
      specGroups.value[gIndex].options.push({ value: '', price: 0, stock: 100 })
    }

    const removeSpecOption = (gIndex, oIndex) => {
      specGroups.value[gIndex].options.splice(oIndex, 1)
      if (specGroups.value[gIndex].options.length === 0) {
        specGroups.value.splice(gIndex, 1)
      }
    }

    const loadProductSpecs = async (productId) => {
      try {
        console.log('加载商品规格, productId:', productId)
        const res = await getSpecList(productId)
        console.log('规格API响应:', res)
        if (res.code === 200) {
          if (!res.data || res.data.length === 0) {
            console.log('无规格数据')
            specGroups.value = []
            return
          }
          const groupMap = {}
          res.data.forEach(spec => {
            console.log('处理规格项:', spec)
            if (!groupMap[spec.specName]) {
              groupMap[spec.specName] = {
                specName: spec.specName,
                options: []
              }
            }
            groupMap[spec.specName].options.push({
              id: spec.id,
              value: spec.specValue,
              price: spec.price || 0,
              stock: spec.stock || 0
            })
          })
          const groups = Object.values(groupMap).map(g => ({
            specName: g.specName,
            options: g.options.map(o => ({ ...o }))
          }))
          specGroups.value = []
          await nextTick()
          specGroups.value = groups
          console.log('最终规格分组:', specGroups.value)
        }
      } catch (error) {
        console.error('加载商品规格失败', error)
        specGroups.value = []
      }
    }

    const saveProductSpecs = async (productId) => {
      try {
        await deleteAllSpecs(productId)
        for (const group of specGroups.value) {
          if (!group.specName) continue
          for (const option of group.options) {
            if (!option.value) continue
            await addSpec({
              productId: productId,
              specName: group.specName,
              specValue: option.value,
              price: option.price || 0,
              stock: option.stock || 0
            })
          }
        }
      } catch (error) {
        console.error('保存商品规格失败', error)
      }
    }

    const handleAnalyzeSpecs = async () => {
      if (!formState.value.name) {
        message.warning('请先输入商品名称')
        return
      }
      analyzingSpecs.value = true
      try {
        const res = await analyzeProductSpecs({
          name: formState.value.name,
          detail: formState.value.detail || ''
        })
        console.log('AI分析响应:', res)
        if (res.code === 200 && res.data && res.data.specs && res.data.specs.length > 0) {
          specGroups.value = res.data.specs.map(spec => ({
            specName: spec.specName,
            options: spec.options.map(opt => ({
              value: opt.value,
              price: opt.price || 0,
              stock: opt.stock || 100
            }))
          }))
          message.success('AI分析完成，请检查并调整规格')
        } else {
          message.info('AI未能分析出规格，请手动添加')
        }
      } catch (error) {
        console.error('AI分析失败', error)
        message.error('AI分析失败')
      } finally {
        analyzingSpecs.value = false
      }
    }

    const handleGenerateDetail = async () => {
      if (!formState.value.name) {
        message.warning('请先输入商品名称')
        return
      }
      generatingDetail.value = true
      try {
        const res = await generateProductDetail(formState.value.name)
        console.log('AI生成详情响应:', res)
        if (res.code === 200 && res.data) {
          if (res.data.detail) {
            formState.value.detail = res.data.detail
          }
          if (res.data.specs && res.data.specs.length > 0) {
            specGroups.value = res.data.specs.map(spec => ({
              specName: spec.specName,
              options: spec.options.map(opt => ({
                value: opt.value,
                price: opt.price || 0,
                stock: opt.stock || 100
              }))
            }))
          }
          message.success('AI生成完成，请检查并调整内容')
        } else {
          message.info('AI生成失败，请手动填写')
        }
      } catch (error) {
        console.error('AI生成详情失败', error)
        message.error('AI生成详情失败')
      } finally {
        generatingDetail.value = false
      }
    }

    const formatImageUrl = (pic) => {
      if (!pic) return ''
      console.log('formatImageUrl输入:', pic)
      // 处理 test.com 路径
      if (pic.includes('test.com')) {
        const fileName = pic.split('/').pop()
        return `/images/${fileName}`
      }
      // 处理完整 URL
      if (pic.startsWith('http')) {
        return pic
      }
      // 处理相对路径，通过 Vite 代理访问
      const result = `/images/${pic.replace('images/', '')}`
      console.log('formatImageUrl输出:', result)
      return result
    }

    onMounted(() => {
      loadProducts()
    })

    return {
      products,
      searchKeyword,
      searchForm,
      resetSearch,
      columns,
      loadProducts,
      search,
      addProduct,
      viewProduct,
      editProduct,
      deleteProduct,
      updateProductStatus,
      isModalVisible,
      modalTitle,
      formState,
      handleOk,
      handleCancel,
      handleViewCancel,
      handleMainPicSuccess,
      handleImagesSuccess,
      handleUploadError,
      removeMainPic,
      removeDetailImage,
      categoryMap,
      categories,
      isViewModalVisible,
      viewProductData,
      formatImageUrl,
      detailImagesCount,
      specGroups,
      analyzingSpecs,
      generatingDetail,
      addSpecGroup,
      removeSpecGroup,
      addSpecOption,
      removeSpecOption,
      handleAnalyzeSpecs,
      handleGenerateDetail,
      selectedRowKeys,
      rowSelection,
      handleBatchShelve,
      handleBatchUnshelve
    }
  }
}
</script>

<style scoped>
.product-management {
  padding: 20px;
}

.product-management h2 {
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

/* 操作按钮样式 */
.product-management .ant-table-cell button {
  margin-left: 3px;
  margin-right: 3px;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.upload-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-label {
  font-weight: 500;
  color: #333;
}

.preview-img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-top: 10px;
}

.images-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.preview-img-small {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.preview-wrapper {
  position: relative;
  display: inline-block;
  margin-top: 10px;
  width: 121px;
}

.preview-item {
  position: relative;
  display: inline-block;
  margin: 5px;
}

.delete-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #ff4d4f;
  color: white;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  line-height: 22px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 10;
}

.delete-btn:hover {
  background-color: #ff7875;
}

.delete-btn-small {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #ff4d4f;
  color: white;
  font-size: 14px;
  font-weight: bold;
  text-align: center;
  line-height: 18px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  z-index: 10;
}

.delete-btn-small:hover {
  background-color: #ff7875;
}

.spec-section {
  background: #fafafa;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.spec-header {
  margin-bottom: 15px;
}

.spec-group {
  background: #fff;
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.spec-group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.spec-options {
  padding-left: 10px;
}

.spec-option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.spec-field {
  display: flex;
  align-items: center;
  gap: 4px;
}

.spec-field-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}
</style>
