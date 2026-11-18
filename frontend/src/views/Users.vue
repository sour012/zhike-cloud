<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">用户管理</h2>
        <p class="zk-page-desc">管理员维护账号与角色；角色决定能进哪些菜单、能点哪些按钮</p>
      </div>
      <el-button v-if="store.has('system:user:add')" type="primary" :icon="Plus" @click="openCreate">
        新增用户
      </el-button>
    </div>

    <div class="zk-card">
      <div class="zk-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 姓名"
          clearable
          style="width: 200px"
          @keyup.enter="reload"
        />
        <el-select v-model="query.userType" placeholder="用户类型" clearable style="width: 140px">
          <el-option label="管理员" :value="1" />
          <el-option label="教师" :value="2" />
          <el-option label="学生" :value="3" />
        </el-select>
        <el-button type="primary" plain @click="reload">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.userType === 1 ? 'danger' : row.userType === 2 ? 'warning' : 'success'">
              {{ row.userType === 1 ? '管理员' : row.userType === 2 ? '教师' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dept" label="院系 / 班级" min-width="160" show-overflow-tooltip />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" effect="plain" style="margin-right: 4px">
              {{ roleLabel(r) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="store.has('system:user:edit')" text type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-if="store.has('system:user:delete')" text type="danger" @click="remove(row)">
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

    <el-dialog v-model="dialog" :title="editing ? '编辑用户' : '新增用户'" width="540px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="editing ? '重置密码' : '初始密码'" :required="!editing">
          <el-input v-model="form.password" placeholder="留空表示不修改" show-password />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="用户类型" required>
          <el-radio-group v-model="form.userType">
            <el-radio :value="1">管理员</el-radio>
            <el-radio :value="2">教师</el-radio>
            <el-radio :value="3">学生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCodes" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="院系/班级">
          <el-input v-model="form.dept" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" />
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
import { roleApi, userApi, type RoleRow, type SysUserRow } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const rows = ref<SysUserRow[]>([])
const roles = ref<RoleRow[]>([])
const total = ref(0)
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const editing = ref<SysUserRow | null>(null)

const query = reactive({ keyword: '', userType: undefined as number | undefined, page: 1, size: 10 })

const form = reactive({
  username: '',
  password: '',
  realName: '',
  userType: 3,
  dept: '',
  phone: '',
  email: '',
  status: 1,
  roleCodes: [] as string[],
})

function roleLabel(code: string) {
  return roles.value.find((r) => r.code === code)?.name ?? code
}

async function load() {
  loading.value = true
  try {
    const res = await userApi.page(query)
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
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    userType: 3,
    dept: '',
    phone: '',
    email: '',
    status: 1,
    roleCodes: ['student'],
  })
  dialog.value = true
}

function openEdit(row: SysUserRow) {
  editing.value = row
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName,
    userType: row.userType,
    dept: row.dept ?? '',
    phone: row.phone ?? '',
    email: row.email ?? '',
    status: row.status,
    roleCodes: [...row.roles],
  })
  dialog.value = true
}

async function save() {
  if (!form.username || !form.realName) {
    ElMessage.warning('用户名与姓名不能为空')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, password: form.password || undefined }
    if (editing.value) {
      await userApi.update(editing.value.id, payload)
    } else {
      await userApi.create(payload)
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: SysUserRow) {
  await ElMessageBox.confirm(`确认删除用户「${row.realName}」？`, '提示', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  roles.value = await roleApi.list().catch(() => [])
  await load()
})
</script>
