import request from '../utils/request'

// 获取购物车列表
export const getCartList = () => {
  return request({
    url: '/api/cart/list',
    method: 'get'
  })
}

// 添加购物车
export const addToCart = (data) => {
  return request({
    url: '/api/cart/add',
    method: 'post',
    params: {
      productId: data.productId,
      quantity: data.quantity
    }
  })
}

// 修改购物车数量
export const updateCartQuantity = (id, quantity) => {
  return request({
    url: '/api/cart/update/quantity',
    method: 'put',
    params: { id, quantity }
  })
}

// 修改购物车选中状态
export const updateCartSelected = (id, selected) => {
  return request({
    url: '/api/cart/update/selected',
    method: 'put',
    params: { id, selected }
  })
}

// 删除购物车
export const deleteCart = (id) => {
  return request({
    url: '/api/cart/delete',
    method: 'delete',
    params: { id }
  })
}

// 清空已选中购物车
export const clearSelectedCart = () => {
  return request({
    url: '/api/cart/clear',
    method: 'delete'
  })
}