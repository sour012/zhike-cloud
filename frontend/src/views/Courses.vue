<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">课程库</h2>
        <p class="zk-page-desc">所有已登记的课程，开课时从这里选取</p>
      </div>
      <el-button v-if="store.has('edu:course:add')" type="primary" :icon="Plus" @click="openCreate">
        新增课程
      </el-button>
    </div>

    <div class="zk-card">
      <div class="zk-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="课程名称 / 编号"
          clearable
          style="width: 220px"
          @keyup.enter="reload"
        />
        <el-select v-model="query.category" placeholder="课程性质" clearable style="width: 150px">
          <el-option label="专业必修" value="专业必修" />
          <el-option label="专业选修" value="专业选修" />
          <el-option label="通识" value="通识" />
        </el-select>
        <el-button type="primary" plain @click="reload">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="code" label="课程编号" width="120" />
        <el-table-column prop="name" label="课程名称" min-width="150" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="hours" label="学时" width="80" />
        <el-table-column prop="category" label="性质" width="110" />
        <el-table-column prop="dept" label="开课单位" width="130" />
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.has('edu:course:edit')" text type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-if="store.has('edu:course:delete')" text type="danger" @click="remove(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 14px; justify-content: flex-end"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="onPage"
      />
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑课程' : '新增课程'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程编号" required>
          <el-input v-model="form.code" placeholder="如 CS403" />
        </el-form-item>
        <el-form-item label="课程名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="学分" required>
          <el-input-number v-model="form.credit" :min="0.5" :max="10" :step="0.5" />
        </el-form-item>
        <el-form-item label="学时">
          <el-input-number v-model="form.hours" :min="0" :max="200" />
        </el-form-item>
        <el-form-item label="课程性质">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="专业必修" value="专业必修" />
            <el-option label="专业选修" value="专业选修" />
            <el-option label="通识" value="通识" />
          </el-select>
        </el-form-item>
        <el-form-item label="开课单位">
          <el-input v-model="form.dept" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { courseApi, type Course } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const rows = ref<Course[]>([])
const total = ref(0)
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const editing = ref<Course | null>(null)

const query = reactive({ keyword: '', category: '', page: 1, size: 10 })
const form = reactive<Course>({ code: '', name: '', credit: 3, hours: 48, category: '专业必修', dept: '', description: '' })

async function load() {
  loading.value = true
  try {
    const res = await courseApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

function onPage(p: number) {
  query.page = p
  load()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { code: '', name: '', credit: 3, hours: 48, category: '专业必修', dept: '', description: '' })
  dialog.value = true
}

function openEdit(row: Course) {
  editing.value = row
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.code || !form.name) {
    ElMessage.warning('课程编号与名称不能为空')
    return
  }
  saving.value = true
  try {
    if (editing.value?.id) {
      await courseApi.update(editing.value.id, { ...form })
      ElMessage.success('已保存')
    } else {
      await courseApi.create({ ...form })
      ElMessage.success('已新增')
    }
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Course) {
  await ElMessageBox.confirm(`确认删除《${row.name}》？已有开课记录的课程不允许删除。`, '提示', {
    type: 'warning',
  })
  await courseApi.remove(row.id!)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
