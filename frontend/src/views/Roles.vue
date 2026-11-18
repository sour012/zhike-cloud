<template>
  <div class="zk-page">
    <div class="zk-page-head">
      <div>
        <h2 class="zk-page-title">角色权限</h2>
        <p class="zk-page-desc">勾选菜单即授权对应的权限点；后端 @PreAuthorize 与前端按钮显隐读的是同一份数据</p>
      </div>
      <el-button type="primary" :loading="saving" :disabled="current?.code === 'admin'" @click="save">
        保存授权
      </el-button>
    </div>

    <el-row :gutter="14">
      <el-col :span="7">
        <div class="zk-card">
          <div class="zk-muted" style="margin-bottom: 10px">角色</div>
          <div
            v-for="r in roles"
            :key="r.id"
            class="role-item"
            :class="{ active: current?.id === r.id }"
            @click="pick(r)"
          >
            <div>
              <div class="role-name">{{ r.name }}</div>
              <div class="zk-muted">{{ r.code }}</div>
            </div>
            <el-tag v-if="r.code === 'admin'" size="small" type="danger" effect="plain">固定全部</el-tag>
          </div>
        </div>
      </el-col>

      <el-col :span="17">
        <div class="zk-card">
          <div class="zk-muted" style="margin-bottom: 10px">
            菜单与按钮权限
            <span v-if="current?.code === 'admin'">（超级管理员固定拥有全部权限，不可裁剪）</span>
          </div>
          <el-tree
            ref="treeRef"
            :data="tree"
            show-checkbox
            node-key="id"
            :props="{ label: 'title', children: 'children' }"
            :default-checked-keys="checkedKeys"
            :default-expand-all="true"
            :disabled="current?.code === 'admin'"
          >
            <template #default="{ data }">
              <span>
                {{ data.title }}
                <el-tag v-if="data.perm" size="small" effect="plain" style="margin-left: 6px">
                  {{ data.perm }}
                </el-tag>
                <el-tag v-else-if="data.menuType === 1" size="small" type="info" effect="plain" style="margin-left: 6px">
                  目录
                </el-tag>
              </span>
            </template>
          </el-tree>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, type ElTree } from 'element-plus'
import { roleApi, type MenuRow, type RoleRow } from '@/api'

interface TreeNode extends MenuRow {
  children: TreeNode[]
}

const roles = ref<RoleRow[]>([])
const flatMenus = ref<MenuRow[]>([])
const tree = ref<TreeNode[]>([])
const checkedKeys = ref<number[]>([])
const current = ref<RoleRow>()
const treeRef = ref<InstanceType<typeof ElTree>>()
const saving = ref(false)

function buildTree(rows: MenuRow[]): TreeNode[] {
  const map = new Map<number, TreeNode>()
  rows.forEach((m) => map.set(m.id, { ...m, children: [] }))
  const roots: TreeNode[] = []
  rows.forEach((m) => {
    const node = map.get(m.id)!
    const parent = map.get(m.parentId)
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

function pick(r: RoleRow) {
  current.value = r
  checkedKeys.value = [...r.menuIds]
  // el-tree 的 default-checked-keys 只在首次渲染生效，切换角色时手动重设
  treeRef.value?.setCheckedKeys(r.menuIds, false)
}

async function save() {
  if (!current.value) return
  saving.value = true
  try {
    const checked = (treeRef.value?.getCheckedKeys(false) ?? []) as number[]
    const half = (treeRef.value?.getHalfCheckedKeys() ?? []) as number[]
    await roleApi.assign(current.value.id, [...checked, ...half])
    ElMessage.success('已保存，相关用户重新登录后生效')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  roles.value = await roleApi.list()
  flatMenus.value = await roleApi.menus()
  tree.value = buildTree(flatMenus.value)
  if (roles.value.length) {
    pick(roles.value[0])
  }
})
</script>

<style scoped>
.role-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
}

.role-item:hover {
  background: #f6f8fc;
}

.role-item.active {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.role-name {
  font-weight: 500;
  margin-bottom: 2px;
}
</style>
