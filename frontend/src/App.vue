<template>
  <div class="page">
    <el-card class="card">
      <el-form :inline="true" :model="queryState">
        <el-form-item label="模型编码">
          <el-input v-model="queryState.modelCode" placeholder="请输入模型编码" clearable />
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="queryState.username" placeholder="Basic 用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="queryState.password" type="password" placeholder="Basic 密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loadingForm" @click="handleLoadForm">加载表单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="formConfig" class="card">
      <template #header>{{ formConfig.modelName }}</template>
      <el-form label-width="120px" :model="formValues">
        <el-form-item
          v-for="field in formConfig.fields"
          :key="field.fieldName"
          :label="field.label"
          :required="field.required"
        >
          <el-input-number
            v-if="field.fieldType === 'NUMBER'"
            v-model="formValues[field.fieldName]"
            controls-position="right"
          />
          <el-date-picker
            v-else-if="field.fieldType === 'DATE'"
            v-model="formValues[field.fieldName]"
            type="date"
            value-format="YYYY-MM-DD"
          />
          <el-date-picker
            v-else-if="field.fieldType === 'DATETIME'"
            v-model="formValues[field.fieldName]"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-switch v-else-if="field.fieldType === 'BOOLEAN'" v-model="formValues[field.fieldName]" />
          <el-select
            v-else-if="field.fieldType === 'SELECT'"
            v-model="formValues[field.fieldName]"
            clearable
            filterable
          >
            <el-option
              v-for="option in parseOptions(field.optionsJson)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input v-else v-model="formValues[field.fieldName]" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="executing" @click="handleExecute">执行查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" class="card">
      <template #header>查询结果</template>
      <vxe-table border stripe height="420" :data="result.rows">
        <vxe-column
          v-for="column in result.columns"
          :key="column.columnName"
          :field="column.columnName"
          :title="column.label"
          min-width="140"
        />
      </vxe-table>
    </el-card>

    <el-card v-if="result" class="card">
      <template #header>最终 SQL</template>
      <pre class="sql-box">{{ result.finalSql }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { executeModel, loadForm } from './api'

const queryState = reactive({
  modelCode: '',
  username: 'admin',
  password: 'admin'
})
const formConfig = ref(null)
const formValues = reactive({})
const result = ref(null)
const loadingForm = ref(false)
const executing = ref(false)

async function handleLoadForm() {
  if (!queryState.modelCode) {
    ElMessage.warning('请输入模型编码')
    return
  }
  loadingForm.value = true
  try {
    const data = await loadForm(queryState.modelCode, queryState.username, queryState.password)
    formConfig.value = data
    result.value = null
    Object.keys(formValues).forEach((key) => delete formValues[key])
    data.fields.forEach((field) => {
      formValues[field.fieldName] = field.defaultValue || null
    })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loadingForm.value = false
  }
}

async function handleExecute() {
  executing.value = true
  try {
    result.value = await executeModel(queryState.modelCode, formValues, queryState.username, queryState.password)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    executing.value = false
  }
}

function parseOptions(optionsJson) {
  if (!optionsJson) {
    return []
  }
  try {
    return JSON.parse(optionsJson)
  } catch {
    return []
  }
}
</script>
