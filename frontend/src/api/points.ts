import request from '../utils/request'

export interface PointsBalance {
  points: number
  totalEarned: number
  totalSpent: number
}

export interface PointsRecord {
  id: number
  userId: number
  points: number
  type: string
  description: string
  createTime: string
}

export interface PointsRecordPage {
  records: PointsRecord[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface SignInResult {
  todaySigned: boolean
  points: number
  totalPoints: number
}

/** 签到 */
export function signIn() {
  return request.post<SignInResult>('/points/sign-in') as Promise<SignInResult>
}

/** 查询今日是否已签到 */
export function getTodaySign() {
  return request.get<boolean>('/points/today-sign') as Promise<boolean>
}

/** 查询积分余额 */
export function getPointsBalance() {
  return request.get<PointsBalance>('/points/balance') as Promise<PointsBalance>
}

/** 查询积分记录（分页） */
export function getPointsRecords(params: { pageNum: number; pageSize: number }) {
  return request.get<PointsRecordPage>('/points/records', { params }) as Promise<PointsRecordPage>
}
