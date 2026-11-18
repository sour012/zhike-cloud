<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">我的课表</h2>
        <p class="zk-page-desc">已选课程与上课时间，退课后名额会立即释放给其他同学</p>
      </div>
      <div style="display: flex; gap: 10px; align-items: center">
        <el-select v-model="termId" style="width: 230px" @change="load">
          <el-option v-for="t in terms" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-button type="primary" plain @click="router.push('/edu/offerings')">去选课</el-button>
      </div>
    </div>

    <el-row :gutter="14" style="margin-bottom: 14px">
      <el-col :span="8">
        <div class="zk-card zk-stat">
          <span class="zk-stat-label">已选课程</span>
          <span class="zk-stat-value">{{ active.length }} 门</span>
          <span class="zk-muted">本学期上限 8 门</span>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="zk-card zk-stat">
          <span class="zk-stat-label">学分合计</span>
          <span class="zk-stat-value">{{ totalCredit }}</span>
          <span class="zk-muted">仅统计已选课程</span>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="zk-card zk-stat">
          <span class="zk-stat-label">已出成绩</span>
          <span class="zk-stat-value">{{ scored.length }} 门</span>
          <span class="zk-muted">成绩可在「成绩管理」查看</span>
        </div>
      </el-col>
    </el-row>

    <div class="zk-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="课程" min-width="170">
          <template #default="{ row }">
            <div>{{ row.courseName }}</div>
            <div class="zk-cell-sub">{{ row.courseCode }} · {{ row.credit }} 学分 · {{ row.category }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="timeText" label="上课时间" width="130" />
        <el-table-column prop="location" label="地点" width="120" />
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">
            <template v-if="row.total != null">
              <strong>{{ row.total }}</strong>
              <span class="zk-cell-sub"> / {{ row.gpa }} 绩点</span>
            </template>
            <span v-else class="zk-muted">未出成绩</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已选' : '已退' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1 && store.has('edu:enroll:drop')"
              text
              type="danger"
              @click="drop(row)"
            >
              退课
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { enrollmentApi, offeringApi, type MyCourse, type Term } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const router = useRouter()
const rows = ref<MyCourse[]>([])
const terms = ref<Term[]>([])
const termId = ref<number>()
const loading = ref(false)

const active = computed(() => rows.value.filter((r) => r.status === 1))
const scored = computed(() => active.value.filter((r) => r.total != null))
const totalCredit = computed(() =>
  active.value.reduce((sum, r) => sum + Number(r.credit || 0), 0),
)

async function load() {
  loading.value = true
  try {
    rows.value = await enrollmentApi.my({ termId: termId.value, onlyActive: false })
  } finally {
    loading.value = false
  }
}

async function drop(row: MyCourse) {
  await ElMessageBox.confirm(`确认退掉《${row.courseName}》？`, '退课确认', { type: 'warning' })
  await enrollmentApi.drop(row.enrollmentId)
  ElMessage.success('已退课')
  load()
}

onMounted(async () => {
  terms.value = await offeringApi.terms()
  termId.value = terms.value.find((t) => t.isCurrent === 1)?.id ?? terms.value[0]?.id
  await load()
})
</script>
