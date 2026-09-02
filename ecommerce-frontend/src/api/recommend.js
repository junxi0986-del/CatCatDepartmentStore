import request from '../utils/request'

export const getRecommend = () => {
  return request({
    url: '/api/recommend',
    method: 'get'
  })
}

export const getUserRecommend = (userId) => {
  return request({
    url: `/api/recommend/${userId}`,
    method: 'get'
  })
}