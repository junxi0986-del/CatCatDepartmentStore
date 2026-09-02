import request from '../utils/request'

export const addAddress = (data) => {
  return request({
    url: '/api/address/add',
    method: 'post',
    data
  })
}

export const getAddressList = () => {
  return request({
    url: '/api/address/list',
    method: 'get'
  })
}

export const updateAddress = (data) => {
  return request({
    url: '/api/address/update',
    method: 'put',
    data
  })
}

export const deleteAddress = (id) => {
  return request({
    url: `/api/address/${id}`,
    method: 'delete'
  })
}