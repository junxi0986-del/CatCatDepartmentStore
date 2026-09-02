import request from '../utils/request'

export const login = (data) => {
  return request({
    url: '/api/user/login',
    method: 'post',
    data
  })
}

export const register = (data) => {
  return request({
    url: '/api/user/register',
    method: 'post',
    data
  })
}

export const getUserInfo = () => {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

export const updateUserInfo = (data) => {
  return request({
    url: '/api/user/update',
    method: 'post',
    data
  })
}

export const changePassword = (data) => {
  return request({
    url: '/api/user/changePassword',
    method: 'post',
    data
  })
}

export const getUserCoupons = () => {
  return request({
    url: '/api/user/coupons',
    method: 'get'
  })
}

export const getCaptcha = () => {
  return request({
    url: '/api/user/captcha',
    method: 'get'
  })
}