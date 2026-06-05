<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px">
      <h3 style="margin-top: 0">公告管理</h3>
      <el-button type="primary" @click="showDialog()">发布公告</el-button>
    </div>
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap">
      <el-input v-model="keyword" placeholder="搜索标题 / 内容" clearable style="width: 260px" @keyup.enter="handleSearch" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="handleSearch">
        <el-option label="已发布" :value="1" />
        <el-option label="草稿" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <el-table :data="notices" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="showDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="600px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" inactive-text="草稿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminNoticeList, addNotice, updateNotice, deleteNotice } from '../../api/notice'
import { ElMessage, ElMessageBox } from 'element-plus'

const notices = ref([])
const keyword = ref('')
const statusFilter = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, title: '', content: '', status: 1 })

const loadNotices = async () => {
  const res = await getAdminNoticeList({ page: 1, size: 100, keyword: keyword.value, status: statusFilter.value })
  notices.value = res.data.records
}

const handleSearch = () => {
  loadNotices()
}

const handleReset = () => {
  keyword.value = ''
  statusFilter.value = null
  loadNotices()
}

const showDialog = (row) => {
  if (row) {
    isEdit.value = true
    form.value = { id: row.id, title: row.title, content: row.content, status: row.status }
  } else {
    isEdit.value = false
    form.value = { id: null, title: '', content: '', status: 1 }
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (isEdit.value) {
    await updateNotice(form.value)
    ElMessage.success('更新成功')
  } else {
    await addNotice(form.value)
    ElMessage.success('发布成功')
  }
  dialogVisible.value = false
  loadNotices()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' })
  await deleteNotice(id)
  ElMessage.success('删除成功')
  loadNotices()
}

onMounted(loadNotices)
</script>
