const { request } = require('./request')
const { setToken, setUserInfo, getToken } = require('./storage')

/**
 * 微信登录：wx.login → 后端换 token
 * @returns {Promise<{token, userId, username, nickname, avatar, points}>}
 */
function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (!res.code) {
          reject(new Error('登录失败：无法获取code'))
          return
        }

        // 调后端接口换 token
        request('/auth/wx-login', 'POST', { code: res.code }, { noAuth: true, showLoading: true })
          .then(data => {
            setToken(data.token)
            setUserInfo({
              userId: data.userId,
              username: data.username,
              nickname: data.nickname,
              points: data.points
            })
            resolve(data)
          })
          .catch(reject)
      },
      fail(err) {
        reject(new Error('微信登录失败：' + (err.errMsg || '未知错误')))
      }
    })
  })
}

/**
 * 检查是否已登录
 */
function isLoggedIn() {
  return !!getToken()
}

module.exports = {
  wxLogin,
  isLoggedIn
}
