import request from '../utils/request'

export const getOrderList = (params) => {
  return request({
    url: '/admin/order/list',
    method: 'get',
    params
  })
}

export const addOrder = (data) => {
  return request({
    url: '/admin/order/add',
    method: 'post',
    data
  })
}

export const updateOrder = (data) => {
  return request({
    url: '/admin/order/update',
    method: 'post',
    data
  })
}

export const deleteOrder = (id) => {
  return request({
    url: '/admin/order/delete',
    method: 'post',
    data: { id }
  })
}

export const updateOrderStatus = (data) => {
  return request({
    url: '/admin/order/status',
    method: 'post',
    data
  })
}

export const getOrderDetail = (id) => {
  return request({
    url: `/admin/order/${id}`,
    method: 'get'
  })
}

export const getOrderItemList = (params) => {
  return request({
    url: '/admin/order/item/list',
    method: 'get',
    params
  })
}

export const getOrderItemsByOrderId = (orderId) => {
  return request({
    url: '/admin/order/item/order',
    method: 'get',
    params: { orderId }
  })
}

export const getOrderItemById = (id) => {
  return request({
    url: `/admin/order/item/${id}`,
    method: 'get'
  })
}

export const confirmPayment = (orderId) => {
  return request({
    url: `/admin/order/confirmPayment`,
    method: 'post',
    data: { orderId }
  })
}

export const updateLogisticsInfo = (data) => {
  return request({
    url: `/admin/order/updateLogistics`,
    method: 'post',
    data
  })
}

export const getRefundList = () => {
  return request({
    url: '/order/refund/list',
    method: 'get'
  })
}

export const handleRefund = (data) => {
  return request({
    url: '/order/refund/handle',
    method: 'post',
    data
  })
}

export const updateOrderItem = (data) => {
  return request({
    url: '/admin/order/item/update',
    method: 'post',
    data
  })
}

export const batchConfirmPayment = (ids) => {
  return request({
    url: '/admin/order/batchConfirmPayment',
    method: 'post',
    data: { ids }
  })
}

export const batchCancelOrder = (ids) => {
  return request({
    url: '/admin/order/batchCancel',
    method: 'post',
    data: { ids }
  })
}

export const batchShipOrder = (data) => {
  return request({
    url: '/admin/order/batchShip',
    method: 'post',
    data
  })
}