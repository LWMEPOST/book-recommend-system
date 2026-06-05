<template>
  <div class="book-manage">
    <div class="page-header">
      <div>
        <h2>图书管理</h2>
      </div>
      <div class="page-header__actions">
        <el-button plain @click="openImportDialog">批量导入</el-button>
        <el-button type="primary" @click="showDialog()">新增图书</el-button>
      </div>
    </div>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索书名、作者或 ISBN"
          clearable
          class="toolbar-item keyword"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="categoryId" placeholder="选择分类" clearable class="toolbar-item" @change="handleSearch">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="上架状态" clearable class="toolbar-item" @change="handleSearch">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="books" stripe>
        <el-table-column label="封面" width="92">
          <template #default="{ row }">
            <el-image :src="row.cover || '/default-book.png'" class="table-cover" fit="cover">
              <template #error>
                <div class="table-cover table-cover--fallback">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="180" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="publishDate" label="出版日期" width="120" />
        <el-table-column prop="totalCount" label="馆藏" width="80" />
        <el-table-column prop="availableCount" label="可借" width="80" />
        <el-table-column label="评分" width="120">
          <template #default="{ row }">
            <div class="rating-cell">
              <span>{{ formatRating(row.avgRating) }}</span>
              <small>{{ row.ratingCount || 0 }} 人</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="prev, pager, next"
        class="pagination"
        @current-change="loadBooks"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑图书' : '新增图书'" width="760px">
      <el-form :model="form" label-width="88px">
        <div class="form-grid">
          <el-form-item label="书名">
            <el-input v-model="form.title" maxlength="120" show-word-limit />
          </el-form-item>
          <el-form-item label="作者">
            <el-input v-model="form.author" maxlength="60" />
          </el-form-item>
          <el-form-item label="ISBN">
            <el-input v-model="form.isbn" maxlength="32" />
          </el-form-item>
          <el-form-item label="出版社">
            <el-input v-model="form.publisher" maxlength="80" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" placeholder="请选择分类">
              <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="出版日期">
            <el-date-picker
              v-model="form.publishDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="价格">
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="馆藏数量">
            <el-input-number v-model="form.totalCount" :min="0" style="width: 100%" />
          </el-form-item>
        </div>

        <el-form-item label="封面地址">
          <el-input v-model="form.cover" placeholder="请输入封面地址" clearable />
        </el-form-item>

        <el-form-item label="本地上传">
          <div class="cover-uploader">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeCoverUpload"
              :http-request="handleCoverUpload"
              accept=".jpg,.jpeg,.png,.webp,.gif"
            >
              <el-button :loading="uploading" type="primary" plain>上传封面图片</el-button>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="封面预览">
          <div class="cover-preview">
            <el-image :src="previewCover" class="cover-preview__image" fit="cover">
              <template #error>
                <div class="cover-preview__image cover-preview__image--fallback">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
        </el-form-item>

        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="上架状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="批量导入图书" width="920px">
      <div class="import-layout">
        <div class="import-toolbar">
          <el-button @click="fillImportTemplate">填入模板</el-button>
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleImportFileChange"
            accept=".csv,.tsv,.txt"
          >
            <el-button plain>读取本地文件</el-button>
          </el-upload>
          <el-button type="primary" plain @click="parseImportContent">解析预览</el-button>
        </div>

        <div class="import-columns">
          <div class="import-column">
            <div class="section-title">导入内容</div>
            <el-input
              v-model="importText"
              type="textarea"
              :rows="16"
              placeholder="粘贴导入内容"
            />
          </div>
          <div class="import-column import-column--narrow">
            <div class="section-title">导入字段</div>
            <div class="field-chip-list">
              <span v-for="field in importFieldTips" :key="field" class="field-chip">{{ field }}</span>
            </div>
          </div>
        </div>

        <div class="import-summary" v-if="importPreview.length || importIssues.length || importResult">
          <el-card shadow="never">
            <div class="section-title">解析结果</div>
            <div class="summary-line">
              <span>可导入 {{ importPreview.length }} 条</span>
              <span v-if="importIssues.length">解析问题 {{ importIssues.length }} 条</span>
              <span v-if="importResult">
                导入结果：新增 {{ importResult.createdCount }}，更新 {{ importResult.updatedCount }}，失败 {{ importResult.failedCount }}
              </span>
            </div>

            <el-table v-if="importPreview.length" :data="importPreview" size="small" max-height="260">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="title" label="书名" min-width="160" />
              <el-table-column prop="author" label="作者" width="110" />
              <el-table-column prop="categoryName" label="分类" width="110" />
              <el-table-column prop="isbn" label="ISBN" min-width="130" />
              <el-table-column prop="cover" label="封面地址" min-width="180" show-overflow-tooltip />
            </el-table>

            <div v-if="importIssues.length" class="issue-list">
              <div class="issue-list__title">解析问题</div>
              <div v-for="issue in importIssues.slice(0, 8)" :key="issue" class="issue-item">{{ issue }}</div>
            </div>

            <div v-if="importResult?.failures?.length" class="issue-list">
              <div class="issue-list__title">导入失败</div>
              <div v-for="issue in importResult.failures.slice(0, 8)" :key="issue" class="issue-item">{{ issue }}</div>
            </div>
          </el-card>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="handleBatchImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addBook,
  batchImportBooks,
  deleteBook,
  getAdminBookList,
  getCategoryList,
  updateBook,
  uploadBookCover
} from '../../api/book'

