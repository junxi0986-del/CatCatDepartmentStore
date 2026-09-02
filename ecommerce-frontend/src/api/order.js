import request from '../utils/request'

export const createOrder = (data) => {
  return request({
    url: '/api/order/create',
    method: 'post',
    data
  })
}

export const getOrderList = (params) => {
  return request({
    url: '/api/order/list',
    method: 'get',
    params
  })
}

export const getOrderDetail = (id) => {
  return request({
    url: `/api/order/${id}`,
    method: 'get'
  })
}

export const cancelOrder = (orderId) => {
  return request({
    url: '/api/order/cancel',
    method: 'post',
    data: { orderId }
  })
}

export const payOrder = (data) => {
  return request({
    url: '/api/order/pay',
    method: 'post',
    data
  })
}

export const confirmReceipt = (data) => {
  return request({
    url: '/api/order/confirm',
    method: 'post',
    data
  })
}

export const applyRefund = (data) => {
  return request({
    url: '/api/order/refund/apply',
    method: 'post',
    data
  })
}