import request from '../utils/request'

export const getCouponList = (params) => {
  return request({
    url: '/admin/userCoupon/list',
    method: 'get',
    params
  })
}

export const getCouponDashboardData = () => {
  return request({
    url: '/admin/userCoupon/dashboard',
    method: 'get'
  })
}

export const getUserCoupons = (userId) => {
  return request({
    url: `/admin/userCoupon/user/${userId}`,
    method: 'get'
  })
}

export const addCoupon = (data) => {
  return request({
    url: '/admin/userCoupon/add',
    method: 'post',
    data
  })
}

export const useCoupon = (data) => {
  return request({
    url: '/admin/userCoupon/use',
    method: 'post',
    data
  })
}

export const updateCouponStatus = (data) => {
  return request({
    url: '/admin/userCoupon/updateStatus',
    method: 'post',
    data
  })
}

export const deleteCoupon = (id) => {
  return request({
    url: '/admin/userCoupon/delete',
    method: 'post',
    data: { id }
  })
}

export const batchDeleteCoupon = (ids) => {
  return request({
    url: '/admin/userCoupon/batchDelete',
    method: 'post',
    data: { ids }
  })
}