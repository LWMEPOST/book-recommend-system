import request from '../utils/request'

export function getNoticeList(params) {
  return request.get('/api/notice/list', { params })
}

export function getNoticeDetail(id) {
  return request.get(`/api/notice/${id}`)
}

export function getAdminNoticeList(params) {
  return request.get('/api/admin/notice/list', { params })
}

export function addNotice(data) {
  return request.post('/api/admin/notice', data)
}

export function updateNotice(data) {
  return request.put('/api/admin/notice', data)
}

export function deleteNotice(id) {
  return request.delete(`/api/admin/notice/${id}`)
}
