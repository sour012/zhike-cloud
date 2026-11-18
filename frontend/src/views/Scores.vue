<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">成绩管理</h2>
        <p class="zk-page-desc">
          {{ store.isStudent ? '总评 = 平时分 30% + 考试分 70%，绩点按学校 4.0 制换算' : '选择教学班后录入平时分与考试分，总评与绩点自动计算' }}
        </p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
    </div>

    <!-- 学生视角：我的成绩 + 绩点概览 -->
    <template v-if="store.isStudent">
      <el-row :gutter="14" style="margin-bottom: 14px">
        <el-col :span="8">
          <div class="zk-card zk-stat">
            <span class="zk-stat-label">加权平均绩点</span>
            <span class="zk-stat-value">{{ summary?.gpa ?? '—' }}</span>
            <span class="zk-muted">按学分加权计算</span>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="zk-card zk-stat">
            <span class="zk-stat-label">已修学分</span>
            <span class="zk-stat-value">{{ summary?.credits ?? 0 }}</span>
            <span class="zk-muted">仅统计已出成绩课程</span>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="zk-card zk-stat">
            <span class="zk-stat-label">出分课程</span>
            <span class="zk-stat-value">{{ summary?.courseCount ?? 0 }} 门</span>
            <span class="zk-muted">可下载成绩单打印</span>
          </div>
        </el-col>
      </el-row>

      <div class="zk-card">
        <div class="zk-toolbar">
          <el-select v-model="termId" placeholder="全部学期" clearable style="width: 230px" @change="loadMyScores">
            <el-option v-for="t in terms" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
        <el-table :data="myScores" v-loading="loading" stripe>
          <el-table-column prop="termName" label="学期" width="200" />
          <el-table-column label="课程" min-width="170">
            <template #default="{ row }">
              <div>{{ row.courseName }}</div>
              <div class="zk-cell-sub">{{ row.courseCode }} · {{ row.credit }} 学分</div>
            </template>
          </el-table-column>
          <el-table-column prop="usualScore" label="平时分" width="90" />
          <el-table-column prop="examScore" label="考试分" width="90" />
          <el-table-column label="总评" width="90">
            <template #default="{ row }"><strong>{{ row.total }}</strong></template>
          </el-table-column>
          <el-table-column prop="gpa" label="绩点" width="80" />
        </el-table>
      </div>
    </template>

    <!-- 教师/管理员视角：选教学班 → 名单 → 录入 -->
    <template v-else>
      <div class="zk-card">
        <div class="zk-toolbar">
          <el-select v-model="termId" style="width: 230px" @change="loadOfferings">
            <el-option v-for="t in terms" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-select
            v-model="offeringId"
            placeholder="选择教学班"
            filterable
            style="width: 320px"
            @change="loadRoster"
          >
            <el-option
              v-for="o in offerings"
              :key="o.id"
              :label="`${o.courseCode} ${o.courseName} · ${o.clazzName || '不分班'} · ${o.timeText}`"
              :value="o.id!"
            />
          </el-select>
          <span class="zk-muted">共 {{ roster.length }} 名学生</span>
        </div>

        <el-table :data="roster" v-loading="rosterLoading" stripe>
          <el-table-column prop="studentNo" label="学号" width="150" />
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="clazzName" label="班级" width="130" />
          <el-table-column prop="usualScore" label="平时分" width="100">
            <template #default="{ row }">{{ row.usualScore ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="examScore" label="考试分" width="100">
            <template #default="{ row }">{{ row.examScore ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="总评 / 绩点" width="130">
            <template #default="{ row }">
              <span v-if="row.total != null"><strong>{{ row.total }}</strong> / {{ row.gpa }}</span>
              <span v-else class="zk-muted">未录入</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="store.has('edu:score:add')"
                text
                type="primary"
                @click="openGrade(row)"
              >
                {{ row.total == null ? '录入' : '修改' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!offeringId" class="zk-empty">请选择一个教学班查看学生名单</div>
      </div>
    </template>

    <el-dialog v-model="dialog" title="录入成绩" width="420px">
      <el-form label-width="80px">
        <el-form-item label="学生">
          <span>{{ editing?.studentName }}（{{ editing?.studentNo }}）</span>
        </el-form-item>
        <el-form-item label="平时分">
          <el-input-number v-model="gradeForm.usualScore" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="考试分">
          <el-input-number v-model="gradeForm.examScore" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="预计总评">
          <strong>{{ predicted }}</strong>
          <span class="zk-muted" style="margin-left: 8px">平时 30% + 考试 70%</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  offeringApi,
  scoreApi,
  type Offering,
  type RosterRow,
  type ScoreRow,
  type ScoreSummary,
  type Term,
} from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const terms = ref<Term[]>([])
const termId = ref<number>()
const loading = ref(false)

// 学生
const myScores = ref<ScoreRow[]>([])
const summary = ref<ScoreSummary>()

// 教师/管理员
const offerings = ref<Offering[]>([])
const offeringId = ref<number>()
const roster = ref<RosterRow[]>([])
const rosterLoading = ref(false)

const dialog = ref(false)
const saving = ref(false)
const editing = ref<RosterRow>()
const gradeForm = reactive({ usualScore: 85, examScore: 85 })

const predicted = computed(() =>
  (gradeForm.usualScore * 0.3 + gradeForm.examScore * 0.7).toFixed(1),
)

async function loadMyScores() {
  loading.value = true
  try {
    myScores.value = await scoreApi.list({ termId: termId.value })
  } finally {
    loading.value = false
  }
}

async function loadOfferings() {
  offerings.value = await offeringApi.list({ termId: termId.value })
  offeringId.value = undefined
  roster.value = []
}

async function loadRoster() {
  if (!offeringId.value) return
  rosterLoading.value = true
  try {
    roster.value = await scoreApi.roster(offeringId.value)
  } finally {
    rosterLoading.value = false
  }
}

function openGrade(row: RosterRow) {
  editing.value = row
  gradeForm.usualScore = Number(row.usualScore ?? 85)
  gradeForm.examScore = Number(row.examScore ?? 85)
  dialog.value = true
}

async function save() {
  if (!editing.value) return
  saving.value = true
  try {
    await scoreApi.save({
      enrollmentId: editing.value.enrollmentId,
      usualScore: gradeForm.usualScore,
      examScore: gradeForm.examScore,
    })
    ElMessage.success('已保存')
    dialog.value = false
    loadRoster()
  } finally {
    saving.value = false
  }
}

async function reload() {
  if (store.isStudent) {
    await loadMyScores()
    summary.value = await scoreApi.summary()
  } else {
    await loadRoster()
  }
}

onMounted(async () => {
  terms.value = await offeringApi.terms()
  termId.value = terms.value.find((t) => t.isCurrent === 1)?.id ?? terms.value[0]?.id
  if (store.isStudent) {
    summary.value = await scoreApi.summary()
    await loadMyScores()
  } else {
    await loadOfferings()
  }
})
</script>
