<template>
  <div>
    <h3 style="margin-top: 0">逾期记录</h3>
    <el-table :data="records" stripe>
      <el-table-column prop="bookTitle" label="书名" />
      <el-table-column prop="overdueDays" label="逾期天数" width="100" />
      <el-table-column prop="fine" label="罚款金额" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'danger' : 'success'">{{ row.status === 0 ? '未处理' : '已处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="记录时间" width="180" />
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyOverdueList } from '../../api/borrow'

const records = ref([])
const page = ref(1)
const total = ref(0)

const loadData = async () => {
  const res = await getMyOverdueList({ page: page.value, size: 10 })
  records.value = res.data.records
  total.value = res.data.total
}

onMounted(loadData)
</script>
