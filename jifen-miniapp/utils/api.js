// API 请求封装
import CONFIG from './config';

// 是否正在刷新 token
let isRefreshing = false;
let pendingRequests = [];

const request = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'Content-Type': 'application/json',
      ...options.header,
    };
    if (token) {
      header['Authorization'] = 'Bearer ' + token;
    }

    wx.request({
      url: CONFIG.API_BASE_URL + url,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: options.timeout || 15000,
      success(res) {
        const data = res.data;
        if (data.code === 200) {
          resolve(data.data);
        } else if (data.code === 401) {
          // 公开接口不需要认证，直接 reject
          const publicPaths = ['/products', '/auth/login', '/auth/register', '/auth/wx-login'];
          const isPublic = publicPaths.some(p => url.startsWith(p));
          if (isPublic) {
            reject(data);
            return;
          }
          // token 过期，尝试静默登录
          if (!isRefreshing) {
            isRefreshing = true;
            wx.login({
              success(loginRes) {
                wx.request({
                  url: CONFIG.API_BASE_URL + '/auth/wx-login',
                  method: 'POST',
                  data: { code: loginRes.code },
                  header: { 'Content-Type': 'application/json' },
                  success(refreshRes) {
                    const refreshData = refreshRes.data;
                    if (refreshData.code === 200) {
                      const newToken = refreshData.data.token;
                      wx.setStorageSync('token', newToken);
                      wx.setStorageSync('userInfo', JSON.stringify(refreshData.data));
                      // 重试之前失败的请求
                      pendingRequests.forEach(cb => cb());
                      pendingRequests = [];
                      // 重试当前请求
                      request(url, options).then(resolve).catch(reject);
                    } else {
                      // 刷新失败，跳登录
                      wx.reLaunch({ url: '/pages/index/index' });
                      reject(data);
                    }
                  },
                  fail() {
                    wx.reLaunch({ url: '/pages/index/index' });
                    reject(data);
                  },
                  complete() {
                    isRefreshing = false;
                  }
                });
              },
              fail() {
                isRefreshing = false;
                reject(data);
              }
            });
          } else {
            // 正在刷新中，排队等待
            pendingRequests.push(() => {
              request(url, options).then(resolve).catch(reject);
            });
          }
        } else {
          reject(data);
        }
      },
      fail(err) {
        reject({ code: -1, message: '网络请求失败: ' + (err.errMsg || '') });
      }
    });
  });
};

// 快捷方法
const api = {
  get(url, data) {
    return request(url, { method: 'GET', data });
  },
  post(url, data) {
    return request(url, { method: 'POST', data });
  },
  put(url, data) {
    return request(url, { method: 'PUT', data });
  },
  del(url) {
    return request(url, { method: 'DELETE' });
  },
};

export { request, api };
export default api;
