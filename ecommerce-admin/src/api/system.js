import request from '../utils/request'

export const getSystemConfig = () => {
  return request({
    url: '/admin/system/config',
    method: 'get'
  })
}

export const updateSystemConfig = (data) => {
  return request({
    url: '/admin/system/config',
    method: 'post',
    data
  })
}
