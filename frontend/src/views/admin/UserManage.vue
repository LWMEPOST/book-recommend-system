<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px">
      <h3 style="margin-top: 0">用户管理</h3>
      <el-input v-model="keyword" placeholder="搜索用户" style="width: 250px" @keyup.enter="loadUsers" clearable>
        <template #append><el-button @click="loadUsers"><el-icon><Search /></el-icon></el-button></template>
      </el-input>
    </div>
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : ''">{{ row.role === 1 ? '管理员' : '用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.role !== 1" size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="loadUsers" style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, updateUserStatus } from '../../api/user'
import { ElMessage } from 'element-plus'

const users = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')

const loadUsers = async () => {
  const res = await getUserList({ page: page.value, size: 10, keyword: keyword.value })
  users.value = res.data.records
  total.value = res.data.total
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadUsers()
}

onMounted(loadUsers)
</script>
