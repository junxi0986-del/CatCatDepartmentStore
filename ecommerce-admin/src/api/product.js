import request from '../utils/request'

export const getProductList = (params) => {
  return request({
    url: '/admin/product/list',
    method: 'get',
    params
  })
}

export const addProduct = (data) => {
  return request({
    url: '/admin/product/add',
    method: 'post',
    data
  })
}

export const updateProduct = (data) => {
  return request({
    url: '/admin/product/update',
    method: 'post',
    data
  })
}

export const updateProductStatus = (data) => {
  return request({
    url: '/admin/product/updateStatus',
    method: 'post',
    data
  })
}

export const deleteProduct = (productId) => {
  return request({
    url: `/admin/product/delete?productId=${productId}`,
    method: 'delete'
  })
}

export const getProductSpecs = (productId) => {
  return request({
    url: `/product/spec/list/${productId}`,
    method: 'get'
  })
}

export const batchUpdateProductStatus = (data) => {
  return request({
    url: '/admin/product/batchUpdateStatus',
    method: 'post',
    data
  })
}
