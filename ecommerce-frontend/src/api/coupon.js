import request from '../utils/request'

export const getCouponList = () => {
  return request({
    url: '/api/admin/coupon/list',
    method: 'get'
  })
}

export const claimCoupon = (couponId) => {
  return request({
    url: '/api/user/claimCoupon',
    method: 'post',
    data: { couponId }
  })
}