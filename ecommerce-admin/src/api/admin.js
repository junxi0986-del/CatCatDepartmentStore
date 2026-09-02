import request from '../utils/request'

export const login = (data) => {
  return request({ url: '/admin/login', method: 'post', data })
}

export const getAdminInfo = () => {
  return request({ url: '/admin/info', method: 'get' })
}

export const logout = () => {
  return request({ url: '/admin/logout', method: 'post' })
}

export const getAdminList = (params) => {
  return request({ url: '/admin/manage/list', method: 'get', params })
}

export const getAdminDetail = (id) => {
  return request({ url: `/admin/manage/detail/${id}`, method: 'get' })
}

export const addAdmin = (data) => {
  return request({ url: '/admin/manage/add', method: 'post', data })
}

export const updateAdmin = (data) => {
  return request({ url: '/admin/manage/update', method: 'put', data })
}

export const deleteAdmin = (id) => {
  return request({ url: `/admin/manage/delete/${id}`, method: 'delete' })
}

export const updateAdminStatus = (data) => {
  return request({ url: '/admin/manage/status', method: 'put', data })
}

export const resetAdminPassword = (data) => {
  return request({ url: '/admin/manage/resetPassword', method: 'put', data })
}

export const getRoleList = () => {
  return request({ url: '/admin/manage/roles', method: 'get' })
}

export const getPermissionList = () => {
  return request({ url: '/admin/manage/permissions', method: 'get' })
}

export const getRolePermissions = (roleKey) => {
  return request({ url: `/admin/manage/role/permissions/${roleKey}`, method: 'get' })
}

export const updateRolePermissions = (data) => {
  return request({ url: '/admin/manage/role/permissions', method: 'put', data })
}
