<template>
  <div>
    <h3 style="margin-top: 0; margin-bottom: 20px">借阅管理</h3>
    <div style="margin-bottom: 16px; display: flex; gap: 12px">
      <el-input v-model="keyword" placeholder="搜索用户 / 书名 / ISBN" clearable style="width: 260px" @keyup.enter="handleSearch" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 150px" @change="loadData">
        <el-option label="借阅中" :value="0" />
        <el-option label="已归还" :value="1" />
        <el-option label="已逾期" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <el-table :data="records" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户" width="100" />
      <el-table-column prop="bookTitle" label="书名" />
      <el-table-column prop="borrowDate" label="借阅日期" width="180" />
      <el-table-column prop="dueDate" label="应还日期" width="180" />
      <el-table-column prop="returnDate" label="归还日期" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="['warning', 'success', 'danger'][row.status]">{{ ['借阅中', '已归还', '已逾期'][row.status] }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminBorrowList } from '../../api/borrow'

const records = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref(null)
const lastStatusFilter = ref(statusFilter.value)

const loadData = async () => {
  if (lastStatusFilter.value !== statusFilter.value) {
    page.value = 1
    lastStatusFilter.value = statusFilter.value
  }
  const res = await getAdminBorrowList({ page: page.value, size: 10, status: statusFilter.value, keyword: keyword.value })
  records.value = res.data.records
  total.value = res.data.total
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  keyword.value = ''
  statusFilter.value = null
  handleSearch()
}

onMounted(loadData)
</script>
