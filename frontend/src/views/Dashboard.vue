<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">工作台</h2>
        <p class="zk-page-desc">
          {{ data?.currentTermName || '—' }} · {{ store.roleLabel }}视图
        </p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-row :gutter="14">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <div class="zk-card zk-stat">
          <span class="zk-stat-label">{{ card.label }}</span>
          <span class="zk-stat-value">{{ card.value }}</span>
          <span class="zk-muted">{{ card.hint }}</span>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="14">
        <div class="zk-card">
          <div class="section-head">
            <span class="section-title">本周排课分布</span>
            <span class="zk-muted">按星期统计当前学期开课门数</span>
          </div>
          <div class="zk-bars">
            <div v-for="w in data?.weekLoad || []" :key="w.weekday" class="zk-bar-col">
              <span class="zk-muted">{{ w.count }}</span>
              <div class="zk-bar" :style="{ height: barHeight(w.count) }"></div>
              <span class="zk-bar-label">{{ w.label }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="10">
        <div class="zk-card">
          <div class="section-head">
            <span class="section-title">选课热度 Top5</span>
            <span class="zk-muted">已选 / 容量</span>
          </div>
          <div v-for="c in data?.hotCourses || []" :key="c.courseName" class="hot-row">
            <div class="hot-main">
              <span class="hot-name">{{ c.courseName }}</span>
              <span class="zk-muted">{{ c.teacherName }}</span>
            </div>
            <el-progress
              :percentage="c.capacity ? Math.round((c.enrolled * 100) / c.capacity) : 0"
              :stroke-width="8"
              :show-text="false"
              style="width: 110px"
            />
            <span class="hot-num">{{ c.enrolled }}/{{ c.capacity }}</span>
          </div>
          <div v-if="!data?.hotCourses?.length" class="zk-empty">暂无开课数据</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="14">
        <div class="zk-card">
          <div class="section-head">
            <span class="section-title">最新公告</span>
          </div>
          <div v-for="n in data?.notices || []" :key="n.id" class="notice-row">
            <div>
              <div class="notice-title">{{ n.title }}</div>
              <div class="notice-content">{{ n.content }}</div>
            </div>
            <span class="zk-muted">{{ n.publishedAt?.slice(0, 10) }}</span>
          </div>
          <div v-if="!data?.notices?.length" class="zk-empty">暂无公告</div>
        </div>
      </el-col>

      <el-col :span="10">
        <div class="zk-card">
          <div class="section-head">
            <span class="section-title">我的学期概况</span>
          </div>
          <template v-if="store.isStudent">
            <div class="mine-row">
              <span class="zk-muted">已选课程</span>
              <strong>{{ data?.myCourseCount ?? 0 }} 门</strong>
            </div>
            <div class="mine-row">
              <span class="zk-muted">本学期学分</span>
              <strong>{{ data?.myCredits ?? 0 }}</strong>
            </div>
            <div class="mine-row">
              <span class="zk-muted">加权绩点</span>
              <strong>{{ data?.myGpa ?? '—' }}</strong>
            </div>
            <el-button text type="primary" @click="router.push('/edu/offerings')">去选课 →</el-button>
          </template>
          <template v-else-if="store.isTeacher">
            <div class="mine-row">
              <span class="zk-muted">当前学期</span>
              <strong>{{ data?.currentTermName || '—' }}</strong>
            </div>
            <div class="mine-row">
              <span class="zk-muted">我的教学班</span>
              <strong>{{ data?.myOfferingCount ?? 0 }} 个</strong>
            </div>
            <div class="mine-row">
              <span class="zk-muted">我班选课人次</span>
              <strong>{{ data?.myStudentCount ?? 0 }} 人次</strong>
            </div>
            <el-button text type="primary" @click="router.push('/edu/scores')">去录成绩 →</el-button>
          </template>
          <template v-else>
            <div class="mine-row">
              <span class="zk-muted">当前学期</span>
              <strong>{{ data?.currentTermName || '—' }}</strong>
            </div>
            <div class="mine-row">
              <span class="zk-muted">平均选课率</span>
              <strong>{{ data?.avgCapacityUsage ?? 0 }}%</strong>
            </div>
            <div class="mine-row">
              <span class="zk-muted">开课总容量使用</span>
              <strong>{{ data?.enrollmentCount ?? 0 }} 人次</strong>
            </div>
          </template>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { dashboardApi, type Dashboard } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const router = useRouter()
const data = ref<Dashboard>()
const loading = ref(false)

const cards = computed(() => [
  { label: '课程总数', value: data.value?.courseCount ?? 0, hint: '课程库中已登记' },
  { label: '本学期开课', value: data.value?.offeringCount ?? 0, hint: '含各教学班' },
  { label: '学生人数', value: data.value?.studentCount ?? 0, hint: '在册学生' },
  { label: '教师人数', value: data.value?.teacherCount ?? 0, hint: '任课教师' },
])

function barHeight(count: number) {
  const max = Math.max(1, ...(data.value?.weekLoad || []).map((w) => w.count))
  return `${Math.max(4, Math.round((count / max) * 96))}px`
}

async function load() {
  loading.value = true
  try {
    data.value = await dashboardApi.load()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
}

.hot-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #eef1f7;
}

.hot-row:last-child {
  border-bottom: none;
}

.hot-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.hot-name {
  font-size: 13.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-num {
  font-size: 12px;
  color: var(--zk-muted);
  width: 48px;
  text-align: right;
}

.notice-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #eef1f7;
}

.notice-row:last-child {
  border-bottom: none;
}

.notice-title {
  font-size: 13.5px;
  font-weight: 500;
  margin-bottom: 4px;
}

.notice-content {
  font-size: 12.5px;
  color: var(--zk-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mine-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #eef1f7;
  font-size: 13.5px;
}
</style>