const books = ref([])
const categories = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const categoryId = ref(null)
const statusFilter = ref(null)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const uploading = ref(false)
const importing = ref(false)
const importText = ref('')
const importPreview = ref([])
const importIssues = ref([])
const importRows = ref([])
const importResult = ref(null)

const importFieldTips = [
  '书名 / title',
  '作者 / author',
  'ISBN / isbn',
  '出版社 / publisher',
  '出版日期 / publishDate',
  '分类 / category',
  '分类ID / categoryId',
  '价格 / price',
  '馆藏数量 / totalCount',
  '封面地址 / cover',
  '简介 / description',
  '状态 / status'
]

const createEmptyForm = () => ({
  title: '',
  author: '',
  isbn: '',
  publisher: '',
  publishDate: '',
  categoryId: null,
  price: 0,
  totalCount: 1,
  cover: '',
  description: '',
  status: 1
})

const form = ref(createEmptyForm())

const previewCover = computed(() => {
  const cover = form.value.cover?.trim()
  return cover || '/default-book.png'
})

const categoryNameMap = computed(() => {
  const map = new Map()
  categories.value.forEach((category) => {
    map.set(category.id, category.name)
  })
  return map
})

const categoryAliasMap = computed(() => {
  const map = new Map()
  categories.value.forEach((category) => {
    map.set(normalizeCategoryKey(category.name), category)
  })
  return map
})

const importTemplate = computed(() => {
  const first = categories.value[0]?.name || '文学小说'
  const second = categories.value[1]?.name || '计算机技术'
  return [
    '书名,作者,ISBN,出版社,出版日期,分类,价格,馆藏数量,封面地址,简介,状态',
    `变量的答案,陈景初,9787500000001,远光出版社,2024-03-15,${first},58,12,https://example.com/book-1.jpg,一本围绕都市成长和人物关系展开的中文小说,上架`,
    `推荐系统工程实战,赵明远,9787500000002,代码工坊,2025-01-20,${second},79,18,/api/files/book-covers/demo.png,适合演示推荐算法和工程落地思路的技术图书,1`
  ].join('\n')
})

