const { API_BASE_URL } = require('./config')
const { getToken, clearAll } = require('./storage')

/**
 * 封装 wx.request，自动注入 token，统一错误处理
 * 
 * @param {string} url      接口路径（不含 /api 前缀）
 * @param {string} method   GET | POST | PUT | DELETE
 * @param {object} data     请求体
 * @param {object} options  { noAuth, showLoading }
 * @returns {Promise}
 */
function request(url, method = 'GET', data = {}, options = {}) {
  const { noAuth = false, showLoading = true } = options

  if (showLoading) {
    wx.showLoading({ title: '加载中...', mask: true })
  }

  const header = {
    'Content-Type': 'application/json'
  }

  if (!noAuth) {
    const token = getToken()
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: API_BASE_URL + url,
      method,
      data,
      header,
      success(res) {
        if (showLoading) wx.hideLoading()

        if (res.statusCode === 200) {
          const body = res.data
          if (body.code === 200) {
            resolve(body.data)
          } else if (body.code === 401) {
            // token失效
            clearAll()
            wx.reLaunch({ url: '/pages/index/index' })
            reject(new Error(body.message || '登录已过期'))
          } else {
            reject(new Error(body.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          clearAll()
          wx.reLaunch({ url: '/pages/index/index' })
          reject(new Error('登录已过期'))
        } else {
          reject(new Error(`请求失败(${res.statusCode})`))
        }
      },
      fail(err) {
        if (showLoading) wx.hideLoading()
        reject(new Error('网络异常，请重试'))
      }
    })
  })
}

module.exports = { request }
