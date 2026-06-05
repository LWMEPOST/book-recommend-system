<template>
  <div>
    <h3 style="margin-top: 0">我的借阅</h3>
    <el-table :data="records" stripe>
      <el-table-column prop="bookTitle" label="书名" />
      <el-table-column prop="bookAuthor" label="作者" width="120" />
      <el-table-column prop="borrowDate" label="借阅日期" width="180" />
      <el-table-column prop="dueDate" label="应还日期" width="180" />
      <el-table-column prop="returnDate" label="归还日期" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="!row.returnDate" type="primary" size="small" @click="handleReturn(row.id)">归还</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyBorrowList, returnBook } from '../../api/borrow'
import { ElMessage } from 'element-plus'

const records = ref([])
const page = ref(1)
const total = ref(0)

const statusText = (s) => ['借阅中', '已归还', '已逾期'][s] || '未知'
const statusType = (s) => ['warning', 'success', 'danger'][s] || 'info'

const loadData = async () => {
  const res = await getMyBorrowList({ page: page.value, size: 10 })
  records.value = res.data.records
  total.value = res.data.total
}

const handleReturn = async (id) => {
  await returnBook(id)
  ElMessage.success('归还成功')
  loadData()
}

onMounted(loadData)
</script>
