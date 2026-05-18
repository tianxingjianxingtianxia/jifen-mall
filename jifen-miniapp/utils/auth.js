// 微信登录逻辑
import api from './api';

// 检查是否已登录
function isLoggedIn() {
  const token = wx.getStorageSync('token');
  return !!token;
}

// 微信一键登录
function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          // 调用后端登录接口（后端需实现 wx-login 接口）
          api.post('/auth/wx-login', { code: res.code })
            .then(userInfo => {
              const app = getApp();
              app.onLoginSuccess(userInfo.token, userInfo);
              resolve(userInfo);
            })
            .catch(err => {
              // wx-login 接口可能还没实现，fallback 到账号页面
              reject(err);
            });
        } else {
          reject({ message: '微信登录失败' });
        }
      },
      fail(err) {
        reject({ message: '微信登录失败: ' + err.errMsg });
      }
    });
  });
}

// 检查登录状态，未登录则静默登录
function checkLogin() {
  if (isLoggedIn()) {
    return Promise.resolve(wx.getStorageSync('userInfo'));
  }
  return wxLogin();
}

export { isLoggedIn, wxLogin, checkLogin };
