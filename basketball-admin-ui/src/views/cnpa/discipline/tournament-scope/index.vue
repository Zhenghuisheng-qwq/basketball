<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="赛事ID">
        <el-input-number v-model="query.tournamentId" :min="1" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item label="管理员ID">
        <el-input-number v-model="query.adminUserId" :min="1" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:tournamentScope:list']">查询</el-button>
        <el-button type="success" icon="Plus" @click="openGrant" v-hasPermi="['basket:tournamentScope:grant']">新增授权</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="管理员" prop="adminUserName" width="120" />
      <el-table-column label="管理员ID" prop="adminUserId" width="90" />
      <el-table-column label="赛事" prop="tournamentName" min-width="160" />
      <el-table-column label="范围" prop="scopeCode" width="100" />
      <el-table-column label="授权人" prop="grantedByName" width="100" />
      <el-table-column label="授权时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.grantedTime) }}</template>
      </el-table-column>
      <el-table-column label="过期时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.expireTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="scope">
          <el-button link type="danger" @click="doRevoke(scope.row.id)" v-hasPermi="['basket:tournamentScope:revoke']">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="grantOpen" title="新增赛事授权" width="480px">
      <el-form label-width="96px">
        <el-form-item label="管理员ID"><el-input-number v-model="form.adminUserId" :min="1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="赛事ID"><el-input-number v-model="form.tournamentId" :min="1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="范围"><el-input v-model="form.scopeCode" placeholder="MANAGE" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantOpen = false">取消</el-button>
        <el-button type="primary" @click="submitGrant">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { grantTournamentScope, listTournamentScope, revokeTournamentScope } from '@/api/basket/tournamentScope'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = reactive<{ tournamentId?: number; adminUserId?: number }>({})
const grantOpen = ref(false)
const form = reactive<any>({ scopeCode: 'MANAGE' })

async function loadList() {
  loading.value = true
  try {
    const res = await listTournamentScope(query)
    rows.value = res.rows || []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function openGrant() {
  Object.assign(form, { adminUserId: query.adminUserId, tournamentId: query.tournamentId, scopeCode: 'MANAGE' })
  grantOpen.value = true
}

async function submitGrant() {
  if (!form.adminUserId || !form.tournamentId) {
    ElMessage.warning('请填写管理员与赛事')
    return
  }
  await grantTournamentScope(form)
  ElMessage.success('授权成功')
  grantOpen.value = false
  loadList()
}

async function doRevoke(id: number) {
  await ElMessageBox.confirm('确认撤销该授权？', '提示')
  await revokeTournamentScope(id)
  ElMessage.success('已撤销')
  loadList()
}

onMounted(loadList)
</script>
