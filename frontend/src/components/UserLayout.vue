<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #409EFF; display: flex; align-items: center; justify-content: space-between; padding: 0 20px">
      <div style="display: flex; align-items: center; color: white; font-size: 20px; font-weight: bold; cursor: pointer" @click="$router.push('/')">
        <el-icon style="margin-right: 8px"><Reading /></el-icon>
        图书推荐系统
      </div>
      <div style="display: flex; align-items: center; gap: 15px">
        <template v-if="userStore.isLoggedIn()">
          <span style="color: white">{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
          <el-dropdown>
            <el-avatar :size="32" style="cursor: pointer">
              <el-icon><User /></el-icon>
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="userStore.isAdmin()" @click="$router.push('/admin')">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button text style="color: white" @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" plain @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background: #304156">
        <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
          <el-menu-item index="/user/books">
            <el-icon><Reading /></el-icon><span>图书浏览</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn()" index="/user/recommend">
            <el-icon><MagicStick /></el-icon><span>个性推荐</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn()" index="/user/borrow">
            <el-icon><Notebook /></el-icon><span>我的借阅</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn()" index="/user/overdue">
            <el-icon><Warning /></el-icon><span>逾期记录</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn()" index="/user/favorite">
            <el-icon><Star /></el-icon><span>我的收藏</span>
          </el-menu-item>
          <el-menu-item index="/user/notice">
            <el-icon><Bell /></el-icon><span>系统公告</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main style="background: #f0f2f5">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>
