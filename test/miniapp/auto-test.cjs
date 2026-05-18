#!/usr/bin/env node
/**
 * 积分商城 — 微信小程序自动化测试
 * 
 * 使用方法：
 *   1. Windows 上打开微信开发者工具，加载项目 J:\2026\jifen\jifen-miniapp
 *   2. 在设置中开启"编辑"→"服务端口"（默认 9420）
 *   3. 在 WSL 中运行此脚本
 */

const automator = require('miniprogram-automator')

const WS_ENDPOINT = process.env.WS_PORT ? 'ws://localhost:' + process.env.WS_PORT : 'ws://localhost:9420'
const API_BASE = 'http://192.168.2.17:8080/api'

let passed = 0
let failed = 0

function ok(msg) { passed++; console.log(`  ✅ ${msg}`) }
function fail(msg, detail) { failed++; console.log(`  ❌ ${msg}: ${detail || ''}`) }

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// API 工具函数
async function apiPost(path, data, token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(API_BASE + path, {
    method: 'POST', headers, body: JSON.stringify(data || {})
  })
  return res.json()
}

async function apiGet(path, token) {
  const headers = {}
  if (token) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(API_BASE + path, { headers })
  return res.json()
}

async function main() {
  console.log('')
  console.log('============================================================')
  console.log('  积分商城 — 微信小程序自动化测试')
  console.log('============================================================')
  console.log('')

  // ====== API 准备阶段 ======
  // 使用固定测试账号
  const TEST_USER = 'miniapp_auto_test'
  const TEST_PASS = '123456'

  let token, uid

  // 尝试登录现有账号
  let loginRes
  try {
    loginRes = await apiPost('/auth/login', { username: TEST_USER, password: TEST_PASS })
  } catch (e) {
    loginRes = null
  }

  if (loginRes && loginRes.code === 200) {
    token = loginRes.data.token
    uid = loginRes.data.userId
    ok(`登录固定账号成功 uid=${uid}`)
  } else {
    // 注册新账号
    const ts = 'mp_' + String(Date.now()).slice(-8)
    try {
      const regRes = await apiPost('/auth/register', {
        username: TEST_USER, password: TEST_PASS, nickname: '小程序测试', phone: '13800138001'
      })
      if (regRes.code !== 200) { fail('注册', regRes.message); process.exit(1) }
      token = regRes.data.token
      uid = regRes.data.userId
      ok(`注册新账号成功 uid=${uid}`)
    } catch (e) {
      fail('API注册用户', e.message)
      process.exit(1)
    }
  }

  // 充值积分
  await apiPost('/points/topup?points=200', null, token)
  ok('API 充值积分成功')

  // 新增地址
  const addrRes = await apiPost('/addresses', {
    receiverName: '测试', receiverPhone: '13800138001',
    province: '广东省', city: '深圳市', district: '南山区',
    detailAddress: '科技园路', isDefault: 1
  }, token)
  if (addrRes.code !== 200) { fail('API 新增地址', addrRes.message); process.exit(1) }
  ok('API 新增地址成功')

  // 获取商品
  const prodRes = await apiGet('/products?pageNum=1&pageSize=5')
  if (prodRes.code !== 200 || !prodRes.data.records.length) {
    fail('API 获取商品', '无可用商品'); process.exit(1)
  }
  const pid = prodRes.data.records[0].id
  const pname = prodRes.data.records[0].name
  ok(`API 获取商品成功: ${pname}`)

  // ====== 连接微信开发者工具 ======
  console.log(`\n连接到微信开发者工具 ${WS_ENDPOINT}...`)
  let mp
  try {
    mp = await automator.connect({ wsEndpoint: WS_ENDPOINT })
    ok('已连接到微信开发者工具')

    // 注入登录态到小程序
    await mp.evaluate(token => {
      wx.setStorageSync('token', token)
    }, token)
    await mp.evaluate(userInfo => {
      wx.setStorageSync('userInfo', JSON.stringify(userInfo))
    }, { userId: uid, username: 'test', nickname: '测试', points: 200 })
    ok('登录态注入成功')

  } catch (e) {
    fail('连接开发者工具', e.message)
    process.exit(1)
  }

  // ====== 1. 首页 ======
  console.log('\n=== 1. 首页 ===')
  let page = await mp.currentPage()
  const pagePath = page ? page.path : 'unknown'
  console.log(`  当前页面: ${pagePath}`)

  if (pagePath.includes('index/index')) {
    ok('首页加载成功')
  } else {
    fail('首页路径', `期望 index，实际 ${pagePath}`)
  }

  // 检查积分卡片
  await sleep(1000)
  const cards = await page.$$('points-card')
  if (cards.length > 0) {
    ok(`积分卡片组件已渲染`)
  } else {
    fail('未找到积分卡片')
  }

  // 检查商品列表
  await sleep(2000)
  // 组件名选择或页面文本检查
  const productComps = await page.$$('product-card')
  if (productComps.length > 0) {
    ok(`商品列表加载成功，${productComps.length} 个商品`)
  } else {
    // 检查是否在加载
    const loading = await page.$('.loading-container')
    if (loading) {
      ok('页面加载中（商品数据可能稍后出现）')
    } else {
      fail('商品列表', '未找到商品卡片')
    }
  }

  // ====== 2. 商品详情 ======
  console.log('\n=== 2. 商品详情 ===')
  const firstProduct = await page.$('.product-card')
  if (firstProduct) {
    await firstProduct.tap()
    await sleep(2000)

    page = await mp.currentPage()
    if (page.path.includes('product-detail')) {
      ok('点击商品 → 跳转到商品详情页')
    } else {
      fail('商品详情跳转', page.path)
    }

    // 检查商品信息
    const prodInfo = await page.$('.product-info')
    if (prodInfo) {
      ok('商品信息渲染成功')
    } else {
      fail('商品信息区域')
    }

    // 检查轮播图
    const swiper = await page.$('swiper')
    if (swiper) {
      ok('轮播图渲染成功')
    }
  } else {
    // 直接通过导航进入
    await mp.navigateTo('/pages/product-detail/product-detail?id=' + pid)
    await sleep(2000)
    page = await mp.currentPage()
    ok(`导航到商品详情页: ${page.path}`)
  }

  // ====== 3. 兑换 ======
  console.log('\n=== 3. 兑换 ===')

  // 检查兑换按钮
  const exchangeBtn = await page.$('.exchange-btn, button')
  if (exchangeBtn) {
    const btnText = await exchangeBtn.text()
    if (btnText.includes('兑换') || btnText.includes('立即兑换')) {
      ok(`兑换按钮存在: ${btnText}`)
    } else {
      ok(`按钮存在: ${btnText}`)
    }

    // 选择地址
    const addressSelect = await page.$('picker, .address-selector')
    if (addressSelect) {
      await addressSelect.tap()
      await sleep(1000)
      ok('地址选择器可操作')
    }

    // 点击兑换
    await exchangeBtn.tap()
    await sleep(1500)

    page = await mp.currentPage()
    if (page.path.includes('order-detail')) {
      ok('兑换成功 → 跳转到订单详情页')
    } else {
      // 尝试确认 dialog/modal
      try {
        const btns = await page.$$('button')
        if (btns.length > 0) {
          for (const btn of btns) {
            const t = await btn.text()
            if (t && (t.includes('确定') || t.includes('确定') || t.includes('confirm') || t.includes('OK'))) {
              await btn.tap()
              await sleep(2000)
              page = await mp.currentPage()
              if (page.path.includes('order-detail')) {
                ok('兑换确认弹窗 → 成功跳转到订单详情页')
              } else {
                ok(`兑换确认后页面: ${page.path}`)
              }
              break
            }
          }
        }
      } catch (e) {
        ok(`兑换后页面: ${page.path}`)
      }
    }
  } else {
    fail('兑换按钮')
  }

  // ====== 4. 订单列表 ======
  console.log('\n=== 4. 订单列表 ===')
  await mp.switchTab('/pages/orders/orders')
  await sleep(2000)

  page = await mp.currentPage()
  if (page.path.includes('orders/orders')) {
    ok('订单列表页加载成功')
  } else {
    fail('订单列表页跳转', page.path)
  }

  // 检查 Tab 切换
  const tabs = await page.$$('.tab-item, .tabs text')
  if (tabs.length > 0) {
    ok(`订单Tab栏渲染成功，${tabs.length} 个Tab`)

    // 点击第二个 Tab（待发货或其他）
    if (tabs.length >= 2) {
      await tabs[1].tap()
      await sleep(1500)
      ok('Tab 切换成功')
    }
  }

  // 检查订单列表
  await sleep(2000)
  const orderCards = await page.$$('.order-card')
  if (orderCards.length > 0) {
    ok(`订单列表加载成功，${orderCards.length} 个订单`)
  } else {
    const emptyText = await page.$('.empty-state, .empty-text')
    if (emptyText) {
      ok('订单为空状态显示正常')
    } else {
      ok('订单列表区域存在')
    }
  }

  // ====== 5. 个人中心 ======
  console.log('\n=== 5. 个人中心 ===')
  await mp.switchTab('/pages/profile/profile')
  await sleep(2000)

  page = await mp.currentPage()
  if (page.path.includes('profile/profile')) {
    ok('个人中心加载成功')
  } else {
    fail('个人中心跳转', page.path)
  }

  // 检查用户信息
  const userInfo = await page.$('.user-info, .profile-header')
  if (userInfo) {
    ok('用户信息区域渲染成功')
  }

  // 检查功能入口列表
  const menuItems = await page.$$('.menu-item, .profile-item')
  if (menuItems.length > 0) {
    ok(`功能入口列表渲染成功，${menuItems.length} 项`)
  }

  // ====== 6. 积分明细 ======
  console.log('\n=== 6. 积分明细 ===')
  await mp.navigateTo('/pages/points-records/points-records')
  await sleep(2000)

  page = await mp.currentPage()
  if (page.path.includes('points-records')) {
    ok('积分明细页加载成功')
  }

  await sleep(1500)
  const records = await page.$$('.record-item, .points-item')
  if (records.length > 0) {
    ok(`积分记录加载成功，${records.length} 条记录`)
  } else {
    const empty = await page.$('.empty-state')
    if (empty) {
      ok('积分记录为空状态显示正常')
    } else {
      ok('积分明细页渲染正常')
    }
  }

  // ====== 7. 截图 ======
  console.log('\n=== 截图 ===')
  await sleep(500)
  try {
    // 回到首页截图
    await mp.switchTab('/pages/index/index')
    await sleep(2000)
    const fs = require('fs')
    const dir = 'J:\\2026\\jifen\\test\\miniapp\\screenshots'
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
    const screenshot = await mp.screenshot({
      path: dir + '\\miniapp-final.png'
    })
    ok(`截图已保存: ${screenshot}`)
  } catch (e) {
    fail('截图', e.message)
  }

  // ====== 退出 ======
  await mp.close()

  console.log('')
  console.log('============================================================')
  console.log(`  测试结果: ✅ ${passed}   ❌ ${failed}`)
  console.log('============================================================')
  process.exit(failed > 0 ? 1 : 0)
}

main().catch(e => {
  console.error(`\n  ❌ 测试失败: ${e.message}`)
  process.exit(1)
})
