import request from '../utils/request'

export function getBookList(params) {
  return request.get('/api/book/list', { params })
}

export function getBookDetail(id) {
  return request.get(`/api/book/${id}`)
}

export function getAdminBookList(params) {
  return request.get('/api/admin/book/list', { params })
}

export function addBook(data) {
  return request.post('/api/admin/book', data)
}

export function updateBook(id, data) {
  return request.put('/api/admin/book', data, { params: { id } })
}

export function deleteBook(id) {
  return request.delete(`/api/admin/book/${id}`)
}

export function getCategoryList() {
  return request.get('/api/category/list')
}

export function uploadBookCover(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/upload/book-cover', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function batchImportBooks(data) {
  return request.post('/api/admin/book/import', data)
}
