<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../utils/api';

const currentStep = ref(0);
const loading = ref(false);
const installStatus = ref<{ installed: boolean; locked: boolean; message: string } | null>(null);

// 表单数据
const envForm = reactive({
  javaVersion: '',
  portAvailable: false,
  mysqlDriver: false
});

const dbForm = reactive({
  host: 'localhost',
  port: 3306,
  database: 'luomiblog',
  username: '',
  password: ''
});

const siteForm = reactive({
  siteName: 'LuomiBlog',
  siteDescription: '一个基于 Astro + Vue + SpringBoot 的 AI 知识库博客',
  defaultTheme: 'auto',
  defaultLanguage: 'zh',
  timezone: 'Asia/Shanghai'
});

const adminForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: ''
});

// 检查安装状态
const checkInstallStatus = async () => {
  try {
    const response = await api.install.getStatus();
    installStatus.value = response;
    
    if (response.locked) {
      ElMessageBox.alert('系统已安装完成，如需重新安装请删除 install.lock 文件', '提示', {
        confirmButtonText: '前往首页',
        callback: () => {
          window.location.href = '/';
        }
      });
    }
  } catch (error) {
    console.error('检查安装状态失败', error);
  }
};

// 环境检测
const checkEnvironment = async () => {
  loading.value = true;
  try {
    const response = await api.install.checkEnvironment();
    
    const javaCheck = response.checks.find(c => c.name === 'Java 版本');
    const portCheck = response.checks.find(c => c.name === '端口 8080');
    const driverCheck = response.checks.find(c => c.name === 'MySQL 驱动');
    
    envForm.javaVersion = javaCheck?.message || '';
    envForm.portAvailable = portCheck?.passed || false;
    envForm.mysqlDriver = driverCheck?.passed || false;
    
    if (response.allPassed) {
      ElMessage.success('环境检测通过');
      currentStep.value++;
    } else {
      const failedChecks = response.checks.filter(c => !c.passed);
      const messages = failedChecks.map(c => `${c.name}: ${c.suggestion}`).join('\n');
      ElMessageBox.alert(messages, '环境检测未通过', {
        confirmButtonText: '我知道了'
      });
    }
  } catch (error) {
    ElMessage.error('环境检测失败');
  } finally {
    loading.value = false;
  }
};

// 测试数据库连接
const testDatabase = async () => {
  if (!dbForm.host || !dbForm.database || !dbForm.username) {
    ElMessage.warning('请填写完整的数据库配置');
    return;
  }
  
  loading.value = true;
  try {
    const response = await api.install.testDatabase(dbForm);
    if (response.success) {
      ElMessage.success('数据库连接成功');
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '数据库连接失败');
  } finally {
    loading.value = false;
  }
};