const headerAliasMap = {
  title: ['title', '书名', '图书名称'],
  author: ['author', '作者'],
  isbn: ['isbn', 'ISBN'],
  publisher: ['publisher', '出版社'],
  publishDate: ['publishdate', '出版日期', '出版时间'],
  category: ['category', '分类', '分类名称'],
  categoryId: ['categoryid', '分类id', '分类ID'],
  price: ['price', '价格', '定价'],
  totalCount: ['totalcount', '馆藏数量', '总数', '库存', '数量'],
  cover: ['cover', 'coverurl', '封面', '封面地址', '图片地址'],
  description: ['description', '简介', '内容简介'],
  status: ['status', '状态']
}

const formatRating = (value) => {
  const numeric = Number(value || 0)
  return numeric > 0 ? numeric.toFixed(1) : '暂无'
}

const loadBooks = async () => {
  const response = await getAdminBookList({
    page: page.value,
    size: 10,
    keyword: keyword.value || undefined,
    categoryId: categoryId.value,
    status: statusFilter.value
  })
  books.value = response.data.records
  total.value = response.data.total
}

const loadCategories = async () => {
  const response = await getCategoryList()
  categories.value = response.data
}

const handleSearch = () => {
  page.value = 1
  loadBooks()
}

const handleReset = () => {
  keyword.value = ''
  categoryId.value = null
  statusFilter.value = null
  handleSearch()
}

const fillForm = (row) => {
  form.value = {
    title: row.title || '',
    author: row.author || '',
    isbn: row.isbn || '',
    publisher: row.publisher || '',
    publishDate: row.publishDate || '',
    categoryId: row.categoryId ?? null,
    price: Number(row.price || 0),
    totalCount: row.totalCount ?? 1,
    cover: row.cover || '',
    description: row.description || '',
    status: row.status ?? 1
  }
}

const showDialog = (row) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    fillForm(row)
  } else {
    isEdit.value = false
    editId.value = null
    form.value = createEmptyForm()
  }
  dialogVisible.value = true
}

