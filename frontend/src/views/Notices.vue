<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">通知公告</h2>
        <p class="zk-page-desc">教务处面向全校发布的通知，学生与教师均可见</p>
      </div>
      <el-button v-if="store.has('notice:add')" type="primary" :icon="Plus" @click="openCreate">
        发布公告
      </el-button>
    </div>

    <div class="zk-card">
      <div class="zk-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索标题"
          clearable
          style="width: 240px"
          @keyup.enter="load"
        />
        <el-button type="primary" plain @click="load">查询</el-button>
      </div>

      <div v-for="n in rows" :key="n.id" class="notice-item">
        <div class="notice-head">
          <span class="notice-title">{{ n.title }}</span>
          <span class="zk-muted">{{ n.publishedAt?.slice(0, 16).replace('T', ' ') }}</span>
        </div>
        <p class="notice-body">{{ n.content }}</p>
        <div class="notice-foot">
          <span class="zk-muted">发布人：{{ n.publisherName || '—' }}</span>
          <span>
            <el-button v-if="store.has('notice:edit')" text type="primary" @click="openEdit(n)">编辑</el-button>
            <el-button v-if="store.has('notice:delete')" text type="danger" @click="remove(n)">删除</el-button>
          </span>
        </div>
      </div>

      <div v-if="!rows.length" class="zk-empty">暂无公告</div>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { noticeApi, type NoticeRow } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const rows = ref<NoticeRow[]>([])
const keyword = ref('')
const dialog = ref(false)
const saving = ref(false)
const editing = ref<NoticeRow | null>(null)
const form = reactive({ title: '', content: '' })

async function load() {
  rows.value = await noticeApi.list(keyword.value || undefined)
}

function openCreate() {
  editing.value = null
  form.title = ''
  form.content = ''
  dialog.value = true
}

function openEdit(n: NoticeRow) {
  editing.value = n
  form.title = n.title
  form.content = n.content ?? ''
  dialog.value = true
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await noticeApi.update(editing.value.id, { ...form })
    } else {
      await noticeApi.publish({ ...form })
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(n: NoticeRow) {
  await ElMessageBox.confirm(`确认删除《${n.title}》？`, '提示', { type: 'warning' })
  await noticeApi.remove(n.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.notice-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f7;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
}

.notice-body {
  margin: 8px 0;
  color: #4a5265;
  font-size: 13.5px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.notice-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