// 执行 SQL 脚本
const executeSql = async () => {
  loading.value = true;
  try {
    const response = await api.install.executeSql(dbForm);
    if (response.success) {
      ElMessage.success('数据库初始化成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'SQL 执行失败');
  } finally {
    loading.value = false;
  }
};

// 创建管理员账号
const createAdmin = async () => {
  if (!adminForm.username || !adminForm.password || !adminForm.email) {
    ElMessage.warning('请填写完整的管理员信息');
    return;
  }
  
  if (adminForm.password !== adminForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致');
    return;
  }
  
  if (adminForm.password.length < 8) {
    ElMessage.error('密码长度至少8位');
    return;
  }
  
  loading.value = true;
  try {
    const response = await api.install.createAdmin(adminForm);
    if (response.success) {
      ElMessage.success('管理员账号创建成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '管理员账号创建失败');
  } finally {
    loading.value = false;
  }
};

// 保存站点配置
const saveSiteConfig = async () => {
  if (!siteForm.siteName) {
    ElMessage.warning('请输入网站名称');
    return;
  }
  
  loading.value = true;
  try {
    const response = await api.install.saveSiteConfig(siteForm);
    if (response.success) {
      ElMessage.success('站点配置保存成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '站点配置保存失败');
  } finally {
    loading.value = false;
  }
};

// 完成安装
const completeInstall = async () => {
  loading.value = true;
  try {
    const response = await api.install.complete();
    if (response.success) {
      ElMessage.success('安装完成！');
      setTimeout(() => {
        window.location.href = '/';
      }, 2000);
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '安装完成操作失败');
  } finally {
    loading.value = false;
  }
};

// 上一步
const prevStep = () => {
  currentStep.value--;
};

onMounted(() => {
  checkInstallStatus();
});
</script>

<template>
  <div class="install-wizard">
    <!-- 步骤条 -->
    <el-steps :active="currentStep" finish-status="success" class="steps">
      <el-step title="环境检测" />
      <el-step title="数据库配置" />
      <el-step title="初始化数据" />
      <el-step title="站点配置" />
      <el-step title="创建管理员" />
      <el-step title="完成安装" />
    </el-steps>

    <!-- 步骤 1: 环境检测 -->
    <div v-if="currentStep === 0" class="step-content">
      <el-card class="step-card">
        <template #header>
          <div class="card-header">
            <span>环境检测</span>
          </div>
        </template>
        
        <div class="env-checks">
          <div class="check-item">
            <el-icon :size="20" :class="envForm.javaVersion ? 'success' : 'error'">
              <CircleCheck v-if="envForm.javaVersion" />
              <CircleClose v-else />
            </el-icon>
            <div class="check-info">
              <div class="check-name">Java 版本</div>
              <div class="check-status">{{ envForm.javaVersion || '待检测' }}</div>
            </div>
          </div>
          
          <div class="check-item">
            <el-icon :size="20" :class="envForm.portAvailable ? 'success' : 'error'">
              <CircleCheck v-if="envForm.portAvailable" />
              <CircleClose v-else />
            </el-icon>
            <div class="check-info">
              <div class="check-name">端口 8080</div>
              <div class="check-status">{{ envForm.portAvailable ? '可用' : '被占用或待检测' }}</div>
            </div>
          </div>
          
          <div class="check-item">
            <el-icon :size="20" :class="envForm.mysqlDriver ? 'success' : 'error'">
              <CircleCheck v-if="envForm.mysqlDriver" />
              <CircleClose v-else />
            </el-icon>
            <div class="check-info">
              <div class="check-name">MySQL 驱动</div>
              <div class="check-status">{{ envForm.mysqlDriver ? '已加载' : '待检测' }}</div>
            </div>
          </div>
        </div>

        <div class="step-actions">
          <el-button type="primary" size="large" :loading="loading" @click="checkEnvironment">
            开始检测
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤 2: 数据库配置 -->
    <div v-if="currentStep === 1" class="step-content">
      <el-card class="step-card">
        <template #header>
          <div class="card-header">
            <span>数据库配置</span>
          </div>
        </template>
        
        <el-form :model="dbForm" label-position="top" class="install-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="数据库主机">
                <el-input v-model="dbForm.host" placeholder="localhost" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="数据库端口">
                <el-input-number v-model="dbForm.port" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="数据库名称">
            <el-input v-model="dbForm.database" placeholder="luomiblog" />
          </el-form-item>
          
          <el-form-item label="数据库用户名">
            <el-input v-model="dbForm.username" placeholder="root" />
          </el-form-item>
          
          <el-form-item label="数据库密码">
            <el-input v-model="dbForm.password" type="password" show-password placeholder="请输入数据库密码" />
          </el-form-item>
        </el-form>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button @click="testDatabase" :loading="loading">测试连接</el-button>
          <el-button type="primary" :loading="loading" @click="executeSql">
            下一步
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤 3: 初始化数据 -->
    <div v-if="currentStep === 2" class="step-content">
      <el-card class="step-card">
        <template #header>
          <div class="card-header">
            <span>初始化数据</span>
          </div>
        </template>
        
        <div class="init-info">
          <el-alert
            title="即将执行数据库初始化"
            description="此步骤将执行 schema.sql 和 data.sql，创建必要的表结构和初始数据（角色、权限等）"
            type="info"
            :closable="false"
            show-icon
          />
          
          <div class="init-details">
            <p><strong>将执行的操作：</strong></p>
            <ul>
              <li>创建用户表、文章表、评论表等核心表结构</li>
              <li>初始化角色数据（访客、会员、博主、管理员）</li>
              <li>初始化权限数据</li>
              <li>创建系统配置表</li>
            </ul>
          </div>
        </div>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" :loading="loading" @click="executeSql">
            开始初始化
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤 4: 站点配置 -->
    <div v-if="currentStep === 3" class="step-content">
      <el-card class="step-card">
        <template #header>
          <div class="card-header">
            <span>站点配置</span>
          </div>
        </template>
        
        <el-form :model="siteForm" label-position="top" class="install-form">
          <el-form-item label="网站名称">
            <el-input v-model="siteForm.siteName" placeholder="LuomiBlog" />
          </el-form-item>
          
          <el-form-item label="网站描述">
            <el-input v-model="siteForm.siteDescription" type="textarea" :rows="3" 
              placeholder="请输入网站描述" />
          </el-form-item>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="默认主题">
                <el-select v-model="siteForm.defaultTheme" style="width: 100%">
                  <el-option label="自动（跟随系统）" value="auto" />
                  <el-option label="亮色模式" value="light" />
                  <el-option label="暗色模式" value="dark" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="默认语言">
                <el-select v-model="siteForm.defaultLanguage" style="width: 100%">
                  <el-option label="简体中文" value="zh" />
                  <el-option label="English" value="en" />
                  <el-option label="日本語" value="ja" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="时区">
            <el-select v-model="siteForm.timezone" style="width: 100%">
              <el-option label="Asia/Shanghai (中国标准时间)" value="Asia/Shanghai" />
              <el-option label="Asia/Tokyo (日本标准时间)" value="Asia/Tokyo" />
              <el-option label="UTC (协调世界时)" value="UTC" />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" :loading="loading" @click="saveSiteConfig">
            下一步
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤 5: 创建管理员 -->
    <div v-if="currentStep === 4" class="step-content">
      <el-card class="step-card">
        <template #header>
          <div class="card-header">
            <span>创建管理员账号</span>
          </div>
        </template>
        
        <el-form :model="adminForm" label-position="top" class="install-form">
          <el-form-item label="管理员用户名">
            <el-input v-model="adminForm.username" placeholder="请输入用户名（3-50位字母数字下划线）" />
          </el-form-item>
          
          <el-form-item label="管理员邮箱">
            <el-input v-model="adminForm.email" placeholder="请输入邮箱地址" />
          </el-form-item>
          
          <el-form-item label="显示昵称">
            <el-input v-model="adminForm.nickname" placeholder="可选，默认为用户名" />
          </el-form-item>
          
          <el-form-item label="登录密码">
            <el-input v-model="adminForm.password" type="password" show-password 
              placeholder="请输入密码（至少8位）" />
          </el-form-item>
          
          <el-form-item label="确认密码">
            <el-input v-model="adminForm.confirmPassword" type="password" show-password 
              placeholder="请再次输入密码" />
          </el-form-item>
        </el-form>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" :loading="loading" @click="createAdmin">
            创建管理员
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤 6: 完成安装 -->
    <div v-if="currentStep === 5" class="step-content">
      <el-card class="step-card">
        <template #header>
          <div class="card-header">
            <span>安装完成</span>
          </div>
        </template>
        
        <div class="complete-content">
          <el-result
            icon="success"
            title="安装成功"
            sub-title="恭喜！LuomiBlog 已成功安装完成"
          >
            <template #extra>
              <div class="complete-info">
                <p><strong>网站名称：</strong>{{ siteForm.siteName }}</p>
                <p><strong>管理员账号：</strong>{{ adminForm.username }}</p>
                <p><strong>登录地址：</strong>/login</p>
              </div>
              
              <el-button type="primary" size="large" :loading="loading" @click="completeInstall">
                完成安装并进入网站
              </el-button>
            </template>
          </el-result>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.install-wizard {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.steps {
  margin-bottom: 2rem;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-card {
  min-height: 400px;
}

.card-header {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a2e;
}

.env-checks {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin: 2rem 0;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.check-item .success {
  color: #67c23a;
}

.check-item .error {
  color: #f56c6c;
}

.check-info {
  flex: 1;
}

.check-name {
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 0.25rem;
}

.check-status {
  font-size: 0.875rem;
  color: #666;
}

.install-form {
  max-width: 600px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e5e7eb;
}

.init-info {
  margin: 1rem 0;
}

.init-details {
  margin-top: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.init-details ul {
  margin: 0.5rem 0 0 1.5rem;
  color: #666;
}

.init-details li {
  margin: 0.5rem 0;
}

.complete-content {
  padding: 2rem 0;
}

.complete-info {
  text-align: left;
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 2rem;
}

.complete-info p {
  margin: 0.5rem 0;
  color: #666;
}

@media (max-width: 640px) {
  .install-wizard {
    padding: 1rem;
  }

  .step-actions {
    flex-direction: column;
  }

  .step-actions .el-button {
    width: 100%;
  }
}
</style>
