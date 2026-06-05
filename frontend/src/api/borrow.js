import request from '../utils/request'

export function borrowBook(bookId) {
  return request.post(`/api/borrow/${bookId}`)
}

export function returnBook(id) {
  return request.put(`/api/borrow/return/${id}`)
}

export function getMyBorrowList(params) {
  return request.get('/api/borrow/my', { params })
}

export function getAdminBorrowList(params) {
  return request.get('/api/admin/borrow/list', { params })
}

export function getMyOverdueList(params) {
  return request.get('/api/overdue/my', { params })
}

export function getAdminOverdueList(params) {
  return request.get('/api/admin/overdue/list', { params })
}

export function handleOverdue(id) {
  return request.put(`/api/admin/overdue/handle/${id}`)
}
