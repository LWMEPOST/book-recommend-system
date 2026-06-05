<template>
  <div>
    <h3 style="margin-top: 0">我的收藏</h3>
    <el-row :gutter="16">
      <el-col :span="6" v-for="book in books" :key="book.id" style="margin-bottom: 16px">
        <el-card shadow="hover" style="cursor: pointer" @click="$router.push(`/user/book/${book.id}`)">
          <div style="text-align: center; margin-bottom: 10px">
            <el-image :src="book.cover || '/default-book.png'" style="width: 120px; height: 160px" fit="contain">
              <template #error><div style="width: 120px; height: 160px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999"><el-icon :size="40"><Reading /></el-icon></div></template>
            </el-image>
          </div>
          <div style="font-weight: bold; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ book.title }}</div>
          <div style="color: #999; font-size: 13px; margin-top: 4px">{{ book.author }}</div>
          <div style="margin-top: 8px; text-align: right">
            <el-button type="danger" size="small" @click.stop="handleRemove(book.id)">取消收藏</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-pagination v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyFavorites, removeFavorite } from '../../api/favorite'
import { ElMessage } from 'element-plus'

const books = ref([])
const page = ref(1)
const total = ref(0)

const loadData = async () => {
  const res = await getMyFavorites({ page: page.value, size: 12 })
  books.value = res.data.records
  total.value = res.data.total
}

const handleRemove = async (bookId) => {
  await removeFavorite(bookId)
  ElMessage.success('已取消收藏')
  loadData()
}

onMounted(loadData)
</script>
