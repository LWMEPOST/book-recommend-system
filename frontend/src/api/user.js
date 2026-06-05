import request from '../utils/request'

export function login(data) {
  return request.post('/api/user/login', data)
}

export function register(data) {
  return request.post('/api/user/register', data)
}

export function getUserInfo() {
  return request.get('/api/user/info')
}

export function updateUserInfo(data) {
  return request.put('/api/user/info', data)
}

export function updatePassword(data) {
  return request.put('/api/user/password', data)
}

export function getUserList(params) {
  return request.get('/api/admin/user/list', { params })
}

export function updateUserStatus(userId, status) {
  return request.put('/api/admin/user/status', null, { params: { userId, status } })
}
