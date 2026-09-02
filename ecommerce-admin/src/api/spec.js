import request from '../utils/request'

export const getSpecList = (productId) => {
  return request({
    url: `/product/spec/list/${productId}`,
    method: 'get'
  })
}

export const addSpec = (data) => {
  return request({
    url: '/product/spec/add',
    method: 'post',
    data
  })
}

export const updateSpec = (data) => {
  return request({
    url: '/product/spec/update',
    method: 'post',
    data
  })
}

export const deleteSpec = (id) => {
  return request({
    url: `/product/spec/delete/${id}`,
    method: 'delete'
  })
}

export const deleteAllSpecs = (productId) => {
  return request({
    url: `/product/spec/deleteAll/${productId}`,
    method: 'delete'
  })
}

export const analyzeProductSpecs = (data) => {
  return request({
    url: '/ai/analyzeProductSpecs',
    method: 'post',
    data
  })
}

export const generateProductDetail = (productName) => {
  return request({
    url: '/ai/generateProductDetail',
    method: 'post',
    data: { name: productName }
  })
}
