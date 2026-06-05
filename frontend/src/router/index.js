import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    component: () => import('../components/UserLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/user/BookList.vue'), meta: { public: true } },
      { path: 'user/books', name: 'BookList', component: () => import('../views/user/BookList.vue'), meta: { public: true } },
      { path: 'user/book/:id', name: 'BookDetail', component: () => import('../views/user/BookDetail.vue'), meta: { public: true } },
      { path: 'user/borrow', name: 'MyBorrow', component: () => import('../views/user/MyBorrow.vue'), meta: { requiresAuth: true } },
      { path: 'user/overdue', name: 'MyOverdue', component: () => import('../views/user/MyOverdue.vue'), meta: { requiresAuth: true } },
      { path: 'user/favorite', name: 'MyFavorite', component: () => import('../views/user/MyFavorite.vue'), meta: { requiresAuth: true } },
      { path: 'user/recommend', name: 'Recommend', component: () => import('../views/user/Recommend.vue'), meta: { requiresAuth: true } },
      { path: 'user/notice', name: 'NoticeList', component: () => import('../views/user/NoticeList.vue'), meta: { public: true } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../components/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/books' },
      { path: 'books', name: 'BookManage', component: () => import('../views/admin/BookManage.vue') },
      { path: 'users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue') },
      { path: 'borrows', name: 'BorrowManage', component: () => import('../views/admin/BorrowManage.vue') },
      { path: 'overdues', name: 'OverdueManage', component: () => import('../views/admin/OverdueManage.vue') },
      { path: 'notices', name: 'NoticeManage', component: () => import('../views/admin/NoticeManage.vue') },
      { path: 'algorithm', name: 'AlgorithmManage', component: () => import('../views/admin/AlgorithmManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login' || to.path === '/register') {
    next()
    return
  }

  if (to.matched.some(record => record.meta.requiresAdmin)) {
    if (!userStore.isLoggedIn()) {
      next('/login')
      return
    }

    if (!userStore.isAdmin()) {
      next('/')
      return
    }
  }

  if (to.matched.some(record => record.meta.requiresAuth) && !userStore.isLoggedIn()) {
    next('/login')
    return
  }

  next()
})

export default router
