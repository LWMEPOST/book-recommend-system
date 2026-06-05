<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero__content">
        <h1>图书浏览</h1>
      </div>
      <div class="hero__stats">
        <div class="stat-card">
          <strong>{{ total }}</strong>
          <span>当前筛选图书</span>
        </div>
        <div class="stat-card">
          <strong>{{ categories.length }}</strong>
          <span>图书分类</span>
        </div>
        <div class="stat-card">
          <strong>{{ hotBooks.length }}</strong>
          <span>热门推荐位</span>
        </div>
      </div>
    </section>

    <el-card class="panel-card" shadow="never">
      <div class="panel-card__header">
        <div>
          <h2>个性化推荐区</h2>
        </div>
        <el-button v-if="isLoggedIn" text @click="router.push('/user/recommend')">查看完整推荐页</el-button>
      </div>

      <template v-if="isLoggedIn">
        <div v-if="personalBooks.length" class="recommend-grid">
          <article
            v-for="book in personalBooks.slice(0, 4)"
            :key="book.id"
            class="recommend-card"
            @click="openBook(book.id)"
          >
            <el-image :src="book.cover || '/default-book.png'" class="recommend-card__cover" fit="cover">
              <template #error>
                <div class="recommend-card__cover recommend-card__cover--fallback">
                  <el-icon><Reading /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="recommend-card__body">
              <div class="recommend-card__badge">猜你喜欢</div>
              <h3>{{ book.title }}</h3>
              <p>{{ book.author }} · {{ book.categoryName || '未分类' }}</p>
              <div class="recommend-card__meta">
                <span>评分 {{ formatRating(book.avgRating) }}</span>
                <span>{{ book.ratingCount || 0 }} 人评分</span>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无推荐" />
      </template>

      <div v-else class="login-hint">
        <el-button type="primary" @click="router.push('/login')">登录后查看个性化推荐</el-button>
      </div>
    </el-card>

    <div class="ranking-layout">
      <el-card class="ranking-card" shadow="never">
        <div class="panel-card__header">
          <div>
            <h2>热门榜</h2>
          </div>
        </div>
        <div class="ranking-list">
          <button
            v-for="(book, index) in hotBooks.slice(0, 6)"
            :key="book.id"
            type="button"
            class="ranking-item"
            @click="openBook(book.id)"
          >
            <span class="ranking-item__index">{{ index + 1 }}</span>
            <div class="ranking-item__content">
              <strong>{{ book.title }}</strong>
              <span>{{ book.author }}</span>
            </div>
            <div class="ranking-item__meta">
              <span>{{ formatRating(book.avgRating) }}</span>
              <small>{{ book.ratingCount || 0 }}评</small>
            </div>
          </button>
        </div>
      </el-card>

      <el-card class="ranking-card" shadow="never">
        <div class="panel-card__header">
          <div>
            <h2>新书榜</h2>
          </div>
        </div>
        <div class="ranking-list">
          <button
            v-for="(book, index) in latestBooks.slice(0, 6)"
            :key="book.id"
            type="button"
            class="ranking-item"
            @click="openBook(book.id)"
          >
            <span class="ranking-item__index ranking-item__index--new">{{ index + 1 }}</span>
            <div class="ranking-item__content">
              <strong>{{ book.title }}</strong>
              <span>{{ book.author }}</span>
            </div>
            <div class="ranking-item__meta">
              <span>{{ formatDate(book.publishDate) }}</span>
              <small>{{ book.categoryName || '未分类' }}</small>
            </div>
          </button>
        </div>
      </el-card>
    </div>

    <el-card class="panel-card" shadow="never">
      <div class="panel-card__header">
        <div>
          <h2>书库浏览</h2>
        </div>
      </div>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索书名、作者或 ISBN"
          clearable
          class="toolbar__keyword"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="categoryId" placeholder="选择分类" clearable class="toolbar__select" @change="handleSearch">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <div v-if="books.length" class="catalog-grid">
        <article v-for="book in books" :key="book.id" class="catalog-card" @click="openBook(book.id)">
          <el-image :src="book.cover || '/default-book.png'" class="catalog-card__cover" fit="cover">
            <template #error>
              <div class="catalog-card__cover catalog-card__cover--fallback">
                <el-icon><Reading /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="catalog-card__body">
            <h3>{{ book.title }}</h3>
            <p class="catalog-card__author">{{ book.author }}</p>
            <p class="catalog-card__desc">{{ book.description || '这本书暂时还没有简介。' }}</p>
            <div class="catalog-card__footer">
              <span class="catalog-card__tag">{{ book.categoryName || '未分类' }}</span>
              <span class="catalog-card__tag catalog-card__tag--status">
                {{ book.availableCount > 0 ? `可借 ${book.availableCount}` : '已借完' }}
              </span>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="没有找到符合条件的图书" />

      <el-pagination
        v-model:current-page="page"
        :page-size="12"
        :total="total"
        layout="prev, pager, next"
        class="pagination"
        @current-change="loadBooks"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getBookList, getCategoryList } from '../../api/book'
