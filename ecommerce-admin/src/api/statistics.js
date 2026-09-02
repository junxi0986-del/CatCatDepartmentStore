import request from '../utils/request'

export const getStatisticsOverview = () => {
  return request({
    url: '/admin/statistics/overview',
    method: 'get'
  })
}

export const getMonthlySales = () => {
  return request({
    url: '/admin/statistics/monthlySales',
    method: 'get'
  })
}

export const getDailySales = (month) => {
  return request({
    url: '/admin/statistics/dailySales',
    method: 'get',
    params: { month }
  })
}
