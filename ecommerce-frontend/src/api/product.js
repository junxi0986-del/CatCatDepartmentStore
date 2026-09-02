import request from '../utils/request'

export const getProductList = (params) => {
  return request({
    url: '/api/product/list',
    method: 'get',
    params
  })
}

export const getProductDetail = (id) => {
  return request({
    url: `/api/product/detail/${id}`,
    method: 'get'
  })
}

export const getProductSpecs = (productId) => {
  return request({
    url: `/api/product/spec/list/${productId}`,
    method: 'get'
  })
}
