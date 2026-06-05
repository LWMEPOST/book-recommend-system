import request from '../utils/request'

export function addFavorite(bookId) {
  return request.post(`/api/favorite/${bookId}`)
}

export function removeFavorite(bookId) {
  return request.delete(`/api/favorite/${bookId}`)
}

export function getFavoriteStatus(bookId) {
  return request.get(`/api/favorite/${bookId}/status`)
}

export function getMyFavorites(params) {
  return request.get('/api/favorite/my', { params })
}
