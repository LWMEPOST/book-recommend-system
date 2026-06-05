import request from '../utils/request'

export function rateBook(data) {
  return request.post('/api/rating', data)
}

export function getBookRating(bookId) {
  return request.get(`/api/rating/book/${bookId}`)
}

export function getMyRating(bookId) {
  return request.get(`/api/rating/my/${bookId}`)
}
