<template>
  <div v-if="book">
    <el-card>
      <el-row :gutter="30">
        <el-col :span="8">
          <el-image :src="book.cover || '/default-book.png'" style="width: 100%; max-height: 400px" fit="contain">
            <template #error><div style="height: 300px; background: #f0f0f0; display: flex; align-items: center; justify-content: center"><el-icon :size="80"><Reading /></el-icon></div></template>
          </el-image>
        </el-col>
        <el-col :span="16">
          <h2 style="margin-top: 0">{{ book.title }}</h2>
          <el-descriptions :column="2" border style="margin-top: 20px">
            <el-descriptions-item label="作者">{{ book.author }}</el-descriptions-item>
            <el-descriptions-item label="出版社">{{ book.publisher || '-' }}</el-descriptions-item>
            <el-descriptions-item label="ISBN">{{ book.isbn || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ book.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="价格">{{ book.price ? '¥' + book.price : '-' }}</el-descriptions-item>
            <el-descriptions-item label="出版日期">{{ book.publishDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="可借数量">
              <el-tag :type="book.availableCount > 0 ? 'success' : 'danger'">{{ book.availableCount }} / {{ book.totalCount }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="评分">
              <el-rate v-model="book.avgRating" disabled /> ({{ book.ratingCount }}人评分)
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 20px; display: flex; gap: 12px">
            <el-button type="primary" :disabled="book.availableCount <= 0" @click="handleBorrow">借阅图书</el-button>
            <el-button :type="isFav ? 'warning' : 'default'" @click="handleFavorite">
              <el-icon><Star /></el-icon>{{ isFav ? '取消收藏' : '加入收藏' }}
            </el-button>
          </div>
          <div style="margin-top: 20px">
            <span style="margin-right: 10px">我的评分:</span>
            <el-rate v-model="myRating" :max="5" @change="handleRate" />
          </div>
        </el-col>
      </el-row>
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header><span style="font-weight: bold">图书简介</span></template>
      <p style="line-height: 1.8">{{ book.description || '暂无简介' }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBookDetail } from '../../api/book'
import { borrowBook } from '../../api/borrow'
import { addFavorite, removeFavorite, getFavoriteStatus } from '../../api/favorite'
import { getMyRating, rateBook } from '../../api/rating'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const book = ref(null)
const isFav = ref(false)
const myRating = ref(0)

const loadBook = async () => {
  const res = await getBookDetail(route.params.id)
  book.value = res.data
}

const loadUserInteractions = async () => {
  const [favoriteRes, ratingRes] = await Promise.all([
    getFavoriteStatus(route.params.id),
    getMyRating(route.params.id)
  ])
  isFav.value = favoriteRes.data
  myRating.value = ratingRes.data || 0
}

const handleBorrow = async () => {
  if (!ensureLoggedIn()) return
  await borrowBook(book.value.id)
  ElMessage.success('借阅成功')
  await loadBook()
}

const handleFavorite = async () => {
  if (!ensureLoggedIn()) return
  if (isFav.value) {
    await removeFavorite(book.value.id)
    ElMessage.success('已取消收藏')
  } else {
    await addFavorite(book.value.id)
    ElMessage.success('收藏成功')
  }
  await loadUserInteractions()
}

const handleRate = async (val) => {
  if (!ensureLoggedIn()) {
    myRating.value = 0
    return
  }

  if (val > 0) {
    await rateBook({ bookId: book.value.id, score: val })
    ElMessage.success('评分成功')
    await Promise.all([loadBook(), loadUserInteractions()])
  }
}

const ensureLoggedIn = () => {
  if (userStore.isLoggedIn()) {
    return true
  }

  ElMessage.warning('登录后可进行借阅、收藏和评分')
  router.push('/login')
  return false
}

onMounted(async () => {
  await loadBook()
  if (userStore.isLoggedIn()) {
    await loadUserInteractions()
  }
})
</script>
