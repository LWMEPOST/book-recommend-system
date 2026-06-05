import request from '../utils/request'

export function getPersonalRecommend() {
  return request.get('/api/recommend/personal')
}

export function getHotRecommend() {
  return request.get('/api/recommend/hot')
}

export function getLatestRecommend() {
  return request.get('/api/recommend/latest')
}

export function getCategoryRecommend() {
  return request.get('/api/recommend/category')
}
