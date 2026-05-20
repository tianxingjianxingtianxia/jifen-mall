import request from '../utils/request'

export interface LoginData {
  username: string
  password: string
  nickname?: string
}

export interface AuthResult {
  token: string
  userId: number
  username: string
  nickname: string
  points?: number
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  points: number
}

/** 用户注册 */
export function register(data: { username: string; password: string; nickname: string; phone: string }) {
  return request.post<AuthResult>('/auth/register', data) as Promise<AuthResult>
}

/** 用户登录 */
export function login(data: { username: string; password: string }) {
  return request.post<AuthResult>('/auth/login', data) as Promise<AuthResult>
}

/** 管理员登录 */
export function adminLogin(data: { username: string; password: string }) {
  return request.post<AuthResult>('/auth/admin/login', data) as Promise<AuthResult>
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request.get<UserInfo>('/auth/userinfo') as Promise<UserInfo>
}
