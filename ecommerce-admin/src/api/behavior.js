import request from '../utils/request'

export const getBehaviorList = (params) => {
  return request({
    url: '/admin/behavior/list',
    method: 'get',
    params
  })
}

export const getDashboardData = () => {
  return request({
    url: '/admin/behavior/dashboard',
    method: 'get'
  })
}

export const getUserBehavior = (userId) => {
  return request({
    url: `/admin/behavior/user/${userId}`,
    method: 'get'
  })
}

export const getProductBehavior = (productId) => {
  return request({
    url: `/admin/behavior/product/${productId}`,
    method: 'get'
  })
}

export const addBehavior = (data) => {
  return request({
    url: '/admin/behavior/add',
    method: 'post',
    data
  })
}

export const deleteBehavior = (id) => {
  return request({
    url: '/admin/behavior/delete',
    method: 'post',
    data: { id }
  })
}

export const batchDeleteBehavior = (ids) => {
  return request({
    url: '/admin/behavior/batchDelete',
    method: 'post',
    data: { ids }
  })
}

export const analyzeUserBehavior = (userId) => {
  return request({
    url: `/admin/behavior/analysis/${userId}`,
    method: 'get'
  })
}