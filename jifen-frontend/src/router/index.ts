import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/addresses',
    name: 'Addresses',
    component: () => import('../views/Addresses.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/points-records',
    name: 'PointsRecords',
    component: () => import('../views/PointsRecords.vue'),
    meta: { requiresAuth: true }
  },
  // 管理后台路由
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/Login.vue'),
    meta: { requiresAuth: false, isAdmin: false }
  },
  {
    path: '/admin',
    component: () => import('../views/admin/Layout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('../views/admin/Products.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/Orders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'config',
        name: 'AdminConfig',
        component: () => import('../views/admin/Config.vue'),
        meta: { title: '系统配置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫：检查登录状态和 admin 权限
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isAdmin = localStorage.getItem('isAdmin') === 'true'

  // 需要管理员权限
  if (to.meta.requiresAdmin && (!token || !isAdmin)) {
    next({ name: 'AdminLogin' })
    return
  }

  // 需要登录但未登录
  if (to.meta.requiresAuth !== false && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录用户访问登录/注册页，跳转到首页
  if ((to.name === 'Login' || to.name === 'Register') && token) {
    next({ name: 'Home' })
    return
  }

  // 已登录管理员访问 admin/login，跳转看板
  if (to.name === 'AdminLogin' && token && isAdmin) {
    next({ name: 'AdminDashboard' })
    return
  }

  next()
})

export default router
