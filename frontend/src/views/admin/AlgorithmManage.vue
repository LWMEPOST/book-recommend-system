<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px">
      <div>
        <h3 style="margin: 0 0 6px">推荐算法管理</h3>
      </div>
      <el-button type="primary" :loading="refreshing" @click="handleRefresh">刷新推荐缓存</el-button>
    </div>

    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover">
          <div style="font-size: 13px; color: #909399; margin-bottom: 8px">用户规模</div>
          <div style="font-size: 28px; font-weight: 600; color: #303133">{{ summary.totalUserCount }}</div>
          <div style="margin-top: 8px; color: #606266">推荐活跃用户 {{ summary.activeRecommendUserCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover">
          <div style="font-size: 13px; color: #909399; margin-bottom: 8px">图书规模</div>
          <div style="font-size: 28px; font-weight: 600; color: #303133">{{ summary.onlineBookCount }}</div>
          <div style="margin-top: 8px; color: #606266">上架 / 全部 {{ summary.onlineBookCount }} / {{ summary.totalBookCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover">
          <div style="font-size: 13px; color: #909399; margin-bottom: 8px">行为数据</div>
          <div style="font-size: 28px; font-weight: 600; color: #303133">{{ summary.ratingCount }}</div>
          <div style="margin-top: 8px; color: #606266">评分 {{ summary.ratingCount }} / 收藏 {{ summary.favoriteCount }} / 借阅 {{ summary.borrowCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover">
          <div style="font-size: 13px; color: #909399; margin-bottom: 8px">缓存状态</div>
          <div style="font-size: 28px; font-weight: 600" :style="{ color: summary.cacheReady ? '#67c23a' : '#e6a23c' }">
            {{ summary.cacheReady ? '已就绪' : '待生成' }}
          </div>
          <div style="margin-top: 8px; color: #606266">热点 {{ summary.hotCacheCount }} / 个性化 {{ summary.personalCacheCount }} / 分类 {{ summary.categoryCacheCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="24" :lg="14">
        <el-card style="height: 100%">
          <template #header><span style="font-weight: bold">算法参数配置</span></template>
          <el-form label-width="160px">
            <el-form-item v-for="config in configs" :key="config.id" :label="config.description || config.configKey">
              <div style="display: flex; gap: 12px; width: 100%">
                <el-input v-model="config.configValue" style="max-width: 220px" />
                <el-button
                  type="primary"
                  size="small"
                  :loading="savingKey === config.configKey"
                  @click="handleUpdate(config)"
                >
                  保存
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card style="height: 100%">
          <template #header><span style="font-weight: bold">当前概览</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="配置项数量">{{ summary.configCount }}</el-descriptions-item>
            <el-descriptions-item label="最近刷新时间">{{ formatDateTime(summary.lastRefreshTime) }}</el-descriptions-item>
            <el-descriptions-item label="缓存状态">{{ summary.cacheReady ? '已生成' : '待刷新' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="lastRefreshResult" style="margin-bottom: 20px">
      <template #header><span style="font-weight: bold">最近一次刷新结果</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="刷新时间">{{ formatDateTime(lastRefreshResult.refreshTime) }}</el-descriptions-item>
        <el-descriptions-item label="活跃用户">{{ lastRefreshResult.activeUserCount }}</el-descriptions-item>
        <el-descriptions-item label="热点推荐数量">{{ lastRefreshResult.hotBookCount }}</el-descriptions-item>
        <el-descriptions-item label="个性化缓存数量">{{ lastRefreshResult.personalCacheCount }}</el-descriptions-item>
        <el-descriptions-item label="分类缓存数量">{{ lastRefreshResult.categoryCacheCount }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAlgorithmConfig, getAlgorithmSummary, updateAlgorithmConfig, refreshRecommend } from '../../api/algorithm'
import { ElMessage } from 'element-plus'

const createEmptySummary = () => ({
  totalUserCount: 0,
  activeRecommendUserCount: 0,
  totalBookCount: 0,
  onlineBookCount: 0,
  ratingCount: 0,
  favoriteCount: 0,
  borrowCount: 0,
  configCount: 0,
  hotCacheCount: 0,
  personalCacheCount: 0,
  categoryCacheCount: 0,
  cacheReady: false,
  lastRefreshTime: null
})

const configs = ref([])
const summary = ref(createEmptySummary())
const lastRefreshResult = ref(null)
const savingKey = ref('')
const refreshing = ref(false)

const loadConfigs = async () => {
  const res = await getAlgorithmConfig()
  configs.value = res.data
}

const loadSummary = async () => {
  const res = await getAlgorithmSummary()
  summary.value = { ...createEmptySummary(), ...res.data }
}

const loadAll = async () => {
  try {
    await Promise.all([loadConfigs(), loadSummary()])
  } catch (error) {
    console.error('加载推荐算法管理数据失败', error)
  }
}

const handleUpdate = async (config) => {
  savingKey.value = config.configKey
  try {
    const res = await updateAlgorithmConfig(config.configKey, config.configValue)
    lastRefreshResult.value = res.data
    ElMessage.success(res.msg || '配置已更新')
    await loadAll()
  } catch (error) {
    console.error('更新算法配置失败', error)
  } finally {
    savingKey.value = ''
  }
}

const handleRefresh = async () => {
  refreshing.value = true
  try {
    const res = await refreshRecommend()
    lastRefreshResult.value = res.data
    ElMessage.success(res.msg || '推荐缓存已刷新')
    await loadSummary()
  } catch (error) {
    console.error('刷新推荐缓存失败', error)
  } finally {
    refreshing.value = false
  }
}

const formatDateTime = (value) => {
  return value || '未刷新'
}

onMounted(loadAll)
</script>
