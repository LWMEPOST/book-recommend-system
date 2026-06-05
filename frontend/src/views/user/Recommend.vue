<template>
  <div class="recommend-page">
    <div class="page-header">
      <div>
        <h2>推荐中心</h2>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="recommend-tabs" @tab-change="loadData">
      <el-tab-pane label="个性化推荐" name="personal">
        <el-empty v-if="personalBooks.length === 0" description="暂无推荐" class="tab-empty" />
        <div v-else class="book-grid">
          <article v-for="book in personalBooks" :key="book.id" class="book-card" @click="openBook(book.id)">
            <el-image :src="book.cover || '/default-book.png'" class="book-card__cover" fit="cover">
              <template #error>
                <div class="book-card__cover book-card__cover--fallback">
                  <el-icon><Reading /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="book-card__body">
              <h3>{{ book.title }}</h3>
              <p>{{ book.author }} · {{ book.categoryName || '未分类' }}</p>
              <div class="book-card__meta">
                <span>评分 {{ formatRating(book.avgRating) }}</span>
                <span>{{ book.ratingCount || 0 }} 人评分</span>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="热门推荐" name="hot">
        <div class="book-grid">
          <article v-for="book in hotBooks" :key="book.id" class="book-card" @click="openBook(book.id)">
            <el-image :src="book.cover || '/default-book.png'" class="book-card__cover" fit="cover">
              <template #error>
                <div class="book-card__cover book-card__cover--fallback">
                  <el-icon><Reading /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="book-card__body">
              <h3>{{ book.title }}</h3>
              <p>{{ book.author }} · {{ book.categoryName || '未分类' }}</p>
              <div class="book-card__meta">
                <span>评分 {{ formatRating(book.avgRating) }}</span>
                <span>{{ book.ratingCount || 0 }} 人评分</span>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="分类推荐" name="category">
        <div class="book-grid">
          <article v-for="book in categoryBooks" :key="book.id" class="book-card" @click="openBook(book.id)">
            <el-image :src="book.cover || '/default-book.png'" class="book-card__cover" fit="cover">
              <template #error>
                <div class="book-card__cover book-card__cover--fallback">
                  <el-icon><Reading /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="book-card__body">
              <h3>{{ book.title }}</h3>
              <p>{{ book.author }} · {{ book.categoryName || '未分类' }}</p>
              <div class="book-card__meta">
                <span>分类偏好推荐</span>
                <span>评分 {{ formatRating(book.avgRating) }}</span>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="新书速递" name="latest">
        <div class="book-grid">
          <article v-for="book in latestBooks" :key="book.id" class="book-card" @click="openBook(book.id)">
            <el-image :src="book.cover || '/default-book.png'" class="book-card__cover" fit="cover">
              <template #error>
                <div class="book-card__cover book-card__cover--fallback">
                  <el-icon><Reading /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="book-card__body">
              <h3>{{ book.title }}</h3>
              <p>{{ book.author }} · {{ book.categoryName || '未分类' }}</p>
              <div class="book-card__meta">
                <span>{{ formatDate(book.publishDate) }}</span>
                <span>最新上架</span>
              </div>
            </div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getCategoryRecommend,
  getHotRecommend,
  getLatestRecommend,
  getPersonalRecommend
} from '../../api/recommend'

const router = useRouter()
const activeTab = ref('personal')
const personalBooks = ref([])
const hotBooks = ref([])
const categoryBooks = ref([])
const latestBooks = ref([])

const formatRating = (value) => {
  const numeric = Number(value || 0)
  return numeric > 0 ? numeric.toFixed(1) : '暂无'
}

const formatDate = (value) => {
  if (!value) {
    return '待完善'
  }
  return String(value).slice(0, 10)
}

const openBook = (id) => {
  router.push(`/user/book/${id}`)
}

const loadData = async () => {
  if (activeTab.value === 'personal') {
    const response = await getPersonalRecommend()
    personalBooks.value = response.data
  } else if (activeTab.value === 'hot') {
    const response = await getHotRecommend()
    hotBooks.value = response.data
  } else if (activeTab.value === 'category') {
    const response = await getCategoryRecommend()
    categoryBooks.value = response.data
  } else {
    const response = await getLatestRecommend()
    latestBooks.value = response.data
  }
}

onMounted(loadData)
</script>

<style scoped>
.recommend-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.recommend-tabs {
  padding: 6px 4px 0;
  border-radius: 24px;
  background: #fff;
}

.tab-empty {
  padding: 24px 0;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 4px 0 8px;
}

.book-card {
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.book-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
}

.book-card__cover {
  width: 100%;
  height: 240px;
  background: #e5e7eb;
}

.book-card__cover--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.book-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
}

.book-card__body h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.book-card__body p {
  margin: 0;
  color: #64748b;
}

.book-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  font-size: 13px;
  color: #334155;
}

@media (max-width: 1100px) {
  .book-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .book-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .book-grid {
    grid-template-columns: 1fr;
  }
}
</style>
