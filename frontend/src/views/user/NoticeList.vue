<template>
  <div>
    <h3 style="margin-top: 0">系统公告</h3>
    <el-card v-for="notice in notices" :key="notice.id" style="margin-bottom: 16px; cursor: pointer" shadow="hover" @click="showDetail(notice)">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span style="font-weight: bold; font-size: 16px">{{ notice.title }}</span>
        <span style="color: #999; font-size: 13px">{{ notice.createTime }}</span>
      </div>
      <div style="color: #666; margin-top: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ notice.content }}</div>
    </el-card>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: center" />

    <el-dialog v-model="dialogVisible" :title="currentNotice.title" width="600px">
      <div style="color: #999; margin-bottom: 12px">{{ currentNotice.createTime }}</div>
      <div style="line-height: 1.8">{{ currentNotice.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNoticeList } from '../../api/notice'

const notices = ref([])
const page = ref(1)
const total = ref(0)
const dialogVisible = ref(false)
const currentNotice = ref({})

const loadData = async () => {
  const res = await getNoticeList({ page: page.value, size: 10 })
  notices.value = res.data.records
  total.value = res.data.total
}

const showDetail = (notice) => {
  currentNotice.value = notice
  dialogVisible.value = true
}

onMounted(loadData)
</script>
