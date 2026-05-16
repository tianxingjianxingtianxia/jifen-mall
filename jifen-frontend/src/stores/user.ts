import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '../api/auth'

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  points: number
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(loadUserInfo())

  function loadUserInfo(): UserInfo | null {
    const stored = localStorage.getItem('userInfo')
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        return null
      }
    }
    return null
  }

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || '')
  const points = computed(() => userInfo.value?.points ?? 0)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function updatePoints(points: number) {
    if (userInfo.value) {
      userInfo.value.points = points
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  async function fetchUserInfo() {
    try {
      const info = await getUserInfo()
      setUserInfo(info)
      return info
    } catch {
      logout()
      return null
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    nickname,
    points,
    setToken,
    setUserInfo,
    updatePoints,
    fetchUserInfo,
    logout
  }
})
