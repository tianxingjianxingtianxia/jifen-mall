// Token 管理
function getToken() {
  return wx.getStorageSync('token') || ''
}

function setToken(token) {
  wx.setStorageSync('token', token)
}

function removeToken() {
  wx.removeStorageSync('token')
}

// 用户信息管理
function getUserInfo() {
  return wx.getStorageSync('userInfo') || null
}

function setUserInfo(info) {
  wx.setStorageSync('userInfo', info)
}

function removeUserInfo() {
  wx.removeStorageSync('userInfo')
}

// 清除所有登录信息
function clearAll() {
  removeToken()
  removeUserInfo()
}

module.exports = {
  getToken,
  setToken,
  removeToken,
  getUserInfo,
  setUserInfo,
  removeUserInfo,
  clearAll
}
