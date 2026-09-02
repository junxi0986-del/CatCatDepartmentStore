import request from '../utils/request'

export const recordBehavior = (data) => {
  return request({
    url: '/api/behavior/record',
    method: 'post',
    data
  })
}

export const updateStayTime = (data) => {
  return request({
    url: '/api/behavior/updateStayTime',
    method: 'post',
    data
  })
}
