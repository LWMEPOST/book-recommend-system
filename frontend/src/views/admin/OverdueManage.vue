<template>
  <div>
    <h3 style="margin-top: 0; margin-bottom: 20px">逾期管理</h3>
    <div style="margin-bottom: 16px; display: flex; gap: 12px">
      <el-input v-model="keyword" placeholder="搜索用户 / 书名 / ISBN" clearable style="width: 260px" @keyup.enter="handleSearch" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 150px" @change="loadData">
        <el-option label="未处理" :value="0" />
        <el-option label="已处理" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <el-table :data="records" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户" width="100" />
      <el-table-column prop="bookTitle" label="书名" />
      <el-table-column prop="overdueDays" label="逾期天数" width="100" />
      <el-table-column prop="fine" label="罚款" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'danger' : 'success'">{{ row.status === 0 ? '未处理' : '已处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="记录时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="primary" size="small" @click="handleOverdue(row.id)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminOverdueList, handleOverdue as handleOverdueApi } from '../../api/borrow'
import { ElMessage } from 'element-plus'

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
  const res = await getAdminOverdueList({ page: page.value, size: 10, status: statusFilter.value, keyword: keyword.value })
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

const handleOverdue = async (id) => {
  await handleOverdueApi(id)
  ElMessage.success('处理成功')
  loadData()
}

onMounted(loadData)
</script>
