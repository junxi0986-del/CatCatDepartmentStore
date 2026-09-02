import request from '../utils/request'

export const getCouponList = (params) => {
  return request({
    url: '/admin/coupon/list',
    method: 'get',
    params
  })
}

export const addCoupon = (data) => {
  return request({
    url: '/admin/coupon/add',
    method: 'post',
    data
  })
}

export const updateCoupon = (data) => {
  return request({
    url: '/admin/coupon/update',
    method: 'post',
    data
  })
}

export const deleteCoupon = (id) => {
  return request({
    url: '/admin/coupon/delete',
    method: 'post',
    data: { id }
  })
}

export const updateCouponStatus = (data) => {
  return request({
    url: '/admin/coupon/status',
    method: 'post',
    data
  })
}

export const getCouponById = (id) => {
  return request({
    url: `/admin/coupon/${id}`,
    method: 'get'
  })
}

export const useCoupon = (id) => {
  return request({
    url: '/admin/coupon/use',
    method: 'post',
    data: { id }
  })
}