const validateForm = () => {
  if (!form.value.title?.trim()) {
    ElMessage.warning('请输入书名')
    return false
  }
  if (!form.value.author?.trim()) {
    ElMessage.warning('请输入作者')
    return false
  }
  if (!form.value.categoryId) {
    ElMessage.warning('请选择分类')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  const payload = {
    ...form.value,
    title: form.value.title.trim(),
    author: form.value.author.trim(),
    isbn: form.value.isbn?.trim() || '',
    publisher: form.value.publisher?.trim() || '',
    cover: form.value.cover?.trim() || '',
    description: form.value.description?.trim() || ''
  }

  if (isEdit.value) {
    await updateBook(editId.value, payload)
    ElMessage.success('图书更新成功')
  } else {
    await addBook(payload)
    ElMessage.success('图书添加成功')
  }

  dialogVisible.value = false
  loadBooks()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除这本图书吗？', '提示', { type: 'warning' })
  await deleteBook(id)
  ElMessage.success('图书删除成功')
  loadBooks()
}

const beforeCoverUpload = (file) => {
  const isImage = file.type.startsWith('image/') || /\.(jpg|jpeg|png|webp|gif)$/i.test(file.name)
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return false
  }

  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleCoverUpload = async ({ file, onSuccess, onError }) => {
  uploading.value = true
  try {
    const response = await uploadBookCover(file)
    form.value.cover = response.data.url
    ElMessage.success(response.msg || '封面上传成功')
    onSuccess?.(response)
  } catch (error) {
    onError?.(error)
  } finally {
    uploading.value = false
  }
}

const openImportDialog = () => {
  importDialogVisible.value = true
  importResult.value = null
}

const fillImportTemplate = () => {
  importText.value = importTemplate.value
  parseImportContent()
}

const handleImportFileChange = async (uploadFile) => {
  try {
    const rawFile = uploadFile.raw
    if (!rawFile) {
      return
    }
    importText.value = await rawFile.text()
    ElMessage.success(`已读取文件：${rawFile.name}`)
    parseImportContent()
  } catch (error) {
    ElMessage.error('读取文件失败，请确认文件为 UTF-8 编码')
  }
}

const normalizeHeader = (value) => String(value || '').replace(/\s+/g, '').toLowerCase()

const normalizeCategoryKey = (value) => String(value || '').replace(/\s+/g, '').toLowerCase()

const mapHeaderField = (header) => {
  const normalized = normalizeHeader(header)
  for (const [field, aliases] of Object.entries(headerAliasMap)) {
    if (aliases.some((alias) => normalizeHeader(alias) === normalized)) {
      return field
    }
  }
  return null
}

const splitDelimitedLine = (line, delimiter) => {
  const cells = []
  let current = ''
  let inQuotes = false

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index]
    if (char === '"') {
      if (inQuotes && line[index + 1] === '"') {
        current += '"'
        index += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (char === delimiter && !inQuotes) {
      cells.push(current)
      current = ''
      continue
    }
    current += char
  }
  cells.push(current)
  return cells.map((cell) => cell.trim())
}

const normalizeDate = (value) => {
  const text = String(value || '').trim()
  if (!text) {
    return undefined
  }
  const normalized = text.replace(/\//g, '-')
  const match = normalized.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (!match) {
    return null
  }
  const [, year, month, day] = match
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
}

const parseNumberField = (value, type) => {
  const text = String(value || '').trim()
  if (!text) {
    return undefined
  }
  const number = type === 'int' ? Number.parseInt(text, 10) : Number.parseFloat(text)
  return Number.isNaN(number) ? null : number
}

const parseStatus = (value) => {
  const text = String(value || '').trim()
  if (!text) {
    return undefined
  }
  if (text === '上架' || text === '1') {
    return 1
  }
  if (text === '下架' || text === '0') {
    return 0
  }
  return null
}

const parseImportContent = () => {
  const text = importText.value.replace(/^\uFEFF/, '').trim()
  const issues = []
  const rows = []

  if (!text) {
    importIssues.value = []
    importPreview.value = []
    importRows.value = []
    ElMessage.warning('请先粘贴导入内容')
    return { rows, issues }
  }

  const lines = text.split(/\r?\n/).filter((line) => line.trim())
  if (!lines.length) {
    importIssues.value = []
    importPreview.value = []
    importRows.value = []
    return { rows, issues }
  }

  const delimiter = lines[0].includes('\t') ? '\t' : ','
  const headers = splitDelimitedLine(lines[0], delimiter).map(mapHeaderField)

  if (!headers.some((field) => field === 'title')) {
    issues.push('表头缺少“书名 / title”列')
  }
  if (!headers.some((field) => field === 'author')) {
    issues.push('表头缺少“作者 / author”列')
  }
  if (!headers.some((field) => field === 'category' || field === 'categoryId')) {
    issues.push('表头缺少“分类 / category”或“分类ID / categoryId”列')
  }

  for (let lineIndex = 1; lineIndex < lines.length; lineIndex += 1) {
    const cells = splitDelimitedLine(lines[lineIndex], delimiter)
    const source = {}

    headers.forEach((field, headerIndex) => {
      if (!field) {
        return
      }
      source[field] = cells[headerIndex] ?? ''
    })

    if (Object.values(source).every((value) => !String(value || '').trim())) {
      continue
    }

    const title = String(source.title || '').trim()
    const author = String(source.author || '').trim()
    const isbn = String(source.isbn || '').trim()
    const publisher = String(source.publisher || '').trim()
    const cover = String(source.cover || '').trim()
    const description = String(source.description || '').trim()
    const publishDate = normalizeDate(source.publishDate)
    const price = parseNumberField(source.price, 'float')
    const totalCount = parseNumberField(source.totalCount, 'int')
    const status = parseStatus(source.status)

    let resolvedCategoryId
    const categoryIdValue = parseNumberField(source.categoryId, 'int')
    if (categoryIdValue !== undefined && categoryIdValue !== null) {
      resolvedCategoryId = categoryIdValue
    } else {
      const categoryName = String(source.category || '').trim()
      if (categoryName) {
        resolvedCategoryId = categoryAliasMap.value.get(normalizeCategoryKey(categoryName))?.id
      }
    }

    const rowErrors = []
    if (!title) {
      rowErrors.push('书名为空')
    }
    if (!author) {
      rowErrors.push('作者为空')
    }
    if (!resolvedCategoryId) {
      rowErrors.push('分类未匹配到系统现有分类')
    }
    if (source.publishDate && publishDate === null) {
      rowErrors.push('出版日期格式错误')
    }
    if (source.price && price === null) {
      rowErrors.push('价格格式错误')
    }
    if (source.totalCount && totalCount === null) {
      rowErrors.push('馆藏数量格式错误')
    }
    if (source.status && status === null) {
      rowErrors.push('状态只能填写上架/下架或 1/0')
    }

    if (rowErrors.length) {
      issues.push(`第 ${lineIndex + 1} 行：${rowErrors.join('；')}`)
      continue
    }

    const item = {
      title,
      author,
      categoryId: resolvedCategoryId,
      categoryName: categoryNameMap.value.get(resolvedCategoryId) || '未分类'
    }

    if (isbn) item.isbn = isbn
    if (publisher) item.publisher = publisher
    if (publishDate) item.publishDate = publishDate
    if (price !== undefined) item.price = price
    if (totalCount !== undefined) item.totalCount = totalCount
    if (cover) item.cover = cover
    if (description) item.description = description
    if (status !== undefined) item.status = status

    rows.push(item)
  }

  importRows.value = rows
  importPreview.value = rows.slice(0, 8)
  importIssues.value = issues
  importResult.value = null

  if (rows.length) {
    ElMessage.success(`已解析 ${rows.length} 条数据`)
  } else if (!issues.length) {
    ElMessage.warning('没有识别到可导入的数据')
  }

  return { rows, issues }
}

const handleBatchImport = async () => {
  const { rows, issues } = parseImportContent()
  if (!rows.length) {
    ElMessage.warning('没有可导入的数据')
    return
  }

  if (issues.length) {
    ElMessage.warning(`有 ${issues.length} 行解析失败，系统将只导入通过解析的记录`)
  }

  importing.value = true
  try {
    const response = await batchImportBooks(rows)
    importResult.value = response.data
    ElMessage.success(
      `${response.msg || '批量导入完成'}：新增 ${response.data.createdCount}，更新 ${response.data.updatedCount}`
    )
    loadBooks()
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadBooks()
  loadCategories()
})
</script>

<style scoped>
.book-manage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.page-header__actions {
  display: flex;
  gap: 12px;
}

.toolbar-card {
  border-radius: 18px;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-item {
  width: 160px;
}

.toolbar-item.keyword {
  width: 280px;
}

.table-cover {
  width: 52px;
  height: 72px;
  border-radius: 10px;
  background: #f3f4f6;
}

.table-cover--fallback,
.cover-preview__image--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.rating-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.rating-cell small {
  color: #94a3b8;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.cover-uploader {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover-preview__image {
  width: 120px;
  height: 168px;
  border-radius: 16px;
  background: #f3f4f6;
  overflow: hidden;
}

.import-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.import-columns {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(260px, 1fr);
  gap: 16px;
}

.import-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.import-column--narrow {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.field-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.field-chip {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
}

.summary-line {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin: 0 0 12px;
  color: #475569;
}

.issue-list {
  margin-top: 14px;
}

.issue-list__title {
  margin-bottom: 8px;
  font-weight: 600;
  color: #b45309;
}

.issue-item {
  margin-bottom: 6px;
  color: #92400e;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-grid,
  .import-columns {
    grid-template-columns: 1fr;
  }

  .cover-preview {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .page-header__actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .page-header__actions .el-button {
    flex: 1;
  }
}
</style>
