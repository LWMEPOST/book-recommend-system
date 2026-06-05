<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #1d1e1f; display: flex; align-items: center; justify-content: space-between; padding: 0 20px">
      <div style="display: flex; align-items: center; color: white; font-size: 20px; font-weight: bold; cursor: pointer" @click="$router.push('/admin')">
        <el-icon style="margin-right: 8px"><Setting /></el-icon>
        管理后台
      </div>
      <div style="display: flex; align-items: center; gap: 15px">
        <el-button type="primary" text style="color: white" @click="$router.push('/')">返回前台</el-button>
        <span style="color: white">{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
        <el-button type="danger" text style="color: white" @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background: #304156">
        <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
          <el-menu-item index="/admin/books">
            <el-icon><Reading /></el-icon><span>图书管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/borrows">
            <el-icon><Notebook /></el-icon><span>借阅管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/overdues">
            <el-icon><Warning /></el-icon><span>逾期管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/notices">
            <el-icon><Bell /></el-icon><span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/algorithm">
            <el-icon><MagicStick /></el-icon><span>算法管理</span>
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
