import request from '../utils/request'

export function getAlgorithmConfig() {
  return request.get('/api/admin/algorithm/config')
}

export function getAlgorithmSummary() {
  return request.get('/api/admin/algorithm/summary')
}

export function updateAlgorithmConfig(configKey, configValue) {
  return request.put('/api/admin/algorithm/config', null, { params: { configKey, configValue } })
}

export function refreshRecommend() {
  return request.post('/api/admin/algorithm/refresh')
}
