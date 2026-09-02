import request from '../utils/request'

export const getUserList = (params) => {
  return request({
    url: '/admin/user/list',
    method: 'get',
    params
  })
}

export const getUserById = (id) => {
  return request({
    url: `/admin/user/${id}`,
    method: 'get'
  })
}

export const addUser = (data) => {
  return request({
    url: '/admin/user/add',
    method: 'post',
    data
  })
}

export const editUser = (data) => {
  return request({
    url: '/admin/user/update',
    method: 'post',
    data
  })
}

export const deleteUser = (id) => {
  return request({
    url: `/admin/user/delete?id=${id}`,
    method: 'delete'
  })
}

export const updateUserStatus = (data) => {
  return request({
    url: '/admin/user/updateStatus',
    method: 'post',
    data
  })
}

export const batchUpdateUserStatus = (data) => {
  return request({
    url: '/admin/user/batchUpdateStatus',
    method: 'post',
    data
  })
}
