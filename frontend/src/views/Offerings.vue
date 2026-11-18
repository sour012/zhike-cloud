<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">{{ store.isStudent ? '选课中心' : '开课管理' }}</h2>
        <p class="zk-page-desc">
          {{ store.isStudent ? '同一时段只能选一门课，名额满或与已选冲突的课程不可选' : '维护本学期教学班：课程、任课教师、上课时间与容量' }}
        </p>
      </div>
      <el-button v-if="store.has('edu:offering:add')" type="primary" :icon="Plus" @click="openCreate">
        新增开课
      </el-button>
    </div>

    <div class="zk-card">
      <div class="zk-toolbar">
        <el-select v-model="termId" style="width: 230px" @change="load">
          <el-option v-for="t in terms" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="课程名称 / 编号"
          clearable
          style="width: 220px"
          @keyup.enter="load"
        />
        <el-button type="primary" plain @click="load">查询</el-button>
        <span class="zk-muted">共 {{ rows.length }} 个教学班</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="课程" min-width="160">
          <template #default="{ row }">
            <div>{{ row.courseName }}</div>
            <div class="zk-cell-sub">{{ row.courseCode }} · {{ row.credit }} 学分 · {{ row.category }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="任课教师" width="110" />
        <el-table-column prop="clazzName" label="面向班级" width="110" />
        <el-table-column prop="timeText" label="上课时间" width="130" />
        <el-table-column prop="location" label="地点" width="120" />
        <el-table-column label="容量" width="170">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, Math.round((row.enrolled * 100) / Math.max(1, row.capacity)))"
              :stroke-width="8"
              :status="row.enrolled >= row.capacity ? 'exception' : undefined"
            />
            <span class="zk-cell-sub">{{ row.enrolled }} / {{ row.capacity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="store.isStudent">
              <el-tag v-if="row.selected" type="success" size="small">已选</el-tag>
              <el-tooltip v-else-if="row.conflict" :content="`与《${row.conflictWith}》时间冲突`">
                <el-button size="small" disabled>时间冲突</el-button>
              </el-tooltip>
              <el-button
                v-else-if="store.has('edu:enroll:select')"
                size="small"
                type="primary"
                :disabled="row.enrolled >= row.capacity || row.status !== 1"
                @click="selectCourse(row)"
              >
                {{ row.enrolled >= row.capacity ? '已满' : '选课' }}
              </el-button>
            </template>
            <template v-else>
              <el-button v-if="store.has('edu:offering:edit')" text type="primary" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-if="store.has('edu:offering:edit')" text type="danger" @click="remove(row)">
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑开课' : '新增开课'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学期" required>
          <el-select v-model="form.termId" style="width: 100%">
            <el-option v-for="t in terms" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" required>
          <el-select v-model="form.courseId" filterable style="width: 100%">
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="`${c.code} ${c.name}（${c.credit} 学分）`"
              :value="c.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任课教师" required>
          <el-select v-model="form.teacherId" filterable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="面向班级">
          <el-select v-model="form.clazzId" clearable style="width: 100%">
            <el-option v-for="c in clazzes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上课时间" required>
          <div style="display: flex; gap: 8px">
            <el-select v-model="form.weekday" style="width: 110px">
              <el-option v-for="d in 7" :key="d" :label="`周${['一', '二', '三', '四', '五', '六', '日'][d - 1]}`" :value="d" />
            </el-select>
            <el-input-number v-model="form.startSection" :min="1" :max="12" controls-position="right" style="width: 100px" />
            <span style="line-height: 32px">到</span>
            <el-input-number v-model="form.endSection" :min="1" :max="12" controls-position="right" style="width: 100px" />
            <span style="line-height: 32px">节</span>
          </div>
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="容量" required>
          <el-input-number v-model="form.capacity" :min="1" :max="300" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="开放选课" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  courseApi,
  offeringApi,
  enrollmentApi,
  type Clazz,
  type Course,
  type Offering,
  type Term,
} from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const rows = ref<Offering[]>([])
const terms = ref<Term[]>([])
const courses = ref<Course[]>([])
const clazzes = ref<Clazz[]>([])
const teachers = ref<{ id: number; realName: string }[]>([])
const termId = ref<number>()
const keyword = ref('')
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const editing = ref<Offering | null>(null)

const form = reactive<Offering>({
  termId: undefined,
  courseId: undefined,
  teacherId: undefined,
  clazzId: undefined,
  weekday: 1,
  startSection: 1,
  endSection: 2,
  location: '',
  capacity: 50,
  status: 1,
})

async function load() {
  loading.value = true
  try {
    rows.value = await offeringApi.list({ termId: termId.value, keyword: keyword.value })
  } finally {
    loading.value = false
  }
}

async function selectCourse(row: Offering) {
  await enrollmentApi.select(row.id!)
  ElMessage.success(`已选上《${row.courseName}》`)
  load()
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    termId: termId.value,
    courseId: undefined,
    teacherId: store.isTeacher ? store.profile?.id : undefined,
    clazzId: undefined,
    weekday: 1,
    startSection: 1,
    endSection: 2,
    location: '',
    capacity: 50,
    status: 1,
  })
  dialog.value = true
}

function openEdit(row: Offering) {
  editing.value = row
  Object.assign(form, {
    termId: row.termId,
    courseId: row.courseId,
    teacherId: row.teacherId,
    clazzId: undefined,
    weekday: row.weekday,
    startSection: row.startSection,
    endSection: row.endSection,
    location: row.location,
    capacity: row.capacity,
    status: row.status,
  })
  dialog.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value?.id) {
      await offeringApi.update(editing.value.id, { ...form })
    } else {
      await offeringApi.create({ ...form })
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Offering) {
  await ElMessageBox.confirm(`删除《${row.courseName}》的这次开课？已有学生选课时不允许删除。`, '提示', {
    type: 'warning',
  })
  await offeringApi.remove(row.id!)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  terms.value = await offeringApi.terms()
  const current = await offeringApi.currentTerm().catch(() => undefined)
  termId.value = current?.id ?? terms.value[0]?.id
  clazzes.value = await offeringApi.clazzes()
  if (!store.isStudent) {
    courses.value = await courseApi.all()
    teachers.value = await offeringApi.teachers()
  }
  await load()
})
</script>