import { getHotRecommend, getLatestRecommend, getPersonalRecommend } from '../../api/recommend'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const books = ref([])
const categories = ref([])
const personalBooks = ref([])
const hotBooks = ref([])
const latestBooks = ref([])
const keyword = ref('')
const categoryId = ref(null)
const page = ref(1)
const total = ref(0)

const isLoggedIn = computed(() => userStore.isLoggedIn())

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

const loadBooks = async () => {
  const normalizedKeyword = keyword.value?.trim()
  const response = await getBookList({
    page: page.value,
    size: 12,
    keyword: normalizedKeyword || undefined,
    categoryId: categoryId.value
  })
  books.value = response.data.records
  total.value = response.data.total
}

const loadCategories = async () => {
  const response = await getCategoryList()
  categories.value = response.data
}

const loadRecommendPanels = async () => {
  const [hotResponse, latestResponse] = await Promise.all([
    getHotRecommend(),
    getLatestRecommend()
  ])
  hotBooks.value = hotResponse.data
  latestBooks.value = latestResponse.data

  if (isLoggedIn.value) {
    const personalResponse = await getPersonalRecommend()
    personalBooks.value = personalResponse.data
  } else {
    personalBooks.value = []
  }
}

const handleSearch = () => {
  page.value = 1
  loadBooks()
}

const handleReset = () => {
  keyword.value = ''
  categoryId.value = null
  page.value = 1
  loadBooks()
}

watch(
  () => userStore.token,
  () => {
    loadRecommendPanels()
  }
)

onMounted(() => {
  loadBooks()
  loadCategories()
  loadRecommendPanels()
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 1fr);
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.26), transparent 32%),
    linear-gradient(135deg, #0f766e, #155e75 55%, #1d4ed8);
  color: #fff;
}

.hero__content h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.2;
}

.hero__content p {
  max-width: 700px;
  margin: 0;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.9);
}

.hero__stats {
  display: grid;
  gap: 14px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 110px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(6px);
}

.stat-card strong {
  font-size: 28px;
  line-height: 1;
}

.stat-card span {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.84);
}

.panel-card,
.ranking-card {
  border-radius: 24px;
}

.panel-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-card__header h2 {
  margin: 0 0 6px;
  font-size: 22px;
}

.panel-card__header p {
  margin: 0;
  color: #6b7280;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.recommend-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  border-radius: 22px;
  background: linear-gradient(180deg, #f8fafc, #eef6ff);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.recommend-card:hover,
.catalog-card:hover,
.ranking-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.recommend-card__cover,
.catalog-card__cover {
  flex-shrink: 0;
  border-radius: 16px;
  overflow: hidden;
  background: #e5e7eb;
}

.recommend-card__cover {
  width: 88px;
  height: 124px;
}

.recommend-card__cover--fallback,
.catalog-card__cover--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.recommend-card__body {
  min-width: 0;
}

.recommend-card__badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-size: 12px;
}

.recommend-card__body h3,
.catalog-card__body h3 {
  margin: 10px 0 8px;
  font-size: 17px;
  line-height: 1.4;
}

.recommend-card__body p,
.catalog-card__author {
  margin: 0;
  color: #64748b;
}

.recommend-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  color: #334155;
  font-size: 13px;
}

.login-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff7ed, #fffbeb);
}

.ranking-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ranking-item__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}

.ranking-item__index--new {
  background: #dcfce7;
  color: #15803d;
}

.ranking-item__content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.ranking-item__content strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-item__content span,
.ranking-item__meta small {
  color: #64748b;
}

.ranking-item__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: #0f172a;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.toolbar__keyword {
  width: 320px;
}

.toolbar__select {
  width: 170px;
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.catalog-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 14px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.catalog-card__cover {
  width: 100%;
  height: 220px;
}

.catalog-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.catalog-card__desc {
  display: -webkit-box;
  margin: 10px 0 14px;
  color: #475569;
  line-height: 1.7;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.catalog-card__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
}

.catalog-card__tag {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
}

.catalog-card__tag--status {
  background: #ecfeff;
  color: #0f766e;
}

.pagination {
  margin-top: 22px;
  justify-content: center;
}

@media (max-width: 1200px) {
  .recommend-grid,
  .catalog-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero,
  .ranking-layout {
    grid-template-columns: 1fr;
  }

  .recommend-grid,
  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .login-hint {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 22px;
  }

  .recommend-grid,
  .catalog-grid {
    grid-template-columns: 1fr;
  }

  .recommend-card {
    flex-direction: column;
  }

  .recommend-card__cover {
    width: 100%;
    height: 220px;
  }

  .toolbar__keyword,
  .toolbar__select {
    width: 100%;
  }
}
</style>
